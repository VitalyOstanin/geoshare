package io.github.vitalyostanin.geoshare.service

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import io.github.vitalyostanin.geoshare.log.AppLog

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
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP) {
            AppLog.i(TAG, "stopping on request")
            stopSelf()

            return START_NOT_STICKY
        }

        return if (enterForeground()) {
            AppLog.i(TAG, "running")
            // The service is restarted without its intent: there is nothing in
            // it that the service needs beyond the stop action.
            START_STICKY
        } else {
            stopSelf()

            START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        AppLog.i(TAG, "stopped")
        super.onDestroy()
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
