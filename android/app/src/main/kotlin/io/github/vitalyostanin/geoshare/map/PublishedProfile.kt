package io.github.vitalyostanin.geoshare.map

import android.content.Context

private const val FILE = "profile"
private const val KEY_MEMBER = "member"
private const val KEY_NAME = "name"
private const val KEY_PICTURE = "picture"
private const val KEY_DEVICE = "device"

/**
 * Which group it was told.
 *
 * A profile lives inside a group, so the same account moving to another one
 * has told the new group nothing, however much it told the old one.
 */
private const val KEY_GROUP = "group"

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
        group: String,
        memberId: String,
        displayName: String,
        pictureUrl: String?,
        deviceId: String,
    ): Boolean = store.getString(KEY_GROUP, null) == group &&
        store.getString(KEY_MEMBER, null) == memberId &&
        store.getString(KEY_NAME, null) == displayName &&
        store.getString(KEY_PICTURE, null) == pictureUrl &&
        store.getString(KEY_DEVICE, null) == deviceId

    /** Called once the server has accepted the write, not before. */
    fun remember(
        group: String,
        memberId: String,
        displayName: String,
        pictureUrl: String?,
        deviceId: String,
    ) {
        store
            .edit()
            .putString(KEY_GROUP, group)
            .putString(KEY_MEMBER, memberId)
            .putString(KEY_NAME, displayName)
            .putString(KEY_PICTURE, pictureUrl)
            .putString(KEY_DEVICE, deviceId)
            .apply()
    }
}
