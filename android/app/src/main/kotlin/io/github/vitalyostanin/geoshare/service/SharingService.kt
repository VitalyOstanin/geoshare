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
import io.github.vitalyostanin.geoshare.collection.CLOCK_TOLERANCE
import io.github.vitalyostanin.geoshare.collection.CircleLimits
import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.github.vitalyostanin.geoshare.collection.CollectionState
import io.github.vitalyostanin.geoshare.collection.Constellation
import io.github.vitalyostanin.geoshare.collection.Counted
import io.github.vitalyostanin.geoshare.collection.FalsePlace
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.IDLE_REPEAT
import io.github.vitalyostanin.geoshare.collection.JumpReason
import io.github.vitalyostanin.geoshare.collection.Moving
import io.github.vitalyostanin.geoshare.collection.SECOND_OPINION_ASKED_EVERY
import io.github.vitalyostanin.geoshare.collection.Trail
import io.github.vitalyostanin.geoshare.collection.WATCH_AHEAD
import io.github.vitalyostanin.geoshare.collection.agreementStands
import io.github.vitalyostanin.geoshare.collection.anchorFrom
import io.github.vitalyostanin.geoshare.collection.andThen
import io.github.vitalyostanin.geoshare.collection.answeredBy
import io.github.vitalyostanin.geoshare.collection.chooseMode
import io.github.vitalyostanin.geoshare.collection.circleDrawn
import io.github.vitalyostanin.geoshare.collection.clockDisagrees
import io.github.vitalyostanin.geoshare.collection.comingHome
import io.github.vitalyostanin.geoshare.collection.distanceMetres
import io.github.vitalyostanin.geoshare.collection.falsePlaceFor
import io.github.vitalyostanin.geoshare.collection.fenceMoves
import io.github.vitalyostanin.geoshare.collection.impliedSpeedMps
import io.github.vitalyostanin.geoshare.collection.knows
import io.github.vitalyostanin.geoshare.collection.makeupOf
import io.github.vitalyostanin.geoshare.collection.networkAnswerStands
import io.github.vitalyostanin.geoshare.collection.noteOf
import io.github.vitalyostanin.geoshare.collection.otherRadiosAreDue
import io.github.vitalyostanin.geoshare.collection.remembering
import io.github.vitalyostanin.geoshare.collection.rescueAfter
import io.github.vitalyostanin.geoshare.collection.routeRepeated
import io.github.vitalyostanin.geoshare.collection.saysMoreThan
import io.github.vitalyostanin.geoshare.collection.secondOpinion
import io.github.vitalyostanin.geoshare.collection.shouldWrite
import io.github.vitalyostanin.geoshare.collection.whyAJump
import io.github.vitalyostanin.geoshare.collection.word
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.log.SkyLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
 * A position as it goes into the document: where the phone was, and whether
 * the cable was in it at the moment that was decided.
 *
 * The pair rather than the fix alone, because the same place written with the
 * cable newly in is a different document, while the same place written with
 * nothing changed is not.
 */
