package io.github.vitalyostanin.geoshare.map

import io.github.vitalyostanin.geoshare.collection.STALE_AFTER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class PositionAgeTest {
    private val now: Instant = Instant.parse("2026-09-11T12:00:00Z")

    private fun ageOf(age: Duration): PositionAge = positionAge(now.minus(age), now)

    @Test
    fun `a fresh position is just now`() {
        assertEquals(PositionAge.JustNow, ageOf(Duration.ofSeconds(59)))
    }

    @Test
    fun `a clock running ahead of ours is just now rather than a negative age`() {
        assertEquals(PositionAge.JustNow, ageOf(Duration.ofMinutes(-5)))
    }

    @Test
    fun `a minute old is counted in minutes`() {
        assertEquals(PositionAge.Minutes(1), ageOf(Duration.ofMinutes(1)))
    }

    @Test
    fun `the last minute before an hour is still minutes`() {
        assertEquals(PositionAge.Minutes(59), ageOf(Duration.ofMinutes(59)))
    }

    @Test
    fun `an hour old is counted in hours`() {
        assertEquals(PositionAge.Hours(1), ageOf(Duration.ofHours(1)))
    }

    @Test
    fun `the count is whole units, not rounded up`() {
        assertEquals(PositionAge.Hours(2), ageOf(Duration.ofMinutes(179)))
    }

    @Test
    fun `a day old is counted in days`() {
        assertEquals(PositionAge.Days(1), ageOf(Duration.ofHours(24)))
    }

    @Test
    fun `a position younger than the stale mark is not stale`() {
        assertFalse(isStale(now.minus(STALE_AFTER).plusSeconds(1), now))
    }

    @Test
    fun `exactly at the stale mark counts as stale`() {
        assertTrue(isStale(now.minus(STALE_AFTER), now))
    }

    @Test
    fun `a phone online with a position as old as the grey is stuck`() {
        assertTrue(isStuck(now.minus(STALE_AFTER), now, now))
        assertFalse(isStuck(now.minus(STALE_AFTER).plusSeconds(60), now, now))
    }

    @Test
    fun `a phone gone quiet is old rather than stuck`() {
        val knownAt = now.minus(STALE_AFTER)

        assertFalse(isStuck(knownAt.minus(Duration.ofHours(3)), knownAt, now))
    }
}
