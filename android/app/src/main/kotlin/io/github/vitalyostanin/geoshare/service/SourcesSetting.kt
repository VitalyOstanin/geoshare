package io.github.vitalyostanin.geoshare.service

import android.content.Context
import android.content.SharedPreferences
import io.github.vitalyostanin.geoshare.collection.FoundBy
import io.github.vitalyostanin.geoshare.collection.SourceChoice
import io.github.vitalyostanin.geoshare.collection.usesWhatWasFoundBy
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

private const val FILE = "collection"
private const val KEY_LEFT_OUT = "sourcesLeftOut"

/**
 * Which sources of a position this device takes into account.
 *
 * Kept in the same store as the jump filter, and for the same reason: the
 * settings screen and the collecting service are two parts of one application
 * that never speak directly, and the service reads this store for every answer
 * anyway.
 *
 * Every source is used unless the member says otherwise. A device that quietly
 * refused half of what the platform tells it, out of the box, would look
 * broken rather than careful.
 */
class SourcesSetting(context: Context) {
    private val store = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** The names of the switches turned off, as [SourceChoice] spells them. */
    var leftOut: Set<String>
        get() = store.readStringSet(KEY_LEFT_OUT, emptySet()).orEmpty()
        // Copied on the way in: the platform keeps the very set it is handed
        // and reading it back later would hand out a live reference to it.
        set(value) = store.edit().putStringSet(KEY_LEFT_OUT, value.toSet()).apply()

    /** Whether a position from this source is one to take into account. */
    fun uses(whence: FoundBy): Boolean = usesWhatWasFoundBy(whence, leftOut)

    /**
     * The switches as they stand, and again whenever one is flipped.
     *
     * The service asks only the sources left on, so a flipped switch has to
     * reach the request rather than wait for the next answer to be read.
     */
    fun changes(): Flow<Set<String>> = callbackFlow {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_LEFT_OUT) {
                    trySend(leftOut)
                }
            }

        store.registerOnSharedPreferenceChangeListener(listener)
        send(leftOut)

        awaitClose { store.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    /** Switches one source on or off, leaving the rest as they were. */
    fun use(choice: SourceChoice, used: Boolean) {
        leftOut = if (used) leftOut - choice.name else leftOut + choice.name
    }
}
