package io.github.vitalyostanin.geoshare.service

import android.content.Context
import android.content.Intent
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
import io.github.vitalyostanin.geoshare.collection.chooseMode
import io.github.vitalyostanin.geoshare.collection.shouldWrite
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.ownSharedWith
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
     * Who this member lets read their position, as the profile last said.
     * Empty until the first answer arrives, which means a position written in
     * that first moment is readable by nobody — the next one, a few seconds
     * later, carries the permissions.
     */
    private var sharedWith: Map<String, Instant> = emptyMap()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP) {
            AppLog.i(TAG, "stopping on request")
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
        collection?.cancel()
        AppLog.i(TAG, "stopped")
        super.onDestroy()
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
            AppLog.w(TAG, "no session, nothing to write as")

            return
        }

        val device = DeviceIdentity(this)
        val writer = PositionWriter(memberId, device)

        lifecycleScope.launch { ownSharedWith(memberId).collect { sharedWith = it } }

        collection =
            lifecycleScope.launch {
                var last: Fix? = null

                modes(memberId, device).collectLatest { mode ->
                    AppLog.i(TAG, "collecting in mode $mode")

                    val spec = requestSpec(mode)

                    if (spec == null) {
                        // Nothing to ask for; the next mode will arrive on its
                        // own, and until then the receiver stays off.
                        return@collectLatest
                    }

                    // Entering a mode is news: somebody started looking, or the
                    // night ended. Neither should wait for the next batch.
                    firstPosition(mode)?.let { fix ->
                        if (shouldWrite(last, fix) && writeQuietly(writer, fix, mode)) {
                            last = fix
                        }
                    }

                    positionUpdates(this@SharingService, spec).collect { fix ->
                        if (shouldWrite(last, fix) && writeQuietly(writer, fix, mode)) {
                            last = fix
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

        val watchers =
            watchersUntil(positionId(memberId, device.id)).onStart { emit(emptyList()) }

        return combine(watchers, reviews) { untils, _ -> currentMode(untils) }
            .distinctUntilChanged()
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
        fix: Fix,
        mode: CollectionMode,
    ): Boolean = try {
        writer.write(fix, mode, batteryPercent(), sharedWith)
        AppLog.i(TAG, "wrote a position taken at ${fix.takenAt}")

        true
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        AppLog.w(TAG, "could not write the position", error)

        false
    }

    private fun currentMode(watchers: List<Instant>): CollectionMode = chooseMode(
        CollectionState(
            watchersUntil = watchers,
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
        /** How often the mode is reconsidered while the service runs. */
        private val MODE_REVIEW: Duration = Duration.ofMinutes(5)

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
