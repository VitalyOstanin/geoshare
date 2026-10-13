package io.github.vitalyostanin.geoshare.service

import android.content.Context

private const val FILE = "appearance"
private const val KEY_THEME = "theme"

/** What the application is painted in, as the member asked for it. */
enum class ThemeChoice {
    Light,
    Dark,
    System,
}

/**
 * The member's answer about the theme.
 *
 * Kept beside the application rather than with the account: the answer
 * belongs to the device it is read on, and the member reading the map in bed
 * on one device may well want the other one light.
 */
class ThemeSetting(context: Context) {
    private val store = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var choice: ThemeChoice
        get() = themeNamed(store.readString(KEY_THEME, null))
        set(value) = store.edit().putString(KEY_THEME, value.name).apply()
}

/**
 * The answer a stored name stands for.
 *
 * Anything that is not one of the names means the member has not answered:
 * a build that drops a choice should leave the device following the system
 * rather than refusing to start.
 */
internal fun themeNamed(stored: String?): ThemeChoice =
    ThemeChoice.entries.firstOrNull { it.name == stored } ?: ThemeChoice.System

/**
 * Whether to paint dark, given the answer and what the system is set to.
 *
 * Stated once for both readers: the window is painted before anything Compose
 * knows about exists, and the screen is painted inside it.
 */
fun darkWanted(choice: ThemeChoice, systemDark: Boolean): Boolean = when (choice) {
    ThemeChoice.Light -> false
    ThemeChoice.Dark -> true
    ThemeChoice.System -> systemDark
}
