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

    /**
     * Turns sharing off and waits for the write to reach disk, for a caller
     * that ends the process right after: [enabled]'s own write runs on a
     * background thread the process may not outlive, and the switch would
     * come back on after the restart.
     */
    fun turnOffNow() {
        store.edit().putBoolean(KEY_ENABLED, false).commit()
    }
}
