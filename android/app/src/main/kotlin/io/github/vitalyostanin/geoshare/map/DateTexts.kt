package io.github.vitalyostanin.geoshare.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
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
 * A time of day the way the language writes one: "14:30" in Russian, "2:30 PM"
 * in American English. Every clock the application shows goes through this, so
 * that one screen never has both kinds side by side.
 */
internal fun clockFormat(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)

/** A day written by [dayPattern], followed by the language's own time of day. */
private fun dayAndClock(dayPattern: String, locale: Locale): DateTimeFormatter {
    val clock =
        DateTimeFormatterBuilder.getLocalizedDateTimePattern(
            null,
            FormatStyle.SHORT,
            IsoChronology.INSTANCE,
            locale,
        )

    return DateTimeFormatter.ofPattern("$dayPattern $clock", locale)
}

/**
 * The end of a search or of a permission: the day as well as the time.
 *
 * The longest permission is a day, so one given in the evening runs out
 * tomorrow, and an hour alone would read as one that ends this morning. The
 * year is left out: nothing here reaches one.
 */
internal fun endsAtText(at: ZonedDateTime, locale: Locale): String =
    dayAndClock(DAY, locale).format(at)

/** The same end, read in the app's current language. */
@Composable
internal fun endsAtText(at: ZonedDateTime): String = endsAtText(at, appLocale())

/** The end of an invitation, with the month written in full. */
internal fun invitationEndsText(at: ZonedDateTime, locale: Locale): String =
    dayAndClock(DAY_LONG, locale).format(at)

/** The same end, read in the app's current language. */
@Composable
internal fun invitationEndsText(at: ZonedDateTime): String = invitationEndsText(at, appLocale())

/** The language the screen is written in right now, which follows the in-app switch. */
@Composable
internal fun appLocale(): Locale = LocalConfiguration.current.locales[0]
