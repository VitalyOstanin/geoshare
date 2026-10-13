package io.github.vitalyostanin.geoshare.service

import android.content.Context
import java.time.Instant

private const val FILE = "siren"
private const val KEY_CALLED_AT = "answeredCalledAt"

/**
 * The call this device last sounded for in each group, by the server's stamp
 * on it.
 *
 * Kept on the device rather than read from the document: the service restarted
 * in the middle of a call hears the same document again, and must not sound a
 * second time for a call it already answered. Kept by group, because a call
 * in one group says nothing about the calls in another.
 */
class AnsweredSiren(context: Context) {
    private val store = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /**
     * The call answered last in [projectId]'s group. What a build that knew
     * one group kept stands in until that group's own is written.
     */
    fun calledAt(projectId: String): Instant? = (
        store.readLong("$KEY_CALLED_AT.$projectId", -1).takeIf { it >= 0 }
            ?: store.readLong(KEY_CALLED_AT, -1).takeIf { it >= 0 }
        )?.let(Instant::ofEpochMilli)

    fun keep(projectId: String, calledAt: Instant) {
        store.edit().putLong("$KEY_CALLED_AT.$projectId", calledAt.toEpochMilli()).apply()
    }
}
