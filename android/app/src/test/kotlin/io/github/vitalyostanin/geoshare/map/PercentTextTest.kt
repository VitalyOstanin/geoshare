package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** A charge level in the language the screen is read in. */
class PercentTextTest {
    @Test
    fun `English writes the sign against the number`() {
        assertEquals("64%", percentText(64, Locale.ENGLISH))
    }

    @Test
    fun `Russian sets the sign apart from the number`() {
        val text = percentText(64, Locale.forLanguageTag("ru"))

        assertTrue(text, text.startsWith("64") && text.endsWith("%") && text.length > 3)
    }
}
