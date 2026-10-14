package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class StillnessTest {
    // The gap the other rules leave open: a road drawn slowly enough implies a
    // walking pace, carries a narrow width and contradicts nothing, so only
    // the device's own sensors are left to say that nobody carried it anywhere.
    @Test
    fun `a kilometre travelled by a device that never moved is a jump`() {
        assertTrue(movedWhileStanding(at(0), fixAt(61.03268, 4.40300, 10), farAway()))
    }

    @Test
    fun `the same journey is a move when the sensors have said nothing`() {
        assertFalse(movedWhileStanding(null, fixAt(61.03268, 4.40300, 10), farAway()))
    }

    // Standing still since a minute ago says nothing about the ten minutes
    // before it, which is where the journey would have happened.
    @Test
    fun `stillness that began after the last position says nothing about it`() {
        assertFalse(movedWhileStanding(at(20), fixAt(61.03268, 4.40300, 10), farAway()))
    }

    @Test
    fun `drifting across the yard is not travelling`() {
        val near = fixAt(61.03268, 4.40300 + 0.005, 70)

        assertFalse(movedWhileStanding(at(0), fixAt(61.03268, 4.40300, 10), near))
    }

    // Two answers a kilometre wide that land a kilometre apart are one place
    // twice, and the sensors are not being contradicted by them.
    @Test
    fun `answers wider than the distance between them are not a journey`() {
        val here = fixAt(61.03268, 4.40300, 10, accuracy = 1_000.0)

        assertFalse(movedWhileStanding(at(0), here, farAway(accuracy = 1_000.0)))
    }

    @Test
    fun `the rule reaches the filter as a jump of its own`() {
        val last = fixAt(61.03268, 4.40300, 10)

        assertEquals(JumpReason.Unmoved, whyAJump(last, farAway(), at(0)))
        assertNull(whyAJump(last, farAway(), null))
    }

    // Half an hour at a walking pace, so that neither the speed nor the width
    // has anything to say about it; a kilometre and a half from the last
    // position, which is more than a device drifts while lying on a table.
    private fun farAway(accuracy: Double? = null) =
        fixAt(61.03268, 4.40300 + 0.027, 40, accuracy = accuracy)

    private fun fixAt(latitude: Double, longitude: Double, minute: Long, accuracy: Double? = null) =
        Fix(
            latitude = latitude,
            longitude = longitude,
            takenAt = at(minute),
            accuracyMetres = accuracy,
        )

    private fun at(minute: Long): Instant = START.plus(Duration.ofMinutes(minute))

    private companion object {
        val START: Instant = Instant.parse("2026-09-13T09:00:00Z")
    }
}
