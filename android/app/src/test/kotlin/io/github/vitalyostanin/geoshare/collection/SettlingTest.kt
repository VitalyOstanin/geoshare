package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/**
 * The numbers here are the run of a phone left on a table overnight: a boot at
 * 22:11:55, an answer twenty-three seconds later a hundred and twenty-two
 * metres wide with no GPS satellite in it, and two moves of sixty-six and two
 * hundred and twenty-three metres in the four minutes after, while the phone
 * lay untouched on a table.
 */
class SettlingTest {
    @Test
    fun `a receiver twenty-three seconds off a boot has not settled`() {
        assertTrue(stillSettling(Duration.ofSeconds(23), heard(gps = 0, glonass = 1, beidou = 1)))
    }

    @Test
    fun `a receiver with four satellites in the fix has settled`() {
        assertFalse(stillSettling(Duration.ofSeconds(40), heard(gps = 2, glonass = 1, beidou = 1)))
    }

    // The clock is the end of the window and not the start of it: a phone
    // indoors reports no satellites for as long as it stays there, and a
    // window waiting for four of them would never close.
    @Test
    fun `a receiver that has found nothing in five minutes is no longer settling`() {
        assertFalse(stillSettling(Duration.ofMinutes(5), heard(gps = 0, glonass = 0, beidou = 0)))
        assertTrue(
            stillSettling(
                Duration.ofMinutes(5).minusSeconds(1),
                heard(gps = 0, glonass = 0, beidou = 0),
            ),
        )
    }

    // A fix read back from the trail file carries no count since the boot:
    // that count starts again at every boot, and the file outlives them.
    @Test
    fun `a fix without a count since the boot is not treated as settling`() {
        assertFalse(stillSettling(null, emptyMap()))
    }

    @Test
    fun `the first answer of that boot is too wide to keep`() {
        assertTrue(settlingJump(null, fixAt(60.9, 4.3, 0, accuracy = 122.0)))
    }

    // The same answer outside the window is kept: five hundred metres is where
    // the ordinary rule stands, and it is the first fix of the round besides.
    @Test
    fun `the same answer of a settled receiver is kept`() {
        assertNull(whyAJump(null, fixAt(60.9, 4.3, 0, accuracy = 122.0)))
    }

    @Test
    fun `sixty-six metres at a cyclist's pace is a jump while the receiver settles`() {
        val first = fixAt(60.9, 4.3, 0, accuracy = 68.0)
        // Sixty-six metres of latitude in ten seconds: 6.6 m/s.
        val second = fixAt(60.9 + 0.00059, 4.3, 10, accuracy = 68.0)

        assertEquals(JumpReason.Settling, whyAJump(first, second, settling = true))
    }

    // What the window is measured against: the same pair passes every rule
    // written for a receiver that is working, which is why the false moves of
    // those minutes reached the map at all.
    @Test
    fun `the same pair is a move once the receiver has settled`() {
        val first = fixAt(60.9, 4.3, 0, accuracy = 68.0)
        val second = fixAt(60.9 + 0.00059, 4.3, 10, accuracy = 68.0)

        assertNull(whyAJump(first, second))
    }

    @Test
    fun `two hundred metres at eighteen metres a second is a jump`() {
        val first = fixAt(60.9, 4.3, 0, accuracy = 40.0)
        // Two hundred and twenty-three metres of latitude in twelve seconds.
        val second = fixAt(60.9 + 0.002, 4.3, 12, accuracy = 40.0)

        assertEquals(JumpReason.Settling, whyAJump(first, second, settling = true))
    }

    // A person walking away from a phone's last position is not what the
    // window is for: it would be caught by nothing outside it either.
    @Test
    fun `a walk is a move even while the receiver settles`() {
        val first = fixAt(60.9, 4.3, 0, accuracy = 20.0)
        // Fourteen metres of latitude in ten seconds: 1.4 m/s.
        val second = fixAt(60.9 + 0.000126, 4.3, 10, accuracy = 20.0)

        assertNull(whyAJump(first, second, settling = true))
    }

    // The width is asked without an anchor, and that is the point of it: the
    // first answer after a boot has nothing before it to be compared with.
    @Test
    fun `the first answer of a round is weighed by its width alone`() {
        assertEquals(
            JumpReason.Settling,
            whyAJump(null, fixAt(60.9, 4.3, 0, accuracy = 122.0), settling = true),
        )
        assertNull(whyAJump(null, fixAt(60.9, 4.3, 0, accuracy = 40.0), settling = true))
    }

    private fun heard(gps: Int, glonass: Int, beidou: Int) = mapOf(
        Constellation.Gps to Counted(heard = 8, usedInFix = gps),
        Constellation.Glonass to Counted(heard = 6, usedInFix = glonass),
        Constellation.Beidou to Counted(heard = 5, usedInFix = beidou),
    )

    private fun fixAt(latitude: Double, longitude: Double, second: Long, accuracy: Double? = null) =
        Fix(
            latitude = latitude,
            longitude = longitude,
            takenAt = Instant.parse("2026-09-14T22:11:55Z").plusSeconds(second),
            accuracyMetres = accuracy,
        )
}
