package io.github.vitalyostanin.geoshare.service

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate

private const val FILE = "appearance"
private const val KEY_LANGUAGE = "language"

/** What language the application speaks, as the member asked for it. */
enum class LanguageChoice {
    Russian,
    English,
    System,
}

/**
 * The tag a choice stands for, and nothing at all for the system's own.
 *
 * "Follow the system" is the absence of a language rather than a language of
 * its own: the platform keeps the answer, and an empty answer is what makes it
 * read the device's setting instead.
 */
fun languageTag(choice: LanguageChoice): String? = when (choice) {
    LanguageChoice.Russian -> "ru"
    LanguageChoice.English -> "en"
    LanguageChoice.System -> null
}

/**
 * The answer a stored tag stands for.
 *
 * A tag carries a region the answer does not — the platform hands back "ru-RU"
 * for a device set up in Russia — and a tag from a build that spoke a language
 * this one does not means the member is back to following the system.
 */
fun languageChosen(tag: String?): LanguageChoice {
    val language = tag?.substringBefore('-')?.lowercase().orEmpty()

    return when (language) {
        "ru" -> LanguageChoice.Russian
        "en" -> LanguageChoice.English
        else -> LanguageChoice.System
    }
}

/**
 * The tag the member chose, kept beside the theme for whoever reads it
 * without a screen: an empty tag is the system's own language, and nothing
 * stored means the choice was made before this was kept.
 */
class LanguageSetting(context: Context) {
    private val store = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var tag: String?
        get() = store.getString(KEY_LANGUAGE, null)
        set(value) = store.edit().putString(KEY_LANGUAGE, value.orEmpty()).apply()
}

/**
 * The language a notification is to be written in, or null for the
 * context's own.
 *
 * From Android 13 the system hands every context of the application the
 * language chosen in it. Before that only the screens get it, and a service
 * reads the device's: its notifications are written in the chosen language
 * only when asked to. The stored tag comes first, since a service started
 * without a screen finds the platform's answer still empty.
 */
internal fun noticeLanguage(sdk: Int, stored: String?, spoken: String): String? = when {
    sdk >= Build.VERSION_CODES.TIRAMISU -> null
    stored != null -> stored.ifEmpty { null }
    else -> spoken.ifEmpty { null }
}

/** This context, speaking the language the member chose for the application. */
fun Context.inAppLanguage(): Context {
    val tag =
        noticeLanguage(
            Build.VERSION.SDK_INT,
            LanguageSetting(this).tag,
            AppCompatDelegate.getApplicationLocales().toLanguageTags(),
        ) ?: return this

    val configuration =
        Configuration(resources.configuration).apply { setLocales(LocaleList.forLanguageTags(tag)) }

    return createConfigurationContext(configuration)
}
