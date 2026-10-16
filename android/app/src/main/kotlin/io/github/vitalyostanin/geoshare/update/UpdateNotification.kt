package io.github.vitalyostanin.geoshare.update

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import io.github.vitalyostanin.geoshare.MainActivity
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.permission.granted
import io.github.vitalyostanin.geoshare.service.ensureChannel

/** The extra that tells [MainActivity] the notification was tapped: show the update. */
const val EXTRA_OPEN_UPDATE = "open_update"

const val UPDATE_NOTIFICATION_ID = 3

private const val CHANNEL_ID = "updates"

/**
 * Tells the member that [version] can be installed. A channel of its own at
 * the default importance: news worth a sound, never worth a pop-up over
 * whatever the member is doing.
 *
 * Returns whether the notification went up. Without the notification
 * permission nothing is shown and the caller must not count the version as
 * announced, so that the member is told once they allow it.
 */
fun showUpdateNotification(context: Context, version: String): Boolean {
    if (!notificationsAllowed(context)) return false

    createUpdateChannel(context)

    val open =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).putExtra(EXTRA_OPEN_UPDATE, true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    val notification =
        NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.update_notification_title, version))
            .setContentText(context.getString(R.string.update_notification_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

    context.getSystemService<NotificationManager>()?.notify(UPDATE_NOTIFICATION_ID, notification)

    return true
}

/** Before Android 13 there is no runtime permission, only the member's switch for the app. */
private fun notificationsAllowed(context: Context): Boolean = (
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        granted(context, Manifest.permission.POST_NOTIFICATIONS)
    ) &&
    NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun createUpdateChannel(context: Context) {
    ensureChannel(
        context,
        CHANNEL_ID,
        R.string.update_channel_name,
        NotificationManager.IMPORTANCE_DEFAULT,
        R.string.update_channel_description,
    )
}
