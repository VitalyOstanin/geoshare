package io.github.vitalyostanin.geoshare.project

import android.content.Context
import io.github.vitalyostanin.geoshare.auth.SignInMark

private const val KEY_PREFIX = "sign_in_started/"

/**
 * The groups whose browser sign-in was started and has not ended, kept beside
 * the list of groups so that a screen made again still knows of it.
 * Forgotten when the sign-in ends either way, and when the group is left.
 */
object SignInStarted {
    fun mark(context: Context, projectId: String): SignInMark {
        val files = Connections.files(context.applicationContext)
        val key = KEY_PREFIX + projectId

        return object : SignInMark {
            override fun has() = files.getBoolean(key, false)

            override fun note() {
                files.edit().putBoolean(key, true).apply()
            }

            override fun forget() {
                files.edit().remove(key).apply()
            }
        }
    }

    fun forget(context: Context, projectId: String) {
        Connections.files(context).edit().remove(KEY_PREFIX + projectId).apply()
    }
}
