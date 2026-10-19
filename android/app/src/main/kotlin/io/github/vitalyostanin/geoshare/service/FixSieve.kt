package io.github.vitalyostanin.geoshare.service

import com.google.firebase.firestore.FirebaseFirestoreException
import io.github.vitalyostanin.geoshare.collection.CircleLimits
import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.github.vitalyostanin.geoshare.collection.Constellation
import io.github.vitalyostanin.geoshare.collection.Counted
import io.github.vitalyostanin.geoshare.collection.FalsePlace
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.FoundBy
import io.github.vitalyostanin.geoshare.collection.Grounds
import io.github.vitalyostanin.geoshare.collection.Judgement
import io.github.vitalyostanin.geoshare.collection.LEFT_OUT_BY_SOURCE
import io.github.vitalyostanin.geoshare.collection.SkySays
import io.github.vitalyostanin.geoshare.collection.Trail
import io.github.vitalyostanin.geoshare.collection.anchorFrom
import io.github.vitalyostanin.geoshare.collection.andThen
import io.github.vitalyostanin.geoshare.collection.choiceFor
import io.github.vitalyostanin.geoshare.collection.fenceMoves
import io.github.vitalyostanin.geoshare.collection.foundBy
import io.github.vitalyostanin.geoshare.collection.judge
import io.github.vitalyostanin.geoshare.collection.knows
import io.github.vitalyostanin.geoshare.collection.otherRadiosAreDue
import io.github.vitalyostanin.geoshare.collection.remembering
import io.github.vitalyostanin.geoshare.collection.said
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.logFailure
import io.github.vitalyostanin.geoshare.map.runCatchingFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

private const val TAG = "geoshare.service"

/**
 * Decides which fixes become positions, writes them, and remembers what the
 * rules need to decide the next one.
 *
 * Kept apart from [SharingService] so that the state the rules read — the
 * anchor, the run of jumps, the trail, the second opinion — lives in one place
 * a change to one rule can be read against, and so that the decision can be
 * driven on the JVM without a running service. The service keeps the life
 * cycle: when to ask, in which mode, and the subscriptions that bring the
 * group's lists in.
 *
 * Everything here runs on the service's main thread, as it did inside the
 * service; only the rules themselves are worked out elsewhere, on a snapshot.
 */
