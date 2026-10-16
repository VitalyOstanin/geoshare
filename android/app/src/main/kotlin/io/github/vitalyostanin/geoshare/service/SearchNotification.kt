package io.github.vitalyostanin.geoshare.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.map.Search
import io.github.vitalyostanin.geoshare.map.endsAtText
import java.time.ZoneId

const val SEARCH_NOTIFICATION_ID = 2

private const val CHANNEL_ID = "search"

/**
 * Told to the member being looked for, on top of the notification the service
 * is required to show, for as long as the search stands.
 *
 * A channel of its own, and a high one: sharing quietly is not news, but the
 * whole group watching for this device is, and a member who has put the device
 * down should be woken by it the way they would be by a call.
 */
fun searchNotification(
    context: Context,
    search: Search,
    openedByName: String,
    /** The group looking, named only while the device is in more than one. */
    group: String?,
): Notification {
    createSearchChannel(context)

    val open = openAppIntent(context)

    val found = serviceAction(context, SharingService.ACTION_FOUND, REQUEST_FOUND)

    return NotificationCompat
        .Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(
            if (group == null) {
                context.getString(R.string.sought_notification_title)
            } else {
                context.getString(R.string.sought_notification_title_in_group, group)
            },
        )
        .setContentText(
            context.getString(
                R.string.sought_notification_text,
                openedByName,
                endsAtText(
                    search.endsAt.atZone(ZoneId.systemDefault()),
                    context.resources.configuration.locales[0],
                    DateFormat.is24HourFormat(context),
                ),
            ),
        )
        .setContentIntent(open)
        .addAction(0, context.getString(R.string.sought_found), found)
        .setOngoing(true)
        // Shown again every minute while the search stands; the sound and the
        // pop-up belong to the first showing only, not to each of those.
        .setOnlyAlertOnce(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()
}

/**
 * Takes the notification of [projectId]'s search down — the search was called
 * off, or the member said they were found — or every group's when it is null.
 */
fun cancelSearchNotification(context: Context, projectId: String? = null) {
    val notifications = context.getSystemService<NotificationManager>() ?: return

    if (projectId != null) {
        notifications.cancel(projectId, SEARCH_NOTIFICATION_ID)

        return
    }

    notifications.activeNotifications
        .filter { it.id == SEARCH_NOTIFICATION_ID }
        .forEach { notifications.cancel(it.tag, it.id) }
}

private fun createSearchChannel(context: Context) {
    ensureChannel(
        context,
        CHANNEL_ID,
        R.string.sought_channel_name,
        NotificationManager.IMPORTANCE_HIGH,
        R.string.sought_channel_description,
    )
}
