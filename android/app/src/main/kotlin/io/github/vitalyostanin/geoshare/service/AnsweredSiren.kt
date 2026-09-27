package io.github.vitalyostanin.geoshare.service

import android.content.Context
import java.time.Instant

private const val FILE = "siren"
private const val KEY_CALLED_AT = "answeredCalledAt"

/**
 * The call this phone last sounded for, by the server's stamp on it.
 *
 * Kept on the phone rather than read from the document: the service restarted
 * in the middle of a call hears the same document again, and must not sound a
 * second time for a call it already answered.
 */
class AnsweredSiren(context: Context) {
    private val store = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var calledAt: Instant?
        get() = store.getLong(KEY_CALLED_AT, -1).takeIf { it >= 0 }?.let(Instant::ofEpochMilli)
        set(value) = store.edit().putLong(KEY_CALLED_AT, value?.toEpochMilli() ?: -1).apply()
}
