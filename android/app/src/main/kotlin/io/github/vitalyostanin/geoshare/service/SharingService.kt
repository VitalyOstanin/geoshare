package io.github.vitalyostanin.geoshare.service

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import io.github.vitalyostanin.geoshare.collection.Accuracy
import io.github.vitalyostanin.geoshare.collection.Asked
import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.github.vitalyostanin.geoshare.collection.CollectionState
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.chooseMode
import io.github.vitalyostanin.geoshare.collection.orbitReference
import io.github.vitalyostanin.geoshare.collection.rescueAccuracy
import io.github.vitalyostanin.geoshare.collection.rescueAfter
import io.github.vitalyostanin.geoshare.collection.sameAnswer
import io.github.vitalyostanin.geoshare.collection.whomToAsk
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.logFailure
import io.github.vitalyostanin.geoshare.map.runCatchingFailure
import io.github.vitalyostanin.geoshare.project.Connections
import io.github.vitalyostanin.geoshare.update.checkForUpdateIfDue
import kotlinx.coroutines.CompletableDeferred
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
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

private const val TAG = "geoshare.service"

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
    private val skyNotes: SkyNotes by lazy {
        SkyNotes(this, { radio }, { sieve.lastFix }, { silence.heardAt })
    }

    /** A line in the log for every fix as it arrives. */
    private val fixLog: FixLog by lazy {
        FixLog(radio, skyNotes, { jumps.enabled }, { sieve.leftOutEvidence() })
    }

    /** What the towers and the Wi-Fi say, kept beside the satellites' own answer. */
    private val opinion = NetworkOpinion(this)

    /**
     * What the device's own sensors say about it being carried.
     *
     * Whether there are sensors to ask is read once: sensors do not come and
     * go, and the permission is asked for before the service starts
     * collecting.
     */
    private val carried by lazy { BeingCarried(sensorsNoticeMovement(this)) }

    /**
     * Which fixes become positions, and everything the rules remember to
     * decide that; the service only feeds it and keeps it alive.
     */
    private val sieve: FixSieve by lazy {
        FixSieve(
            writer = writer,
            radio = radio,
            opinion = opinion,
            carried = carried,
            jumps = jumps,
            sources = sources,
            silence = silence,
            fenceCentre = fenceCentre,
            noteFix = { fix, last, mode, verdict -> fixLog.note(fix, last, mode, verdict) },
            satellites = { lastSatellitePosition(this) },
            charging = { status.charging() },
            batteryPercent = { status.batteryPercent() },
            keepTrail = { trailStore.write(it) },
            awake = ::awakeFor,
        )
    }

    /** Where this device keeps being, kept across restarts of the process. */
    private val trailStore by lazy { TrailStore(this) }

    /**
     * Completed once the [sieve] holds what the files had: the trail, the
     * anchor and the false places. They are read off the main thread, so the
     * work that reads or extends them waits here instead of racing the read.
     */
    private val restored = CompletableDeferred<Unit>()

    /**
     * The copy of this group's false places that survives the process being
     * killed.
     *
     * Read from the file first and from the database after, because the fix
     * these places are there to catch is the first one of the session: a device
     * waiting for its subscription has nothing to catch it with.
     */
    private val falsePlaceStore by lazy { FalsePlaceStore(this) }

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
        RadioWatch(this) { orbitReference(opinion.said, sieve.lastFix, Instant.now()) }
    }
    private var lastMode: CollectionMode = CollectionMode.Standby

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
        ModeReview(this, lifecycleScope, links, fenceCentre) { untils, moving, inside, ownMap ->
            carried.hear(moving, Instant.now())
            currentMode(untils, moving.going, inside, ownMap)
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

        // Read off the main thread, and applied before anything that reads or
        // appends to them is let go: the collection waits on [restored] and
        // the keepers are only launched once it is complete, so a fix is never
        // judged against an empty trail and the list a group sends is never
        // overwritten by the older file.
        lifecycleScope.launch {
            val (remembered, places) =
                withContext(Dispatchers.IO) { trailStore.read() to falsePlaceStore.read() }

            sieve.restore(remembered, places)
            restored.complete(Unit)

            AppLog.i(
                TAG,
                "the device remembers ${sieve.trail.fixes.size} places it has been" +
                    if (sieve.lastFix == null) "" else ", and measures from the last of them",
            )

            launch { checkForUpdatesDaily() }
            launch { keepTheFalsePlaces() }
            launch { noticeBeingCarried() }
            launch { keepWhatCountsAsCircling() }
            launch { keepTheOrbits(this@SharingService) { radio.orbits = it } }
        }
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
                runCatchingFailure {
                    endSearch(link.phoneId, link.connection)
                }.onFailure { refusal ->
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
                runCatchingFailure {
                    stopSiren(link.phoneId, link.memberId, link.connection)
                }.onFailure { refusal ->
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
                restored.await()
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
            silence.keepSaying { sieve.sayAgain(mode) }

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
            sieve.writeIfWorth(fix, mode, always = true)
        }

        coroutineScope {
            silence.heardAt = Instant.now()
            launch { rescueWhileSilent(mode) }
            launch { skyNotes.watchTheSatellites(mode) }
            launch { skyNotes.noteTheSkyWhileSilent(mode) }
            launch { opinion.keep(mode) }

            positionUpdates(this@SharingService, spec, asked).collect { fix ->
                silence.heardAt = Instant.now()

                if (sameAnswer(first, fix)) {
                    return@collect
                }

                opinion.freshen(mode)
                sieve.writeIfWorth(fix, mode)
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
    private suspend fun rescueWhileSilent(mode: CollectionMode) {
        val silent = rescueAfter(mode) ?: return
        val priority = rescuePriority(mode) ?: return
        val asked = asking(rescueAccuracy(mode)) ?: return

        silence.rescue(
            silent,
            mode,
            ask = { positionNow(priority, asked) },
            write = { fix -> sieve.writeIfWorth(fix, mode, always = true) },
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
            sieve.falsePlaces = places
            // Off the main thread: the list arrives whenever a member edits it.
            withContext(Dispatchers.IO) { falsePlaceStore.write(places) }
        }
    }

    /**
     * Looks for a newer release once a day for as long as the service runs.
     *
     * The service is what stays alive for days while the screen is rarely
     * opened, so the check made at the screen's start would otherwise be the
     * only one. [checkForUpdateIfDue] decides by the stored time of the last
     * check, so a restart of the service does not ask GitHub again. It is
     * looked at every hour rather than every day: a check the screen made an
     * hour before this loop started would otherwise push the next one almost
     * two days away.
     */
    private suspend fun checkForUpdatesDaily() {
        while (true) {
            checkForUpdateIfDue(this)
            delay(UPDATE_LOOK_INTERVAL.toMillis())
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
        }.collect { sieve.circleLimits = it }
    }

    /** Who to ask with this accuracy, given the device and the switches. */
    private fun asking(accuracy: Accuracy): Asked? =
        whomToAsk(accuracy, playServices, sources.leftOut, providers)

    /**
     * One position, asked for now; a provider that will not answer is not a
     * reason to stop collecting, and the next round asks again.
     */
    private suspend fun positionNow(priority: Int, asked: Asked): Fix? =
        runCatchingFailure { currentPosition(this, priority, asked) }.getOrElse { error ->
            AppLog.w(TAG, "the provider gave no position", error)

            null
        }

    private fun currentMode(
        untils: List<Instant>,
        moving: Boolean,
        insideGeofence: Boolean,
        ownMap: Boolean,
    ): CollectionMode = chooseMode(
        CollectionState(
            watchersUntil = untils,
            now = Instant.now(),
            localTime = LocalTime.now(),
            insideGeofence = insideGeofence,
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

        /** How often the daily update check looks whether its day is up. */
        private val UPDATE_LOOK_INTERVAL: Duration = Duration.ofHours(1)

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
