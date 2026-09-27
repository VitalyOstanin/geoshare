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
import io.github.vitalyostanin.geoshare.map.Search
import java.time.ZoneId
import java.time.format.DateTimeFormatter

const val SEARCH_NOTIFICATION_ID = 2

private const val CHANNEL_ID = "search"

/** When a search ends: the day as well as the time, the same reading a permission's end has. */
private val SOUGHT_ENDS_AT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM HH:mm")

/**
 * Told to the member being looked for, on top of the notification the service
 * is required to show, for as long as the search stands.
 *
 * A channel of its own, and a high one: sharing quietly is not news, but the
 * whole group watching for this phone is, and a member who has put the phone
 * down should be woken by it the way they would be by a call.
 */
fun searchNotification(context: Context, search: Search, openedByName: String): Notification {
    createSearchChannel(context)

    val open =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

    val found =
        PendingIntent.getService(
            context,
            0,
            Intent(context, SharingService::class.java).setAction(SharingService.ACTION_FOUND),
            PendingIntent.FLAG_IMMUTABLE,
        )

    return NotificationCompat
        .Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getString(R.string.sought_notification_title))
        .setContentText(
            context.getString(
                R.string.sought_notification_text,
                openedByName,
                SOUGHT_ENDS_AT.format(search.endsAt.atZone(ZoneId.systemDefault())),
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

/** Takes the notification down: the search was called off, or the member said they were found. */
fun cancelSearchNotification(context: Context) {
    context.getSystemService<NotificationManager>()?.cancel(SEARCH_NOTIFICATION_ID)
}

private fun createSearchChannel(context: Context) {
    val channel =
        NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.sought_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.sought_channel_description)
        }

    context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
}
