package io.github.vitalyostanin.geoshare.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import io.github.vitalyostanin.geoshare.MainActivity
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.map.SIREN_SECONDS
import java.time.Instant

const val SIREN_NOTIFICATION_ID = 4

private const val CHANNEL_ID = "siren"

/**
 * Told to the member whose phone was made to sound, on top of the alarm
 * itself. The alarm is what carries the noise, so the notification's own
 * sound is turned off in the channel; otherwise the two would fight over the
 * same moment. Full-screen intent is left out on purpose: on a locked
 * screen this is meant to be seen, not to seize it.
 */
fun sirenNotification(context: Context, callerName: String, until: Instant): Notification {
    createSirenChannel(context)

    val open =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

    val turnOff =
        PendingIntent.getService(
            context,
            1,
            Intent(context, SharingService::class.java).setAction(SharingService.ACTION_SIREN_OFF),
            PendingIntent.FLAG_IMMUTABLE,
        )

    return NotificationCompat
        .Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getString(R.string.siren_notification_title, callerName))
        .setContentText(context.getString(R.string.siren_notification_text))
        .setContentIntent(open)
        .addAction(0, context.getString(R.string.siren_off), turnOff)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setOngoing(true)
        // Counts down to the end in the notification itself, the "17 s left"
        // the mockup shows, without the service showing it again every second.
        .setWhen(until.toEpochMilli())
        .setUsesChronometer(true)
        .setChronometerCountDown(true)
        .setTimeoutAfter(SIREN_SECONDS * 1000)
        .build()
}

/** Takes the notification down: the siren stopped, on its own or by hand. */
fun cancelSirenNotification(context: Context) {
    context.getSystemService<NotificationManager>()?.cancel(SIREN_NOTIFICATION_ID)
}

private fun createSirenChannel(context: Context) {
    val channel =
        NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.siren_channel),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            setSound(null, null)
        }

    context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
}
