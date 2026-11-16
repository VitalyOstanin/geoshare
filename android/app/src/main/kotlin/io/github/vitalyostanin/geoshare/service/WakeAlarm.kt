package io.github.vitalyostanin.geoshare.service

import android.app.AlarmManager
import android.content.Context
import android.os.SystemClock
import java.time.Duration

private const val REQUEST_WAKE = 2

/**
 * The one alarm that wakes [SharingService] while the processor sleeps.
 *
 * Not an exact alarm: those need a permission the member would have to grant
 * for a thing that is allowed to be a few minutes late. This one is delivered
 * even in deep sleep, but at most about once in nine minutes, which is why
 * [io.github.vitalyostanin.geoshare.collection.nextWake] never asks for sooner.
 * Setting it again replaces the one before, so there is never more than one.
 */
internal class WakeAlarm(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    private fun pending() = serviceAction(context, SharingService.ACTION_WAKE, REQUEST_WAKE)

    /** Rings once, [after] from now; null takes the alarm down. */
    fun set(after: Duration?) {
        if (after == null) {
            cancel()

            return
        }

        alarms?.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + after.toMillis(),
            pending(),
        )
    }

    fun cancel() {
        alarms?.cancel(pending())
    }
}
