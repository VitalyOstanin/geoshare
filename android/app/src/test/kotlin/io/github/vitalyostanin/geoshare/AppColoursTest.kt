package io.github.vitalyostanin.geoshare

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.vitalyostanin.geoshare.map.SOUGHT_COLOUR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppColoursTest {
    @Test
    fun `the words of a search read on every surface they stand on`() {
        for (dark in listOf(false, true)) {
            val scheme = appColours(dark)
            val words = soughtWords(dark)

            for (ground in listOf(scheme.surface, scheme.surfaceContainer)) {
                val ratio = contrast(words, ground)

                assertTrue("dark=$dark: $ratio on $ground", ratio >= 4.5f)
            }
        }
    }

    @Test
    fun `by day the words are the red of the pin`() {
        assertEquals(Color(SOUGHT_COLOUR), soughtWords(dark = false))
    }

    private fun contrast(a: Color, b: Color): Float {
        val lighter = maxOf(a.luminance(), b.luminance())
        val darker = minOf(a.luminance(), b.luminance())

        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
