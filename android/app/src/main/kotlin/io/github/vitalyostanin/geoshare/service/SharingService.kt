package io.github.vitalyostanin.geoshare.service

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.LocationManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import io.github.vitalyostanin.geoshare.collection.Accuracy
import io.github.vitalyostanin.geoshare.collection.Answered
import io.github.vitalyostanin.geoshare.collection.Asked
import io.github.vitalyostanin.geoshare.collection.CircleLimits
import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.github.vitalyostanin.geoshare.collection.CollectionState
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
import io.github.vitalyostanin.geoshare.collection.answeredBy
import io.github.vitalyostanin.geoshare.collection.choiceFor
import io.github.vitalyostanin.geoshare.collection.chooseMode
import io.github.vitalyostanin.geoshare.collection.driftedWhilePutDown
import io.github.vitalyostanin.geoshare.collection.fenceMoves
import io.github.vitalyostanin.geoshare.collection.foundBy
import io.github.vitalyostanin.geoshare.collection.judge
import io.github.vitalyostanin.geoshare.collection.knows
import io.github.vitalyostanin.geoshare.collection.networkAnswerStands
import io.github.vitalyostanin.geoshare.collection.noteOf
import io.github.vitalyostanin.geoshare.collection.orbitReference
import io.github.vitalyostanin.geoshare.collection.otherRadiosAreDue
import io.github.vitalyostanin.geoshare.collection.remembering
import io.github.vitalyostanin.geoshare.collection.rescueAccuracy
import io.github.vitalyostanin.geoshare.collection.rescueAfter
import io.github.vitalyostanin.geoshare.collection.said
import io.github.vitalyostanin.geoshare.collection.sameAnswer
import io.github.vitalyostanin.geoshare.collection.saysMoreThan
import io.github.vitalyostanin.geoshare.collection.shouldWrite
import io.github.vitalyostanin.geoshare.collection.whomToAsk
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.logFailure
import io.github.vitalyostanin.geoshare.map.stands
import io.github.vitalyostanin.geoshare.project.Connections
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

private const val TAG = "geoshare.service"

/**
 * A position as it goes into the document: where the device was, and whether
 * the cable was in it at the moment that was decided.
 *
 * The pair rather than the fix alone, because the same place written with the
 * cable newly in is a different document, while the same place written with
 * nothing changed is not.
 */
private data class Written(
    val fix: Fix,
    val charging: Boolean,
    /**
     * Whether the receiver had not settled when the fix was taken.
     *
     * Carried beside the fix rather than worked out again at the write: the
     * count of satellites it rests on belongs to the epoch the fix came out
     * of, and by the time the document is put together the receiver has moved
     * on. It is part of what makes two writes the same write, so a device whose
     * receiver settles while it stands still says so once.
     */
    val settling: Boolean = false,
)

/**
 * The part of the application that keeps running: it holds the position up to
 * date while nothing of ours is on screen.
 *
 * A foreground service rather than periodic work, because the point is to
 * answer "where are you" within seconds of being asked, and because the
 * platform stops handing out locations to anything else for long.
 */
class SharingService : LifecycleService() {
    /**
     * Whether [onCreate] actually wired anything up.
     *
     * Read before every later touch of Firebase, rather than [Connections]
     * itself: what matters here is not whether a project has since been
     * connected, but whether this instance is the one that registered the
     * listener [onDestroy] is about to remove.
     */
    private var usable = false

    private var collection: Job? = null

    /**
     * How long the service waits for a session before admitting it has none.
     *
     * A service started at boot can be quicker than the restoring of the
     * signed-in account, and refusing to work because of a second's difference
     * would mean a device that shares nothing until somebody opens the
     * application.
     */
    private var sessionWait: Job? = null

    /** The groups the device is signed in to, each with what writes into it and listens to it. */
    private val groups by lazy {
        GroupLinks(this, lifecycleScope, sirens, jumps) { before, after ->
            linksChanged(before, after)
        }
    }

    private val links: StateFlow<List<GroupLink>> get() = groups.links

    /** Every write outside the screens, carried into each linked group. */
    private val writer = EveryGroup { links.value }

