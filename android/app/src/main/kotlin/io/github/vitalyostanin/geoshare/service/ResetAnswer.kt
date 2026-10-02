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
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.shouldForget
import io.github.vitalyostanin.geoshare.permission.devicePermissions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val TAG = "geoshare.service"

private const val FILE = "reset"
private const val KEY_ASKED_AT = "doneAskedAt"
private const val KEY_BY = "doneBy"
private const val KEY_DONE_AT = "doneAt"

const val RESET_NOTIFICATION_ID = 5

private const val CHANNEL_ID = "reset"

/** How long the notification waits for the asker's name before a short id. */
private val RESET_NAME_WAIT: Duration = Duration.ofSeconds(3)

private val RESET_AT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * The last request this phone carried out: its server stamp, to leave the same
 * document alone after a restart, and who asked and when it was done, for the
 * line under the settings' own "forget" button.
 */
class DoneReset(context: Context) {
    private val store = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    val askedAt: Instant?
        get() = store.getLong(KEY_ASKED_AT, -1).takeIf { it >= 0 }?.let(Instant::ofEpochMilli)

    /** Who asked, as a name, or null while nobody has asked from elsewhere. */
    val by: String? get() = store.getString(KEY_BY, null)

    val doneAt: Instant?
        get() = store.getLong(KEY_DONE_AT, -1).takeIf { it >= 0 }?.let(Instant::ofEpochMilli)

    fun keep(askedAt: Instant, by: String, doneAt: Instant) {
        store.edit()
            .putLong(KEY_ASKED_AT, askedAt.toEpochMilli())
            .putString(KEY_BY, by)
            .putLong(KEY_DONE_AT, doneAt.toEpochMilli())
            .apply()
    }
}

/** The line under the settings' "forget" button, or null while nobody has asked from elsewhere. */
fun doneResetLine(context: Context): String? {
    val done = DoneReset(context)
    val by = done.by ?: return null
    val at = done.doneAt ?: return null

    return context.getString(
        R.string.reset_done_line,
        by,
        RESET_AT.format(at.atZone(ZoneId.systemDefault())),
    )
}

/**
 * This phone's side of a request to forget its place: the same flag the
 * settings' own button raises, so the next fix is weighed as if the phone had
 * just been switched on, then a word back to the asking screen and a quiet
 * notification saying who asked.
 */
suspend fun answerResets(
    context: Context,
    memberId: String,
    phoneId: String,
    jumps: JumpFilterSetting,
) {
    val done = DoneReset(context)

    phoneReset(phoneId).collect { asked ->
        if (asked == null || !shouldForget(asked, done.askedAt, Instant.now())) {
            return@collect
        }

        jumps.askToForget()
        AppLog.i(TAG, "asked from elsewhere to forget the place")

        val name =
            if (asked.askedBy == memberId) {
                context.getString(R.string.siren_called_by_you)
            } else {
                withTimeoutOrNull(RESET_NAME_WAIT.toMillis()) {
                    memberDisplayName(asked.askedBy)
                } ?: asked.askedBy.take(SHORTENED_ID_LENGTH)
            }
        val now = Instant.now()

        done.keep(asked.askedAt, name, now)

        try {
            resetDone(phoneId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (refusal: Exception) {
            AppLog.w(TAG, "could not say the place was forgotten", refusal)
        }

        if (devicePermissions(context).notificationsGranted) {
            context.getSystemService<NotificationManager>()
                ?.notify(RESET_NOTIFICATION_ID, resetNotification(context, name, now))
        }
    }
}

private fun resetNotification(context: Context, name: String, at: Instant): Notification {
    val channel =
        NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reset_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.reset_channel_description)
        }

    context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)

    val open =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

    return NotificationCompat
        .Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getString(R.string.reset_notification_title, name))
        .setContentText(
            context.getString(
                R.string.reset_notification_text,
                RESET_AT.format(at.atZone(ZoneId.systemDefault())),
            ),
        )
        .setContentIntent(open)
        .setAutoCancel(true)
        .build()
}
