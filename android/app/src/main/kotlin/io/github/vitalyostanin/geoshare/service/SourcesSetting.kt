package io.github.vitalyostanin.geoshare.service

import android.content.Context
import io.github.vitalyostanin.geoshare.collection.FoundBy
import io.github.vitalyostanin.geoshare.collection.SourceChoice
import io.github.vitalyostanin.geoshare.collection.usesWhatWasFoundBy

private const val FILE = "collection"
private const val KEY_LEFT_OUT = "sourcesLeftOut"

/**
 * Which sources of a position this phone takes into account.
 *
 * Kept in the same store as the jump filter, and for the same reason: the
 * settings screen and the collecting service are two parts of one application
 * that never speak directly, and the service reads this store for every answer
 * anyway.
 *
 * Every source is used unless the member says otherwise. A phone that quietly
 * refused half of what the platform tells it, out of the box, would look
 * broken rather than careful.
 */
class SourcesSetting(context: Context) {
    private val store = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** The names of the switches turned off, as [SourceChoice] spells them. */
    var leftOut: Set<String>
        get() = store.getStringSet(KEY_LEFT_OUT, emptySet()).orEmpty()
        // Copied on the way in: the platform keeps the very set it is handed
        // and reading it back later would hand out a live reference to it.
        set(value) = store.edit().putStringSet(KEY_LEFT_OUT, value.toSet()).apply()

    /** Whether a position from this source is one to take into account. */
    fun uses(whence: FoundBy): Boolean = usesWhatWasFoundBy(whence, leftOut)

    /** Switches one source on or off, leaving the rest as they were. */
    fun use(choice: SourceChoice, used: Boolean) {
        leftOut = if (used) leftOut - choice.name else leftOut + choice.name
    }
}
