package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnglesTest {
    @Test
    fun `an angle already within half a turn is left alone`() {
        assertEquals(30.0, wrapDegrees(30.0), 1e-9)
        assertEquals(-179.0, wrapDegrees(-179.0), 1e-9)
    }

    @Test
    fun `a turn beyond the far side comes back on the near one`() {
        assertEquals(-170.0, wrapDegrees(190.0), 1e-9)
        assertEquals(170.0, wrapDegrees(-190.0), 1e-9)
    }

    // The number comes out of another member's document, and a loop taking
    // three hundred and sixty off at a time would be at it for weeks.
    @Test
    fun `a degree count the globe has no room for answers at once`() {
        val answer = wrapDegrees(1e15)

        assertTrue("$answer is not on the globe", answer in -180.0..180.0)
    }

    @Test
    fun `an angle that is not a number is not carried into the drawing`() {
        assertEquals(0.0, wrapDegrees(Double.POSITIVE_INFINITY), 1e-9)
        assertEquals(0.0, wrapDegrees(Double.NaN), 1e-9)
    }

    @Test
    fun `a place on the globe is placeable and one outside it is not`() {
        assertTrue(placeable(60.93, 4.34))
        assertFalse(placeable(91.0, 4.34))
        assertFalse(placeable(60.93, 1e15))
        assertFalse(placeable(60.93, Double.NaN))
        assertFalse(placeable(null, 4.34))
        assertFalse(placeable(60.93, null))
    }
}
