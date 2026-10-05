package io.github.vitalyostanin.geoshare.project

import android.content.Context

private const val KEY_PREFIX = "removed_elsewhere/"

/**
 * The groups this device was signed out of because another device of the
 * member removed it, kept beside the list of groups.
 *
 * The service signs out on its own, often with no screen open, and the
 * sign-in screen shown afterwards has to say why the device is no longer in.
 * Forgotten at the next sign-in, and when the group is left.
 */
object RemovedHere {
    fun has(context: Context, projectId: String): Boolean =
        Connections.files(context).getBoolean(KEY_PREFIX + projectId, false)

    fun note(context: Context, projectId: String) {
        Connections.files(context).edit().putBoolean(KEY_PREFIX + projectId, true).apply()
    }

    fun forget(context: Context, projectId: String) {
        Connections.files(context).edit().remove(KEY_PREFIX + projectId).apply()
    }
}
