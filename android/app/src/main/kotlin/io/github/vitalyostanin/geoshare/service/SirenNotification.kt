package io.github.vitalyostanin.geoshare.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import io.github.vitalyostanin.geoshare.MainActivity
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.map.SIREN_SECONDS
import java.time.Duration
import java.time.Instant

const val SIREN_NOTIFICATION_ID = 4

private const val CHANNEL_ID = "siren"

/**
 * Told to the member whose phone was made to sound, on top of the alarm
 * itself. The alarm is what carries the noise, so the notification's own
 * sound is turned off in the channel; otherwise the two would fight over the
 * same moment. Full-screen intent is left out on purpose: on a locked
 * screen this is meant to be seen, not to seize it.
 *
 * Laid out by hand rather than by the system: a locked screen shows a
 * notification folded, and the buttons of an ordinary one appear only once it
 * is unfolded. Somebody who has found the phone should not have to know that
 * to stop the noise, so the folded one carries its own button.
 */
fun sirenNotification(
    context: Context,
    callerName: String,
    /** The group that called, named only while the device is in more than one. */
    group: String?,
    until: Instant,
): Notification {
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

    val title = context.getString(R.string.siren_notification_title, callerName)
    val text =
        if (group == null) {
            context.getString(R.string.siren_notification_text)
        } else {
            context.getString(R.string.siren_notification_text_in_group, group)
        }

    val folded =
        RemoteViews(context.packageName, R.layout.siren_notification_collapsed).apply {
            setTextViewText(R.id.siren_title, title)
            setTextViewText(R.id.siren_off, context.getString(R.string.siren_off_short))
            setOnClickPendingIntent(R.id.siren_off, turnOff)
        }

    // The action below is the unfolded one's button, so this layout has none.
    val unfolded =
        RemoteViews(context.packageName, R.layout.siren_notification_expanded).apply {
            setTextViewText(R.id.siren_title, title)
            setTextViewText(R.id.siren_text, text)
        }

    return NotificationCompat
        .Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.DecoratedCustomViewStyle())
        .setCustomContentView(folded)
        .setCustomBigContentView(unfolded)
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
        .setTimeoutAfter(Duration.ofSeconds(SIREN_SECONDS).toMillis())
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
