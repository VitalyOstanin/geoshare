package io.github.vitalyostanin.geoshare.download

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Instant

private const val FILE = "downloads"
private const val KEY_DECLINED_AT = "declinedAt"
private const val KEY_GRANTED = "grantedOnce"
private const val KEY_WAITING = "waitingInRoaming"

/** The member's network choice, and where the roaming question stands. */
data class DownloadState(
    val allowed: Set<DownloadNetwork>,
    /** Asked about a download in roaming, and not answered yet. */
    val waitingInRoaming: Boolean,
    /** Answered "download", and the download has not gone yet. */
    val granted: Boolean = false,
)

/**
 * Over which networks the device downloads on its own, and what the member
 * last answered when asked in roaming.
 *
 * Kept in a store of its own: the settings screen writes the switches, the
 * answer to the notification is written by a receiver with no screen at all,
 * and the download itself reads both from the service.
 */
class DownloadSetting(context: Context) {
    private val store = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    val allowed: Set<DownloadNetwork>
        get() =
            DownloadNetwork.entries
                .filter { store.getBoolean(it.name, it in DEFAULT_DOWNLOAD_NETWORKS) }
                .toSet()

    fun allow(network: DownloadNetwork, allowed: Boolean) {
        store.edit().putBoolean(network.name, allowed).apply()
    }

    var declinedAt: Instant?
        get() = store.getLong(KEY_DECLINED_AT, 0L).takeIf { it != 0L }?.let(Instant::ofEpochMilli)
        set(value) = store.edit().putLong(KEY_DECLINED_AT, value?.toEpochMilli() ?: 0L).apply()

    var waitingInRoaming: Boolean
        get() = store.getBoolean(KEY_WAITING, false)
        set(value) = store.edit().putBoolean(KEY_WAITING, value).apply()

    /** The member said "download" to the question: one download may go. */
    fun grantOnce() {
        store.edit().putBoolean(KEY_GRANTED, true).putBoolean(KEY_WAITING, false).apply()
    }

    /** Spends the one download the member allowed, if there is one. */
    fun takeGrant(): Boolean {
        if (!store.getBoolean(KEY_GRANTED, false)) {
            return false
        }

        store.edit().putBoolean(KEY_GRANTED, false).apply()

        return true
    }

    fun decline(at: Instant) {
        store.edit().putLong(
            KEY_DECLINED_AT,
            at.toEpochMilli(),
        ).putBoolean(KEY_WAITING, false).apply()
    }

    fun state(): DownloadState = DownloadState(
        allowed = allowed,
        waitingInRoaming = waitingInRoaming,
        granted = store.getBoolean(KEY_GRANTED, false),
    )

    /**
     * The state as it stands, and again whenever anything in it changes.
     *
     * Also what a waiting download listens to: the member's answer arrives
     * through the receiver, and the download goes as soon as it is written.
     */
    fun changes(): Flow<DownloadState> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(state())
        }

        store.registerOnSharedPreferenceChangeListener(listener)
        send(state())

        awaitClose { store.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()
}
