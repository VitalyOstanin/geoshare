package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** What is made of one fix, from what the service read at the moment it came. */
class JudgementTest {
    @Test
    fun `a fix too far to have been reached is left out while jumps are kept out`() {
        val next = fixAt(61.07770, 4.40310, 34, accuracy = 12.0)

        val judged = judge(next, trusted = false, grounds(lastFix = ANCHOR, now = next.takenAt))

        assertEquals(JumpReason.Speed, judged.jump)
        assertFalse(judged.agreed)
        assertFalse(judged.home)
        assertTrue(judged.leftOutAsJump(jumpsFiltered = true))
        assertFalse(judged.leftOutAsJump(jumpsFiltered = false))
    }

    // The towers answer in kilometres: measured against the satellites' rules
    // the one source the spoofer cannot reach would be thrown away.
    @Test
    fun `an answer from the towers is never a jump and never settling`() {
        val next =
            fixAt(61.07770, 4.40310, 34, accuracy = 1_500.0, sinceBoot = Duration.ofSeconds(5))

        val judged = judge(next, trusted = true, grounds(lastFix = ANCHOR, now = next.takenAt))

        assertNull(judged.jump)
        assertFalse(judged.settling)
        assertFalse(judged.leftOutAsJump(jumpsFiltered = true))
    }

    @Test
    fun `a receiver just switched on with no satellites under it is settling`() {
        val next = fixAt(61.03270, 4.40310, 34, accuracy = 12.0, sinceBoot = Duration.ofSeconds(5))

        val judged = judge(next, trusted = false, grounds(lastFix = null, now = next.takenAt))

        assertTrue(judged.settling)
    }

    // The last position gone wrong: the fix before agrees with this one and the
    // Wi-Fi around the device names the same place, so the jump is let through.
    @Test
    fun `a jump the fix before and the wi-fi agree with is not left out`() {
        val candidate = fixAt(61.07768, 4.40300, 17, accuracy = 15.0)
        val next = fixAt(61.07770, 4.40310, 34, accuracy = 12.0)
        val wifi = fixAt(61.07775, 4.40290, 30, accuracy = 60.0)

        val judged =
            judge(
                next,
                trusted = false,
                grounds(
                    lastFix = ANCHOR,
                    now = next.takenAt,
                ).copy(candidate = candidate, networkSays = wifi),
            )

        assertNotNull(judged.jump)
        assertTrue(judged.agreed)
        assertFalse(judged.leftOutAsJump(jumpsFiltered = true))
    }

    // Visits are positions that were written, and while a spoofer runs
    // nothing is: answering from home is what says the anchor is the lie.
    @Test
    fun `a jump back to a place the device keeps coming back to is not left out`() {
        val home = { minute: Long -> fixAt(61.03268, 4.40300, minute * 60) }
        val faraway = fixAt(61.428, 5.420, 61 * 60)
        val next = home(62)

        val judged =
            judge(
                next,
                trusted = false,
                grounds(
                    lastFix = faraway,
                    now = next.takenAt,
                ).copy(trail = Trail(listOf(home(0), home(30), home(60)))),
            )

        assertNotNull(judged.jump)
        assertFalse(judged.agreed)
        assertTrue(judged.home)
        assertFalse(judged.leftOutAsJump(jumpsFiltered = true))
    }

    // DEV-07. A device lying at home wrote a place 290 kilometres away,
    // stamped a year earlier by a replayed recording; the time it carried gave
    // it away.
    @Test
    fun `an answer stamped a year behind the device's clock is left out as long past`() {
        val next = fixAt(61.03270, 4.40310, 34, accuracy = 12.0)
        val now = next.takenAt.plus(Duration.ofDays(406))

        val judged = judge(next, trusted = false, grounds(lastFix = ANCHOR, now = now))

        assertEquals(JumpReason.PastTime, judged.jump)
        assertTrue(judged.leftOutAsJump(jumpsFiltered = true))
    }

