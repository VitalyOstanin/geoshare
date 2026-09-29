package io.github.vitalyostanin.geoshare.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.location.LocationManager
import android.os.BatteryManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import io.github.vitalyostanin.geoshare.collection.Accuracy
import io.github.vitalyostanin.geoshare.collection.Answered
import io.github.vitalyostanin.geoshare.collection.CARRIED_STEPS
import io.github.vitalyostanin.geoshare.collection.CLOCK_TOLERANCE
import io.github.vitalyostanin.geoshare.collection.CircleLimits
import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.github.vitalyostanin.geoshare.collection.CollectionState
import io.github.vitalyostanin.geoshare.collection.Constellation
import io.github.vitalyostanin.geoshare.collection.Counted
import io.github.vitalyostanin.geoshare.collection.FalsePlace
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.FoundBy
import io.github.vitalyostanin.geoshare.collection.IDLE_REPEAT
import io.github.vitalyostanin.geoshare.collection.JumpReason
import io.github.vitalyostanin.geoshare.collection.LEFT_OUT_BY_SOURCE
import io.github.vitalyostanin.geoshare.collection.Moving
import io.github.vitalyostanin.geoshare.collection.PutDown
import io.github.vitalyostanin.geoshare.collection.SECOND_OPINION_ASKED_EVERY
import io.github.vitalyostanin.geoshare.collection.STIRS_KEPT
import io.github.vitalyostanin.geoshare.collection.SkyDrift
import io.github.vitalyostanin.geoshare.collection.Stir
import io.github.vitalyostanin.geoshare.collection.Trail
import io.github.vitalyostanin.geoshare.collection.agreementStands
import io.github.vitalyostanin.geoshare.collection.anchorFrom
import io.github.vitalyostanin.geoshare.collection.andThen
import io.github.vitalyostanin.geoshare.collection.answeredBy
import io.github.vitalyostanin.geoshare.collection.carryingSince
import io.github.vitalyostanin.geoshare.collection.choiceFor
import io.github.vitalyostanin.geoshare.collection.chooseMode
import io.github.vitalyostanin.geoshare.collection.circleDrawn
import io.github.vitalyostanin.geoshare.collection.clockDisagrees
import io.github.vitalyostanin.geoshare.collection.comingHome
import io.github.vitalyostanin.geoshare.collection.distanceMetres
import io.github.vitalyostanin.geoshare.collection.driftedWhilePutDown
import io.github.vitalyostanin.geoshare.collection.falsePlaceFor
import io.github.vitalyostanin.geoshare.collection.fenceMoves
import io.github.vitalyostanin.geoshare.collection.foundBy
import io.github.vitalyostanin.geoshare.collection.impliedSpeedMps
import io.github.vitalyostanin.geoshare.collection.knows
import io.github.vitalyostanin.geoshare.collection.makeupOf
import io.github.vitalyostanin.geoshare.collection.networkAnswerStands
import io.github.vitalyostanin.geoshare.collection.noteOf
import io.github.vitalyostanin.geoshare.collection.otherRadiosAreDue
import io.github.vitalyostanin.geoshare.collection.pinnedWhileWalking
import io.github.vitalyostanin.geoshare.collection.putDownAt
import io.github.vitalyostanin.geoshare.collection.remembering
import io.github.vitalyostanin.geoshare.collection.rescueAfter
import io.github.vitalyostanin.geoshare.collection.routeRepeated
import io.github.vitalyostanin.geoshare.collection.said
import io.github.vitalyostanin.geoshare.collection.saysMoreThan
import io.github.vitalyostanin.geoshare.collection.secondOpinion
import io.github.vitalyostanin.geoshare.collection.shouldWrite
import io.github.vitalyostanin.geoshare.collection.stillSettling
import io.github.vitalyostanin.geoshare.collection.walkingNow
import io.github.vitalyostanin.geoshare.collection.watchedUntil
import io.github.vitalyostanin.geoshare.collection.whyAJump
import io.github.vitalyostanin.geoshare.collection.word
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.log.SkyLog
import io.github.vitalyostanin.geoshare.map.stands
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.util.Locale

private const val TAG = "geoshare.service"

/**
 * How far the satellites' own answer has to be from the written one before the
 * disagreement is worth a line.
 *
 * Two hundred metres. Below that the two are answering about the same place
 * with different instruments — a position from the towers is a few hundred
 * metres wide on a good day — and above it they are answering about different
 * places, which is the case worth reading about.
 */
private const val SATELLITES_APART_METRES = 200.0

/** How often that disagreement is said out loud while it goes on. */
private val DISAGREEMENT_SAID_EVERY: Duration = Duration.ofMinutes(1)

/**
 * How long the receiver may answer nothing before the sky it is hearing is put
 * down without a position, and how often it is put down after that.
 *
 * Half a minute, against the fifteen seconds a watched phone asks at: a
 * receiver that is working never reaches it, so nothing is written while there
 * is nothing to explain. A receiver being talked over reaches it at once and
 * keeps reaching it, which is when the rows are worth their room.
 */
private val SKY_NOTED_WHILE_SILENT: Duration = Duration.ofSeconds(30)

/**
 * How often the group is told that this phone's answers are being left out.
 *
 * Once a minute. Under a transmitter an answer arrives every seven seconds,
 * and telling the database about each of them would spend its free daily
 * allowance in an afternoon; what a reader wants from the field is whether
 * this is happening and roughly how much of it, and a minute answers that.
 */
private val LEFT_OUT_TOLD_EVERY: Duration = Duration.ofMinutes(1)

/**
 * How often the phone says how it is when nothing else has made it say so.
 *
 * On the beat a phone at rest writes its position again, for the same reason:
 * under a transmitter no position is written at all, and this is the only
 * news that keeps the marker out of the grey. An hour let it go grey for half
 * of every hour, and the card then read a phone that is working and being
 * lied to as a phone that has gone quiet.
 */
private val STATUS_EVERY: Duration = IDLE_REPEAT

