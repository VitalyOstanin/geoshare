package io.github.vitalyostanin.geoshare.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import io.github.vitalyostanin.geoshare.R

const val SHARING_NOTIFICATION_ID = 1

// Renamed rather than changed in place: whether a channel badges the launcher
// icon is fixed once the channel exists, and the first one did not.
private const val CHANNEL_ID = "sharing_badged"

/** The channel before it badged the icon, removed so the settings list it once. */
private const val OLD_CHANNEL_ID = "sharing"

/**
 * The notification a foreground service is required to show.
 *
 * It is the only thing that says the application is running, so it carries the
 * way back into the application and a way to stop sharing without opening it.
 */
fun sharingNotification(context: Context): Notification {
    createChannel(context)

    val open = openAppIntent(context)

    val stop = serviceAction(context, SharingService.ACTION_STOP, REQUEST_STOP_SHARING)

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
    // The badge says on the home screen that the place is going out,
    // which the notification alone says only inside the shade.
    context.getSystemService<NotificationManager>()?.deleteNotificationChannel(OLD_CHANNEL_ID)

    ensureChannel(
        context,
        CHANNEL_ID,
        R.string.sharing_channel_name,
        NotificationManager.IMPORTANCE_LOW,
        R.string.sharing_channel_description,
    )
}
