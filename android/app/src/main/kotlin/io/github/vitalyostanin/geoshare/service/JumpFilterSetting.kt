package io.github.vitalyostanin.geoshare.service

import android.content.Context

private const val FILE = "collection"
private const val KEY_JUMPS = "filterJumps"

/**
 * Whether this phone leaves out the positions it cannot have reached.
 *
 * Off unless the member says otherwise: the filter throws answers away, and a
 * phone that quietly threw away a real position would be worse than one that
 * showed a wrong one. Where the satellites are jammed the answer is different,
 * and that is the member's call rather than this file's.
 */
class JumpFilterSetting(context: Context) {
    private val store = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = store.getBoolean(KEY_JUMPS, false)
        set(value) = store.edit().putBoolean(KEY_JUMPS, value).apply()
}
