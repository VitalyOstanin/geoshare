package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

/**
 * That a card tells the age of the position apart from the age of the news.
 *
 * The two part whenever a device says a position again, which is what a device
 * does while its satellites are being spoofed: the stamp moves every few
 * seconds and the position stays where it was hours ago. A reader counting the
 * age from the stamp is told the member is standing where the marker is.
 */
class PositionMomentsTest {
    private val taken: Instant = Instant.parse("2026-09-15T09:00:00Z")

    private fun position(takenAt: Instant, writtenAt: Instant?): MemberPosition = MemberPosition(
        positionId = "member.phone",
        memberId = "member",
        deviceName = null,
        latitude = 61.0,
        longitude = 4.0,
        takenAt = takenAt,
        writtenAt = writtenAt,
        mode = null,
        batteryPercent = null,
    )

    @Test
    fun `a position said again keeps the age of the position`() {
        val said = position(taken, taken.plus(Duration.ofHours(3)))

        assertEquals(taken, said.positionAt)
        assertEquals(taken.plus(Duration.ofHours(3)), said.knownAt)
    }

    @Test
    fun `a fresh position has the two moments a breath apart`() {
        val fresh = position(taken, taken.plusSeconds(1))

        assertEquals(taken, fresh.positionAt)
        assertEquals(taken.plusSeconds(1), fresh.knownAt)
    }

    // A device whose clock runs ahead would otherwise date its position in the
    // future, and the card would count the age backwards from a moment that
    // has not arrived.
    @Test
    fun `a clock running ahead of the server is capped at the stamp`() {
        val ahead = position(taken.plus(Duration.ofMinutes(10)), taken)

        assertEquals(taken, ahead.positionAt)
        assertEquals(taken, ahead.knownAt)
    }

    // The moment between the device writing and the server stamping, where the
    // local copy carries no stamp at all.
    @Test
    fun `a position not yet stamped is dated by the phone that took it`() {
        val unstamped = position(taken, null)

        assertEquals(taken, unstamped.positionAt)
        assertEquals(taken, unstamped.knownAt)
    }
}
