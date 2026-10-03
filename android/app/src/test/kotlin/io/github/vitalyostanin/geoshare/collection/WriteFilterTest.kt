package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class WriteFilterTest {
    private val start: Instant = Instant.parse("2026-09-11T12:00:00Z")

    // A northern latitude, which is what the metre-per-degree figures below
    // are taken at.
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

    // The wall clock was stepped ten minutes forward between the fix and the
    // moment it is judged at. On the wall clock the answer is ten minutes
    // old; on the clock that is not set back, one minute.
    @Test
    fun `the age of a fix survives a stepped wall clock`() {
        val fix = home.copy(sinceBoot = Duration.ofMinutes(20))
        val age = since(fix, start.plus(Duration.ofMinutes(10)), Duration.ofMinutes(21))

        assertEquals(Duration.ofMinutes(1), age)
    }

    // Where either side has no count since boot — a fix read back from the
    // trail file, or a caller with no such count to offer — the wall clock
    // answers, as it did before there was a second clock at all.
    @Test
    fun `without a count since boot the age comes off the wall clock`() {
        assertEquals(
            Duration.ofMinutes(10),
            since(home, start.plus(Duration.ofMinutes(10))),
        )
        assertEquals(
            Duration.ofMinutes(10),
            since(
                home.copy(sinceBoot = Duration.ofMinutes(20)),
                start.plus(Duration.ofMinutes(10)),
            ),
        )
    }

    // Two fixes an hour apart on the wall clock and a minute apart since
    // boot: the silence between writes is the minute.
    @Test
    fun `the silence between writes is counted since boot`() {
        val last = home.copy(sinceBoot = Duration.ofMinutes(20))
        val next = home.copy(
            takenAt = start.plus(Duration.ofHours(1)),
            sinceBoot = Duration.ofMinutes(21),
        )

        assertFalse(shouldWrite(last = last, next = next, mode = CollectionMode.Standby))
    }

    @Test
    fun `the distance is measured in metres`() {
        val metres = distanceMetres(home, north(metres = 100.0, afterSeconds = 0))

        assertEquals(100.0, metres, 1.0)
    }

    // Points on opposite sides of the Earth, where rounding carries the
    // haversine past one, and half the way round is the page's figure: both
    // measure on the mean sphere.
    @Test
    fun `points on opposite sides of the earth are half the mean circumference apart`() {
        val here = Fix(latitude = 0.2329, longitude = 12.427, takenAt = start)
        val there = Fix(latitude = -0.2329, longitude = -167.573, takenAt = start)

        val metres = distanceMetres(here, there)

        assertFalse(metres.isNaN())
        assertEquals(20_015_114.0, metres, 1.0)
    }
}
