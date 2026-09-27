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

const val SHARING_NOTIFICATION_ID = 1

private const val CHANNEL_ID = "sharing"

/**
 * The notification a foreground service is required to show.
 *
 * It is the only thing that says the application is running, so it carries the
 * way back into the application and a way to stop sharing without opening it.
 */
fun sharingNotification(context: Context): Notification {
    createChannel(context)

    val open =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

    val stop =
        PendingIntent.getService(
            context,
            0,
            Intent(context, SharingService::class.java).setAction(SharingService.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )

    return NotificationCompat
        .Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getString(R.string.sharing_notification_title))
        .setContentText(context.getString(R.string.sharing_notification_text))
        .setContentIntent(open)
        .addAction(0, context.getString(R.string.sharing_notification_stop), stop)
        .setOngoing(true)
        // The notification is a requirement rather than news; it should not
        // interrupt anything.
        .setSilent(true)
        .build()
}

private fun createChannel(context: Context) {
    val channel =
        NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.sharing_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.sharing_channel_description)
            setShowBadge(false)
        }

    context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
}
