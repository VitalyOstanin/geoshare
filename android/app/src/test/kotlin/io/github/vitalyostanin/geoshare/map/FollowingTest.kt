package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FollowingTest {
    private val noon: Instant = Instant.parse("2026-09-20T12:00:00Z")

    private fun positionOf(id: String, latitude: Double, longitude: Double): MemberPosition =
        MemberPosition(
            positionId = id,
            memberId = id,
            deviceName = null,
            latitude = latitude,
            longitude = longitude,
            takenAt = noon,
            writtenAt = null,
            mode = null,
            batteryPercent = null,
        )

    @Test
    fun `no gesture at all is never a pause`() {
        assertFalse(isPaused(null, noon))
    }

    @Test
    fun `a gesture a moment ago still holds the camera`() {
        assertTrue(isPaused(noon, noon.plusSeconds(1)))
    }

    // The boundary itself has finished waiting out its own allowance, not
    // still inside it.
    @Test
    fun `a gesture exactly twenty seconds old is no longer a pause`() {
        assertFalse(isPaused(noon, noon.plus(PAUSE_AFTER_A_GESTURE)))
    }

    @Test
    fun `a gesture just short of twenty seconds old still is`() {
        assertTrue(isPaused(noon, noon.plus(PAUSE_AFTER_A_GESTURE).minusMillis(1)))
    }

    @Test
    fun `an empty list has no bounds to fit`() {
        assertNull(boundsOf(emptyList()))
    }

    @Test
    fun `a single position is its own bounds on every edge`() {
        val alone = positionOf("a", latitude = 61.0, longitude = 4.0)

        val expected = MapBounds(west = 4.0, south = 61.0, east = 4.0, north = 61.0)

        assertEquals(expected, boundsOf(listOf(alone)))
    }

    @Test
    fun `several positions are held by their outermost edges`() {
        val positions = listOf(
            positionOf("a", latitude = 61.0, longitude = 4.0),
            positionOf("b", latitude = 61.5, longitude = 3.5),
            positionOf("c", latitude = 60.5, longitude = 4.5),
        )

        assertEquals(
            MapBounds(west = 3.5, south = 60.5, east = 4.5, north = 61.5),
            boundsOf(positions),
        )
    }

    @Test
    fun `bounds within the tolerance on every edge are close`() {
        val one = MapBounds(west = 4.0, south = 61.0, east = 4.5, north = 61.5)
        val other = MapBounds(
            west = 4.0 + BOUNDS_TOLERANCE_DEGREES / 2,
            south = 61.0 - BOUNDS_TOLERANCE_DEGREES / 2,
            east = 4.5,
            north = 61.5,
        )

        assertTrue(boundsAreClose(one, other))
    }

    // A household that grew by one member at the rectangle's corner should
    // move the camera even where most edges barely shifted.
    @Test
    fun `bounds one edge past the tolerance are not close`() {
        val one = MapBounds(west = 4.0, south = 61.0, east = 4.5, north = 61.5)
        // Twice the tolerance rather than exactly it: added to a longitude of
        // thirty degrees and taken away again, the tolerance itself comes back
        // a hair short of its own value, and the edge case being measured here
        // is the rule, not the arithmetic of doubles.
        val other = one.copy(east = 4.5 + 2 * BOUNDS_TOLERANCE_DEGREES)

        assertFalse(boundsAreClose(one, other))
    }

    @Test
    fun `a target present in the list is found by its position id`() {
        val positions = listOf(
            positionOf("a", latitude = 61.0, longitude = 4.0),
            positionOf("b", latitude = 62.0, longitude = 5.0),
        )

        assertEquals("b", targetPosition(positions, "b")?.positionId)
    }

    // The phone stopped sending, or the position it last wrote was rejected —
    // either way, nothing to follow.
    @Test
    fun `a target that has gone quiet is not found`() {
        val positions = listOf(positionOf("a", latitude = 61.0, longitude = 4.0))

        assertNull(targetPosition(positions, "b"))
    }

    @Test
    fun `no target at all is not found`() {
        val positions = listOf(positionOf("a", latitude = 61.0, longitude = 4.0))

        assertNull(targetPosition(positions, null))
    }
}
