package io.github.vitalyostanin.geoshare

import org.junit.Assert.assertEquals
import org.junit.Test

class TextTest {
    @Test
    fun `a cut that ends between the halves of a pair drops the first half too`() {
        assertEquals("a".repeat(39), cutToLimit("a".repeat(39) + "\uD83D\uDE00", 40))
    }

    @Test
    fun `a pair that fits whole is kept`() {
        val fits = "a".repeat(38) + "\uD83D\uDE00"

        assertEquals(fits, cutToLimit(fits, 40))
        assertEquals("abc", cutToLimit("abc", 40))
    }

    // Shared with text.test.mjs: both clients must strip the same characters.
    private val stripped = listOf(
        0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x20, 0xa0, 0x1680, 0x2000, 0x2001, 0x2002, 0x2003,
        0x2004, 0x2005, 0x2006, 0x2007, 0x2008, 0x2009, 0x200a, 0x2028, 0x2029, 0x202f, 0x205f,
        0x3000, 0xfeff,
    )

    // The ones the two platforms' own trim() disagree on or neither strips.
    private val kept = listOf(
        0x1c, 0x1d, 0x1e, 0x1f, 0x85, 0x180e, 0x200b, 0x200c, 0x200d, 0x2060,
    )

    @Test
    fun `trimText strips every character of the shared table from both edges`() {
        for (code in stripped) {
            val mark = code.toChar()

            assertEquals("U+${code.toString(16)}", "ab c", trimText("$mark${mark}ab c$mark"))
        }
    }

    @Test
    fun `trimText keeps the characters the shared table leaves out`() {
        for (code in kept) {
            val mark = code.toChar()

            assertEquals("U+${code.toString(16)}", "${mark}ab$mark", trimText("${mark}ab$mark"))
        }
    }

    @Test
    fun `trimText of only white space is empty and inner white space is kept`() {
        assertEquals("", trimText("\u00a0\ufeff \u3000"))
        assertEquals("a\u00a0b", trimText(" a\u00a0b "))
    }
}
