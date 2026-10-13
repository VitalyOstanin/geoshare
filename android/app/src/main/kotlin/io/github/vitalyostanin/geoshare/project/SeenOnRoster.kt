package io.github.vitalyostanin.geoshare.project

import android.content.Context
import io.github.vitalyostanin.geoshare.service.readStringSet

private const val KEY = "seen_on_roster"

/**
 * The accounts this device has seen on the roster of each group, kept beside
 * the list of groups.
 *
 * A refusal to read the group document is all a device taken off a group
 * hears, and it is also what a device that has not joined yet hears. Only
 * having been in tells them apart, and the device is usually taken off while
 * the application is closed, so this outlives the process.
 *
 * Kept by account as well as by project: signing in to the same group with
 * another account is not being taken off it.
 */
object SeenOnRoster {
    fun has(context: Context, projectId: String, uid: String): Boolean =
        entry(projectId, uid) in seen(context)

    fun note(context: Context, projectId: String, uid: String) {
        val entry = entry(projectId, uid)
        val seen = seen(context)

        if (entry !in seen) {
            Connections.files(context).edit().putStringSet(KEY, seen + entry).apply()
        }
    }

    /** Forgets every account seen in [projectId]'s group, once the device leaves it. */
    fun forget(context: Context, projectId: String) {
        val seen = seen(context)
        val kept = seen.filterNot { it.startsWith("$projectId/") }.toSet()

        if (kept.size != seen.size) {
            Connections.files(context).edit().putStringSet(KEY, kept).apply()
        }
    }

    // A copy: the set the preferences hand out is theirs and must not change.
    private fun seen(context: Context): Set<String> =
        Connections.files(context).readStringSet(KEY, null)?.toSet().orEmpty()

    private fun entry(projectId: String, uid: String) = "$projectId/$uid"
}