/**
 * A position as it goes into the document: where the phone was, and whether
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
     * on. It is part of what makes two writes the same write, so a phone whose
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
    private var collection: Job? = null

    /**
     * How long the service waits for a session before admitting it has none.
     *
     * A service started at boot can be quicker than the restoring of the
     * signed-in account, and refusing to work because of a second's difference
     * would mean a phone that shares nothing until somebody opens the
     * application.
     */
    private var sessionWait: Job? = null

    /**
     * The account, whenever there is one.
     *
     * Registered rather than read once: the answer at the moment the service
     * starts is not the answer a second later, and a service holding up a
     * notification that says it is sharing has to be either sharing or gone.
     */
    private val session =
        FirebaseAuth.AuthStateListener { auth ->
            val memberId = auth.currentUser?.uid

            when (sessionChange(collectingFor, memberId)) {
                SessionChange.None -> Unit

                SessionChange.Start -> {
                    sessionWait?.cancel()
                    sessionWait = null
                    startCollecting()
                }

                SessionChange.Restart -> {
                    AppLog.i(TAG, "another account signed in, collecting for it instead")
                    stopCollecting()
                    startCollecting()
                }

                SessionChange.Stop -> {
                    AppLog.i(TAG, "signed out, no longer collecting")
                    stopCollecting()
                    awaitSession()
                }
            }
        }

    /**
     * The account the running collection writes for, or nothing while none runs.
     *
     * The phone can change hands: signed out of one account and into another,
     * it keeps the same id, and a collection left running for the first account
     * would go on writing under its name with the second one's session — every
     * write refused, and the notification saying it shares all the same.
     */
    private var collectingFor: String? = null

    /**
     * Everything a write outside the collection loop needs: where the phone
     * was last seen to be, how it was collecting at the time and what writes
     * its documents. Held on the service because the cable is pulled at a
     * moment the loop is not looking.
     */
    private var writer: PositionWriter? = null
    private var status: PhoneStatusWriter? = null

    /** The alarm stream at full volume while a call to this phone sounds. */
    private val sirens by lazy { SirenAnswer(this, lifecycleScope) }

    /** This phone's siren address while the service collects, for the stop action. */
    private var ownPhoneId: String? = null

    private var lastFix: Fix? = null

    /**
     * The fix left out just before this one, kept so that the next one can
     * agree with it.
     */
    private var candidate: Fix? = null

    /**
     * How many answers in a row the filter has left out.
     *
     * A phone being fed a false signal is answered promptly and wrongly, so
     * this counts up while the phone looks perfectly healthy and says nothing.
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

    /**
     * The last thing the towers and the Wi-Fi said, whoever asked them.
     *
     * Kept because it is the one opinion a transmitter drawing a false road
     * cannot reach, and because the rule that uses it has to run while nothing
     * looks wrong: a road drawn slowly breaks no other rule, so by the time
     * another rule fires there would be nothing left to compare.
     */
    private var networkSaid: Fix? = null

    /**
     * The answers the satellites have given lately, newest last.
     *
     * Every one of them, whether or not it was written out: what the shape of
     * the run gives away is exactly what a pair of answers hides, and the
     * answers a rule has already refused are the ones that draw the shape
     * while the anchor still stands where the phone really is.
     */
    private var lately: List<Fix> = emptyList()

    /**
     * Since when the phone's own sensors say it has not been carried anywhere,
     * or nothing while it is moving and while they have said nothing yet.
     *
     * Kept as the moment the stillness began rather than as a flag, because
     * what a fix is weighed against is whether the stillness covers the way
     * from the last position to this one.
     */
    private var standingSince: Instant? = null

    /**
     * What the phone's own motion sensors have reported lately, oldest first.
     *
     * Kept as moments rather than as a flag because they arrive late: the step
     * counter is read in batches, and a batch landing a minute after the fact
     * still has to undo an anchor that covered the minute it describes.
     */
    private val stirsHeard = mutableListOf<Stir>()

    /** What the step counter last stood at, and when anything last stirred. */
    private var stepsNow: Long? = null
    private var lastStir: Instant? = null

    /**
     * Where the phone was standing when it was last put down.
     *
     * The place the answers are weighed against while nothing touches the
     * phone, which is the one comparison a slow creep cannot walk away from
     * unnoticed: the position written before each answer creeps along with the
     * lie, and this does not.
     */
    private var putDown: PutDown? = null

    /**
     * When the stretch of walking the phone is in the middle of began, and
     * where it said it was at the time.
     *
     * The pair is the other rule's anchor turned around: there a place is kept
     * while the phone lies still, here a place is kept while the phone is
     * carried, so that an answer refusing to follow the walking can be seen.
     * Only a narrow answer measured off the sky is kept — see
     * [pinnedWhileWalking] for why the others say nothing.
     */
    private var walkStarted: Instant? = null

    private var walkedFrom: Fix? = null

    /**
     * The run of answers left out since the last position was written: how
     * many, why the last one was, when the run began and when the group was
     * last told about it.
     *
     * Kept so that the card of a phone under a transmitter can say what is
     * happening to it. Everything here is cleared by a position being written,
     * because a written position is the run ending.
     */
    private var leftOutCount = 0

    private var leftOutReason: String? = null

    private var leftOutSince: Instant? = null

    private var leftOutToldAt: Instant? = null

    /**
     * Whether this phone has sensors to ask at all.
     *
     * Read once: sensors do not come and go, and the permission is asked for
     * before the service starts collecting.
     */
    private val sensorsTell by lazy { sensorsNoticeMovement(this) }

    /** Where this phone keeps being, kept across restarts of the process. */
    private val trailStore by lazy { TrailStore(this) }
    private var trail: Trail = Trail()

    /**
     * The places this group says a false signal uses, and the copy of them
     * that survives the process being killed.
     *
     * Read from the file first and from the database after, because the fix
     * these places are there to catch is the first one of the session: a phone
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
     * The last position this phone was ever sure of, however long ago.
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

    /** Which sources of a position this phone takes into account, as asked on the screen. */
    private val sources by lazy { SourcesSetting(this) }

    /**
     * What the radio itself says, apart from any position that comes out of it.
     *
     * Watched for the whole life of the service rather than around each fix:
     * the verdict is a comparison of the last ten epochs against the fifty
     * before them, and a subscription taken out for the moment of a fix would
     * have nothing to compare against.
     */
    private val radio by lazy { RadioWatch(this) }
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

    /**
     * When the provider last said anything at all.
     *
     * Kept apart from [lastFix], which is the last position written down: a fix
     * the filter dropped is still the provider answering, and what the rescue
     * below watches for is silence rather than stillness.
     */
    private var heardAt: Instant = Instant.now()

    /** When the satellites were last said to disagree with what is written. */
    private var disagreedAt: Instant = Instant.EPOCH

    /** Which of the satellites' answers has already been put in the record. */
    private var satellitesLastSaid: Duration? = null

    /**
     * When the last write actually reached the database.
     *
     * Not the same thing as the moment of the fix in it: the quiet mode writes
     * a position taken half an hour ago on purpose, and what keeps the card
     * from going grey is when the document was written rather than when the
     * phone was there.
     */
    private var wroteAt: Instant? = null

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

    /**
     * The cable going in or coming out, said at once.
     *
     * Waiting for the next fix would leave a phone reading "12%" for the
     * fifteen minutes it is already filling up, which is the case the reader
     * most wants told — and under a transmitter there is no next fix at all.
     */
    private val power =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                AppLog.i(TAG, "power changed: ${intent.action}")
                tellHowThePhoneIs()
            }
        }

    override fun onCreate() {
        super.onCreate()
        FirebaseAuth.getInstance().addAuthStateListener(session)
        radio.start()

        trail = trailStore.read()
        lastKnown = trail.fixes.lastOrNull()
        lastFix = anchorFrom(trail, Instant.now())
        falsePlaces = falsePlaceStore.read()

        lifecycleScope.launch { keepTheFalsePlaces() }
        lifecycleScope.launch { noticeBeingCarried() }
        lifecycleScope.launch { keepWhatCountsAsCircling() }

        AppLog.i(
            TAG,
            "the phone remembers ${trail.fixes.size} places it has been" +
                if (lastFix == null) "" else ", and measures from the last of them",
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP) {
            AppLog.i(TAG, "stopping on request")
            // The notification and the switch on the settings screen are the
            // same statement, and stopping through one of them has to turn the
            // other off: otherwise the switch goes on saying we are sharing,
            // and the next time the application is opened — or the phone is
            // restarted — the service starts itself again.
            SharingSetting(this).enabled = false
            stopSelf()

            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_FOUND) {
            AppLog.i(TAG, "this phone says it has been found")

            val phoneId = ownPhoneId

            // Not a reason to stop sharing: only the search on this phone is
            // over, and the service goes on exactly as it was. The
            // notification comes down on its own, once [keepSearchNotification]
            // hears the document is gone.
            if (phoneId != null) {
                lifecycleScope.launch {
                    try {
                        endSearch(phoneId)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (refusal: Exception) {
                        AppLog.w(TAG, "could not end the search from its own notification", refusal)
                    }
                }
            }
        }

        if (intent?.action == ACTION_SIREN_OFF) {
            AppLog.i(TAG, "the siren is stopped on the phone itself")
            sirens.quieten()

            val memberId = FirebaseAuth.getInstance().currentUser?.uid
            val phoneId = ownPhoneId

            // Written so that the caller's screen reads "stopped" rather than
            // counting down twenty seconds that are no longer sounding.
            if (memberId != null && phoneId != null) {
                lifecycleScope.launch {
                    try {
                        stopSiren(phoneId, memberId)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (refusal: Exception) {
                        AppLog.w(TAG, "could not say the siren was stopped", refusal)
                    }
                }
            }
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

    override fun onDestroy() {
        FirebaseAuth.getInstance().removeAuthStateListener(session)
        radio.stop()
        stopCollecting()
        AppLog.i(TAG, "stopped")
        super.onDestroy()
    }

    /**
     * Takes down everything [startCollecting] set up for one account: the
     * collection, the writers made for it, the cable receiver and whatever it
     * was showing or sounding for that account.
     */
    private fun stopCollecting() {
        collection?.cancel()
        collection = null
        collectingFor = null
        writer = null
        status = null
        ownPhoneId = null
        runCatching { unregisterReceiver(power) }
        // [keepSearchNotification] is cancelled along with [collection] and
        // leaves nothing to take its own notification down; the phone may
        // still be being looked for when the collection stops.
        cancelSearchNotification(this)
        sirens.stop()
    }

    /**
     * Says how the phone is, apart from where it is: in touch now, in this
     * mode, this charged, on a cable or off it.
     *
     * Not through the position, and not past the filter: a phone whose
     * answers are all being thrown away is still in touch and still on its
     * cable, and a reader deciding whether it has gone quiet or run flat needs
     * to hear that. A failure costs nothing but the news, which the next mode,
     * cable or hour says again.
     */
    private fun tellHowThePhoneIs() {
        val status = status ?: return
        val mode = lastMode
        val percent = batteryPercent()
        val cable = charging()

        lifecycleScope.launch {
            try {
                status.write(mode, percent, cable)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                AppLog.w(TAG, "could not say how the phone is", error)
            }
        }
    }

    /**
     * Says how the phone is once an hour, whatever else it is or is not
     * writing: the moment it was last in touch is what tells a working phone
     * whose position is held back from one that has gone quiet.
     */
    private suspend fun keepTellingHowThePhoneIs() {
        while (true) {
            delay(STATUS_EVERY.toMillis())
            tellHowThePhoneIs()
        }
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
        writer: PositionWriter,
        fix: Fix,
        mode: CollectionMode,
        always: Boolean = false,
        // An answer that did not come from the satellites at all, and so has
        // no business being measured against the rules written for them: the
        // towers answer in kilometres, and the width rule would throw away the
        // one source the spoofer cannot reach.
        trusted: Boolean = false,
    ): Boolean {
        if (jumps.takeForgetRequest()) {
            forgetThePlace()
        }

        rememberASecondOpinion(fix)
        rememberTheShapeOfIt(fix, trusted)

        // Read once, before anything is decided: the receiver goes on
        // reporting while the rules run, and an epoch read afterwards would
        // be a different epoch from the one the fix came out of.
        restsOn = if (trusted) emptyMap() else radio.restingOn()

        // How the phone arrived at this answer, worked out before anything is
        // decided: it settles both whether this is a source the member allows
        // and what the written document says about itself. The receiver's own
        // last answer is what makes it possible at all — the fused answer
        // being weighed carries no note of its own on these phones.
        val whence = foundBy(fix, lastSatellitePosition(this))

        // Whether the receiver has had time to settle since the phone was
        // switched on, read off the same epoch as the rules below. Never asked
        // of an answer from the towers: it is a statement about the satellites,
        // and such an answer rests on none.
        val settling = !trusted && stillSettling(fix.sinceBoot, restsOn)

        val jump =
            if (trusted) {
                null
            } else {
                whyAJump(
                    lastFix,
                    fix,
                    standingSince,
                    networkSaid,
                    Instant.now(),
                    lately,
                    falsePlaces,
                    circleLimits,
                    restsOn = restsOn,
                    skySays = radio.skySays(),
                    putDown = putDown,
                    carrying = carryingSince(putDown, stirsHeard, sensorsTell),
                    settling = settling,
                )
            }
        // A fix left out does not move the last position, and that is how the
        // marker can be left standing somewhere the phone has long left: a
        // wrong last position turns every true fix that follows into a jump.
        // A second fix that agrees with the one before it is the way out of a
        // width nothing can outlive, and of the rules that measure against
        // that last position. Never on the word of the two answers alone,
        // because a spoofed signal draws a road whose fixes all agree: the
        // towers or the Wi-Fi around the phone have to name the same place,
        // and past the width only an answer as narrow as the Wi-Fi's will do.
        val agreed =
            jump != null && agreementStands(jump, candidate, fix, networkSaid, Instant.now())

        // The other way out of an anchor that has gone wrong, and the only one
        // that works against a width or a false place: the phone answering
        // from somewhere it has been three times over an hour. Those visits
        // are positions that were written, and while a spoofer runs, nothing
        // is — so this cannot be arranged by feeding the phone a signal.
        val home = jump != null && !agreed && comingHome(trail, lastFix, fix)

        // Every fix that arrives is written down, whether or not it is written
        // out: what the provider answers where it is being jammed is the thing
        // to be read afterwards, and a filtered log would answer questions
        // about the filter instead.
        noteFix(fix, mode, jump, agreed, home, trusted)

        noticeAnAnswerThatWillNotFollow(fix, whence)

        if (jump != null && jumps.enabled && !agreed && !home) {
            candidate = fix
            jumpsInARow += 1

            tellThemAnswersAreBeingLeftOut(writer, jump.name)

            askTheOtherRadios(writer, mode)

            return false
        }

        // Left out the same way a jump is, and for a reason of the same kind:
        // the member has said this is not a source to take into account. So it
        // moves no marker, becomes no last position and reaches nobody — while
        // staying in the log, where the question of what the provider was
        // saying is answered.
        if (!sources.uses(whence)) {
            AppLog.i(
                TAG,
                "left out a position found by ${whence.said}: " +
                    "this phone was told not to take that source into account",
            )

            // The switch as well as the fact, because "a source was switched
            // off" answers none of what a member opens the card for.
            tellThemAnswersAreBeingLeftOut(
                writer,
                "$LEFT_OUT_BY_SOURCE:${choiceFor(whence)?.name.orEmpty()}",
            )

            return false
        }

        candidate = null
        jumpsInARow = 0

        if (!always && !shouldWrite(lastFix, fix, mode)) {
            return false
        }

        // Read once and carried into the write: asking twice would let the
        // cable come out between the two answers, and then the document would
        // disagree with what was decided here.
        val written = Written(fix, charging(), settling)

        // News passes the filter by; a repeat of itself is not news.
        if (written == lastWritten) {
            return false
        }

        if (!writeQuietly(writer, written, mode, whence)) {
            return false
        }

        lastFix = fix
        lastWritten = written
        wroteAt = Instant.now()
        forgetTheAnswersLeftOut()
        rememberThePlace(fix)
        anchorWhereItLies(fix)

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
     * what is wanted here. The phone is standing where it was and asking the
     * provider for nothing; the news is that it is still there to say so.
     */
    private suspend fun sayAgain(writer: PositionWriter, mode: CollectionMode): Boolean {
        val known = lastFix ?: lastKnown ?: return false
        // The same position said again carries the same note about the
        // receiver: it is a statement about the answer, and the answer has not
        // changed. Worked out afresh here it would say something about an
        // epoch this position never came out of.
        val written = Written(known, charging(), lastWritten?.settling == true)

        // Not weighed against what the member shares: this is the position
        // they have already shared, said again so that the marker does not go
        // grey while the phone sits quietly at home.
        if (!writeQuietly(writer, written, mode, foundBy(known, lastSatellitePosition(this)))) {
            return false
        }

        lastWritten = written
        wroteAt = Instant.now()

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

        val memberId = FirebaseAuth.getInstance().currentUser?.uid

        if (memberId == null) {
            AppLog.w(TAG, "no session yet, waiting for one")
            awaitSession()

            return
        }

        collectingFor = memberId

        val device = DeviceIdentity(this)
        val writer = PositionWriter(memberId, device)

        this.writer = writer

        ContextCompat.registerReceiver(
            this,
            power,
            IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            },
            // Only the system sends these, and nothing else may reach in.
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        status = PhoneStatusWriter(memberId, device)

        // Once, after the move to the system's id: the old position would
        // otherwise stay on the map beside the new one.
        lifecycleScope.launch { leavePreviousId(memberId, device) }

        collection =
            lifecycleScope.launch {
                val ownDevice = positionId(memberId, device.id)

                launch { keepTellingHowThePhoneIs() }
                ownPhoneId = ownDevice
                launch { keepSearchNotification(this@SharingService, ownDevice) }
                launch { sirens.answer(memberId, ownDevice) }

                modes(memberId, device).collectLatest { mode ->
                    AppLog.i(TAG, "collecting in mode $mode")
                    lastMode = mode
                    // A mode beginning is news about the phone as well as a
                    // reason to ask for a position, and the first of them is
                    // the service starting.
                    tellHowThePhoneIs()

                    val spec = requestSpec(mode)

                    if (spec == null) {
                        // Nothing to ask for, so nothing would be written
                        // either; the position already known is repeated
                        // instead, and the next mode arrives on its own.
                        keepSaying(writer, mode)

                        return@collectLatest
                    }

                    // Entering a mode is news: somebody started looking, or the
                    // night ended. Neither should wait for the next threshold,
                    // and a watcher who has just opened the map should see
                    // where the phone is rather than where it was ten minutes
                    // ago — which is what the filter would have answered for a
                    // phone standing still.
                    firstPosition(mode)?.let { fix ->
                        writeIfWorth(writer, fix, mode, always = true)
                    }

                    coroutineScope {
                        heardAt = Instant.now()
                        launch { rescueWhileSilent(writer, mode) }
                        launch { watchTheSatellites(mode) }
                        launch { noteTheSkyWhileSilent(mode) }
                        launch { keepASecondOpinion() }

                        positionUpdates(this@SharingService, spec).collect { fix ->
                            heardAt = Instant.now()
                            writeIfWorth(writer, fix, mode)
                        }
                    }
                }
            }
    }

    /**
     * The mode as it stands, and again whenever it changes.
     *
     * Two things move it: the viewers, which arrive through the subscription
     * within about a second of somebody opening a map, and the clock and the
     * battery, which are looked at on a timer. Repeats are dropped, so a review
     * that finds the same mode does not interrupt the collection.
     */
    private fun modes(memberId: String, device: DeviceIdentity): Flow<CollectionMode> {
        val reviews =
            flow {
                while (true) {
                    emit(Unit)
                    delay(MODE_REVIEW.toMillis())
                }
            }

        val claims = watchers(positionId(memberId, device.id)).onStart { emit(emptyList()) }
        val permissions: Flow<Map<String, Instant>?> = ownSharedWith(memberId)
        // Said again, as none, at the moment the search runs out: nothing
        // arrives from the database then, and without it every viewer would
        // go on counting until the claims themselves happened to change.
        val search =
            channelFlow {
                ownSearch(positionId(memberId, device.id)).collectLatest { found ->
                    send(found)

                    if (found != null && found.stands(Instant.now())) {
                        delay(Duration.between(Instant.now(), found.endsAt).toMillis())
                        send(null)
                    }
                }
            }.onStart { emit(null) }

        // Who is looking is the screen's business; the service needs only the
        // moments the claims of those allowed to see this phone run out. While
        // the group is looking for this very phone every claim counts,
        // whatever the grant says: the rules already answer every viewer, and
        // a phone still gating them by who it shares with would be fast for
        // fewer of the group than can actually see it.
        val looking =
            combine(claims, permissions.onStart { emit(null) }, search) { seen, shared, standing ->
                val sought = standing?.stands(Instant.now()) == true

                watchedUntil(seen, memberId, shared, sought)
            }

        return combine(
            looking,
            movement(this),
            standingStill(this, fenceCentre),
            MapOnScreen.state,
            reviews,
        ) { untils, moving, standing, ownMap, _ ->
            noteMovement(moving)
            currentMode(untils, moving.going, standing, ownMap)
        }.distinctUntilChanged()
    }

    /**
     * Listens to the phone's own motion sensors for as long as the service
     * runs.
     *
     * Apart from the activity recogniser feeding the modes, and deliberately:
     * that one answers what the person is doing, in its own time and only when
     * it is sure, and this one answers whether the phone was moved at all. The
     * second question is the one a transmitter cannot answer for us.
     */
    private suspend fun noticeBeingCarried() {
        stirs(this).collect { stir -> noteStir(stir) }
    }

    /**
     * Keeps what the motion sensors have said, so that the anchor can be
     * undone by a report that arrives after the fact.
     */
    private fun noteStir(stir: Stir) {
        stirsHeard += stir
        lastStir = maxOf(lastStir ?: stir.at, stir.at)
        stir.steps?.let { counted -> stepsNow = maxOf(stepsNow ?: counted, counted) }

        val old = Instant.now().minus(STIRS_KEPT)

        stirsHeard.removeAll { it.at.isBefore(old) }
    }

    /**
     * Moves the anchor to where the phone has just said it is, where the
     * anchor it had is no longer about a phone lying untouched.
     *
     * An anchor is left where it is for as long as nothing has stirred: that
     * is the whole of its value, since an anchor that followed the answers
     * would follow a creep as readily as the last position does. It is moved
     * once something has been carried somewhere and then put down again, and
     * [putDownAt] is what decides that the phone has settled.
     */
    private fun anchorWhereItLies(believed: Fix) {
        val carrying = carryingSince(putDown, stirsHeard, sensorsTell)

        if (putDown != null && !carrying.stirred && (carrying.steps ?: 0) < CARRIED_STEPS) {
            return
        }

        val settled = putDownAt(believed, lastStir, sensorsTell, Instant.now(), stepsNow)

        // Said once per anchor rather than per fix: what this line answers is
        // whether the rule is awake at all on a given phone, and a phone
        // standing still would otherwise repeat it every few minutes.
        if (settled?.at != putDown?.at) {
            AppLog.i(
                TAG,
                settled?.let { "the phone is standing where it was put down, at ${theSpot(it)}" }
                    ?: "nothing to anchor on: the phone has been moved too recently",
            )
        }

        putDown = settled
    }

    /**
     * Tells the group that answers are arriving and being thrown away.
     *
     * The count and the reason go into the phone's own position document, so
     * that a reader sees a phone being lied to rather than a phone that has
     * gone quiet. Nothing else in the document is touched: the coordinates
     * stay as old as they are.
     *
     * Said at most once every [LEFT_OUT_TOLD_EVERY] however fast the answers
     * come. Under a transmitter an answer arrives every seven seconds, and a
     * write for each of them would spend the day's free allowance of the
     * database in an afternoon — while saying nothing a minute's worth of
     * counting does not say just as well.
     *
     * A failure is nothing happening: a phone whose answers were being thrown
     * out before it ever wrote a position has no document to add this to, and
     * that is the one case the write fails in.
     */
    private suspend fun tellThemAnswersAreBeingLeftOut(writer: PositionWriter, reason: String) {
        val now = Instant.now()

        leftOutCount += 1
        leftOutReason = reason

        if (leftOutSince == null) {
            leftOutSince = now
        }

        val told = leftOutToldAt

        if (told != null && Duration.between(told, now) < LEFT_OUT_TOLD_EVERY) {
            return
        }

        leftOutToldAt = now

        runCatching { writer.noteLeftOut(leftOutCount, reason, leftOutSince ?: now) }
            .onFailure { AppLog.i(TAG, "nowhere to say answers are being left out yet") }
    }

    /**
     * Forgets the run of left-out answers, because a position has been
     * written.
     *
     * The document itself needs no clearing: it is written whole, and the
     * three fields simply stop being in it.
     */
    private fun forgetTheAnswersLeftOut() {
        leftOutCount = 0
        leftOutReason = null
        leftOutSince = null
        leftOutToldAt = null
    }

    /**
     * Says so when the phone has been carried somewhere and the answer has
     * stayed behind.
     *
     * Nothing is decided by it yet: the line goes in the log, the position is
     * treated exactly as it would have been, and what the line is for is the
     * measurement — how often this happens over a day of ordinary carrying,
     * and what it looks like when it does. The threshold of [PINNED_STEPS] was
     * chosen to be far beyond a room rather than measured, which is the other
     * way round from every other threshold here, and it should be measured
     * before anything is left out on the strength of it.
     *
     * The place a walk is measured from is taken from a narrow satellite
     * answer and no other, because only such an answer claims to know where
     * the phone is closely enough for standing still to mean anything.
     */
    private fun noticeAnAnswerThatWillNotFollow(fix: Fix, whence: FoundBy) {
        if (whence != FoundBy.Satellites) {
            return
        }

        val walk = walkingNow(stirsHeard) ?: return

        if (walkStarted != walk.since || walkedFrom == null) {
            walkStarted = walk.since
            walkedFrom = fix

            return
        }

        val from = walkedFrom ?: return

        if (!pinnedWhileWalking(walk, from, fix, sensorsTell)) {
            return
        }

        val away = "%.0f m from where the walk began, ±%.0f m, at %.5f,%.5f"
            .format(
                Locale.ROOT,
                distanceMetres(from, fix),
                fix.accuracyMetres ?: 0.0,
                fix.latitude,
                fix.longitude,
            )

        // Kept apart: PINNED_STEPS is waiting on a day of the phone being
        // carried, and the ordinary log does not hold a day.
        AppLog.keep(TAG, "carried ${walk.steps} steps and the answer has not followed: $away")
    }

    /** Where an anchor stands, for the one line that says it was set. */
    private fun theSpot(anchor: PutDown): String =
        "%.5f,%.5f".format(Locale.ROOT, anchor.at.latitude, anchor.at.longitude)

    /**
     * Keeps the moment the phone was last carried anywhere.
     *
     * The stillness is dated from the first report of it and left alone by
     * every report that repeats it: the timer is reviewed on a clock, so the
     * same answer arrives again every few minutes, and taking the latest of
     * them would keep the stillness permanently one review old and cover
     * nothing.
     */
    private fun noteMovement(moving: Moving) {
        standingSince =
            when {
                !moving.told -> null
                moving.going -> null
                else -> standingSince ?: Instant.now()
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

                if (FirebaseAuth.getInstance().currentUser == null) {
                    AppLog.w(TAG, "no session turned up, stopping rather than promising to share")
                    stopSelf()
                }
            }
    }

    private suspend fun firstPosition(mode: CollectionMode): Fix? {
        val priority = requestSpec(mode)?.priority ?: return null

        return positionNow(priority)
    }

    /**
     * Repeats the last position while the mode asks for nothing.
     *
     * The quiet mode is the one the phone reaches by staying at home, and it
     * collects nothing at all: without this the household would go grey on
     * each other's maps every evening, which is the opposite of what standing
     * still means. Counted from the last write rather than from entering the
     * mode, because the phone arrives here with a write already behind it.
     */
    private suspend fun keepSaying(writer: PositionWriter, mode: CollectionMode) {
        while (true) {
            val wrote = wroteAt
            val quiet = if (wrote == null) IDLE_REPEAT else Duration.between(wrote, Instant.now())

            if (quiet < IDLE_REPEAT) {
                delay((IDLE_REPEAT - quiet).toMillis())

                continue
            }

            if (!sayAgain(writer, mode)) {
                // Nothing written yet to repeat, or the write did not reach
                // the database. Either way the next round tries again.
                delay(IDLE_REPEAT.toMillis())
            }
        }
    }

    /**
     * Asks another way while the provider says nothing.
     *
     * The subscription is the only thing that normally writes anything, and it
     * can go quiet for reasons the service has no way of hearing about: the
     * fast mode asks the satellites alone, and a phone on a desk indoors gets
     * no answer from them at all. The card then goes grey on everybody else's
     * map while this service is running perfectly well, which is the one
     * failure that looks exactly like the application being broken.
     *
     * So the silence is measured, and past [rescueAfter] the phone asks once
     * with a coarser accuracy — the network answer, a hundred metres wide, is
     * worth more than an exact one nobody ever hears. The write goes through
     * whatever the filter would have said: what is being reported is not a new
     * place but the fact that the phone is still there to report it.
     */
    private suspend fun rescueWhileSilent(writer: PositionWriter, mode: CollectionMode) {
        val silence = rescueAfter(mode) ?: return
        val priority = rescuePriority(mode) ?: return

        while (true) {
            val quiet = Duration.between(heardAt, Instant.now())

            if (quiet < silence) {
                delay((silence - quiet).toMillis())

                continue
            }

            AppLog.w(TAG, "nothing heard for $quiet in mode $mode; asking a cheaper way")

            // Counted from the asking rather than from an answer, so that a
            // rescue which finds nothing either waits its turn like the rest
            // instead of turning into a loop.
            heardAt = Instant.now()

            val fix = positionNow(priority)

            if (fix != null) {
                writeIfWorth(writer, fix, mode, always = true)

                continue
            }

            noteWhatTheNetworkKnows()
        }
    }

    /**
     * Keeps an answer the phone did not measure, for the rule that weighs the
     * satellites against something they cannot reach.
     *
     * An assistance service answers over the network, from a list of the Wi-Fi
     * around the phone rather than from the sky, and arrives under the name of
     * the satellites like everything else the provider hands over. The name is
     * therefore not what it is taken by — the note is, and a note admitting
     * that nothing was measured is a note from beyond the transmitter's reach.
     *
     * It matters most where there is no second opinion otherwise: a phone with
     * no SIM has no towers to ask, and these answers are the only ones it ever
     * gets that the spoofer did not write.
     */
    private fun rememberASecondOpinion(fix: Fix) {
        if (answeredBy(noteOf(fix.reported?.alongside)) != Answered.Assistance) {
            return
        }

        networkSaid = fix
    }

    /**
     * Keeps the group's list of false places up to date, and on disk.
     *
     * The subscription runs for as long as the service does, whatever mode it
     * is in: the list is read by the rules on every fix, and a list that
     * stopped arriving while the phone was quiet would be a list that is wrong
     * exactly when the phone starts answering again.
     */
    private suspend fun keepTheFalsePlaces() {
        namedFalsePlaces().collect { places ->
            falsePlaces = places
            falsePlaceStore.write(places)
        }
    }

    /**
     * Keeps this group's idea of going round in circles up to date.
     *
     * Nothing is kept on disk for it: a phone that has not reached the
     * settings weighs answers against the limits in the build, which catch a
     * transmitter of any shape and only take a few more answers to do it.
     */
    private suspend fun keepWhatCountsAsCircling() {
        circlingLimits().collect { circleLimits = it }
    }

    /**
     * Keeps an answer for the rule that reads the shape of the run.
     *
     * Only what the satellites answered goes in. An answer the towers gave,
     * and one an assistance service worked out from the Wi-Fi around the
     * phone, are answers from outside the transmitter's reach: mixing them
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
     * Keeps an answer from the towers and the Wi-Fi standing beside the
     * satellites' own.
     *
     * The only rule here that has to run while everything looks right. A
     * transmitter drawing a false road slowly breaks no other rule — not the
     * speed, not the width, not the sensors of a phone in a moving car — and
     * the first moment anything is visibly wrong is the moment the road ends,
     * hours later. So the second opinion is fetched on a clock rather than on
     * suspicion, and the cost of that is one scan every
     * [SECOND_OPINION_ASKED_EVERY].
     *
     * What comes back is never written, only compared: an answer from the
     * towers is worth a district, and the rules that decide what goes in the
     * document are asked about it elsewhere.
     */
    private suspend fun keepASecondOpinion() {
        while (true) {
            val asked =
                withTimeoutOrNull(NETWORK_ANSWER_WAIT.toMillis()) {
                    networkPosition(this@SharingService)
                } ?: lastNetworkPosition(this)

            if (asked != null) {
                networkSaid = asked
            }

            delay(SECOND_OPINION_ASKED_EVERY.toMillis())
        }
    }

    /**
     * Puts the satellites aside and asks the towers and the Wi-Fi instead.
     *
     * Called from the one place a spoofed phone reaches: the branch where an
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
    private suspend fun askTheOtherRadios(writer: PositionWriter, mode: CollectionMode) {
        if (!otherRadiosAreDue(jumpsInARow, askedTheOtherRadiosAt, Instant.now())) {
            return
        }

        // Counted from the asking, so that an answer that never comes does not
        // turn into a scan on every fix.
        askedTheOtherRadiosAt = Instant.now()

        AppLog.w(TAG, "$jumpsInARow answers in a row left out; asking the towers and the Wi-Fi")

        val asked =
            withTimeoutOrNull(NETWORK_ANSWER_WAIT.toMillis()) {
                networkPosition(this@SharingService)
            } ?: lastNetworkPosition(this)

        if (!networkAnswerStands(asked, Instant.now(), falsePlaces, awakeFor())) {
            AppLog.w(TAG, "the other radios had nothing to put in its place: ${saying(asked)}")

            return
        }

        val fix = asked ?: return

        // Kept whatever happens next: an answer that is not worth writing is
        // still worth having as the second opinion, which is what the rules
        // about contradiction and about confirming a width are asked against.
        networkSaid = fix

        // The towers answer about a district and the Wi-Fi about a flat, and
        // whichever answers first is the one that arrives. An answer that
        // merely repeats a place already written, only wider, would move the
        // marker half a kilometre and widen the circle around it for nothing.
        if (!saysMoreThan(fix, lastFix, Instant.now(), awakeFor())) {
            AppLog.i(TAG, "the other radios repeated what is written, only wider: ${saying(fix)}")

            return
        }

        // The one rule that is asked of this answer too, and the reason is
        // what a field measurement showed: a phone lying at home was offered
        // a place four kilometres away, and the answer that came back from
        // the other radios stood within two metres of the false one just
        // rejected.
        // Whatever built it, it was not a receiver that had been carried
        // there, and the sensors are the thing that says so. Everything else
        // the filter asks stays waived, as above.
        if (driftedWhilePutDown(putDown, fix, carryingSince(putDown, stirsHeard, sensorsTell))) {
            AppLog.w(
                TAG,
                "the other radios walked away from a phone nobody touched too: ${saying(fix)}",
            )

            return
        }

        AppLog.i(TAG, "standing on what the other radios say: ${saying(fix)}")

        jumpsInARow = 0
        writeIfWorth(writer, fix, mode, always = true, trusted = true)
    }

    /**
     * What the towers and the Wi-Fi have to say when Play Services has nothing.
     *
     * Written down rather than used: the question being answered here is
     * whether a phone whose satellites are jammed could be placed by its other
     * radios at all, and that has to be known before the collection is moved
     * onto them.
     */
    private suspend fun noteWhatTheNetworkKnows() {
        val asked =
            withTimeoutOrNull(NETWORK_ANSWER_WAIT.toMillis()) {
                networkPosition(this@SharingService)
            }
        val known = lastNetworkPosition(this)

        AppLog.i(TAG, "the network says ${saying(asked)}, and knew ${saying(known)} before that")
    }

    /** How a fix reads in the line above, and how its absence reads. */
    private fun saying(fix: Fix?): String = when (fix) {
        null -> "nothing"

        else ->
            String.format(
                Locale.ROOT,
                "%.5f,%.5f ±%.0f m, %d s old",
                fix.latitude,
                fix.longitude,
                fix.accuracyMetres ?: 0.0,
                Duration.between(fix.takenAt, Instant.now()).seconds,
            )
    }

    /**
     * One position, asked for now; a provider that will not answer is not a
     * reason to stop collecting, and the next round asks again.
     */
    private suspend fun positionNow(priority: Int): Fix? = try {
        currentPosition(this, priority)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        AppLog.w(TAG, "the provider gave no position", error)

        null
    }

    /**
     * The satellites' last answer, put in the record beside the written one.
     *
     * Read from the store rather than asked for, so it costs nothing and works
     * in the modes where the receiver is asleep — which is where the question
     * matters most, since a phone nobody is watching is a phone whose position
     * nobody is checking either.
     *
     * The same answer is not written twice: the store keeps handing over the
     * same fix until the receiver works out another one, and a file full of
     * repeats would hide how often the receiver actually answered.
     */
    private fun noteWhatTheSatellitesLastSaid() {
        val satellites = lastSatellitePosition(this) ?: return
        val stamp =
            satellites.sinceBoot ?: satellites.takenAt.toEpochMilli().let(Duration::ofMillis)

        if (stamp == satellitesLastSaid) {
            return
        }

        satellitesLastSaid = stamp

        SkyLog.note(satellites.takenAt, satellites)
        noteTheDisagreement(satellites)
    }

    /**
     * What the satellites alone say, while the receiver is on anyway.
     *
     * Nothing is written to the database from this and nothing is filtered by
     * it: it is a second opinion put in the record beside the first. The fused
     * provider will not say whether an answer came from the sky or from the
     * Wi-Fi, and the difference is the whole question — a position from the
     * towers that is a kilometre out is a stale database, and a position from
     * the satellites that is a kilometre out is somebody transmitting.
     */
    private suspend fun watchTheSatellites(mode: CollectionMode) {
        if (mode.accuracy != Accuracy.High) {
            return
        }

        satellitePositions(this).collect { fix ->
            SkyLog.note(fix.takenAt, fix)
            noteTheDisagreement(fix)
        }
    }

    /**
     * Writes down what the receiver is hearing while it works out nothing from
     * it.
     *
     * Only where the receiver is being asked often enough for silence to mean
     * something: in the slow modes a minute without an answer is the ordinary
     * state of a phone in a pocket, and rows about it would fill the file with
     * a phone behaving exactly as intended.
     *
     * The phone this was written for hears twenty-one satellites and produces
     * no position at all, and until this ran, that phone wrote nothing into the
     * sky file — the one case where the sky is the entire evidence was the one
     * case with no evidence kept.
     */
    private suspend fun noteTheSkyWhileSilent(mode: CollectionMode) {
        if (mode.accuracy != Accuracy.High) {
            return
        }

        while (true) {
            delay(SKY_NOTED_WHILE_SILENT.toMillis())

            val quiet = Duration.between(heardAt, Instant.now())

            if (quiet < SKY_NOTED_WHILE_SILENT) {
                continue
            }

            radio.noteSkyWithoutFix(Instant.now(), mode.name, quiet)

            // The satellites go on talking while the receiver works out
            // nothing from them, and a phone that is being talked over is
            // exactly the phone whose fixes have stopped: without this, the
            // count of what they said would only be written while things were
            // going well.
            radio.navigationSaying()?.let { AppLog.i(TAG, it) }
        }
    }

    /**
     * Says so when the satellites and the written position are not in the same
     * place, and not more often than a reader can use.
     *
     * The distance is the finding. Two answers about one phone that differ by
     * more than either of them claims to be wide cannot both be right, and
     * which of the two is wrong is not decided here — only that they disagree,
     * with the numbers to show it.
     */
    private fun noteTheDisagreement(satellites: Fix) {
        val written = lastFix ?: return
        val apart = distanceMetres(written, satellites)

        if (apart < SATELLITES_APART_METRES) {
            return
        }

        if (Duration.between(disagreedAt, Instant.now()) < DISAGREEMENT_SAID_EVERY) {
            return
        }

        disagreedAt = Instant.now()

        AppLog.w(
            TAG,
            String.format(
                Locale.ROOT,
                "the satellites put the phone %.0f m from the position being written: " +
                    "%.5f,%.5f ±%.0f m against %.5f,%.5f ±%.0f m",
                apart,
                satellites.latitude,
                satellites.longitude,
                satellites.accuracyMetres ?: 0.0,
                written.latitude,
                written.longitude,
                written.accuracyMetres ?: 0.0,
            ),
        )
    }

    /**
     * Writes down a fix as it arrived, beside what it would take to be true.
     *
     * A line per fix rather than per write: the ones left out are the ones
     * worth reading later, and the distance and the speed are what tell a walk
     * from an answer given by a jammed receiver.
     */
    private fun noteFix(
        fix: Fix,
        mode: CollectionMode,
        jump: JumpReason?,
        agreed: Boolean,
        home: Boolean,
        trusted: Boolean,
    ) {
        val last = lastFix
        val place = String.format(Locale.ROOT, "%.5f,%.5f", fix.latitude, fix.longitude)
        val width = fix.accuracyMetres?.let { String.format(Locale.ROOT, " ±%.0f m", it) }.orEmpty()
        // Which way of knowing answered. Written down beside the place
        // because a position from the towers and a position from the
        // satellites are worth reading differently, and until now the log
        // said nothing about which of the two had just arrived.
        //
        // The name is followed by what the provider's own note said about how
        // it arrived at the answer, where it said anything: the name `gps` has
        // been seen on a position an assistance service handed over with not
        // one satellite audible, and the two read the same until this is put
        // beside them.
        val said = answeredBy(noteOf(fix.reported?.alongside))
        val how = if (said == Answered.Unsaid) "" else ", ${said.word}"
        val source = fix.from?.let { " from $it$how" }.orEmpty()
        val since =
            if (last == null) {
                // Not necessarily the first fix to arrive: while every answer
                // is being left out, none of them becomes the position the
                // next one is measured against.
                "with no position to measure it from"
            } else {
                val moved = distanceMetres(last, fix)
                val speed = impliedSpeedMps(last, fix)
                val pace = speed?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "no"

                String.format(Locale.ROOT, "%.0f m from the last at %s m/s", moved, pace)
            }
        val verdict =
            when {
                jump == null -> ""
                home -> "; a jump ${caughtBy(jump, fix)}, taken: the phone has been here before"
                agreed -> "; a jump ${caughtBy(jump, fix)}, taken because the fix before it agrees"
                jumps.enabled -> "; a jump ${caughtBy(jump, fix)}, left out"
                else -> "; a jump ${caughtBy(jump, fix)}, kept because the filter is off"
            }

        // The clock is asked about the satellites only. An answer from the
        // towers is handed over seconds after it was taken, and its age would
        // read as a receiver whose time has been tampered with.
        val clock = if (trusted) "" else aClockApart(fix.takenAt)

        AppLog.i(TAG, "fix $place$width$source in mode $mode, $since$verdict$clock")

        radio.saying()?.let { AppLog.i(TAG, it) }
        radio.satellitesSaying()?.let { AppLog.i(TAG, it) }
        radio.skySaying()?.let { AppLog.i(TAG, it) }
        radio.driftSaying()?.let { AppLog.i(TAG, it) }
        radio.navigationSaying()?.let { AppLog.i(TAG, it) }

        // The epoch behind the line just written, in its own file. Everything
        // above is one sentence about a minute of averages; this is the sky
        // itself, satellite by satellite, for the questions that come later.
        radio.noteSky(fix.takenAt, fix)
        noteWhatTheSatellitesLastSaid()
    }

    /**
     * Adds a written position to what the phone remembers about itself.
     *
     * Written positions only. An answer left out never becomes part of what
     * counts as familiar, which is what stops an hour of a false signal from
     * teaching the phone that a false place is where it lives.
     */
    private fun rememberThePlace(fix: Fix) {
        val before = trail.knows(fix)

        lastKnown = fix

        trail = trail.remembering(fix)
        trailStore.write(trail)

        if (!before && trail.knows(fix)) {
            AppLog.i(TAG, "this is a place the phone now counts as its own")
        }
    }

    /**
     * What to add about the clock of the fix, when there is anything to add.
     *
     * A receiver takes its time from the satellites, so a false signal can
     * carry a false clock, and that shows before the coordinates do. It cannot
     * stand alone as a verdict — a phone setting its own clock from the same
     * satellites moves with the lie — but it costs nothing to write down.
     */
    private fun aClockApart(takenAt: Instant): String {
        val now = Instant.now()

        if (!clockDisagrees(takenAt, now, CLOCK_TOLERANCE)) {
            return ""
        }

        return "; stamped ${Duration.between(takenAt, now).seconds} s from the phone's own clock"
    }

    /**
     * Drops what the phone believes about where it is.
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

    /** Says which of the rules caught the fix, in the words of the log. */
    private fun caughtBy(jump: JumpReason, fix: Fix): String = when (jump) {
        JumpReason.Width -> "by the width of the answer"

        JumpReason.Speed -> "by the speed it implies"

        JumpReason.Contradiction -> "by two places at one time"

        JumpReason.FalsePlace -> "by landing on ${theFalsePlace(fix)}"

        JumpReason.Unmoved -> "by the phone never having been carried there"

        JumpReason.Drifted -> "by the answer walking away from a phone nobody touched"

        JumpReason.Hurried ->
            "by the speed it claims for a phone nobody touched: ${theSpeedClaimed(fix)}"

        JumpReason.Contradicted ->
            "by the other radios placing the phone ${apartFromTheRadios(fix)}"

        JumpReason.Circling -> "by the shape of the road: ${theCircleDrawn()}"

        JumpReason.Repeating -> "by the road being gone round again: ${theRouteRepeated()}"

        JumpReason.OneBand -> "by what it rests on: ${theBandItRestsOn()}"

        JumpReason.OneTransmitter -> "by the sky it was built out of: ${theSkyItCameFrom()}"

        JumpReason.Settling -> "by the receiver still coming up: ${theReceiverSettling(fix)}"

        JumpReason.TimeApart -> "by the satellites' own time: ${theClockThatStepped()}"
    }

    /**
     * What speed the provider claimed, for the line that says a fix was left
     * out because a phone nobody touched was said to be travelling.
     */
    private fun theSpeedClaimed(fix: Fix): String {
        val speed = fix.reported?.speedMps ?: return "no speed given"

        return String.format(Locale.ROOT, "%.1f m/s", speed)
    }

    /**
     * Which signal's time left the phone's clock and by how much, for the line
     * that says a fix was left out.
     *
     * Both differences are written, against the receiver's clock and against
     * the count since the boot, because they say different things: the first
     * can be moved by the same signal that moved the time, and the second
     * cannot be reached at all. See SatelliteClock.kt.
     */
    private fun theClockThatStepped(): String {
        val stepped = radio.clockThatStepped() ?: return "no signal named"

        return String.format(
            Locale.ROOT,
            "%s %d, %d ns from the receiver's clock and %d ns from the phone's own",
            stepped.signal.constellation.word,
            stepped.signal.id,
            stepped.apart.receiverNanos,
            stepped.apart.sinceBootNanos,
        )
    }

    /**
     * How long the phone had been awake and how much of the sky its answer
     * rests on, for the line that says an answer of the first minutes was left
     * out.
     *
     * Both numbers, because either alone reads as the wrong complaint: the
     * seconds on their own say a phone was restarted, which is not a fault,
     * and the count on its own says a receiver hears nothing, which is what a
     * phone indoors does all day.
     */
    private fun theReceiverSettling(fix: Fix): String = String.format(
        Locale.ROOT,
        "%d s since the boot, %d satellites in the fix",
        fix.sinceBoot?.seconds ?: 0L,
        restsOn.values.sumOf { it.usedInFix },
    )

    /**
     * Which systems the fix was built out of, for the line that says a fix was
     * left out.
     */
    private fun theBandItRestsOn(): String {
        val makeup = makeupOf(restsOn)

        return String.format(
            Locale.ROOT,
            "%d satellites of the GPS band and none of the %d GLONASS heard",
            makeup.bandInFix,
            makeup.glonassHeard,
        )
    }

    /**
     * Why the sky reads as one transmitter's, for the line that says a fix was
     * left out.
     */
    private fun theSkyItCameFrom(): String {
        val says = radio.skySays()
        val alongside =
            listOfNotNull(
                "the far band silent".takeIf { says.otherBandSilent },
                "the satellites moving as one block".takeIf { says.drift == SkyDrift.Together },
            )

        return String.format(
            Locale.ROOT,
            "the loudness %s and %s",
            says.shape.toString().lowercase(),
            alongside.joinToString(" and "),
        )
    }

    /**
     * The route the last answers are going round again, for the line that
     * says a fix was left out.
     */
    private fun theRouteRepeated(): String {
        val round = routeRepeated(lately) ?: return "a route traced again"

        return String.format(
            Locale.ROOT,
            "a route %.0f m across every %d s, within %.0f m of the round before",
            round.reachMetres,
            round.period.seconds,
            round.apartMetres,
        )
    }

    /**
     * The circle the last answers are drawing, for the line that says a fix
     * was left out.
     */
    private fun theCircleDrawn(): String {
        val circle = circleDrawn(lately, circleLimits) ?: return "a circle at a steady speed"

        return String.format(
            Locale.ROOT,
            "a circle of %.0f m at a steady %.0f m/s, %.0f degrees of it so far",
            circle.radiusMetres,
            circle.speedMps,
            circle.turnedDegrees,
        )
    }

    /** What the group calls the place this fix landed on. */
    private fun theFalsePlace(fix: Fix): String =
        falsePlaceFor(fix, falsePlaces)?.named ?: "a place a false signal uses"

    /**
     * How far the towers and the Wi-Fi put the phone from where the satellites
     * just put it, for the line that says a fix was left out.
     */
    private fun apartFromTheRadios(fix: Fix): String {
        val said =
            secondOpinion(fix, networkSaid, Instant.now(), awakeFor())
                ?: return "somewhere else"

        return String.format(
            Locale.ROOT,
            "%.0f m away, against the %.0f m they were allowed",
            said.apartMetres,
            said.allowedMetres,
        )
    }

    /**
     * A write that failed is not a reason to stop collecting: the network comes
     * back, and the next fix carries the same news.
     */
    private suspend fun writeQuietly(
        writer: PositionWriter,
        written: Written,
        mode: CollectionMode,
        // How the phone arrived at this, worked out by the caller: on these
        // phones the fused answer being written carries no note of its own,
        // and what says whether it is the satellites' measurement under
        // another name is the receiver's own last answer, read from the store.
        whence: FoundBy,
    ): Boolean = try {
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
            "wrote a position taken at ${written.fix.takenAt}, found by ${whence.said}" +
                // Said in the line about the write rather than left to the
                // document: what a phone wrote is the one thing a log of a
                // boot cannot be read back from, and the first minutes after
                // a boot are exactly when somebody is reading it.
                if (written.settling) ", with the receiver still settling" else "",
        )

        true
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        AppLog.w(TAG, "could not write the position", error)

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
            batteryPercent = batteryPercent(),
            ownMapOpen = ownMap,
        ),
    )

    private fun batteryPercent(): Int = getSystemService<BatteryManager>()
        ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        ?: FULL_BATTERY

    /**
     * Whether the phone is on a charger, which is what tells a low percentage
     * that is about to grow from one that is about to run out.
     *
     * Read as "plugged into something" rather than through `isCharging`: a
     * phone that has reached a hundred percent stops charging while the cable
     * is still in it, and the reader is asking about the cable.
     */
    private fun charging(): Boolean {
        val battery =
            registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return false

        return battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
    }

    /**
     * The platform checks the location permission again here and answers a
     * missing one with an exception, so a refusal has to be survivable rather
     * than merely unlikely.
     */
    private fun enterForeground(): Boolean = try {
        ServiceCompat.startForeground(
            this,
            SHARING_NOTIFICATION_ID,
            sharingNotification(this),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )

        true
    } catch (refusal: Exception) {
        AppLog.e(TAG, "the platform refused the foreground start", refusal)

        false
    }

    companion object {
        /**
         * How often the mode is reconsidered while the service runs.
         *
         * A viewer who leaves quietly — a closed lid, a flat battery — writes
         * nothing more, so the subscription brings no change and the claim
         * simply ages; the phone learns it has run out only on a review. Once
         * a minute, rather than once a claim, keeps the fifteen-second mode
         * from outliving the last watcher by as long again as the claim
         * itself. A review reads nothing: it only asks the same question of
         * the clock.
         */
        private val MODE_REVIEW: Duration = Duration.ofMinutes(1)

        /** How long a freshly started service waits for the account to be restored. */
        private val SESSION_WAIT: Duration = Duration.ofMinutes(1)

        /** What a phone that will not say counts as, so it is not treated as flat. */
        private const val FULL_BATTERY = 100

        /**
         * How long the network is given to answer when Play Services will not.
         *
         * A tower answers in seconds and a scan of the Wi-Fi around the phone
         * takes a few more; half a minute is long enough for both and short
         * enough to be over before the next round of asking.
         */
        private val NETWORK_ANSWER_WAIT: Duration = Duration.ofSeconds(30)

        const val ACTION_STOP = "io.github.vitalyostanin.geoshare.STOP"

        /** The member being looked for says they have been found. */
        const val ACTION_FOUND = "io.github.vitalyostanin.geoshare.FOUND"

        /** Whoever holds this phone stops its siren before its twenty seconds are up. */
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
