package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

class AccuracyRingTest {
    @Test
    fun `the ring is closed`() {
        val ring = accuracyRing(61.03268, 4.40300, 100.0)

        assertEquals(ring.first(), ring.last())
    }

    // A hundred metres north of sixty degrees is the same hundred metres east,
    // and the point of dividing by the cosine is that both come out that way.
    @Test
    fun `a hundred metres is a hundred metres in both directions`() {
        val metres = 100.0
        val latitude = 61.0
        val ring = accuracyRing(latitude, 30.0, metres, corners = 4)

        val north = ring[0]
        val east = ring[1]

        assertEquals(metres, northwardsMetres(north.second - latitude), 0.5)
        assertEquals(metres, eastwardsMetres(east.first - 30.0, latitude), 0.5)
    }

    // The width the towers answer with, which is what the circle is drawn for
    // in the first place: it has to be a claim about a district, not a dot.
    @Test
    fun `a tower's width draws a ring a kilometre and a half across`() {
        val ring = accuracyRing(61.0, 4.0, 1_700.0, corners = 4)

        assertEquals(1_700.0, northwardsMetres(ring[0].second - 60.0), 5.0)
    }

    @Test
    fun `a ring at the pole stays on the globe`() {
        val ring = accuracyRing(89.999, 30.0, 10_000.0)

        assertTrue(ring.all { (east, north) -> abs(north) <= 90.0 && abs(east) <= 180.0 })
    }

    private fun northwardsMetres(degrees: Double): Double = degrees / 180.0 * PI * EARTH

    private fun eastwardsMetres(degrees: Double, latitude: Double): Double =
        northwardsMetres(degrees) * cos(latitude / 180.0 * PI)

    private companion object {
        const val EARTH = 6_371_008.8
    }
}
