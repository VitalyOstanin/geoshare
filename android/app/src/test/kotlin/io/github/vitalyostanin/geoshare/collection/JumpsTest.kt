package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class JumpsTest {
    @Test
    fun `the first fix of a round has nothing to have jumped from`() {
        assertNull(whyAJump(null, fixAt(60.9, 4.3, 0)))
    }

    @Test
    fun `a walk down the street is a move`() {
        val first = fixAt(60.9, 4.3, 0)
        // Eighty metres of longitude in a minute: a brisk walk.
        val second = fixAt(60.9, 4.3 + 0.0014, 60)

        assertNull(whyAJump(first, second))
    }

    @Test
    fun `a ride at motorway speed is a move`() {
        val first = fixAt(60.9, 4.3, 0)
        // Two kilometres of latitude in a minute: 120 km/h.
        val second = fixAt(60.9 + 0.018, 4.3, 60)

        assertNull(whyAJump(first, second))
    }

    @Test
    fun `the other side of the city in a quarter of a minute is a jump`() {
        val first = fixAt(60.9, 4.3, 0)
        val second = fixAt(60.98, 4.56, 15)

        assertEquals(JumpReason.Speed, whyAJump(first, second))
    }

    @Test
    fun `an answer half a kilometre wide says nothing at all`() {
        val first = fixAt(60.9, 4.3, 0, accuracy = 10.0)
        val second = fixAt(60.9, 4.3, 60, accuracy = USELESS_ACCURACY_METRES)

        assertEquals(JumpReason.Width, whyAJump(first, second))
    }

    // Two answers whose doubt overlaps are one place said twice, however far
    // apart their coordinates happen to fall.
    @Test
    fun `a step inside the doubt of both answers is not a jump`() {
        val first = fixAt(60.9, 4.3, 0, accuracy = 60.0)
        val second = fixAt(60.9 + 0.0008, 4.3, 1, accuracy = 60.0)

        assertNull(whyAJump(first, second))
    }

    @Test
    fun `two places at one time is the provider contradicting itself`() {
        val first = fixAt(60.9, 4.3, 60, accuracy = 5.0)
        val second = fixAt(60.98, 4.56, 60, accuracy = 5.0)

        assertEquals(JumpReason.Contradiction, whyAJump(first, second))
    }

    @Test
    fun `the implied speed is the distance over the time`() {
        val first = fixAt(60.9, 4.3, 0)
        val second = fixAt(60.9 + 0.0009, 4.3, 10)
        val speed = impliedSpeedMps(first, second)

        // A hundred metres in ten seconds, give or take the earth's shape.
        assertEquals(10.0, speed!!, 0.5)
    }

    @Test
    fun `a clock that does not run forward gives no speed`() {
        assertNull(impliedSpeedMps(fixAt(60.9, 4.3, 60), fixAt(60.9, 4.4, 60)))
    }

    // The answer that started all this came both too wide and too far at
    // once. The log names one rule, and the width is the one to name: it is
    // the provider's own admission, whereas the speed is our arithmetic.
    @Test
    fun `a wide answer far away is caught by its width`() {
        val first = fixAt(60.9, 4.3, 0, accuracy = 30.0)
        val second = fixAt(60.98, 4.56, 15, accuracy = 528.0)

        assertEquals(JumpReason.Width, whyAJump(first, second))
    }

    // What lets the marker out of a last position that has gone wrong: two
    // answers from the same place agree with each other even when neither of
    // them agrees with where the phone was last believed to be.
    @Test
    fun `two fixes from the same place agree`() {
        val one = fixAt(60.94, 4.31, 0, accuracy = 20.0)
        val other = fixAt(60.9401, 4.3103, 18, accuracy = 20.0)

        assertTrue(fixesAgree(one, other))
    }

    @Test
    fun `answers thrown in opposite directions agree with nothing`() {
        val one = fixAt(60.94, 4.31, 0, accuracy = 20.0)
        val other = fixAt(61.02, 4.56, 18, accuracy = 20.0)

        assertFalse(fixesAgree(one, other))
    }

    // Width is what the provider says about its own answer, not about where
    // the phone is. Two wide answers from one place still agree, and refusing
    // them is what leaves a jammed phone standing on the map for good.
    @Test
    fun `a pair of wide answers from one place still agree`() {
        val one = fixAt(60.94, 4.31, 0, accuracy = 700.0)
        val other = fixAt(60.9401, 4.3103, 18, accuracy = 700.0)

        assertTrue(fixesAgree(one, other))
        assertEquals(JumpReason.Width, whyAJump(one, other))
    }

    // A spoofed signal draws a road: every pair of its fixes agrees, so a rule
    // that lets agreement overrule the speed hands the anchor over in the time
    // between two answers. Against the speed the clock is the way out instead.
    @Test
    fun `agreement overrules a width and never a speed`() {
        assertTrue(mayBeConfirmed(JumpReason.Width))
        assertFalse(mayBeConfirmed(JumpReason.Speed))
        assertFalse(mayBeConfirmed(JumpReason.Contradiction))
        assertFalse(mayBeConfirmed(JumpReason.Unmoved))
    }

    // The pair that took the anchor seventy-three kilometres in seventeen
    // seconds: both fixes narrow, both agreeing with each other, and the rule
    // that caught them the speed rather than the width. The place the spoofer
    // actually used is now caught by the list of false places before the speed
    // is ever asked about, so the same geometry is mirrored west of the city to
    // leave this test about the speed rule alone.
    @Test
    fun `a narrow pair far from the anchor cannot confirm each other`() {
        val anchor = fixAt(61.03268, 4.40300, 0, accuracy = 20.0)
        val first = fixAt(61.42545, 3.36127, 17, accuracy = 7.0)
        val second = fixAt(61.42291, 3.36874, 34, accuracy = 13.0)

        assertEquals(JumpReason.Speed, whyAJump(anchor, first))
        assertEquals(JumpReason.Speed, whyAJump(anchor, second))
        assertTrue(fixesAgree(first, second))
        assertFalse(mayBeConfirmed(whyAJump(anchor, second)!!))
    }

    // The pair that carried the second phone out of its own city: answers
    // twenty-one and nineteen kilometres wide, forty kilometres from the flat,
    // agreeing with each other for no better reason than that answers that
    // wide agree with anything within a region.
    @Test
    fun `two answers a region wide agreeing is not a phone saying where it is`() {
        val anchor = fixAt(61.03268, 4.40300, 0, accuracy = 20.0)
        val first = fixAt(61.30000, 4.90000, 60, accuracy = 21_781.0)
        val second = fixAt(61.31000, 4.91000, 304, accuracy = 19_200.0)

        assertEquals(JumpReason.Width, whyAJump(anchor, first))
        assertTrue("wide answers agree with anything", fixesAgree(first, second))
        assertFalse(agreementStands(JumpReason.Width, first, second))
    }

    // What agreement was meant for: an answer too wide to place the phone, a
    // second one agreeing with it, and something the transmitter cannot reach
    // naming the same place.
    @Test
    fun `a moderate width is confirmed where the other radios name the place`() {
        val first = fixAt(61.10000, 4.50000, 0, accuracy = 600.0)
        val second = fixAt(61.10050, 4.50100, 60, accuracy = 700.0)
        val towers = fixAt(61.10020, 4.50040, 55, accuracy = 900.0)

        assertTrue(agreementStands(JumpReason.Width, first, second, towers, second.takenAt))
        assertFalse(
            "nothing to ask is not agreement",
            agreementStands(JumpReason.Width, first, second),
        )
    }

    // The towers answering about somewhere else while the two wide answers
    // vouch for each other. The width they carry is granted, and they are
    // still not where the phone is.
    @Test
    fun `a width is not confirmed where the other radios name another place`() {
        val first = fixAt(61.10000, 4.50000, 0, accuracy = 600.0)
        val second = fixAt(61.10050, 4.50100, 60, accuracy = 700.0)
        val towers = fixAt(61.03268, 4.40300, 55, accuracy = 900.0)

        assertFalse(agreementStands(JumpReason.Width, first, second, towers, second.takenAt))
    }

    private fun fixAt(latitude: Double, longitude: Double, second: Long, accuracy: Double? = null) =
        Fix(
            latitude = latitude,
            longitude = longitude,
            takenAt = Instant.parse("2026-09-13T12:00:00Z").plusSeconds(second),
            accuracyMetres = accuracy,
        )
}
