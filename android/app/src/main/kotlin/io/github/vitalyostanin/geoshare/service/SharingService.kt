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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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

        val writer = PositionWriter(memberId)

        collection =
            lifecycleScope.launch {
                var last: Fix? = null

                // Something to show straight away, before the scheduled
                // request has had a chance to batch anything.
                val first = currentMode().let { mode -> firstPosition(mode)?.let { it to mode } }

                if (first != null && writeQuietly(writer, first.first, first.second)) {
                    last = first.first
                }

                while (isActive) {
                    val mode = currentMode()
                    val spec = requestSpec(mode)

                    if (spec == null) {
                        AppLog.i(TAG, "mode $mode asks for nothing")
                        delay(MODE_REVIEW.toMillis())

                        continue
                    }

                    AppLog.i(TAG, "collecting in mode $mode")

                    withTimeoutOrNull(MODE_REVIEW.toMillis()) {
                        positionUpdates(this@SharingService, spec).collect { fix ->
                            if (shouldWrite(last, fix)) {
                                if (writeQuietly(writer, fix, mode)) {
                                    last = fix
                                }
                            }
                        }
                    }
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
        fix: Fix,
        mode: CollectionMode,
    ): Boolean = try {
        writer.write(fix, mode, batteryPercent())
        AppLog.i(TAG, "wrote a position taken at ${fix.takenAt}")

        true
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        AppLog.w(TAG, "could not write the position", error)

        false
    }

    private fun currentMode(): CollectionMode = chooseMode(
        CollectionState(
            // Viewers and the geofence arrive with the subscription; until
            // then the phone keeps to the standby and night modes.
            watchersUntil = emptyList(),
            now = Instant.now(),
            localTime = LocalTime.now(),
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
