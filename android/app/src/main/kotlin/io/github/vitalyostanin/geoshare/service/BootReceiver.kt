package io.github.vitalyostanin.geoshare.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.vitalyostanin.geoshare.log.AppLog

private const val TAG = "geoshare.boot"

/**
 * Brings the service back after a restart.
 *
 * Without this the phone would go quiet after every reboot until somebody
 * opened the application, and a position nobody is updating is worse than no
 * position at all: it looks current.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val refusal = refusalToStart(startConditions(context, fromBackground = true))

        if (refusal != null) {
            AppLog.i(TAG, "not restarting after boot: $refusal")

            return
        }

        AppLog.i(TAG, "restarting after boot")
        SharingService.start(context)
    }
}
