package io.github.vitalyostanin.geoshare.map

import android.content.Context

private const val FILE = "profile"
private const val KEY_MEMBER = "member"
private const val KEY_NAME = "name"
private const val KEY_PICTURE = "picture"
private const val KEY_DEVICE = "device"

/**
 * What this phone has already told the group about its member.
 *
 * Kept here rather than read back out of the database, because what the
 * database hands a client includes writes it has not accepted yet: a write
 * refused by the rules appears in the subscription and then disappears, and a
 * client that took that for an answer would write again, and again.
 */
class PublishedProfile(context: Context) {
    private val store = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** True when the account says exactly what was last accepted for it. */
    fun matches(
        memberId: String,
        displayName: String,
        pictureUrl: String?,
        deviceId: String,
    ): Boolean = store.getString(KEY_MEMBER, null) == memberId &&
        store.getString(KEY_NAME, null) == displayName &&
        store.getString(KEY_PICTURE, null) == pictureUrl &&
        store.getString(KEY_DEVICE, null) == deviceId

    /** Called once the server has accepted the write, not before. */
    fun remember(memberId: String, displayName: String, pictureUrl: String?, deviceId: String) {
        store
            .edit()
            .putString(KEY_MEMBER, memberId)
            .putString(KEY_NAME, displayName)
            .putString(KEY_PICTURE, pictureUrl)
            .putString(KEY_DEVICE, deviceId)
            .apply()
    }
}
