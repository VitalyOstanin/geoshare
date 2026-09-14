package io.github.vitalyostanin.geoshare.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.vitalyostanin.geoshare.log.AppLog

private const val TAG = "geoshare.boot"

/**
 * Brings the service back after the two things that stop it without anybody
 * meaning to: a restart of the phone, and an update of the application.
 *
 * Without this the phone goes quiet until somebody opens the application, and
 * a position nobody is updating is worse than no position at all: it looks
 * current. The update case cost two phones several hours of silence on
 * 14.09.2026 — installing a build stops every process of the package, and
 * nothing here was listening for it.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val after = whatStoppedIt(intent.action) ?: return

        val refusal = refusalToStart(startConditions(context, fromBackground = true))

        if (refusal != null) {
            AppLog.i(TAG, "not restarting after $after: $refusal")

            return
        }

        AppLog.i(TAG, "restarting after $after")
        SharingService.start(context)
    }
}

/**
 * Which of the two the phone is coming back from, or nothing for anything
 * else that reaches this receiver.
 *
 * Named rather than merely accepted so that the log says which happened: a
 * phone that restarted and a phone that was updated look the same in a file of
 * positions and mean different things when the collection stops afterwards.
 */
internal fun whatStoppedIt(action: String?): String? = when (action) {
    Intent.ACTION_BOOT_COMPLETED -> "boot"

    // Sent to the application whose own package has just been replaced. The
    // platform delivers it once, to this package only, and it is the one
    // moment at which an updated application may start itself again.
    Intent.ACTION_MY_PACKAGE_REPLACED -> "an update"

    else -> null
}
