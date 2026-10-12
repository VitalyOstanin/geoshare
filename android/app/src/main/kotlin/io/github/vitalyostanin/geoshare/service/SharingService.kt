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
import io.github.vitalyostanin.geoshare.collection.Accuracy
import io.github.vitalyostanin.geoshare.collection.Answered
import io.github.vitalyostanin.geoshare.collection.Asked
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
import io.github.vitalyostanin.geoshare.collection.Grounds
import io.github.vitalyostanin.geoshare.collection.IDLE_REPEAT
import io.github.vitalyostanin.geoshare.collection.Judgement
import io.github.vitalyostanin.geoshare.collection.JumpReason
import io.github.vitalyostanin.geoshare.collection.LEFT_OUT_BY_SOURCE
import io.github.vitalyostanin.geoshare.collection.PutDown
import io.github.vitalyostanin.geoshare.collection.STIRS_KEPT
import io.github.vitalyostanin.geoshare.collection.SkySays
import io.github.vitalyostanin.geoshare.collection.Stir
import io.github.vitalyostanin.geoshare.collection.Trail
import io.github.vitalyostanin.geoshare.collection.anchorFrom
import io.github.vitalyostanin.geoshare.collection.andThen
import io.github.vitalyostanin.geoshare.collection.answeredBy
import io.github.vitalyostanin.geoshare.collection.carryingSince
import io.github.vitalyostanin.geoshare.collection.choiceFor
import io.github.vitalyostanin.geoshare.collection.chooseMode
import io.github.vitalyostanin.geoshare.collection.clockDisagrees
import io.github.vitalyostanin.geoshare.collection.distanceMetres
import io.github.vitalyostanin.geoshare.collection.driftedWhilePutDown
import io.github.vitalyostanin.geoshare.collection.fenceMoves
import io.github.vitalyostanin.geoshare.collection.foundBy
import io.github.vitalyostanin.geoshare.collection.impliedSpeedMps
import io.github.vitalyostanin.geoshare.collection.judge
import io.github.vitalyostanin.geoshare.collection.knows
import io.github.vitalyostanin.geoshare.collection.networkAnswerStands
import io.github.vitalyostanin.geoshare.collection.noteOf
import io.github.vitalyostanin.geoshare.collection.orbitReference
import io.github.vitalyostanin.geoshare.collection.otherRadiosAreDue
import io.github.vitalyostanin.geoshare.collection.pinnedWhileWalking
import io.github.vitalyostanin.geoshare.collection.putDownAt
import io.github.vitalyostanin.geoshare.collection.remembering
import io.github.vitalyostanin.geoshare.collection.rescueAccuracy
import io.github.vitalyostanin.geoshare.collection.rescueAfter
import io.github.vitalyostanin.geoshare.collection.said
import io.github.vitalyostanin.geoshare.collection.sameAnswer
import io.github.vitalyostanin.geoshare.collection.saysMoreThan
import io.github.vitalyostanin.geoshare.collection.secondOpinionDue
import io.github.vitalyostanin.geoshare.collection.secondOpinionEvery
import io.github.vitalyostanin.geoshare.collection.secondOpinionWithEachFix
import io.github.vitalyostanin.geoshare.collection.shouldWrite
import io.github.vitalyostanin.geoshare.collection.walkNote
import io.github.vitalyostanin.geoshare.collection.walkingNow
import io.github.vitalyostanin.geoshare.collection.watchedUntil
import io.github.vitalyostanin.geoshare.collection.whomToAsk
import io.github.vitalyostanin.geoshare.collection.word
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.PublishedProfile
import io.github.vitalyostanin.geoshare.map.stands
import io.github.vitalyostanin.geoshare.membership.Presence
import io.github.vitalyostanin.geoshare.membership.presenceBeforeAnswer
import io.github.vitalyostanin.geoshare.project.Connections
import io.github.vitalyostanin.geoshare.project.GroupConnection
import io.github.vitalyostanin.geoshare.project.RemovedHere
import io.github.vitalyostanin.geoshare.project.SeenOnRoster
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.util.Locale

private const val TAG = "geoshare.service"

/**
 * How often the device says how it is when nothing else has made it say so.
 *
 * On the beat a device at rest writes its position again, for the same reason:
 * under a transmitter no position is written at all, and this is the only
 * news that keeps the marker out of the grey. An hour let it go grey for half
 * of every hour, and the card then read a device that is working and being
 * lied to as a device that has gone quiet.
 */
