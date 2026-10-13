package io.github.vitalyostanin.geoshare.service

import android.content.SharedPreferences
import io.github.vitalyostanin.geoshare.log.AppLog

private const val TAG = "geoshare.settings"

/**
 * Reads a boolean, or [default] when the key holds a value of another type.
 *
 * A settings file can be left holding the wrong type under a key: by a build
 * that kept something else there. The platform
 * answers that with a ClassCastException, and a read made while the
 * application starts would take the start down with it. The key is logged so
 * that the odd value can be found, and the setting goes by its default.
 */
fun SharedPreferences.readBoolean(key: String, default: Boolean): Boolean =
    safely(key, default) { getBoolean(key, default) }

/** Reads a long, or [default] when the key holds a value of another type. */
fun SharedPreferences.readLong(key: String, default: Long): Long =
    safely(key, default) { getLong(key, default) }

/** Reads a string, or [default] when the key holds a value of another type. */
fun SharedPreferences.readString(key: String, default: String?): String? =
    safely(key, default) { getString(key, default) }

/** Reads a set of strings, or [default] when the key holds a value of another type. */
fun SharedPreferences.readStringSet(key: String, default: Set<String>?): Set<String>? =
    safely(key, default) { getStringSet(key, default) }

private fun <T> safely(key: String, default: T, read: () -> T): T = try {
    read()
} catch (error: ClassCastException) {
    AppLog.w(TAG, "the value kept under \"$key\" is of another type, using the default", error)
    default
}
