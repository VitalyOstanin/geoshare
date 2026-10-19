package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class DateTextsTest {
    private val moment = ZonedDateTime.of(2026, 10, 12, 14, 30, 0, 0, ZoneId.of("UTC"))

    @Test
    fun `a month is written in the language asked for after another was used first`() {
        val english = endsAtText(moment, Locale.US, false)
        val russian = endsAtText(moment, Locale.forLanguageTag("ru"), true)

        assertTrue(english.startsWith("Oct 12 2:30"))
        assertNotEquals(english, russian)
        assertTrue(russian.startsWith("12 окт"))
        assertTrue(russian.endsWith("14:30"))
    }

    @Test
    fun `the day comes before or after the month as the language writes it`() {
        assertEquals("MMM d", dayPattern(Locale.US, false))
        assertEquals("MMMM d", dayPattern(Locale.US, true))
        assertEquals("d MMM", dayPattern(Locale.forLanguageTag("ru"), false))
        assertEquals("d MMMM", dayPattern(Locale.forLanguageTag("ru"), true))
        assertEquals("d MMM", withoutYear("d MMM y 'г'."))
        assertEquals("MMM d", withoutYear("MMM d, y"))
        assertEquals("M月d日", withoutYear("y年M月d日"))
    }

    // A screen that wrote some times in the language's own way and others
    // always on a 24-hour clock showed "2:30 PM" and "14:30" side by side.
    @Test
    fun `every clock follows the same hour setting`() {
        assertTrue(clockFormat(Locale.US, false).format(moment).endsWith("PM"))
        assertEquals("14:30", clockFormat(Locale.forLanguageTag("ru"), true).format(moment))
        assertEquals("14:30", clockFormat(Locale.US, true).format(moment))
        assertTrue(endsAtText(moment, Locale.US, true).endsWith(" 14:30"))
        assertTrue(invitationEndsText(moment, Locale.US, true).endsWith(" 14:30"))
        assertTrue(momentText(moment.toInstant(), Locale.US, moment.zone, true).endsWith(" 14:30"))
        assertTrue(endsAtText(moment, Locale.US, false).endsWith("PM"))
    }

    @Test
    fun `an invitation end keeps the language asked for as well`() {
        val russian = invitationEndsText(moment, Locale.forLanguageTag("ru"), true)

        assertEquals("12 октября, 14:30", russian)
        assertTrue(invitationEndsText(moment, Locale.US, false).startsWith("October 12, 2:30"))
    }

    // The card of a member wrote these in the language the process started
    // with, so a device switched to Russian inside the app showed "Oct 12".
    @Test
    fun `a moment on the card is written in the language asked for, not the process's`() {
        val started = Locale.getDefault()
        Locale.setDefault(Locale.US)

        try {
            val russian =
                momentText(moment.toInstant(), Locale.forLanguageTag("ru"), moment.zone, true)

            assertTrue(russian, russian.startsWith("12 окт. 2026"))
            assertTrue(russian, russian.endsWith("14:30"))
            assertTrue(
                momentText(
                    moment.toInstant(),
                    Locale.US,
                    moment.zone,
                    false,
                ).startsWith("Oct 12, 2026"),
            )
        } finally {
            Locale.setDefault(started)
        }
    }

    @Test
    fun `the clock pattern follows the hour setting while the rest follows the language`() {
        fun clock(locale: Locale, twentyFourHour: Boolean) =
            DateTimeFormatter.ofPattern(clockPattern(locale, twentyFourHour), locale).format(moment)

        val russian = Locale.forLanguageTag("ru")

        assertEquals("14:30", clock(Locale.US, true))
        assertEquals("14:30", clock(russian, true))
        assertEquals(
            "2:30 PM",
            clock(Locale.US, false).replace('\u202F', ' ').replace('\u00A0', ' '),
        )
        val russianTwelve = clock(russian, false)
        assertTrue(russianTwelve, russianTwelve.contains("2:30"))
        assertTrue(russianTwelve, !russianTwelve.contains("14"))
    }

    // CLDR writes the short time of some locales with a day period (`B`), for
    // example "Bh:mm" in zh-TW; only `h` became `H` and "下午13:05" was shown.
    @Test
    fun `a day period letter goes away with the 12-hour clock`() {
        assertEquals("H:mm", toTwentyFourHour("Bh:mm"))
        assertEquals("H:mm", toTwentyFourHour("bh:mm"))
        assertEquals("H:mm", toTwentyFourHour("h:mm a"))
        assertEquals("H:mm", toTwentyFourHour("h:mm\u202FB"))
        assertEquals("H 'B' mm", toTwentyFourHour("h 'B' mm"))
    }
}