private data class Written(val fix: Fix, val charging: Boolean)

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
            if (auth.currentUser != null) {
                sessionWait?.cancel()
                sessionWait = null
                startCollecting()
            }
        }

    /**
     * Everything a write outside the collection loop needs: where the phone
     * was last seen to be, how it was collecting at the time and what writes
     * its documents. Held on the service because the cable is pulled at a
     * moment the loop is not looking.
     */
    private var writer: PositionWriter? = null
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
     * The cable going in or coming out, written down at once.
     *
     * Waiting for the next fix would leave a phone reading "12%" for the
     * fifteen minutes it is already filling up, which is the case the reader
     * most wants told.
     */
    private val power =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                AppLog.i(TAG, "power changed: ${intent.action}")
                writeWherePossible()
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
        collection?.cancel()
        runCatching { unregisterReceiver(power) }
        AppLog.i(TAG, "stopped")
        super.onDestroy()
    }

    /**
     * A position written because something other than the position changed.
     *
     * What changed is the cable, not the place, so the known position goes
     * first and goes at once: asking the provider first would leave the reader
     * looking at "12%" for the half minute the phone is already filling up,
     * which is the case this exists for.
     *
     * The fresh fix follows when the provider answers, and only if it has
     * somewhere new to report — the same fix a second time would be the same
     * document written twice.
     */
    private fun writeWherePossible() {
        val writer = writer ?: return

        lifecycleScope.launch {
            lastFix?.let { known -> writeIfWorth(writer, known, lastMode, always = true) }

            firstPosition(lastMode)?.let { fresh ->
                writeIfWorth(writer, fresh, lastMode, always = true)
            }
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
                )
            }
        // A fix left out does not move the last position, and that is how the
        // marker can be left standing somewhere the phone has long left: a
        // wrong last position turns every true fix that follows into a jump.
        // A second fix that agrees with the one before it is the way out of a
        // width nothing can outlive — and only out of that one, because a
        // spoofed signal draws a road whose fixes all agree, and against the
        // speed rule the clock is the way out anyway. The agreement is not
        // taken on the word of the two wide answers alone: the towers or the
        // Wi-Fi around the phone have to name the same place.
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

        if (jump != null && jumps.enabled && !agreed && !home) {
            candidate = fix
            jumpsInARow += 1

            askTheOtherRadios(writer, mode)

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
        val written = Written(fix, charging())

        // News passes the filter by; a repeat of itself is not news.
        if (written == lastWritten) {
            return false
        }

        if (!writeQuietly(writer, written, mode)) {
            return false
        }

        lastFix = fix
        lastWritten = written
        wroteAt = Instant.now()
        rememberThePlace(fix)

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
        val written = Written(known, charging())

        if (!writeQuietly(writer, written, mode)) {
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

        collection =
            lifecycleScope.launch {
                modes(memberId, device).collectLatest { mode ->
                    AppLog.i(TAG, "collecting in mode $mode")
                    lastMode = mode

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

        val looking = watchers(positionId(memberId, device.id)).onStart { emit(emptyList()) }

        // Who is looking is the screen's business; the service needs only the
        // moments their claims run out.
        return combine(
            looking,
            movement(this),
            standingStill(this, fenceCentre),
            MapOnScreen.state,
            reviews,
        ) { seen, moving, standing, ownMap, _ ->
            noteMovement(moving)
            currentMode(seen.map { it.until }, moving.going, standing, ownMap)
        }.distinctUntilChanged()
    }

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

        if (!networkAnswerStands(asked, Instant.now(), falsePlaces)) {
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
        if (!saysMoreThan(fix, lastFix, Instant.now())) {
            AppLog.i(TAG, "the other radios repeated what is written, only wider: ${saying(fix)}")

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

        JumpReason.Contradicted ->
            "by the other radios placing the phone ${apartFromTheRadios(fix)}"

        JumpReason.Circling -> "by the shape of the road: ${theCircleDrawn()}"

        JumpReason.Repeating -> "by the road being gone round again: ${theRouteRepeated()}"

        JumpReason.OneBand -> "by what it rests on: ${theBandItRestsOn()}"
    }

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
            secondOpinion(fix, networkSaid, Instant.now()) ?: return "somewhere else"

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
    ): Boolean = try {
        writer.write(written.fix, mode, batteryPercent(), written.charging)
        AppLog.i(TAG, "wrote a position taken at ${written.fix.takenAt}")

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
         * No rarer than a claim to be looking runs out. A viewer who leaves
         * quietly — a closed lid, a flat battery — writes nothing more, so the
         * subscription brings no change and the claim simply ages; reviewing
         * more slowly than it ages would keep the phone in the fifteen-second
         * mode for minutes after the last watcher was gone.
         */
        private val MODE_REVIEW: Duration = WATCH_AHEAD

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

        fun start(context: Context) = ContextCompat.startForegroundService(
            context,
            Intent(context, SharingService::class.java),
        )

        fun stop(context: Context) = context.startService(
            Intent(context, SharingService::class.java).setAction(ACTION_STOP),
        )
    }
}
