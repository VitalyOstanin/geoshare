package io.github.vitalyostanin.geoshare.service

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.UUID

private const val FILE = "device"
private const val KEY_ID = "id"

/** As much of a device's name as fits above a marker without pushing it off screen. */
private const val NAME_LIMIT = 40

/** What the system's id of a device looks like: sixteen hexadecimal digits, give or take. */
private val SYSTEM_ID = Regex("[0-9A-Za-z]{1,64}")

/**
 * Which device this is.
 *
 * A member can carry more than one device, and each of them has a position of
 * its own: without this they wrote into one document and the marker showed
 * whichever of them wrote last.
 *
 * The system's id of the device for this application, which outlives a
 * reinstall: the names the other members give a device are kept under its id,
 * and a random value made on first use was lost with every reinstall, taking
 * the names with it and leaving a second marker behind. The id is one per
 * device, account on the device and signing key, so it says nothing another
 * application could join up with.
 *
 * A random value kept by the application is still made where the system has
 * no id to give, and a device that had one before this read the system's id is
 * told what it was, so the documents written under it can be taken back.
 */
class DeviceIdentity(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val store = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** The identifier itself: the system's where there is one, a made one otherwise. */
    val id: String
        get() = systemIdFrom(readSystemId()) ?: madeId()

    /**
     * The random id this device went by before it went by the system's, where
     * it did and the documents under it have not yet been taken back.
     */
    val previousId: String?
        get() = store.getString(KEY_ID, null)?.takeIf { it != id }

    /** Says that the documents under [previousId] are gone, so it need not be asked about again. */
    fun forgetPrevious(previous: String) {
        if (store.getString(KEY_ID, null) == previous) {
            store.edit().remove(KEY_ID).apply()
        }
    }

    // Lint warns that a hardware id can follow a person across applications.
    // This one cannot: since Android 8 it differs for every signing key, and
    // what it is used for — telling the devices of one member apart and keeping
    // the names given to them — is what it is for.
    @SuppressLint("HardwareIds")
    private fun readSystemId(): String? =
        Settings.Secure.getString(resolver, Settings.Secure.ANDROID_ID)

    private fun madeId(): String =
        store.getString(KEY_ID, null) ?: UUID.randomUUID().toString().also { made ->
            store.edit().putString(KEY_ID, made).apply()
        }

    /**
     * What the device is called on the other screens.
     *
     * The model as the manufacturer writes it: not a name anybody chose, but
     * enough to tell two devices of one member apart until naming them is worth
     * a screen of its own.
     */
    val name: String get() = Build.MODEL.orEmpty().take(NAME_LIMIT)
}

/**
 * The id of the document one device writes, which is the account and the device
 * joined by a dot.
 *
 * The rules read the part before the dot, so a member reaches their own
 * documents and nobody else's; the readers group the markers by the owner
 * written inside the document.
 */
fun positionId(memberId: String, deviceId: String): String = "$memberId.$deviceId"

/**
 * The system's id as this application uses it, or nothing where it is missing
 * or does not look like one: an id with a dot in it would split the documents
 * of one device between two owners.
 */
fun systemIdFrom(value: String?): String? = value?.takeIf { SYSTEM_ID.matches(it) }
