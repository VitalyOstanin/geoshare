package io.github.vitalyostanin.geoshare.service

import android.content.Context
import io.github.vitalyostanin.geoshare.collection.Constellation

private const val FILE = "collection"
private const val KEY_DOUBTED = "aidingDoubted"

/**
 * Which constellations the receiver has been hearing for nothing, as the
 * collecting service last left them.
 *
 * The same shape of channel as the forget-the-place flag beside it and for the
 * same reason: the service and the settings screen are two processes of the
 * same application that never speak directly, and this file is one both of
 * them already open. Written only when the answer changes, since the service
 * reads an epoch a second and the screen reads the answer once.
 *
 * Kept by name rather than by ordinal: a build that adds a constellation must
 * not turn a stored answer into a different one. A name this build does not
 * know is dropped, which is what a phone downgraded to an older build should
 * do with a word it has never heard.
 */
class AidingDoubtSetting(context: Context) {
    private val store = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var doubted: Set<Constellation>
        get() =
            store.getString(KEY_DOUBTED, "")
                .orEmpty()
                .split(",")
                .filter { it.isNotBlank() }
                .mapNotNull { word -> Constellation.entries.firstOrNull { it.name == word } }
                .toSet()
        set(value) =
            store.edit()
                .putString(KEY_DOUBTED, value.joinToString(",") { it.name })
                .apply()
}
