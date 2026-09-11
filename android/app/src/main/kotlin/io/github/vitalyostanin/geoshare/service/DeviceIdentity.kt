package io.github.vitalyostanin.geoshare.service

import android.content.Context
import android.os.Build
import java.util.UUID

private const val FILE = "device"
private const val KEY_ID = "id"

/** As much of a phone's name as fits above a marker without pushing it off screen. */
private const val NAME_LIMIT = 40

/**
 * Which phone this is.
 *
 * A member can carry more than one phone, and each of them has a position of
 * its own: without this they wrote into one document and the marker showed
 * whichever of them wrote last.
 *
 * Made up here rather than read off the system, because the identifiers a
 * phone offers are either unavailable without a permission, shared between
 * applications, or both. What this needs is only that two phones differ and
 * that one phone keeps its answer, which a stored random value does.
 */
class DeviceIdentity(context: Context) {
    private val store = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /**
     * The identifier itself, made on first use and kept from then on.
     *
     * Reinstalling the application makes a new one, which leaves the old
     * document behind until it is deleted; a stale marker ages on screen like
     * any other and says so.
     */
    val id: String
        get() =
            store.getString(KEY_ID, null) ?: UUID.randomUUID().toString().also { made ->
                store.edit().putString(KEY_ID, made).apply()
            }

    /**
     * What the phone is called on the other screens.
     *
     * The model as the manufacturer writes it: not a name anybody chose, but
     * enough to tell two phones of one member apart until naming them is worth
     * a screen of its own.
     */
    val name: String get() = Build.MODEL.orEmpty().take(NAME_LIMIT)
}

/**
 * The id of the document one phone writes, which is the account and the phone
 * joined by a dot.
 *
 * The rules read the part before the dot, so a member reaches their own
 * documents and nobody else's; the readers group the markers by the owner
 * written inside the document.
 */
fun positionId(memberId: String, deviceId: String): String = "$memberId.$deviceId"
