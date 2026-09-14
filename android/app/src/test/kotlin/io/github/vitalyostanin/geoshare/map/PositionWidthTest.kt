package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PositionWidthTest {
    // The four kinds of answer seen in the logs of the phones: the satellites
    // under open sky, the Wi-Fi in a flat, the assistance service, and a tower.
    @Test
    fun `an answer under a kilometre is said in whole metres`() {
        assertEquals(PositionWidth.Metres(12), positionWidth(11.7))
        assertEquals(PositionWidth.Metres(28), positionWidth(28.1))
        assertEquals(PositionWidth.Metres(122), positionWidth(122.4))
    }

    @Test
    fun `an answer from a tower is said in kilometres to one place`() {
        assertEquals(PositionWidth.Kilometres(1.7), positionWidth(1_700.0))
    }

    // Nine hundred and ninety-nine and a half metres rounds to a thousand, and
    // "±1000 m" is the one thing the rule exists to avoid writing.
    @Test
    fun `an answer that rounds up to a kilometre is written as one`() {
        assertEquals(PositionWidth.Kilometres(1.0), positionWidth(999.6))
        assertEquals(PositionWidth.Metres(999), positionWidth(999.4))
    }

    // A receiver that calls its answer a fraction of a metre wide is not
    // claiming the phone is at a point, and "±0 m" would say it is.
    @Test
    fun `an answer narrower than a metre is still a metre`() {
        assertEquals(PositionWidth.Metres(1), positionWidth(0.2))
    }

    @Test
    fun `a hundred metres is where the colour changes`() {
        assertFalse(widthIsWide(99.9))
        assertTrue(widthIsWide(WIDTH_WORTH_NOTICING_METRES))
        assertTrue(widthIsWide(1_700.0))
    }
}
