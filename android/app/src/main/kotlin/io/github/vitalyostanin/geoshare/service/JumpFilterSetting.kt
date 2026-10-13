package io.github.vitalyostanin.geoshare.service

import android.content.Context

private const val FILE = "collection"
private const val KEY_JUMPS = "filterJumps"
private const val KEY_FORGET = "forgetPlace"

/**
 * Whether this device leaves out the positions it cannot have reached.
 *
 * Off unless the member says otherwise: the filter throws answers away, and a
 * device that quietly threw away a real position would be worse than one that
 * showed a wrong one. Where the satellites are jammed the answer is different,
 * and that is the member's call rather than this file's.
 */
class JumpFilterSetting(context: Context) {
    private val store = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = store.readBoolean(KEY_JUMPS, false)
        set(value) = store.edit().putBoolean(KEY_JUMPS, value).apply()

    /**
     * Asks the collecting service to forget where it thinks the device is.
     *
     * A flag rather than a call: the settings screen and the service are two
     * processes of the same application that never speak directly, and the
     * service reads this file for every fix anyway. It lowers the flag itself
     * once it has forgotten, so the request is spent by the fix that answers
     * it.
     */
    fun askToForget() = store.edit().putBoolean(KEY_FORGET, true).apply()

    /** True once, for the service, if forgetting has been asked for. */
    fun takeForgetRequest(): Boolean {
        if (!store.readBoolean(KEY_FORGET, false)) {
            return false
        }

        store.edit().putBoolean(KEY_FORGET, false).apply()

        return true
    }
}