    /** How the device is, apart from where it is, said into every group. */
    private val status by lazy {
        DeviceStatus(this, lifecycleScope, writer, { links.value.isNotEmpty() }, { lastMode })
    }

    /** The alarm stream at full volume while a call to this device sounds. */
    private val sirens by lazy { SirenAnswer(this, lifecycleScope) }

    /** What the satellites themselves are saying, apart from the written fix. */
    private val skyNotes by lazy { SkyNotes(this, { radio }, { lastFix }, { silence.heardAt }) }

    /** A line in the log for every fix as it arrives. */
    private val fixLog by lazy { FixLog(radio, skyNotes, { jumps.enabled }, ::leftOutEvidence) }

    private var lastFix: Fix? = null

    /**
     * The fix left out just before this one, kept so that the next one can
     * agree with it.
     */
    private var candidate: Fix? = null

    /**
     * How many answers in a row the filter has left out.
     *
     * A device being fed a false signal is answered promptly and wrongly, so
     * this counts up while the device looks perfectly healthy and says nothing.
     */
    private var jumpsInARow: Int = 0

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

    /** What the towers and the Wi-Fi say, kept beside the satellites' own answer. */
    private val opinion = NetworkOpinion(this)

    /**
     * The answers the satellites have given lately, newest last.
     *
     * Every one of them, whether or not it was written out: what the shape of
     * the run gives away is exactly what a pair of answers hides, and the
     * answers a rule has already refused are the ones that draw the shape
     * while the anchor still stands where the device really is.
     */
    private var lately: List<Fix> = emptyList()

    /**
     * What the device's own sensors say about it being carried.
     *
     * Whether there are sensors to ask is read once: sensors do not come and
     * go, and the permission is asked for before the service starts
     * collecting.
     */
    private val carried by lazy { BeingCarried(sensorsNoticeMovement(this)) }

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
    private val trailStore by lazy { TrailStore(this) }
    private var trail: Trail = Trail()

    /**
     * The places this group says a false signal uses, and the copy of them
     * that survives the process being killed.
     *
     * Read from the file first and from the database after, because the fix
     * these places are there to catch is the first one of the session: a device
     * waiting for its subscription has nothing to catch it with.
     */
    private val falsePlaceStore by lazy { FalsePlaceStore(this) }
    private var falsePlaces: List<FalsePlace> = emptyList()

    /**
     * What this group counts as going round in circles.
     *
     * The build's own limits until the group says otherwise, and the group
     * says it in its settings rather than in a release: what a transmitter
     * over one neighbourhood draws is not a fact about the application.
     */
    private var circleLimits: CircleLimits = CircleLimits()

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

    /** Whether the member has asked for the impossible positions to be left out. */
    private val jumps by lazy { JumpFilterSetting(this) }

    /** Which sources of a position this device takes into account, as asked on the screen. */
    private val sources by lazy { SourcesSetting(this) }

    private val playServices by lazy {
        playServicesPresent(this).also { present ->
            if (!present) {
                AppLog.w(TAG, "no Play Services on this device; the platform is asked instead")
            }
        }
    }

    private val providers by lazy { presentProviders(this) }

    /**
     * What the radio itself says, apart from any position that comes out of it.
     *
     * Watched for the whole life of the service rather than around each fix:
     * the verdict is a comparison of the last ten epochs against the fifty
     * before them, and a subscription taken out for the moment of a fix would
     * have nothing to compare against.
     */
    private val radio by lazy {
        RadioWatch(this) { orbitReference(opinion.said, lastFix, Instant.now()) }
    }
    private var lastMode: CollectionMode = CollectionMode.Standby

    /**
     * What the last write actually put in the document.
     *
     * Kept beside the fix because a write that passes the filter by has to be
     * told apart from a repeat of itself: the same place with the cable in the
     * same state is the document the database already holds, and the quota is
     * counted in writes rather than in changes.
     */
    private var lastWritten: Written? = null

    /** How long the provider and the writes have been quiet. */
    private val silence = Silence()

    /**
     * The place the fence is drawn around: the last position written that was
     * far enough from the previous one to be somewhere else.
     *
     * A flow rather than a field because the fence is watched by the platform
     * and has to be moved from here; null until this service has written
     * anything, which is also the state it comes back to after a restart —
     * until the first position is written there is nothing to stand still in.
     */
    private val fenceCentre = MutableStateFlow<Fix?>(null)

