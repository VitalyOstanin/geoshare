package io.github.vitalyostanin.geoshare.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.canStopSharing
import io.github.vitalyostanin.geoshare.managed.ManagedConfiguration
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map

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
 * While the managed configuration locks sharing the stop action is left out: a
 * button that only says no is worse than none.
 */
fun sharingNotification(context: Context): Notification {
    createChannel(context)

    val open = openAppIntent(context)

    val builder = NotificationCompat
        .Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getString(R.string.sharing_notification_title))
        .setContentText(context.getString(R.string.sharing_notification_text))
        .setContentIntent(open)
        .setOngoing(true)
        // The notification is a requirement rather than news; it should not
        // interrupt anything.
        .setSilent(true)

    if (canStopSharing(ManagedConfiguration.current)) {
        val stop = serviceAction(context, SharingService.ACTION_STOP, REQUEST_STOP_SHARING)

        builder.addAction(0, context.getString(R.string.sharing_notification_stop), stop)
    }

    return builder.build()
}

/**
 * Shows the notification again whenever the lock on sharing changes, so the stop
 * action appears or goes while the service runs rather than at its next start.
 * The first value is skipped: the service has just posted the notification
 * itself. Runs until its scope is cancelled.
 */
suspend fun keepNotificationInStep(context: Context, build: () -> Notification) {
    ManagedConfiguration.state
        .map { canStopSharing(it) }
        .distinctUntilChanged()
        .drop(1)
        .collect {
            context.getSystemService<NotificationManager>()
                ?.notify(SHARING_NOTIFICATION_ID, build())
        }
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
