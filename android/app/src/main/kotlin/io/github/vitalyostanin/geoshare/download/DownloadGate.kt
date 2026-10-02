package io.github.vitalyostanin.geoshare.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.log.AppLog
import java.time.Instant

const val DOWNLOAD_NOTIFICATION_ID = 6

private const val CHANNEL_ID = "downloads"
private const val ACTION_GO = "io.github.vitalyostanin.geoshare.download.GO"
private const val ACTION_NOT_NOW = "io.github.vitalyostanin.geoshare.download.NOT_NOW"
private const val TAG = "geoshare.download"

/** What a download asks the member in roaming: a title and a line under it. */
data class RoamingQuestion(val title: String, val text: String)

/**
 * Whether a download the application starts by itself may go now.
 *
 * In roaming with roaming switched off, the member is asked by a notification
 * and the answer is "no" until they say "download"; the caller listens to
 * [DownloadSetting.changes] and asks here again when it changes. A "not now"
 * keeps the question away for a day.
 */
fun mayDownloadNow(
    context: Context,
    question: RoamingQuestion,
    now: Instant = Instant.now(),
): Boolean {
    val setting = DownloadSetting(context)

    return when (downloadVerdict(networkKind(context), setting.allowed)) {
        DownloadVerdict.Go -> true

        DownloadVerdict.Wait -> false

        DownloadVerdict.AskFirst -> {
            if (setting.takeGrant()) {
                return true
            }

            if (!setting.waitingInRoaming && mayAskAgain(setting.declinedAt, now)) {
                setting.waitingInRoaming = true
                ask(context, question)
                AppLog.i(TAG, "roaming: asked whether to download")
            }

            false
        }
    }
}

/**
 * The network the device stands on now.
 *
 * Under a VPN the device's own network is the tunnel, which says nothing
 * about roaming; the networks underneath it are read instead, the unmetered
 * kind first.
 */
fun networkKind(context: Context): NetworkKind {
    val connectivity = context.getSystemService<ConnectivityManager>() ?: return NetworkKind.None
    val active =
        connectivity.activeNetwork?.let(connectivity::getNetworkCapabilities)
            ?: return NetworkKind.None

    if (!active.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
        return kindOf(active)
    }

    @Suppress("DEPRECATION")
    val underneath =
        connectivity.allNetworks
            .mapNotNull(connectivity::getNetworkCapabilities)
            .filter {
                !it.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                    it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
            .map(::kindOf)

    return underneath.minByOrNull { it.ordinal } ?: kindOf(active)
}

private fun kindOf(capabilities: NetworkCapabilities): NetworkKind = when {
    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkKind.WiFi

    !capabilities.hasCapability(
        NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING,
    ) -> NetworkKind.Roaming

    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> NetworkKind.WiFi

    else -> NetworkKind.Mobile
}

private fun ask(context: Context, question: RoamingQuestion) {
    val manager = context.getSystemService<NotificationManager>() ?: return

    manager.createNotificationChannel(
        NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.downloads_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = context.getString(R.string.downloads_channel_description) },
    )

    val notification =
        NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(question.title)
            .setContentText(question.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(question.text))
            .addAction(0, context.getString(R.string.downloads_go), answer(context, ACTION_GO))
            .addAction(
                0,
                context.getString(R.string.downloads_not_now),
                answer(context, ACTION_NOT_NOW),
            )
            .setDeleteIntent(answer(context, ACTION_NOT_NOW))
            .build()

    manager.notify(DOWNLOAD_NOTIFICATION_ID, notification)
}

private fun answer(context: Context, action: String): PendingIntent = PendingIntent.getBroadcast(
    context,
    action.hashCode(),
    Intent(context, DownloadAnswerReceiver::class.java).setAction(action),
    PendingIntent.FLAG_IMMUTABLE,
)

/** Writes down the member's answer to the roaming question. */
class DownloadAnswerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val setting = DownloadSetting(context)

        when (intent.action) {
            ACTION_GO -> setting.grantOnce()
            ACTION_NOT_NOW -> setting.decline(Instant.now())
            else -> return
        }

        context.getSystemService<NotificationManager>()?.cancel(DOWNLOAD_NOTIFICATION_ID)
        AppLog.i(TAG, "roaming: answered ${intent.action?.substringAfterLast('.')}")
    }
}
