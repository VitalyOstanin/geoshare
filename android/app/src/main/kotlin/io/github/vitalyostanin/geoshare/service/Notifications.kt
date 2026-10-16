package io.github.vitalyostanin.geoshare.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.core.content.getSystemService
import io.github.vitalyostanin.geoshare.MainActivity

/**
 * Request codes of the pending intents the notifications carry. Two intents
 * with the same action and the same code are one intent to the system, so a
 * code only has to differ between notifications that share an action; they are
 * named here to keep the values the notifications already ship with.
 */
internal const val REQUEST_OPEN_APP = 0
internal const val REQUEST_STOP_SHARING = 0
internal const val REQUEST_FOUND = 0
internal const val REQUEST_SIREN_OFF = 1

/** The way back into the application, for the body of a notification. */
internal fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
    context,
    REQUEST_OPEN_APP,
    Intent(context, MainActivity::class.java),
    PendingIntent.FLAG_IMMUTABLE,
)

/** An answer the notification sends to [SharingService] without opening the application. */
internal fun serviceAction(context: Context, action: String, requestCode: Int): PendingIntent =
    PendingIntent.getService(
        context,
        requestCode,
        Intent(context, SharingService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE,
    )

/**
 * Creates the channel, or leaves it as it is: Android keeps what the member
 * has set on an existing channel and only the name and description follow
 * a new language. [configure] is for what the common arguments do not cover.
 */
internal fun ensureChannel(
    context: Context,
    id: String,
    @StringRes name: Int,
    importance: Int,
    @StringRes description: Int? = null,
    configure: NotificationChannel.() -> Unit = {},
) {
    val channel =
        NotificationChannel(id, context.getString(name), importance).apply {
            if (description != null) this.description = context.getString(description)
            configure()
        }

    context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
}