internal class FixSieve(
    /** Every write outside the screens, carried into each linked group. */
    private val writer: EveryGroup,
    /** What the radio itself says, apart from any position that comes out of it. */
    private val radio: RadioWatch,
    /** What the towers and the Wi-Fi say, kept beside the satellites' own answer. */
    private val opinion: NetworkOpinion,
    /** What the device's own sensors say about it being carried. */
    private val carried: BeingCarried,
    /** Whether the member has asked for the impossible positions to be left out. */
    private val jumps: JumpFilterSetting,
    /** Which sources of a position this device takes into account, as asked on the screen. */
    private val sources: SourcesSetting,
    /** How long the provider and the writes have been quiet. */
    private val silence: Silence,
    /** The place the fence is drawn around; moved from here, watched elsewhere. */
    private val fenceCentre: MutableStateFlow<Fix?>,
    /** A line in the log for every fix as it arrives. */
    private val noteFix: (Fix, Fix?, CollectionMode, FixVerdict) -> Unit,
    /** The receiver's own last answer, read from the store. */
    private val satellites: () -> Fix?,
    /** Whether the cable is in, asked at the moment of the write. */
    private val charging: () -> Boolean,
    /** How full the battery is, asked at the moment of the write. */
    private val batteryPercent: () -> Int,
    /** Puts the trail where it survives the process being killed. */
    private val keepTrail: (Trail) -> Unit,
    /** How long the device has been switched on. */
    private val awake: () -> Duration,
) {
    /** The position the rules measure every fix from. */
    var lastFix: Fix? = null
        private set

    /**
     * The fix left out just before this one, kept so that the next one can
     * agree with it, and how many answers in a row the filter has left out.
     *
     * A device being fed a false signal is answered promptly and wrongly, so
     * the count goes up while the device looks perfectly healthy and says
     * nothing.
     */
    private var jumpRun = JumpRun()

    /** When the towers and the Wi-Fi were last asked in place of the satellites. */
    private var askedTheOtherRadiosAt: Instant? = null

    /**
     * What the receiver's own status said the last fix rested on.
     *
     * Kept rather than asked for twice: the rule reads it while the fix is
     * being judged and the log says the numbers afterwards, and the epoch
     * moves on between the two.
     */
    private var restsOn: Map<Constellation, Counted> = emptyMap()

    /**
     * The answers the satellites have given lately, newest last.
     *
     * Every one of them, whether or not it was written out: what the shape of
     * the run gives away is exactly what a pair of answers hides, and the
     * answers a rule has already refused are the ones that draw the shape
     * while the anchor still stands where the device really is.
     */
    private var lately: List<Fix> = emptyList()

    /** The run of answers left out since the last position was written. */
    private val leftOut = LeftOutRun()

    /**
     * Held while a fix is judged, written and remembered; see [inTurn]. The
     * rules are worked out off the main thread and the write waits for the
     * database, and a fix arriving meanwhile from another source must not
     * read what the one before it has not yet written back.
     */
    private val judging = Mutex()

    /** Where this device keeps being, kept across restarts of the process. */
    var trail: Trail = Trail()
        private set

    /**
     * The places this group says a false signal uses.
     *
     * Set from the file first and from the database after, because the fix
     * these places are there to catch is the first one of the session: a device
     * waiting for its subscription has nothing to catch it with.
     */
    var falsePlaces: List<FalsePlace> = emptyList()

    /**
     * What this group counts as going round in circles.
     *
     * The build's own limits until the group says otherwise, and the group
     * says it in its settings rather than in a release: what a transmitter
     * over one neighbourhood draws is not a fact about the application.
     */
    var circleLimits: CircleLimits = CircleLimits()

    /**
     * The last position this device was ever sure of, however long ago.
     *
     * Apart from [lastFix], which the rules measure against and which an old
     * position has no business being: what this one is for is having something
     * true to say again while the satellites are lying and the other radios
     * have nothing. Said with the time it was taken, so that the age on the
     * card is the age of the position rather than the age of the writing.
     */
    private var lastKnown: Fix? = null

    /**
     * What the last write actually put in the document.
     *
     * Kept beside the fix because a write that passes the filter by has to be
     * told apart from a repeat of itself: the same place with the cable in the
     * same state is the document the database already holds, and the quota is
     * counted in writes rather than in changes.
     */
    private var lastWritten: Written? = null

    /**
     * Takes up what the files kept from the process before: the trail, the
     * anchor it gives, and the group's false places as last heard.
     */
    fun restore(remembered: Trail, places: List<FalsePlace>) {
        trail = remembered
        lastKnown = trail.fixes.lastOrNull()
        lastFix = anchorFrom(trail, Instant.now())
        falsePlaces = places
    }

    /**
     * Writes a fix and remembers it, when it is worth writing.
     *
     * The one place that decides both, because deciding them in three places
     * is three places to keep in step. News — the cable going in, a mode
     * beginning — passes the filter by, since the point of it is to be told
     * now rather than at the next threshold.
     */
    suspend fun writeIfWorth(
        fix: Fix,
        mode: CollectionMode,
        always: Boolean = false,
        // An answer that did not come from the satellites at all, and so has
        // no business being measured against the rules written for them: the
        // towers answer in kilometres, and the width rule would throw away the
        // one source the spoofer cannot reach.
        trusted: Boolean = false,
    ): Boolean {
        // Read once for both halves below: a switch flipped in between would
        // count a fix as a jump and then write it out.
        val jumpsOn = jumps.enabled
        // Read inside the turn and kept for the rest, for the same reason.
        var used = false
        val (judged, wrote) =
            inTurn(
                judging,
                judge = {
                    val judged = whatToMakeOf(fix, trusted)

                    // Every fix that arrives is written down, whether or not it
                    // is written out: what the provider answers where it is
                    // being jammed is the thing to be read afterwards, and a
                    // filtered log would answer questions about the filter
                    // instead.
                    noteFix(
                        fix,
                        lastFix,
                        mode,
                        FixVerdict(judged.jump, judged.agreed, judged.home, trusted),
                    )

                    carried.noticeAnAnswerThatWillNotFollow(fix, judged.whence, Instant.now())

                    used = sources.uses(judged.whence)

                    jumpRun = jumpRun.after(fix, judged.leftOutAsJump(jumpsOn), used)

                    judged
                },
                worthWriting = { outcomeOf(it, jumpsOn, used) == Outcome.GoesOn },
                write = { writeAndRemember(fix, mode, it, always) },
            )

        return when (outcomeOf(judged, jumpsOn, used)) {
            Outcome.LeftOutAsJump -> {
                tellThemAnswersAreBeingLeftOut(judged.jump?.name.orEmpty())

                askTheOtherRadios(mode)

                false
            }

            // Left out the same way a jump is, and for a reason of the same
            // kind: the member has said this is not a source to take into
            // account. So it moves no marker, becomes no last position and
            // reaches nobody — while staying in the log, where the question of
            // what the provider was saying is answered.
            Outcome.LeftOutBySource -> {
                AppLog.i(
                    TAG,
                    "left out a position found by ${judged.whence.said}: " +
                        "this device was told not to take that source into account",
                )

                // The switch as well as the fact, because "a source was
                // switched off" answers none of what a member opens the card
                // for.
                tellThemAnswersAreBeingLeftOut(
                    "$LEFT_OUT_BY_SOURCE:${choiceFor(judged.whence)?.name.orEmpty()}",
                )

                false
            }

            Outcome.GoesOn -> wrote
        }
    }

    /**
     * Says the last position again, exactly as it was.
     *
     * Past the filter and past the check for a repeat, both of which exist to
     * keep the same document from being written twice — which is precisely
     * what is wanted here. The device is standing where it was and asking the
     * provider for nothing; the news is that it is still there to say so.
     */
    suspend fun sayAgain(mode: CollectionMode): Boolean = judging.withLock { sayAgainInTurn(mode) }

    /**
     * What the filter was looking at when a jump it caught needs describing,
     * gathered from the fields above at the moment [caughtBy] is asked for.
     */
    fun leftOutEvidence(): LeftOutEvidence = LeftOutEvidence(
        radio = radio,
        restsOn = restsOn,
        lately = lately,
        circleLimits = circleLimits,
        falsePlaces = falsePlaces,
        networkSaid = opinion.said,
        awakeFor = awake(),
    )

    /**
     * What is made of a fix before it is written or left out: the sieve's
     * own memory brought up to date with it, then read once into the
     * [Grounds] the decision is taken on.
     */
    private suspend fun whatToMakeOf(fix: Fix, trusted: Boolean): Judgement {
        if (jumps.takeForgetRequest()) {
            forgetThePlace()
        }

        opinion.remember(fix)
        rememberTheShapeOfIt(fix, trusted)

        // Read once, before anything is decided: the receiver goes on
        // reporting while the rules run, and an epoch read afterwards would
        // be a different epoch from the one the fix came out of.
        restsOn = if (trusted) emptyMap() else radio.restingOn()

        val grounds =
            Grounds(
                lastFix = lastFix,
                candidate = jumpRun.candidate,
                satellites = satellites(),
                networkSays = opinion.said,
                now = Instant.now(),
                lately = lately,
                falsePlaces = falsePlaces,
                circleLimits = circleLimits,
                restsOn = restsOn,
                skySays = if (trusted) SkySays() else radio.skySays(),
                standingSince = carried.standingSince,
                putDown = carried.putDown,
                carrying = carried.carrying(),
                trail = trail,
            )

        // The rules are a pure function of the fix and of a snapshot nothing
        // else holds, so they are worked out off the main thread.
        return withContext(Dispatchers.Default) { judge(fix, trusted, grounds) }
    }

    /**
     * Writes a fix that was not left out, when it passes the filter, and
     * remembers it as the last position once a group took it.
     */
    private suspend fun writeAndRemember(
        fix: Fix,
        mode: CollectionMode,
        judged: Judgement,
        always: Boolean,
    ): Boolean {
        if (!passesTheFilter(always, lastFix, fix, mode)) {
            return false
        }

        // Read once and carried into the write: asking twice would let the
        // cable come out between the two answers, and then the document would
        // disagree with what was decided here.
        val written = Written(fix, charging(), judged.settling)

        // News passes the filter by; a repeat of itself is not news.
        if (written == lastWritten) {
            return false
        }

        if (!writeQuietly(written, mode, judged.whence)) {
            return false
        }

        lastFix = fix
        lastWritten = written
        silence.wroteAt = Instant.now()
        leftOut.forget()
        rememberThePlace(fix)
        carried.anchorWhereItLies(fix, Instant.now())

        if (fenceMoves(fenceCentre.value, fix)) {
            fenceCentre.value = fix
        }

        return true
    }

    private suspend fun sayAgainInTurn(mode: CollectionMode): Boolean {
        val written = sayAgainOf(lastFix, lastKnown, lastWritten, charging()) ?: return false
        val known = written.fix

        // Not weighed against what the member shares: this is the position
        // they have already shared, said again so that the marker does not go
        // grey while the device sits quietly at home.
        if (!writeQuietly(written, mode, foundBy(known, satellites()))) {
            return false
        }

        lastWritten = written
        silence.wroteAt = Instant.now()

        return true
    }

    /**
     * Tells the group that answers are arriving and being thrown away.
     *
     * The count and the reason go into the device's own position document, so
     * that a reader sees a device being lied to rather than a device that has
     * gone quiet. Nothing else in the document is touched: the coordinates
     * stay as old as they are.
     *
     * Said at most once every [LEFT_OUT_TOLD_EVERY] however fast the answers
     * come. Under a transmitter an answer arrives every seven seconds, and a
     * write for each of them would spend the day's free allowance of the
     * database in an afternoon — while saying nothing a minute's worth of
     * counting does not say just as well.
     *
     * A failure is nothing happening: a device whose answers were being thrown
     * out before it ever wrote a position has no document to add this to, and
     * that is the one case the write fails in.
     */
    private suspend fun tellThemAnswersAreBeingLeftOut(reason: String) {
        val news = leftOut.note(reason, Instant.now()) ?: return

        runCatchingFailure {
            writer.noteLeftOut(news.count, news.reason, news.since)
        }.onFailure { error ->
            val notThereYet =
                (error as? FirebaseFirestoreException)?.code ==
                    FirebaseFirestoreException.Code.NOT_FOUND
            if (notThereYet) {
                AppLog.i(TAG, "nowhere to say answers are being left out yet")
            } else {
                AppLog.w(TAG, "could not say answers are being left out", error)
            }
        }
    }

    /**
     * Keeps an answer for the rule that reads the shape of the run.
     *
     * Only what the satellites answered goes in. An answer the towers gave,
     * and one an assistance service worked out from the Wi-Fi around the
     * device, are answers from outside the transmitter's reach: mixing them
     * into the run would break the very shape the run is kept for.
     */
    private fun rememberTheShapeOfIt(fix: Fix, trusted: Boolean) {
        if (belongsInTheShape(fix, trusted)) {
            lately = lately.andThen(fix)
        }
    }

    /**
     * Puts the satellites aside and asks the towers and the Wi-Fi instead.
     *
     * Called from the one place a spoofed device reaches: the branch where an
     * answer has just been left out. Three left out in a row is the satellites
     * having stopped being a source of positions, however promptly they keep
     * answering, and the other radios are the only ones the spoofer cannot
     * reach. What comes back is written past the filter — the rules it applies
     * were written for a receiver that answers in metres, and would throw away
     * a true answer a kilometre wide.
     *
     * It also puts the anchor back. That matters as much as the write itself:
     * with a true position to measure from, the road the spoofer keeps drawing
     * is seventy kilometres away and is caught by the speed rule again.
     */
    private suspend fun askTheOtherRadios(mode: CollectionMode) {
        if (!otherRadiosAreDue(jumpRun.inARow, askedTheOtherRadiosAt, Instant.now())) {
            return
        }

        // Counted from the asking, so that an answer that never comes does not
        // turn into a scan on every fix.
        askedTheOtherRadiosAt = Instant.now()

        AppLog.w(
            TAG,
            "${jumpRun.inARow} answers in a row left out; asking the towers and the Wi-Fi",
        )

        val asked = opinion.answerNow()
        val verdict =
            verdictOnTheOtherRadios(
                asked,
                lastFix,
                falsePlaces,
                carried.putDown,
                carried.carrying(),
                Instant.now(),
                awake(),
            )

        if (verdict == OtherRadiosVerdict.NothingToPutInItsPlace) {
            AppLog.w(TAG, "the other radios had nothing to put in its place: ${saying(asked)}")

            return
        }

        val fix = asked ?: return

        // Kept whatever happens next: an answer that is not worth writing is
        // still worth having as the second opinion, which is what the rules
        // about contradiction and about confirming a width are asked against.
        opinion.said = fix

        // The towers answer about a district and the Wi-Fi about a flat, and
        // whichever answers first is the one that arrives. An answer that
        // merely repeats a place already written, only wider, would move the
        // marker half a kilometre and widen the circle around it for nothing.
        if (verdict == OtherRadiosVerdict.RepeatsWhatIsWritten) {
            AppLog.i(TAG, "the other radios repeated what is written, only wider: ${saying(fix)}")

            return
        }

        // The one rule of the filter that is asked of this answer too, and the
        // reason is what a field measurement showed: a device lying at home was
        // offered a place four kilometres away, and the answer that came back
        // from the other radios stood within two metres of the false one just
        // rejected. Everything else the filter asks stays waived, as above.
        if (verdict == OtherRadiosVerdict.WalkedAway) {
            AppLog.w(
                TAG,
                "the other radios walked away from a device nobody touched too: ${saying(fix)}",
            )

            return
        }

        AppLog.i(TAG, "standing on what the other radios say: ${saying(fix)}")

        jumpRun = JumpRun()
        writeIfWorth(fix, mode, always = true, trusted = true)
    }

    /**
     * Adds a written position to what the device remembers about itself.
     *
     * Written positions only. An answer left out never becomes part of what
     * counts as familiar, which is what stops an hour of a false signal from
     * teaching the device that a false place is where it lives.
     */
    private fun rememberThePlace(fix: Fix) {
        val before = trail.knows(fix)

        lastKnown = fix

        trail = trail.remembering(fix)
        keepTrail(trail)

        if (!before && trail.knows(fix)) {
            AppLog.i(TAG, "this is a place the device now counts as its own")
        }
    }

    /**
     * Drops what the device believes about where it is.
     *
     * Asked for from the settings screen, for the case the filter cannot get
     * out of on its own: a last position far enough from the truth that
     * nothing plausible reaches it, and a member looking at a marker that has
     * not moved in an hour.
     */
    private fun forgetThePlace() {
        lastFix = null
        lastWritten = null
        jumpRun = JumpRun()

        AppLog.i(TAG, "forgetting the last place, as asked")
    }

    /**
     * A write that failed is not a reason to stop collecting: the network comes
     * back, and the next fix carries the same news.
     */
    private suspend fun writeQuietly(
        written: Written,
        mode: CollectionMode,
        // How the device arrived at this, worked out by the caller: on these
        // devices the fused answer being written carries no note of its own,
        // and what says whether it is the satellites' measurement under
        // another name is the receiver's own last answer, read from the store.
        whence: FoundBy,
    ): Boolean = runCatchingFailure {
        val into =
            writer.write(
                written.fix,
                mode,
                batteryPercent(),
                written.charging,
                whence,
                written.settling,
            )
        AppLog.i(
            TAG,
            // The groups are named: one that refused the write has just said
            // so, and a bare "wrote" after it would read as that one too.
            "wrote a position in ${into.joinToString()} taken at ${written.fix.takenAt}, " +
                "found by ${whence.said}" +
                // Said in the line about the write rather than left to the
                // document: what a device wrote is the one thing a log of a
                // boot cannot be read back from, and the first minutes after
                // a boot are exactly when somebody is reading it.
                if (written.settling) ", with the receiver still settling" else "",
        )

        true
    }.getOrElse { error ->
        logFailure(TAG, "could not write the position", error)

        false
    }
}