    /** The mode collected in, and again whenever it changes. */
    private val modeReview by lazy {
        ModeReview(this, lifecycleScope, links, fenceCentre) { untils, moving, standing, ownMap ->
            carried.hear(moving, Instant.now())
            currentMode(untils, moving.going, standing, ownMap)
        }
    }

    override fun onCreate() {
        super.onCreate()

        // The platform can restart a foreground service on its own after a
        // process death, past every gate `startConditions` puts in front of
        // `start`. A device that has never been connected to a group has
        // nothing to write into, and nothing to wait for either.
        if (!Connections.connected) {
            AppLog.w(TAG, "not connected to any group yet; nothing to collect for")
            stopSelf()

            return
        }

        usable = true
        lifecycleScope.launch { groups.follow() }
        radio.start()

        trail = trailStore.read()
        lastKnown = trail.fixes.lastOrNull()
        lastFix = anchorFrom(trail, Instant.now())
        falsePlaces = falsePlaceStore.read()

        lifecycleScope.launch { keepTheFalsePlaces() }
        lifecycleScope.launch { noticeBeingCarried() }
        lifecycleScope.launch { keepWhatCountsAsCircling() }
        lifecycleScope.launch { keepTheOrbits(this@SharingService) { radio.orbits = it } }

        AppLog.i(
            TAG,
            "the device remembers ${trail.fixes.size} places it has been" +
                if (lastFix == null) "" else ", and measures from the last of them",
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // [onCreate] already stopped this instance; nothing below may run
        // without the setup it skipped.
        if (!usable) {
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_STOP) {
            AppLog.i(TAG, "stopping on request")
            // The notification and the switch on the settings screen are the
            // same statement, and stopping through one of them has to turn the
            // other off: otherwise the switch goes on saying we are sharing,
            // and the next time the application is opened — or the device is
            // restarted — the service starts itself again.
            SharingSetting(this).enabled = false
            stopSelf()

            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_FOUND) {
            endTheSearchEverywhere()
        }

        if (intent?.action == ACTION_SIREN_OFF) {
            quietenTheSirenEverywhere()
        }

        return if (enterForeground()) {
            AppLog.i(TAG, "running")
            startCollecting()
            // The service is restarted without its intent: there is nothing in
            // it that the service needs beyond the stop action.
            START_STICKY
        } else {
            stopSelf()

            START_NOT_STICKY
        }
    }

    /** The person holding this device says it has been found. */
    private fun endTheSearchEverywhere() {
        AppLog.i(TAG, "this device says it has been found")

        // Not a reason to stop sharing: only the search on this device is
        // over, and the service goes on exactly as it was. The
        // notification comes down on its own, once [keepSearchNotification]
        // hears the document is gone. Ended in every group: a device found
        // is found for all of them, and a group not looking for it refuses
        // the deletion, which costs a line in the log.
        for (link in links.value) {
            lifecycleScope.launch {
                try {
                    endSearch(link.phoneId, link.connection)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (refusal: Exception) {
                    logFailure(
                        TAG,
                        "could not end the search in $link from its own notification",
                        refusal,
                    )
                }
            }
        }
    }

    /** Whoever holds this device stops its siren before its time is up. */
    private fun quietenTheSirenEverywhere() {
        AppLog.i(TAG, "the siren is stopped on the device itself")
        sirens.quieten()

        // Written so that the caller's screen reads "stopped" rather than
        // counting down twenty seconds that are no longer sounding — in
        // every group, since the sound is one and stops for all of them; a
        // group that called no siren refuses the write, which costs a line
        // in the log.
        for (link in links.value) {
            lifecycleScope.launch {
                try {
                    stopSiren(link.phoneId, link.memberId, link.connection)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (refusal: Exception) {
                    logFailure(TAG, "could not say the siren was stopped in $link", refusal)
                }
            }
        }
    }

    override fun onDestroy() {
        if (usable) {
            radio.stop()
            groups.unlinkAll()
            stopCollecting()
        }

        AppLog.i(TAG, "stopped")
        super.onDestroy()
    }

    /**
     * Starts the collection when a group is linked and stops it when the
     * last one goes.
     */
    private fun linksChanged(before: List<GroupLink>, after: List<GroupLink>) {
        when {
            after.isEmpty() && before.isNotEmpty() -> {
                AppLog.i(TAG, "signed out of every group, no longer collecting")
                stopCollecting()
                awaitSession()
            }

            after.isNotEmpty() -> {
                sessionWait?.cancel()
                sessionWait = null
                startCollecting()
            }
        }
    }

    /**
     * Takes down everything [startCollecting] set up: the collection, the
     * cable receiver and whatever it was showing or sounding. What listens to
     * each group is taken down by [GroupLinks], group by group.
     */
    private fun stopCollecting() {
        collection?.cancel()
        collection = null
        status.stopHearingTheCable()
        // [keepSearchNotification] is cancelled along with [collection] and
        // leaves nothing to take its own notification down; the device may
        // still be being looked for when the collection stops.
        cancelSearchNotification(this)
        sirens.stop()
    }

    /**
     * Writes a fix and remembers it, when it is worth writing.
     *
     * The one place that decides both, because deciding them in three places
     * is three places to keep in step. News — the cable going in, a mode
     * beginning — passes the filter by, since the point of it is to be told
     * now rather than at the next threshold.
     */
    private suspend fun writeIfWorth(
        writer: EveryGroup,
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
                    fixLog.note(
                        fix,
                        lastFix,
                        mode,
                        FixVerdict(judged.jump, judged.agreed, judged.home, trusted),
                    )

                    carried.noticeAnAnswerThatWillNotFollow(fix, judged.whence, Instant.now())

                    used = sources.uses(judged.whence)

                    if (judged.leftOutAsJump(jumpsOn)) {
                        candidate = fix
                        jumpsInARow += 1
                    } else if (used) {
                        candidate = null
                        jumpsInARow = 0
                    }

                    judged
                },
                worthWriting = { !it.leftOutAsJump(jumpsOn) && used },
                write = { writeAndRemember(writer, fix, mode, it, always) },
            )

        if (judged.leftOutAsJump(jumpsOn)) {
            tellThemAnswersAreBeingLeftOut(writer, judged.jump?.name.orEmpty())

            askTheOtherRadios(writer, mode)

            return false
        }

        // Left out the same way a jump is, and for a reason of the same kind:
        // the member has said this is not a source to take into account. So it
        // moves no marker, becomes no last position and reaches nobody — while
        // staying in the log, where the question of what the provider was
        // saying is answered.
        if (!used) {
            AppLog.i(
                TAG,
                "left out a position found by ${judged.whence.said}: " +
                    "this device was told not to take that source into account",
            )

            // The switch as well as the fact, because "a source was switched
            // off" answers none of what a member opens the card for.
            tellThemAnswersAreBeingLeftOut(
                writer,
                "$LEFT_OUT_BY_SOURCE:${choiceFor(judged.whence)?.name.orEmpty()}",
            )

            return false
        }

        return wrote
    }

    /**
     * What is made of a fix before it is written or left out: the service's
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
                candidate = candidate,
                satellites = lastSatellitePosition(this),
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
        writer: EveryGroup,
        fix: Fix,
        mode: CollectionMode,
        judged: Judgement,
        always: Boolean,
    ): Boolean {
        if (!always && !shouldWrite(lastFix, fix, mode)) {
            return false
        }

        // Read once and carried into the write: asking twice would let the
        // cable come out between the two answers, and then the document would
        // disagree with what was decided here.
        val written = Written(fix, status.charging(), judged.settling)

        // News passes the filter by; a repeat of itself is not news.
        if (written == lastWritten) {
            return false
        }

        if (!writeQuietly(writer, written, mode, judged.whence)) {
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

    /**
     * Says the last position again, exactly as it was.
     *
     * Past the filter and past the check for a repeat, both of which exist to
     * keep the same document from being written twice — which is precisely
     * what is wanted here. The device is standing where it was and asking the
     * provider for nothing; the news is that it is still there to say so.
     */
    private suspend fun sayAgain(writer: EveryGroup, mode: CollectionMode): Boolean =
        judging.withLock { sayAgainInTurn(writer, mode) }

    private suspend fun sayAgainInTurn(writer: EveryGroup, mode: CollectionMode): Boolean {
        val known = lastFix ?: lastKnown ?: return false
        // The same position said again carries the same note about the
        // receiver: it is a statement about the answer, and the answer has not
        // changed. Worked out afresh here it would say something about an
        // epoch this position never came out of.
        val written = Written(known, status.charging(), lastWritten?.settling == true)

        // Not weighed against what the member shares: this is the position
        // they have already shared, said again so that the marker does not go
        // grey while the device sits quietly at home.
        if (!writeQuietly(writer, written, mode, foundBy(known, lastSatellitePosition(this)))) {
            return false
        }

        lastWritten = written
        silence.wroteAt = Instant.now()

        return true
    }

    /**
     * Collects positions and writes the ones worth writing.
     *
     * The mode decides what to ask the provider for, and it can change while
     * the service runs — the night comes, the battery drops — so the request is
     * reviewed on a timer rather than made once. A review that finds the same
     * mode costs one cancelled subscription and one new one.
     */
    private fun startCollecting() {
        if (collection?.isActive == true) {
            return
        }

        if (links.value.isEmpty()) {
            AppLog.w(TAG, "no session yet, waiting for one")
            awaitSession()

            return
        }

        status.hearTheCable()

        collection =
            lifecycleScope.launch {
                launch { status.keepTelling() }

                // A flipped source switch restarts the round like a new mode
                // does: the request itself has to change, not only the reading.
                modeReview.modes().combine(sources.changes()) { mode, _ ->
                    mode
                }.collectLatest { mode -> collectIn(mode) }
            }
    }

    /**
     * Collects in one mode until the next one replaces it: asks once, then
     * listens, rescuing the silence and keeping the second opinion meanwhile.
     */
    private suspend fun collectIn(mode: CollectionMode) {
        AppLog.i(TAG, "collecting in mode $mode")
        lastMode = mode
        // A mode beginning is news about the device as well as a reason to ask
        // for a position, and the first of them is the service starting.
        status.tell()

        val spec = requestSpec(mode)

        if (spec == null) {
            // Nothing to ask for, so nothing would be written either; the
            // position already known is repeated instead, and the next mode
            // arrives on its own.
            silence.keepSaying { sayAgain(writer, mode) }

            return
        }

        // Entering a mode is news: somebody started looking, or the night
        // ended. Neither should wait for the next threshold, and a watcher who
        // has just opened the map should see where the device is rather than
        // where it was ten minutes ago — which is what the filter would have
        // answered for a device standing still.
        val asked = asking(mode.accuracy)

        if (asked == null) {
            // Every source the member left on is one this device does not have:
            // an answer could only be left out.
            AppLog.w(TAG, "nothing to ask in mode $mode with the sources left on")
            awaitCancellation()
        }

        val first = firstPosition(mode)

        first?.let { fix ->
            opinion.freshen(mode)
            writeIfWorth(writer, fix, mode, always = true)
        }

        coroutineScope {
            silence.heardAt = Instant.now()
            launch { rescueWhileSilent(writer, mode) }
            launch { skyNotes.watchTheSatellites(mode) }
            launch { skyNotes.noteTheSkyWhileSilent(mode) }
            launch { opinion.keep(mode) }

            positionUpdates(this@SharingService, spec, asked).collect { fix ->
                silence.heardAt = Instant.now()

                if (sameAnswer(first, fix)) {
                    return@collect
                }

                opinion.freshen(mode)
                writeIfWorth(writer, fix, mode)
            }
        }
    }

    /**
     * Listens to the device's own motion sensors for as long as the service
     * runs.
     *
     * Apart from the activity recogniser feeding the modes, and deliberately:
     * that one answers what the person is doing, in its own time and only when
     * it is sure, and this one answers whether the device was moved at all. The
     * second question is the one a transmitter cannot answer for us.
     */
    private suspend fun noticeBeingCarried() {
        stirs(this).collect { stir -> carried.hear(stir, Instant.now()) }
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
    private suspend fun tellThemAnswersAreBeingLeftOut(writer: EveryGroup, reason: String) {
        val news = leftOut.note(reason, Instant.now()) ?: return

        try {
            writer.noteLeftOut(news.count, news.reason, news.since)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            AppLog.i(TAG, "nowhere to say answers are being left out yet: ${error.message}")
        }
    }

    /**
     * Gives the session a minute to turn up, and stops the service if it does
     * not.
     *
     * Collecting nothing while a notification says a position is being shared
     * is the one outcome to avoid: it is a promise on the screen with nothing
     * behind it. The listener starts the collection the moment the account is
     * restored, and this is what happens when it never is.
     */
    private fun awaitSession() {
        if (sessionWait?.isActive == true) {
            return
        }

        sessionWait =
            lifecycleScope.launch {
                delay(SESSION_WAIT.toMillis())

                if (links.value.isEmpty()) {
                    AppLog.w(TAG, "no session turned up, stopping rather than promising to share")
                    stopSelf()
                }
            }
    }

    private suspend fun firstPosition(mode: CollectionMode): Fix? {
        val priority = requestSpec(mode)?.priority ?: return null
        val asked = asking(mode.accuracy) ?: return null

        return positionNow(priority, asked)
    }

    /** Asks another way while the provider says nothing; see [Silence.rescue]. */
    private suspend fun rescueWhileSilent(writer: EveryGroup, mode: CollectionMode) {
        val silent = rescueAfter(mode) ?: return
        val priority = rescuePriority(mode) ?: return
        val asked = asking(rescueAccuracy(mode)) ?: return

        silence.rescue(
            silent,
            mode,
            ask = { positionNow(priority, asked) },
            write = { fix -> writeIfWorth(writer, fix, mode, always = true) },
            foundNothing = { opinion.noteWhatItKnows() },
        )
    }

    /**
     * Keeps the group's list of false places up to date, and on disk.
     *
     * The subscription runs for as long as the service does, whatever mode it
     * is in: the list is read by the rules on every fix, and a list that
     * stopped arriving while the device was quiet would be a list that is wrong
     * exactly when the device starts answering again.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun keepTheFalsePlaces() {
        // Every group's list at once: a place that puts devices somewhere false
        // does so whichever group the position is going to.
        links.flatMapLatest { linked ->
            if (linked.isEmpty()) {
                emptyFlow()
            } else {
                combine(linked.map { namedFalsePlaces(it.connection) }) { perGroup ->
                    perGroup.toList().flatten().distinct()
                }
            }
        }.collect { places ->
            falsePlaces = places
            falsePlaceStore.write(places)
        }
    }

    /**
     * Keeps this group's idea of going round in circles up to date.
     *
     * Nothing is kept on disk for it: a device that has not reached the
     * settings weighs answers against the limits in the build, which catch a
     * transmitter of any shape and only take a few more answers to do it.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun keepWhatCountsAsCircling() {
        // The first group's, since one device can go by only one idea of it;
        // the groups that settle it are the ones a device joined first.
        links.flatMapLatest { linked ->
            linked.firstOrNull()?.let { circlingLimits(it.connection) } ?: emptyFlow()
        }.collect { circleLimits = it }
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
        if (trusted || answeredBy(noteOf(fix.reported?.alongside)) == Answered.Assistance) {
            return
        }

        if (fix.from != null && fix.from != LocationManager.GPS_PROVIDER) {
            return
        }

        lately = lately.andThen(fix)
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
    private suspend fun askTheOtherRadios(writer: EveryGroup, mode: CollectionMode) {
        if (!otherRadiosAreDue(jumpsInARow, askedTheOtherRadiosAt, Instant.now())) {
            return
        }

        // Counted from the asking, so that an answer that never comes does not
        // turn into a scan on every fix.
        askedTheOtherRadiosAt = Instant.now()

        AppLog.w(TAG, "$jumpsInARow answers in a row left out; asking the towers and the Wi-Fi")

        val asked = opinion.answerNow()

        if (!networkAnswerStands(asked, Instant.now(), falsePlaces, awakeFor())) {
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
        if (!saysMoreThan(fix, lastFix, Instant.now(), awakeFor())) {
            AppLog.i(TAG, "the other radios repeated what is written, only wider: ${saying(fix)}")

            return
        }

        // The one rule that is asked of this answer too, and the reason is
        // what a field measurement showed: a device lying at home was offered
        // a place four kilometres away, and the answer that came back from
        // the other radios stood within two metres of the false one just
        // rejected.
        // Whatever built it, it was not a receiver that had been carried
        // there, and the sensors are the thing that says so. Everything else
        // the filter asks stays waived, as above.
        if (driftedWhilePutDown(carried.putDown, fix, carried.carrying())) {
            AppLog.w(
                TAG,
                "the other radios walked away from a device nobody touched too: ${saying(fix)}",
            )

            return
        }

        AppLog.i(TAG, "standing on what the other radios say: ${saying(fix)}")

        jumpsInARow = 0
        writeIfWorth(writer, fix, mode, always = true, trusted = true)
    }

    /** Who to ask with this accuracy, given the device and the switches. */
    private fun asking(accuracy: Accuracy): Asked? =
        whomToAsk(accuracy, playServices, sources.leftOut, providers)

    /**
     * One position, asked for now; a provider that will not answer is not a
     * reason to stop collecting, and the next round asks again.
     */
    private suspend fun positionNow(priority: Int, asked: Asked): Fix? = try {
        currentPosition(this, priority, asked)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        AppLog.w(TAG, "the provider gave no position", error)

        null
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
        trailStore.write(trail)

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
        candidate = null
        lastWritten = null
        jumpsInARow = 0

        AppLog.i(TAG, "forgetting the last place, as asked")
    }

    /**
     * What the filter was looking at when a jump it caught needs describing,
     * gathered from the fields above at the moment [caughtBy] is asked for.
     */
    private fun leftOutEvidence(): LeftOutEvidence = LeftOutEvidence(
        radio = radio,
        restsOn = restsOn,
        lately = lately,
        circleLimits = circleLimits,
        falsePlaces = falsePlaces,
        networkSaid = opinion.said,
        awakeFor = awakeFor(),
    )

    /**
     * A write that failed is not a reason to stop collecting: the network comes
     * back, and the next fix carries the same news.
     */
    private suspend fun writeQuietly(
        writer: EveryGroup,
        written: Written,
        mode: CollectionMode,
        // How the device arrived at this, worked out by the caller: on these
        // devices the fused answer being written carries no note of its own,
        // and what says whether it is the satellites' measurement under
        // another name is the receiver's own last answer, read from the store.
        whence: FoundBy,
    ): Boolean = try {
        val into =
            writer.write(
                written.fix,
                mode,
                status.batteryPercent(),
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
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        logFailure(TAG, "could not write the position", error)

        false
    }

    private fun currentMode(
        untils: List<Instant>,
        moving: Boolean,
        standing: Boolean,
        ownMap: Boolean,
    ): CollectionMode = chooseMode(
        CollectionState(
            watchersUntil = untils,
            now = Instant.now(),
            localTime = LocalTime.now(),
            insideGeofence = standing,
            moving = moving,
            batteryPercent = status.batteryPercent(),
            ownMapOpen = ownMap,
        ),
    )

    /**
     * The platform checks the location permission again here and answers a
     * missing one with an exception, so a refusal has to be survivable rather
     * than merely unlikely.
     */
    private fun enterForeground(): Boolean = try {
        ServiceCompat.startForeground(
            this,
            SHARING_NOTIFICATION_ID,
            sharingNotification(inAppLanguage()),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )

        true
    } catch (refusal: Exception) {
        AppLog.e(TAG, "the platform refused the foreground start", refusal)

        false
    }

    companion object {
        /** How long a freshly started service waits for the account to be restored. */
        private val SESSION_WAIT: Duration = Duration.ofMinutes(1)

        const val ACTION_STOP = "io.github.vitalyostanin.geoshare.STOP"

        /** The member being looked for says they have been found. */
        const val ACTION_FOUND = "io.github.vitalyostanin.geoshare.FOUND"

        /** Whoever holds this device stops its siren before its twenty seconds are up. */
        const val ACTION_SIREN_OFF = "io.github.vitalyostanin.geoshare.SIREN_OFF"

        fun start(context: Context) = ContextCompat.startForegroundService(
            context,
            Intent(context, SharingService::class.java),
        )

        fun stop(context: Context) = context.startService(
            Intent(context, SharingService::class.java).setAction(ACTION_STOP),
        )
    }
}
