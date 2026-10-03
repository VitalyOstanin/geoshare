package io.github.vitalyostanin.geoshare.service

import android.app.NotificationManager
import android.content.Context
import android.os.PowerManager
import androidx.core.content.getSystemService
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.SIREN_SECONDS
import io.github.vitalyostanin.geoshare.map.Siren
import io.github.vitalyostanin.geoshare.map.shouldSound
import io.github.vitalyostanin.geoshare.permission.devicePermissions
import io.github.vitalyostanin.geoshare.project.GroupConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.Instant

private const val TAG = "geoshare.service"

/** How long the siren's notification waits for the caller's name before a short id. */
private val SIREN_NAME_WAIT: Duration = Duration.ofSeconds(3)

/**
 * This phone's side of a siren: sounds it when somebody calls, for twenty
 * seconds, and stops early when the call is stopped — from a caller's screen
 * or from the notification on the phone itself.
 *
 * Kept by the sharing service for as long as it collects, and launched into
 * the service's own scope, so the sound outlives nothing the service does not.
 */
class SirenAnswer(private val context: Context, private val scope: CoroutineScope) {
    private val player by lazy { SirenPlayer(context) }

    /** The twenty seconds of one call sounding, outlived by nothing else. */
    private var sounding: Job? = null

    /**
     * Sounds the phone when somebody calls its siren, and stops it when
     * somebody stops the call.
     *
     * Each call sounds once: the call answered is kept on the phone, so the
     * same document heard again after a restart stays quiet, and a call more
     * than a minute old is left alone — the phone switched back on later
     * rings for nobody.
     */
    suspend fun answer(memberId: String, phoneId: String, connection: GroupConnection) {
        val answered = AnsweredSiren(context)

        phoneSiren(phoneId, connection).collect { called ->
            if (called == null) {
                return@collect
            }

            if (called.stoppedBy != null) {
                sounding?.cancel()

                return@collect
            }

            if (!shouldSound(called, answered.calledAt(connection.projectId), Instant.now())) {
                return@collect
            }

            answered.keep(connection.projectId, called.calledAt)
            sounding?.cancel()
            // Apart from the subscription: the phone's own "heard" write
            // comes back through it, and must not cut the sound short.
            sounding = scope.launch { sound(memberId, phoneId, called, connection) }
        }
    }

    /** One call: twenty seconds of sound and the notification that stops it early. */
    private suspend fun sound(
        memberId: String,
        phoneId: String,
        called: Siren,
        connection: GroupConnection,
    ) {
        val until = Instant.now().plusSeconds(SIREN_SECONDS)

        AppLog.i(TAG, "the siren sounds")

        var screen: PowerManager.WakeLock? = null

        try {
            player.start()

            // On its own: a write waits for the server, and a phone without a
            // network still sounds.
            scope.launch {
                try {
                    heardSiren(phoneId, connection)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (refusal: Exception) {
                    AppLog.w(TAG, "could not say the siren was heard", refusal)
                }
            }

            val callerName =
                if (called.calledBy == memberId) {
                    context.getString(R.string.siren_called_by_you)
                } else {
                    withTimeoutOrNull(SIREN_NAME_WAIT.toMillis()) {
                        memberDisplayName(called.calledBy, connection)
                    } ?: called.calledBy.take(SHORTENED_ID_LENGTH)
                }

            val group = groupNameForNotice(context, connection)

            if (devicePermissions(context).notificationsGranted) {
                context.getSystemService<NotificationManager>()?.notify(
                    SIREN_NOTIFICATION_ID,
                    sirenNotification(context, callerName, group, until),
                )
            }

            // After the notification, so the screen comes on with it there.
            screen = lightScreen(context, Duration.ofSeconds(SIREN_SECONDS))

            delay(Duration.between(Instant.now(), until).toMillis().coerceAtLeast(0))
        } finally {
            player.stop()
            screen?.letGo()
            cancelSirenNotification(context)
            AppLog.i(TAG, "the siren is quiet")
        }
    }

    /** Cuts the call sounding now short, leaving the next one to sound. */
    fun quieten() {
        sounding?.cancel()
    }

    /** Quiet, and nothing left on screen: the service stops collecting. */
    fun stop() {
        sounding?.cancel()
        player.stop()
        cancelSirenNotification(context)
    }
}
