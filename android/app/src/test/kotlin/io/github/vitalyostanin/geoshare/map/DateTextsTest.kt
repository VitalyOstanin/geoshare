package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class DateTextsTest {
    private val moment = ZonedDateTime.of(2026, 10, 12, 14, 30, 0, 0, ZoneId.of("UTC"))

    @Test
    fun `a month is written in the language asked for after another was used first`() {
        val english = endsAtText(moment, Locale.ENGLISH)
        val russian = endsAtText(moment, Locale.forLanguageTag("ru"))

        assertEquals("12 Oct 14:30", english)
        assertNotEquals(english, russian)
        assertEquals(true, russian.startsWith("12 окт"))
        assertEquals(true, russian.endsWith("14:30"))
    }

    @Test
    fun `an invitation end keeps the language asked for as well`() {
        val russian = invitationEndsText(moment, Locale.forLanguageTag("ru"))

        assertEquals("12 октября, 14:30", russian)
        assertEquals("12 October, 14:30", invitationEndsText(moment, Locale.ENGLISH))
    }
}
