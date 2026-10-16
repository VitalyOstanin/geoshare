package io.github.vitalyostanin.geoshare.project

import java.util.Random

private val AWKWARD_PIECES = listOf(
    "a", "Z", "7", "Я", "ж", "é", " ", "  ", "\t", "\n", "\r", "\u0000", "\u0001", "\u001f",
    "\u007f", " ", " ", " ", "\"", "'", "\\", "\\\\", "\\n", "\\u0041", "/", "{",
    "}", "[", "]", ",", ":", "null", "true", "0", "-1", "😀", "🇷🇺",
    "‍", "…", "&", "%41", "#", "<", ">",
)

private const val SITE_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789-"

/**
 * A string of awkward pieces — quotes, backslashes, control characters, emoji,
 * Cyrillic — sometimes empty, sometimes of blanks only, sometimes long. Any
 * of it, blank included, is possible; [randomNonBlank] is for the places that
 * refuse blank.
 */
internal fun randomAwkward(random: Random): String = when (random.nextInt(10)) {
    0 -> ""

    1 -> " ".repeat(random.nextInt(5))

    2 -> buildString {
        repeat(300 + random.nextInt(900)) {
            append(AWKWARD_PIECES[random.nextInt(AWKWARD_PIECES.size)])
        }
    }

    else -> buildString {
        repeat(random.nextInt(14)) {
            append(AWKWARD_PIECES[random.nextInt(AWKWARD_PIECES.size)])
        }
    }
}

/** An awkward string with a letter in it, so that nothing counts it as blank. */
internal fun randomNonBlank(random: Random): String {
    val text = randomAwkward(random)
    val at = random.nextInt(text.length + 1)

    return text.substring(0, at) + "x" + text.substring(at)
}

/** A site name the way `tools/project.sh` makes one: lowercase letters, digits and hyphens. */
internal fun randomSite(random: Random): String = buildString {
    repeat(1 + random.nextInt(40)) { append(SITE_ALPHABET[random.nextInt(SITE_ALPHABET.length)]) }
}

/** Settings [ProjectSettings.parse] accepts: version 1, no blank field, a well-formed site. */
internal fun randomSettings(random: Random) = ProjectSettings(
    version = 1,
    projectId = randomNonBlank(random),
    applicationId = randomNonBlank(random),
    apiKey = randomNonBlank(random),
    gcmSenderId = randomNonBlank(random),
    site = randomSite(random),
)
