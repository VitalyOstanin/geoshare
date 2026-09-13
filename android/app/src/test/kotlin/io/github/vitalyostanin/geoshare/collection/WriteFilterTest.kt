package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class WriteFilterTest {
    private val start: Instant = Instant.parse("2026-09-11T12:00:00Z")

    // the city, where the phones actually are: the metre-per-degree
    // figures below are the ones at this latitude.
    private val home = Fix(latitude = 60.9386, longitude = 4.3141, takenAt = start)

    /** Roughly [metres] north of [home], which avoids the longitude scaling. */
    private fun north(metres: Double, afterSeconds: Long) = home.copy(
        latitude = home.latitude + metres / 111_320.0,
        takenAt = start.plusSeconds(afterSeconds),
    )

    @Test
    fun `the first fix is always written`() {
        assertTrue(shouldWrite(last = null, next = home, mode = CollectionMode.Standby))
    }

    @Test
    fun `jitter within the threshold is not written`() {
        assertFalse(
            shouldWrite(
                last = home,
                next = north(metres = 40.0, afterSeconds = 60),
                mode = CollectionMode.Standby,
            ),
        )
    }

    @Test
    fun `a move past the threshold is written`() {
        assertTrue(
            shouldWrite(
                last = home,
                next = north(metres = 80.0, afterSeconds = 60),
                mode = CollectionMode.Standby,
            ),
        )
    }

    @Test
    fun `silence is broken on schedule even standing still`() {
        val next = home.copy(takenAt = start.plus(CollectionMode.Standby.writeSilence))

        assertTrue(shouldWrite(last = home, next = next, mode = CollectionMode.Standby))
    }

    @Test
    fun `just short of the silence window nothing is written`() {
        val next = home.copy(
            takenAt = start.plus(CollectionMode.Standby.writeSilence).minusSeconds(1),
        )

        assertFalse(shouldWrite(last = home, next = next, mode = CollectionMode.Standby))
    }

    @Test
    fun `the distance is measured in metres`() {
        val metres = distanceMetres(home, north(metres = 100.0, afterSeconds = 0))

        assertEquals(100.0, metres, 1.0)
    }
}
