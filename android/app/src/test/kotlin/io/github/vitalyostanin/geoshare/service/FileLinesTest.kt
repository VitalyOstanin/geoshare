package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.collection.FalsePlace
import io.github.vitalyostanin.geoshare.collection.Fix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class FileLinesTest {
    private val fix =
        Fix(
            latitude = 59.93,
            longitude = 30.31,
            takenAt = Instant.ofEpochMilli(1_760_000_000_000),
            accuracyMetres = 12.5,
        )

    @Test
    fun `a place of the trail comes back from its line`() {
        assertEquals(fix, trailFixOf(trailLine(fix)))
    }

    @Test
    fun `a place without an accuracy comes back without one`() {
        val unsure = fix.copy(accuracyMetres = null)

        assertEquals("59.93,30.31,1760000000000,", trailLine(unsure))
        assertEquals(unsure, trailFixOf(trailLine(unsure)))
    }

    @Test
    fun `a line of the trail that is not a place is left out`() {
        assertNull(trailFixOf(""))
        assertNull(trailFixOf("59.93,30.31,1760000000000"))
        assertNull(trailFixOf("north,30.31,1760000000000,5"))
        assertNull(trailFixOf("59.93,30.31,yesterday,5"))
    }

    private val place =
        FalsePlace(
            latitude = 60.45,
            longitude = 31.6,
            radiusMetres = 800.0,
            named = "Lake, north shore",
        )

    @Test
    fun `a false place comes back from its line, a comma in its name and all`() {
        assertEquals(place, falsePlaceOf(falsePlaceLine(place)))
    }

    @Test
    fun `a line break in a false place's name does not lose the place`() {
        val broken = place.copy(named = "Lake\r\nnorth shore\nby the pier")
        val line = falsePlaceLine(broken)

        assertEquals(1, line.lines().size)
        assertEquals(place.copy(named = "Lake north shore by the pier"), falsePlaceOf(line))
    }

    @Test
    fun `a line that is not a false place, or not one that can be, is left out`() {
        assertNull(falsePlaceOf("60.45,31.6,800"))
        assertNull(falsePlaceOf("60.45,east,800,Lake"))
        assertNull(falsePlaceOf("91.0,31.6,800,Past the pole"))
        assertNull(falsePlaceOf("60.45,31.6,0,No width"))
    }
}