private val STATUS_EVERY: Duration = IDLE_REPEAT

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

    /**
     * The groups the device is signed in to, each with what writes into it
     * and listens to it, whenever that changes.
     *
     * Followed rather than read once: the answer at the moment the service
     * starts is not the answer a second later, a group can be joined or left
     * while it runs, and a service holding up a notification that says it is
     * sharing has to be either sharing or gone.
     */
    private val links = MutableStateFlow<List<GroupLink>>(emptyList())

    /** What listens to each group for a siren, a search or a reset, by link. */
    private val listening = mutableMapOf<GroupLink, Job>()

    /** Every write outside the screens, carried into each linked group. */
    private val writer = EveryGroup { links.value }

    /** The alarm stream at full volume while a call to this device sounds. */
    private val sirens by lazy { SirenAnswer(this, lifecycleScope) }

    /** What the satellites themselves are saying, apart from the written fix. */
    private val skyNotes by lazy { SkyNotes(this, { radio }, { lastFix }, { silence.heardAt }) }

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
                AppLog.w(TAG, "no Play Services on this phone; the platform is asked instead")
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

    /**
     * The cable going in or coming out, said at once.
     *
     * Waiting for the next fix would leave a device reading "12%" for the
     * fifteen minutes it is already filling up, which is the case the reader
     * most wants told — and under a transmitter there is no next fix at all.
     */
    private var powerSettling: Job? = null

    private val power =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                AppLog.i(TAG, "power changed: ${intent.action}")
                // A loose cable says connected and disconnected several times
                // a second, and each would be a write into every group: only
                // where it settles is news.
                powerSettling?.cancel()
                powerSettling =
                    lifecycleScope.launch {
                        delay(POWER_SETTLES.toMillis())
                        tellHowThePhoneIs()
                    }
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
        lifecycleScope.launch { followTheGroups() }
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
            "the phone remembers ${trail.fixes.size} places it has been" +
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
        AppLog.i(TAG, "this phone says it has been found")

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
                    AppLog.w(
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
        AppLog.i(TAG, "the siren is stopped on the phone itself")
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
                    AppLog.w(TAG, "could not say the siren was stopped in $link", refusal)
                }
            }
        }
    }

    override fun onDestroy() {
        if (usable) {
            radio.stop()
            unlink(links.value)
            links.value = emptyList()
            stopCollecting()
        }

        AppLog.i(TAG, "stopped")
        super.onDestroy()
    }

    /**
     * Follows the groups and who is signed in to each, and keeps a link to
     * every group with a session: made when a session turns up, taken down
     * when it ends or the group is left. The collection runs while there is
     * at least one link and stops when the last one goes.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun followTheGroups() {
        Connections.all.flatMapLatest { connections ->
            if (connections.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    connections.map { connection ->
                        // A group the device was taken off, or has not got
                        // into yet, refuses every write; it is left alone
                        // rather than written into and refused once a
                        // minute. Founding or joining one republishes the
                        // groups, and this is asked again from the start.
                        // Until the first answer only a group once stood in
                        // is written into; any other is held unlinked rather
                        // than left out of the combination, which would hold
                        // up every group with it.
                        signedInTo(connection).flatMapLatest { uid ->
                            if (uid == null) {
                                flowOf(connection to null)
                            } else {
                                val wasIn =
                                    SeenOnRoster.has(
                                        this@SharingService,
                                        connection.projectId,
                                        uid,
                                    )

                                presenceIn(this@SharingService, connection, uid)
                                    .map<Presence, Presence?> { it }
                                    .onStart { emit(presenceBeforeAnswer(wasIn)) }
                                    .map { presence ->
                                        connection to
                                            uid.takeIf {
                                                presence == Presence.IN ||
                                                    presence == Presence.UNKNOWN
                                            }
                                    }
                            }
                        }
                    },
                ) {
                    it.toList()
                }
            }
        }.collect { signedIn -> relink(signedIn) }
    }

    private fun relink(signedIn: List<Pair<GroupConnection, String?>>) {
        val device = DeviceIdentity(this)
        val kept = links.value
        val next =
            signedIn.mapNotNull { (connection, uid) ->
                uid?.let {
                    kept.firstOrNull { link -> link.serves(connection, uid) }
                        ?: GroupLink(connection, uid, device)
                }
            }

        unlink(kept.filter { it !in next })
        links.value = next
        next.filter { it !in kept }.forEach(::listenTo)

        when {
            next.isEmpty() && kept.isNotEmpty() -> {
                AppLog.i(TAG, "signed out of every group, no longer collecting")
                stopCollecting()
                awaitSession()
            }

            next.isNotEmpty() -> {
                sessionWait?.cancel()
                sessionWait = null
                startCollecting()
            }
        }
    }

    /**
     * What the service does for one group besides writing into it: hears a
     * siren, a search or a request to forget the place there, and takes back
     * what the device's previous id left in it.
     */
    private fun listenTo(link: GroupLink) {
        AppLog.i(TAG, "writing into $link")

        listening[link] =
            lifecycleScope.launch {
                val device = DeviceIdentity(this@SharingService)

                LeavingHere.forget(link.connection.projectId)
                launch { signOutWhenRemoved(link, device) }
                // Once, after the move to the system's id: the old position
                // would otherwise stay on the map beside the new one.
                launch { leavePreviousId(link.memberId, device, link.connection) }
                launch {
                    keepSearchNotification(this@SharingService, link.phoneId, link.connection)
                }
                launch { sirens.answer(link.memberId, link.phoneId, link.connection) }
                launch {
                    answerResets(
                        this@SharingService,
                        link.memberId,
                        link.phoneId,
                        jumps,
                        link.connection,
                    )
                }
            }
    }

    /**
     * Signs out of [link]'s group once another device of the member has
     * removed this one from "My devices".
     *
     * Writing on would undo the removal: the next status puts the device's
     * line back, and with it the marker on everybody's map. A device removed
     * that way has usually changed hands, so it stops there rather than asks.
     */
    private suspend fun signOutWhenRemoved(link: GroupLink, device: DeviceIdentity) {
        val projectId = link.connection.projectId

        ownLine(link.connection, link.memberId, device.id).goneAfterListed().firstOrNull() ?: return

        if (LeavingHere.has(projectId)) {
            return
        }

        AppLog.i(TAG, "removed from $link by another device, signing out of it")
        RemovedHere.note(this, projectId)
        PublishedProfile(this).forget()
        link.connection.auth.signOut()
    }

    private fun unlink(gone: List<GroupLink>) {
        for (link in gone) {
            AppLog.i(TAG, "no longer writing into $link")
            listening.remove(link)?.cancel()
        }
    }

    /**
     * Takes down everything [startCollecting] set up: the collection, the
     * cable receiver and whatever it was showing or sounding. What listens to
     * each group is taken down by [unlink], group by group.
     */
    private fun stopCollecting() {
        collection?.cancel()
        collection = null
        runCatching { unregisterReceiver(power) }
        // [keepSearchNotification] is cancelled along with [collection] and
        // leaves nothing to take its own notification down; the device may
        // still be being looked for when the collection stops.
        cancelSearchNotification(this)
        sirens.stop()
    }

    /**
     * Says how the device is, apart from where it is: in touch now, in this
     * mode, this charged, on a cable or off it.
     *
     * Not through the position, and not past the filter: a device whose
     * answers are all being thrown away is still in touch and still on its
     * cable, and a reader deciding whether it has gone quiet or run flat needs
     * to hear that. A failure costs nothing but the news, which the next mode,
     * cable or [STATUS_EVERY] says again.
     */
    private fun tellHowThePhoneIs() {
        if (links.value.isEmpty()) {
            return
        }

        val mode = lastMode
        val percent = batteryPercent()
        val cable = charging()

        lifecycleScope.launch {
            try {
                writer.tellStatus(mode, percent, cable)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                AppLog.w(TAG, "could not say how the phone is", error)
            }
        }
    }

    /**
     * Says how the device is every [STATUS_EVERY], whatever else it is or is
     * not writing: the moment it was last in touch is what tells a working
     * device whose position is held back from one that has gone quiet.
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
        val judged = whatToMakeOf(fix, trusted)

        // Every fix that arrives is written down, whether or not it is written
        // out: what the provider answers where it is being jammed is the thing
        // to be read afterwards, and a filtered log would answer questions
        // about the filter instead.
        noteFix(fix, mode, judged.jump, judged.agreed, judged.home, trusted)

        carried.noticeAnAnswerThatWillNotFollow(fix, judged.whence)

        if (judged.leftOutAsJump(jumps.enabled)) {
            candidate = fix
            jumpsInARow += 1

            tellThemAnswersAreBeingLeftOut(writer, judged.jump?.name.orEmpty())

            askTheOtherRadios(writer, mode)

            return false
        }

        // Left out the same way a jump is, and for a reason of the same kind:
        // the member has said this is not a source to take into account. So it
        // moves no marker, becomes no last position and reaches nobody — while
        // staying in the log, where the question of what the provider was
        // saying is answered.
        if (!sources.uses(judged.whence)) {
            AppLog.i(
                TAG,
                "left out a position found by ${judged.whence.said}: " +
                    "this phone was told not to take that source into account",
            )

            // The switch as well as the fact, because "a source was switched
            // off" answers none of what a member opens the card for.
            tellThemAnswersAreBeingLeftOut(
                writer,
                "$LEFT_OUT_BY_SOURCE:${choiceFor(judged.whence)?.name.orEmpty()}",
            )

            return false
        }

        candidate = null
        jumpsInARow = 0

        return writeAndRemember(writer, fix, mode, judged, always)
    }

    /**
     * What is made of a fix before it is written or left out: the service's
     * own memory brought up to date with it, then read once into the
     * [Grounds] the decision is taken on.
     */
    private fun whatToMakeOf(fix: Fix, trusted: Boolean): Judgement {
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

        return judge(fix, trusted, grounds)
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
        val written = Written(fix, charging(), judged.settling)

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
    private suspend fun sayAgain(writer: EveryGroup, mode: CollectionMode): Boolean {
        val known = lastFix ?: lastKnown ?: return false
        // The same position said again carries the same note about the
        // receiver: it is a statement about the answer, and the answer has not
        // changed. Worked out afresh here it would say something about an
        // epoch this position never came out of.
        val written = Written(known, charging(), lastWritten?.settling == true)

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

        hearTheCable()

        collection =
            lifecycleScope.launch {
                launch { keepTellingHowThePhoneIs() }

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
        tellHowThePhoneIs()

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

    /** Listens for the cable going in or coming out, which is said at once. */
    private fun hearTheCable() {
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
                "the other radios walked away from a phone nobody touched too: ${saying(fix)}",
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

                home ->
                    "; a jump ${caughtBy(jump, fix, leftOutEvidence())}, taken: the phone has " +
                        "been here before"

                agreed ->
                    "; a jump ${caughtBy(jump, fix, leftOutEvidence())}, taken because the fix " +
                        "before it agrees"

                jumps.enabled -> "; a jump ${caughtBy(jump, fix, leftOutEvidence())}, left out"

                else ->
                    "; a jump ${caughtBy(jump, fix, leftOutEvidence())}, kept because the " +
                        "filter is off"
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
        radio.orbitsSaying()?.let { AppLog.i(TAG, it) }

        // The epoch behind the line just written, in its own file. Everything
        // above is one sentence about a minute of averages; this is the sky
        // itself, satellite by satellite, for the questions that come later.
        radio.noteSky(fix.takenAt, fix)
        skyNotes.noteWhatTheSatellitesLastSaid()
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
            AppLog.i(TAG, "this is a place the phone now counts as its own")
        }
    }

    /**
     * What to add about the clock of the fix, when there is anything to add.
     *
     * A receiver takes its time from the satellites, so a false signal can
     * carry a false clock, and that shows before the coordinates do. It cannot
     * stand alone as a verdict — a device setting its own clock from the same
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
     * Whether the device is on a charger, which is what tells a low percentage
     * that is about to grow from one that is about to run out.
     *
     * Read as "plugged into something" rather than through `isCharging`: a
     * device that has reached a hundred percent stops charging while the cable
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
            sharingNotification(inAppLanguage()),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )

        true
    } catch (refusal: Exception) {
        AppLog.e(TAG, "the platform refused the foreground start", refusal)

        false
    }

    companion object {
        /** How long the cable has to stay in or out before it is said. */
        private val POWER_SETTLES: Duration = Duration.ofSeconds(2)

        /** How long a freshly started service waits for the account to be restored. */
        private val SESSION_WAIT: Duration = Duration.ofMinutes(1)

        /** What a device that will not say counts as, so it is not treated as flat. */
        private const val FULL_BATTERY = 100

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
