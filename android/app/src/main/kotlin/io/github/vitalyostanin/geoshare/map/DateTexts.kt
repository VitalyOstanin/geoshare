package io.github.vitalyostanin.geoshare.map

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.time.ZonedDateTime
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Dates that carry a month name are written with the locale of the moment, not
 * of the first time the file was loaded: the language is switched inside the
 * app without restarting the process, and a formatter held in a top-level
 * value kept the months in the language the process started with.
 */
private const val DAY = "d MMM"

private const val DAY_LONG = "d MMMM,"

/**
 * A time of day the way the language writes one, on the clock the system
 * setting asks for: "14:30" in Russian, "2:30 PM" in American English, "14:30"
 * in American English with the 24-hour switch on. Every clock the application
 * shows goes through [clockPattern], so that one screen never has both kinds
 * side by side.
 */
internal fun clockFormat(locale: Locale, twentyFourHour: Boolean): DateTimeFormatter =
    DateTimeFormatter.ofPattern(clockPattern(locale, twentyFourHour), locale)

/**
 * The pattern of a time of day, hours and minutes, with the hours on the clock
 * the system setting asks for and everything else in the language's own way.
 *
 * Android's "24-hour format" switch can disagree with the language, for example
 * American English with a 24-hour clock, and a person who switched it expects
 * the hours to follow it. Letters inside single quotes are literal text and are
 * left alone. The no-break spaces CLDR puts around the day-period marker (U+00A0
 * and U+202F) go away together with the marker. The marker is the AM/PM letter
 * `a` or, in locales such as zh-TW, the day-period letters `B` and `b`.
 */
internal fun clockPattern(locale: Locale, twentyFourHour: Boolean): String {
    val base =
        DateTimeFormatterBuilder.getLocalizedDateTimePattern(
            null,
            FormatStyle.SHORT,
            IsoChronology.INSTANCE,
            locale,
        )
    val unquoted = unquotedLetters(base)
    val is24 = 'H' in unquoted || 'k' in unquoted
    val is12 = 'h' in unquoted || 'K' in unquoted

    return when {
        twentyFourHour && is12 -> toTwentyFourHour(base)
        !twentyFourHour && is24 && !is12 -> toTwelveHour(base)
        else -> base
    }
}

private fun unquotedLetters(pattern: String): String {
    val result = StringBuilder()
    var quoted = false

    for (c in pattern) {
        if (c == '\'') {
            quoted = !quoted
        } else if (!quoted) {
            result.append(c)
        }
    }

    return result.toString()
}

private fun isClockSpace(c: Char): Boolean = c == ' ' || c == '\u00A0' || c == '\u202F'

internal fun toTwentyFourHour(pattern: String): String {
    val result = StringBuilder()
    var quoted = false
    var skipSpaces = false

    for (c in pattern) {
        when {
            c == '\'' -> {
                quoted = !quoted
                skipSpaces = false
                result.append(c)
            }

            quoted -> result.append(c)

            c == 'a' || c == 'b' || c == 'B' -> {
                while (result.isNotEmpty() && isClockSpace(result.last())) {
                    result.setLength(result.length - 1)
                }
                skipSpaces = true
            }

            skipSpaces && isClockSpace(c) -> Unit

            else -> {
                skipSpaces = false
                result.append(if (c == 'h' || c == 'K') 'H' else c)
            }
        }
    }

    return result.toString()
}

private fun toTwelveHour(pattern: String): String {
    val result = StringBuilder()
    var quoted = false

    for (c in pattern) {
        if (c == '\'') quoted = !quoted
        result.append(if (!quoted && (c == 'H' || c == 'k')) 'h' else c)
    }

    return "$result a"
}

/** A day written by [dayPattern], followed by the time of day of [clockPattern]. */
private fun dayAndClock(
    dayPattern: String,
    locale: Locale,
    twentyFourHour: Boolean,
): DateTimeFormatter =
    DateTimeFormatter.ofPattern("$dayPattern ${clockPattern(locale, twentyFourHour)}", locale)

/**
 * The language's own pattern of a date with a time, its date written in
 * [dateStyle] and its time of day swapped for the one of [clockPattern].
 */
internal fun dateAndClockPattern(
    dateStyle: FormatStyle,
    locale: Locale,
    twentyFourHour: Boolean,
): String {
    fun localized(date: FormatStyle?, time: FormatStyle?) =
        DateTimeFormatterBuilder.getLocalizedDateTimePattern(
            date,
            time,
            IsoChronology.INSTANCE,
            locale,
        )

    return localized(dateStyle, FormatStyle.SHORT)
        .replace(localized(null, FormatStyle.SHORT), clockPattern(locale, twentyFourHour))
}

/**
 * The end of a search or of a permission: the day as well as the time.
 *
 * The longest permission is a day, so one given in the evening runs out
 * tomorrow, and an hour alone would read as one that ends this morning. The
 * year is left out: nothing here reaches one.
 */
internal fun endsAtText(at: ZonedDateTime, locale: Locale, twentyFourHour: Boolean): String =
    dayAndClock(DAY, locale, twentyFourHour).format(at)

/** The same end, read in the app's current language. */
@Composable
internal fun endsAtText(at: ZonedDateTime): String = endsAtText(at, appLocale(), twentyFourHour())

/** The end of an invitation, with the month written in full. */
internal fun invitationEndsText(
    at: ZonedDateTime,
    locale: Locale,
    twentyFourHour: Boolean,
): String = dayAndClock(DAY_LONG, locale, twentyFourHour).format(at)

/** The same end, read in the app's current language. */
@Composable
internal fun invitationEndsText(at: ZonedDateTime): String =
    invitationEndsText(at, appLocale(), twentyFourHour())

/** The language the screen is written in right now, which follows the in-app switch. */
@Composable
internal fun appLocale(): Locale = LocalConfiguration.current.locales[0]

/** Whether the system's own setting asks for a 24-hour clock. */
@Composable
internal fun twentyFourHour(): Boolean = DateFormat.is24HourFormat(LocalContext.current)
