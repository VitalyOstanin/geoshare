package io.github.vitalyostanin.geoshare.map

import android.content.Context

private const val FILE = "profile"
private const val KEY_GROUP = "group"
private const val KEY_MEMBER = "member"
private const val KEY_NAME = "name"
private const val KEY_PICTURE = "picture"
private const val KEY_DEVICE = "device"

/**
 * What a device tells its group about the member signed in on it.
 *
 * The group is part of it, not context around it: a profile document lives
 * inside a group, so the same account in another group has told that one
 * nothing, however much it told the group it came from.
 */
data class ProfileTold(
    val group: String,
    val memberId: String,
    val displayName: String,
    val pictureUrl: String?,
    val deviceId: String,
)

/**
 * What this device has already told the group about its member.
 *
 * Kept here rather than read back out of the database, because what the
 * database hands a client includes writes it has not accepted yet: a write
 * refused by the rules appears in the subscription and then disappears, and a
 * client that took that for an answer would write again, and again.
 */
class PublishedProfile(context: Context) {
    private val store = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** What was last accepted, or nothing on a device that has told nobody anything. */
    fun told(): ProfileTold? {
        val group = store.getString(KEY_GROUP, null) ?: return null
        val memberId = store.getString(KEY_MEMBER, null) ?: return null
        val displayName = store.getString(KEY_NAME, null) ?: return null
        val deviceId = store.getString(KEY_DEVICE, null) ?: return null

        return ProfileTold(
            group = group,
            memberId = memberId,
            displayName = displayName,
            // The one field a profile is allowed to be without, so its absence
            // is an answer rather than a gap.
            pictureUrl = store.getString(KEY_PICTURE, null),
            deviceId = deviceId,
        )
    }

    /**
     * Forgets what was told, so the next chance writes it all again.
     *
     * For a device coming back to a group it was taken off, or signing in
     * again after signing out: either way the documents that held this device
     * in the group's lists are gone, and what was remembered would otherwise
     * say there is nothing to write.
     */
    fun forget() {
        store.edit().clear().apply()
    }

    /** Called once the server has accepted the write, not before. */
    fun remember(told: ProfileTold) {
        store
            .edit()
            .putString(KEY_GROUP, told.group)
            .putString(KEY_MEMBER, told.memberId)
            .putString(KEY_NAME, told.displayName)
            .putString(KEY_PICTURE, told.pictureUrl)
            .putString(KEY_DEVICE, told.deviceId)
            .apply()
    }
}

/**
 * Whether what this device remembers telling the group still stands.
 *
 * Remembered on the device, so it cannot see the profile taken away behind its
 * back — by an administrator tidying the group, or by a removal and a return.
 * The server saying the document is gone is the one thing that overrules it.
 */
fun stillTold(told: ProfileTold?, telling: ProfileTold, goneOnServer: Boolean): Boolean =
    told == telling && !goneOnServer
