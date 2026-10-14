package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class TrailTest {
    @Test
    fun `the trail keeps the last fifty places and no more`() {
        val trail = (1..60).fold(Trail()) { it, minute -> it.remembering(home(minute.toLong())) }

        assertEquals(TRAIL_LENGTH, trail.fixes.size)
        assertEquals(home(60), trail.fixes.last())
    }

    @Test
    fun `a place from last week is no longer where the device has been`() {
        val old = Trail(listOf(home(0)))
        val now = home(Duration.ofDays(4).toMinutes())

        assertEquals(listOf(now), old.remembering(now).fixes)
    }

    @Test
    fun `one visit does not make a place familiar`() {
        assertFalse(Trail(listOf(home(0))).knows(home(0)))
    }

    // Three visits inside a minute are one visit that happened to be written
    // three times: what makes a place the device's own is coming back to it.
    @Test
    fun `three visits in a minute do not make a place familiar`() {
        val trail = Trail(listOf(home(0), home(1), home(2)))

        assertFalse(trail.knows(home(3)))
    }

    @Test
    fun `three visits across an hour make a place familiar`() {
        val trail = Trail(listOf(home(0), home(30), home(60)))

        assertTrue(trail.knows(home(61)))
    }

    @Test
    fun `a place three hundred metres away is the same place`() {
        val trail = Trail(listOf(home(0), home(30), home(60)))
        // Two hundred and twenty metres north of home.
        val nearby = Fix(HOME_LATITUDE + 0.002, HOME_LONGITUDE, at(61))

        assertTrue(trail.knows(nearby))
    }

    @Test
    fun `a place far off is not one this device knows`() {
        val trail = Trail(listOf(home(0), home(30), home(60)))

        assertFalse(trail.knows(faraway(61)))
    }

    // The anchor left standing far off, and the device answering from home:
    // every true fix reads as a jump, and the width rule has no clock to run
    // out. Being home is what says which of the two is the lie.
    @Test
    fun `a fix from a familiar place beats an anchor from nowhere`() {
        val trail = Trail(listOf(home(0), home(30), home(60)))

        assertTrue(comingHome(trail, faraway(61), home(62)))
    }

    @Test
    fun `a fix out to the false place is not coming home`() {
        val trail = Trail(listOf(home(0), home(30), home(60)))

        assertFalse(comingHome(trail, home(61), faraway(62)))
    }

    @Test
    fun `standing still at home is not coming home`() {
        val trail = Trail(listOf(home(0), home(30), home(60)))

        assertFalse(comingHome(trail, home(61), home(62)))
    }

    @Test
    fun `with no anchor there is nothing to come back from`() {
        val trail = Trail(listOf(home(0), home(30), home(60)))

        assertFalse(comingHome(trail, null, home(62)))
    }

    // While the spoofing runs, nothing is written, so nothing about the false place
    // reaches the trail — which is what keeps an hour of it from teaching the
    // device that the false place is home.
    @Test
    fun `a trail of false fixes would make the false place familiar, and is never gathered`() {
        val spoofed = Trail(listOf(faraway(0), faraway(30), faraway(60)))

        assertTrue(spoofed.knows(faraway(61)))
    }

    // A process killed inside a spoofing episode comes back with no anchor at
    // all, and the first false answer then becomes one. The last place the
    // device was sure of is a better start, as long as it is recent enough for
    // the speed rule to still mean something.
    @Test
    fun `a place from half an hour ago is where to measure from`() {
        val trail = Trail(listOf(home(0), home(30)))

        assertEquals(home(30), anchorFrom(trail, at(60)))
    }

    // Seventy kilometres in a day is a walk: an old anchor does not catch a
    // spoofer, it excuses one.
    @Test
    fun `a place from yesterday is not where to measure from`() {
        val trail = Trail(listOf(home(0)))

        assertEquals(null, anchorFrom(trail, at(Duration.ofDays(1).toMinutes())))
    }

    @Test
    fun `a device that has never written anything has nothing to measure from`() {
        assertEquals(null, anchorFrom(Trail(), at(0)))
    }

    // A device switched on in the morning where it spent the night: the last
    // position is hours old, and what makes it worth measuring from is not its
    // freshness but the trail saying the device belongs there.
    @Test
    fun `a place of its own is still where to measure from hours later`() {
        val trail = Trail(listOf(home(0), home(60), home(120)))

        assertEquals(home(120), anchorFrom(trail, at(360)))
    }

    // One written position is a place the device passed through, and a device
    // that passed through somewhere four hours ago is anywhere by now.
    @Test
    fun `a place passed through once does not stand as an anchor past the hour`() {
        val trail = Trail(listOf(home(0)))

        assertEquals(null, anchorFrom(trail, at(240)))
    }

    @Test
    fun `even a place of its own stops being an anchor after half a day`() {
        val trail = Trail(listOf(home(0), home(60), home(120)))

        assertEquals(null, anchorFrom(trail, at(120 + Duration.ofHours(13).toMinutes())))
    }

    private fun home(minute: Long) = Fix(HOME_LATITUDE, HOME_LONGITUDE, at(minute))

    private fun faraway(minute: Long) = Fix(61.428, 5.420, at(minute))

    private fun at(minute: Long): Instant = START.plus(Duration.ofMinutes(minute))

    private companion object {
        const val HOME_LATITUDE = 61.03268
        const val HOME_LONGITUDE = 4.40300
        val START: Instant = Instant.parse("2026-09-13T09:00:00Z")
    }
}
