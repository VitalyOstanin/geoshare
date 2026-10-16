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
}
