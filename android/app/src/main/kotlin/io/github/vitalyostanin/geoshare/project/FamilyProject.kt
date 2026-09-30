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
     * them now.
     */
    fun save(context: Context, settings: ProjectSettings) {
        settingOf(context).edit().putString(KEY_SETTINGS, settings.toJson()).apply()
        apply(context, settings)
    }

    private fun apply(context: Context, settings: ProjectSettings) {
        FirebaseApp.initializeApp(context, settings.toOptions())
        current = settings
        AppLog.i(TAG, "talking to the project ${settings.projectId}")
    }

    private fun settingOf(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
