package io.github.vitalyostanin.geoshare.service

import android.content.Context

private const val FILE = "sharing"
private const val KEY_ENABLED = "enabled"

/**
 * Whether the member wants their position shared.
 *
 * Kept outside the session on purpose: the answer is needed after a restart,
 * before anything has had a chance to ask Firebase who is signed in.
 */
class SharingSetting(context: Context) {
    private val store = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = store.getBoolean(KEY_ENABLED, false)
        set(value) = store.edit().putBoolean(KEY_ENABLED, value).apply()
}
