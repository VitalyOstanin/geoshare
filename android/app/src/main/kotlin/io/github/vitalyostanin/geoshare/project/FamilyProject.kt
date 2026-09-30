package io.github.vitalyostanin.geoshare.project

import android.content.Context
import com.google.firebase.FirebaseApp
import io.github.vitalyostanin.geoshare.log.AppLog

private const val FILE = "family_project"
private const val KEY_SETTINGS = "settings"
private const val TAG = "geoshare.project"

/**
 * The Firebase project this phone talks to, which is a family's own rather
 * than one baked into the build.
 *
 * One build serves every family, so it carries no project of its own; what a
 * phone reaches instead is decided by [ProjectSettings] a connection link
 * downloads and hands here to keep. Kept outside the session and
 * read back before anything else touches Firebase — the library refuses to
 * be redirected once it has been used, and a service started at boot reaches
 * for `FirebaseAuth.getInstance()` before a screen has had the chance to ask.
 */
object FamilyProject {
    private var current: ProjectSettings? = null

    /** Whether this phone has settings to start Firebase with. */
    val connected: Boolean
        get() = current != null

    /**
     * The project this phone talks to, or null before any [start] or
     * [save]. Read by `ConnectFlow` to tell a link back to it apart from
     * one naming a different project.
     */
    val projectId: String?
        get() = current?.projectId

    /**
     * The Hosting site this phone's project answers to, or null before any
     * [start] or [save]. What an invitation link is built against
     * (`ConnectLink.invitationLink`): the join page a code sends somebody to
     * lives there, not at a host baked into the build.
     */
    val site: String?
        get() = current?.site

    /**
     * Reads back the settings saved on a previous run, and starts Firebase
     * with them when there are any.
     *
     * Called once, before anything else in the application: a second
     * [FirebaseApp.initializeApp] on the default instance is refused, so
     * this has to be the only place that makes the first one.
     */
    fun start(context: Context) {
        val kept = settingOf(context).getString(KEY_SETTINGS, null) ?: return
        val settings = ProjectSettings.parse(kept)

        if (settings == null) {
            AppLog.w(TAG, "the settings kept on the phone could not be read")

            return
        }

        apply(context, settings)
    }

    /**
     * Keeps a family's settings for the next start, and starts Firebase with
     * them now — unless this process already started it for a different
     * project, in which case only the settings are kept: the default
     * Firebase app cannot be re-created in place (ADR-0001), so what starts
     * it is the restart that follows this call rather than this call itself.
     * [FirebaseApp.getApps] is empty exactly when nothing in this process
     * has called `initializeApp` yet, which is how the two cases are told
     * apart.
     *
     * Written with `commit` rather than `apply`: the caller of this today
     * ends the process right after it returns
     * (`MainActivity.acceptConnection`, reacting to `ConnectState.Done`), and
     * `apply`'s write runs on a background thread this process may not live
     * long enough to finish.
     */
    fun save(context: Context, settings: ProjectSettings) {
        settingOf(context).edit().putString(KEY_SETTINGS, settings.toJson()).commit()

        if (FirebaseApp.getApps(context).isNotEmpty()) {
            current = settings
            AppLog.i(TAG, "kept the project ${settings.projectId} for the next start")

            return
        }

        apply(context, settings)
    }

    private fun apply(context: Context, settings: ProjectSettings) {
        FirebaseApp.initializeApp(context, settings.toOptions())
        current = settings
        AppLog.i(TAG, "talking to the project ${settings.projectId}")
    }

    /** The file [PendingInvitationStore] keeps its own key in, next to [KEY_SETTINGS]. */
    internal fun settingOf(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
