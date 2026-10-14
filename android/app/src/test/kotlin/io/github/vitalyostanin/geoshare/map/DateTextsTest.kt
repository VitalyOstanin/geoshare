package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class DateTextsTest {
    private val moment = ZonedDateTime.of(2026, 10, 12, 14, 30, 0, 0, ZoneId.of("UTC"))

    @Test
    fun `a month is written in the language asked for after another was used first`() {
        val english = endsAtText(moment, Locale.US)
        val russian = endsAtText(moment, Locale.forLanguageTag("ru"))

        assertTrue(english.startsWith("12 Oct 2:30"))
        assertNotEquals(english, russian)
        assertTrue(russian.startsWith("12 окт"))
        assertTrue(russian.endsWith("14:30"))
    }

    // A screen that wrote some times in the language's own way and others
    // always on a 24-hour clock showed "2:30 PM" and "14:30" side by side.
    @Test
    fun `the clock follows the language, twelve hours or twenty-four`() {
        assertTrue(clockFormat(Locale.US).format(moment).endsWith("PM"))
        assertEquals("14:30", clockFormat(Locale.forLanguageTag("ru")).format(moment))
        assertTrue(endsAtText(moment, Locale.US).endsWith("PM"))
    }

    @Test
    fun `an invitation end keeps the language asked for as well`() {
        val russian = invitationEndsText(moment, Locale.forLanguageTag("ru"))

        assertEquals("12 октября, 14:30", russian)
        assertTrue(invitationEndsText(moment, Locale.US).startsWith("12 October, 2:30"))
    }

    // The card of a member wrote these in the language the process started
    // with, so a device switched to Russian inside the app showed "Oct 12".
    @Test
    fun `a moment on the card is written in the language asked for, not the process's`() {
        val started = Locale.getDefault()
        Locale.setDefault(Locale.US)

        try {
            val russian = momentText(moment.toInstant(), Locale.forLanguageTag("ru"), moment.zone)

            assertTrue(russian, russian.startsWith("12 окт. 2026"))
            assertTrue(russian, russian.endsWith("14:30"))
            assertTrue(
                momentText(moment.toInstant(), Locale.US, moment.zone).startsWith("Oct 12, 2026"),
            )
        } finally {
            Locale.setDefault(started)
        }
    }
}
