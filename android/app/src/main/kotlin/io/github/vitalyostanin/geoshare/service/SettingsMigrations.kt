package io.github.vitalyostanin.geoshare.service

import android.content.Context
import io.github.vitalyostanin.geoshare.log.AppLog

/**
 * Brings what the settings keep on the device up to the shape this build reads.
 *
 * The shape carries a number of its own, apart from the application's version,
 * so that a change to how a setting is stored is one step in a list rather
 * than a guess at what an older build left behind. A step takes the shape
 * from N to N + 1 and is written once the whole of it has run.
 */
object SettingsMigrations {
    /** The shape this build reads and writes. */
    internal const val CURRENT = 1

    private const val FILE = "schema"
    private const val KEY_VERSION = "version"
    private const val TAG = "geoshare.schema"

    /**
     * The steps, the one at index N taking the shape from N + 1 to N + 2;
     * none leads to version 1, which is the shape before there was a number.
     */
    private val steps: List<(Context) -> Unit> = emptyList()

    /** Called once, from the application object, before anything reads a setting. */
    fun run(context: Context) = run(context, CURRENT, steps)

    internal fun run(context: Context, current: Int, steps: List<(Context) -> Unit>) {
        val file = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

        // No number is either a new installation or version 0.1.0 and earlier,
        // which stored what version 1 stores.
        if (!file.contains(KEY_VERSION)) {
            file.edit().putInt(KEY_VERSION, current).commit()

            return
        }

        val stored = file.getInt(KEY_VERSION, current)

        if (stored > current) {
            // An older build installed over a newer one: what the newer one
            // wrote is not for this one to rewrite.
            AppLog.w(TAG, "settings are of version $stored, this build reads $current")

            return
        }

        for (version in stored until current) {
            steps[version - 1](context)
            file.edit().putInt(KEY_VERSION, version + 1).commit()
        }
    }
}
