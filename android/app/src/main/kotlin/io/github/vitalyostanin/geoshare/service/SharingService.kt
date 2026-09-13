package io.github.vitalyostanin.geoshare.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.github.vitalyostanin.geoshare.collection.CollectionState
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.WATCH_AHEAD
import io.github.vitalyostanin.geoshare.collection.chooseMode
import io.github.vitalyostanin.geoshare.collection.shouldWrite
import io.github.vitalyostanin.geoshare.log.AppLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

private const val TAG = "geoshare.service"

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
    ): Boolean {
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
                        // Nothing to ask for; the next mode will arrive on its
                        // own, and until then the receiver stays off.
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

                    positionUpdates(this@SharingService, spec).collect { fix ->
                        writeIfWorth(writer, fix, mode)
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
        return combine(looking, reviews) { seen, _ -> currentMode(seen.map { it.until }) }
            .distinctUntilChanged()
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

        return try {
            currentPosition(this, priority)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            AppLog.w(TAG, "no position to start from", error)

            null
        }
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

    private fun currentMode(untils: List<Instant>): CollectionMode = chooseMode(
        CollectionState(
            watchersUntil = untils,
            now = Instant.now(),
            localTime = LocalTime.now(),
            // The geofence arrives with the next change; until then the phone
            // never counts as standing still.
            insideGeofence = false,
            moving = false,
            batteryPercent = batteryPercent(),
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