    // DEV-07. The replayed answer reached the device through the fallback to
    // the towers too.
    @Test
    fun `a long past answer from the towers is left out all the same`() {
        val next = fixAt(61.03270, 4.40310, 34, accuracy = 1_500.0)
        val now = next.takenAt.plus(Duration.ofDays(406))

        val judged = judge(next, trusted = true, grounds(lastFix = ANCHOR, now = now))

        assertEquals(JumpReason.PastTime, judged.jump)
        assertTrue(judged.leftOutAsJump(jumpsFiltered = true))
    }

    // DEV-07. An answer that waited a few minutes to be handed over is still
    // about this outing.
    @Test
    fun `an answer nine minutes behind the clock is not long past`() {
        val next = fixAt(61.03270, 4.40310, 34, accuracy = 12.0)
        val now = next.takenAt.plus(Duration.ofMinutes(9))

        val judged = judge(next, trusted = false, grounds(lastFix = ANCHOR, now = now))

        assertNull(judged.jump)
    }

    // DEV-07. A moment ahead of the device's clock says the clocks disagree,
    // not that the answer is old.
    @Test
    fun `an answer stamped ahead of the clock is not long past`() {
        val next = fixAt(61.03270, 4.40310, 34, accuracy = 12.0)
        val now = next.takenAt.minus(Duration.ofDays(406))

        val judged = judge(next, trusted = true, grounds(lastFix = ANCHOR, now = now))

        assertNull(judged.jump)
    }

    // DEV-07. Both ways out ask whether the place is right, and a long past
    // answer may name a place the device has been: what is wrong is when.
    @Test
    fun `a long past answer is let through neither by agreement nor by coming home`() {
        val home = { minute: Long -> fixAt(61.03268, 4.40300, minute * 60) }
        val faraway = fixAt(61.428, 5.420, 61 * 60)
        val next = home(62)
        val now = next.takenAt.plus(Duration.ofDays(406))
        val candidate = fixAt(61.03269, 4.40302, 62 * 60 - 17, accuracy = 15.0)
        val wifi = fixAt(61.03271, 4.40298, 62 * 60 - 4, accuracy = 60.0)

        val judged =
            judge(
                next,
                trusted = false,
                grounds(lastFix = faraway, now = now).copy(
                    candidate = candidate,
                    networkSays = wifi,
                    trail = Trail(listOf(home(0), home(30), home(60))),
                ),
            )

        assertEquals(JumpReason.PastTime, judged.jump)
        assertFalse(judged.agreed)
        assertFalse(judged.home)
        assertTrue(judged.leftOutAsJump(jumpsFiltered = true))
    }

    @Test
    fun `how the fix was found is read beside the receiver's own last answer`() {
        val next = fixAt(61.03270, 4.40310, 34, accuracy = 12.0)
        val satellites = fixAt(61.03270, 4.40310, 33, accuracy = 8.0)

        val judged =
            judge(
                next,
                trusted = false,
                grounds(lastFix = null, now = next.takenAt).copy(satellites = satellites),
            )

        assertEquals(foundBy(next, satellites), judged.foundBy)
    }

    private fun grounds(lastFix: Fix?, now: Instant) = Grounds(
        lastFix = lastFix,
        candidate = null,
        satellites = null,
        networkSays = null,
        now = now,
    )

    private fun fixAt(
        latitude: Double,
        longitude: Double,
        second: Long,
        accuracy: Double? = null,
        sinceBoot: Duration? = null,
    ) = Fix(
        latitude = latitude,
        longitude = longitude,
        takenAt = START.plusSeconds(second),
        accuracyMetres = accuracy,
        sinceBoot = sinceBoot,
    )

    private companion object {
        val START: Instant = Instant.parse("2026-09-13T12:00:00Z")
        val ANCHOR = Fix(61.03268, 4.40300, START, accuracyMetres = 20.0)
    }
}
