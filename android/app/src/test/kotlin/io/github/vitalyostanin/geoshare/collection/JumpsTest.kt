package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class JumpsTest {
    @Test
    fun `the first fix of a round has nothing to have jumped from`() {
        assertFalse(looksLikeAJump(null, fixAt(60.9, 4.3, 0)))
    }

    @Test
    fun `a walk down the street is a move`() {
        val first = fixAt(60.9, 4.3, 0)
        // Eighty metres of longitude in a minute: a brisk walk.
        val second = fixAt(60.9, 4.3 + 0.0014, 60)

        assertFalse(looksLikeAJump(first, second))
    }

    @Test
    fun `a ride at motorway speed is a move`() {
        val first = fixAt(60.9, 4.3, 0)
        // Two kilometres of latitude in a minute: 120 km/h.
        val second = fixAt(60.9 + 0.018, 4.3, 60)

        assertFalse(looksLikeAJump(first, second))
    }

    @Test
    fun `the other side of the city in a quarter of a minute is a jump`() {
        val first = fixAt(60.9, 4.3, 0)
        val second = fixAt(60.98, 4.56, 15)

        assertTrue(looksLikeAJump(first, second))
    }

    @Test
    fun `an answer half a kilometre wide says nothing at all`() {
        val first = fixAt(60.9, 4.3, 0, accuracy = 10.0)
        val second = fixAt(60.9, 4.3, 60, accuracy = USELESS_ACCURACY_METRES)

        assertTrue(looksLikeAJump(first, second))
    }

    // Two answers whose doubt overlaps are one place said twice, however far
    // apart their coordinates happen to fall.
    @Test
    fun `a step inside the doubt of both answers is not a jump`() {
        val first = fixAt(60.9, 4.3, 0, accuracy = 60.0)
        val second = fixAt(60.9 + 0.0008, 4.3, 1, accuracy = 60.0)

        assertFalse(looksLikeAJump(first, second))
    }

    @Test
    fun `two places at one time is the provider contradicting itself`() {
        val first = fixAt(60.9, 4.3, 60, accuracy = 5.0)
        val second = fixAt(60.98, 4.56, 60, accuracy = 5.0)

        assertTrue(looksLikeAJump(first, second))
    }

    @Test
    fun `the implied speed is the distance over the time`() {
        val first = fixAt(60.9, 4.3, 0)
        val second = fixAt(60.9 + 0.0009, 4.3, 10)
        val speed = impliedSpeedMps(first, second)

        // A hundred metres in ten seconds, give or take the earth's shape.
        assertEquals(10.0, speed!!, 0.5)
    }

    @Test
    fun `a clock that does not run forward gives no speed`() {
        assertNull(impliedSpeedMps(fixAt(60.9, 4.3, 60), fixAt(60.9, 4.4, 60)))
    }

    private fun fixAt(latitude: Double, longitude: Double, second: Long, accuracy: Double? = null) =
        Fix(
            latitude = latitude,
            longitude = longitude,
            takenAt = Instant.parse("2026-09-13T12:00:00Z").plusSeconds(second),
            accuracyMetres = accuracy,
        )
}
