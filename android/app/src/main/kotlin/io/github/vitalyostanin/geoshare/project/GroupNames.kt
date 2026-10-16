package io.github.vitalyostanin.geoshare.project

import android.content.Context
import io.github.vitalyostanin.geoshare.service.readString
import io.github.vitalyostanin.geoshare.trimText

private const val KEY_PREFIX = "group_caption/"

/**
 * What each group was last known to be called, kept beside the list of
 * groups.
 *
 * The group document is read only by its members, so a group the device has
 * not signed in to yet, or has signed out of, cannot say its own name. Without
 * this the list and the chip on the sign-in screen would call it by its
 * project id, which means nothing to anybody who did not set the project up.
 * The name comes from the invitation link the group was joined by, and then
 * from every read of the group document.
 */
object GroupNames {
    fun of(context: Context, projectId: String): String? =
        Connections.files(context).readString(KEY_PREFIX + projectId, null)

    /** Keeps [caption] for [projectId]'s group; a blank one says nothing new. */
    fun note(context: Context, projectId: String, caption: String?) {
        val name = caption?.let { trimText(it) }

        if (name.isNullOrEmpty() || name == of(context, projectId)) {
            return
        }

        Connections.files(context).edit().putString(KEY_PREFIX + projectId, name).apply()
    }

    /** Forgets the name of [projectId]'s group, once the device leaves it. */
    fun forget(context: Context, projectId: String) {
        Connections.files(context).edit().remove(KEY_PREFIX + projectId).apply()
    }
}
