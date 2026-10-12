package io.github.vitalyostanin.geoshare.service

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
