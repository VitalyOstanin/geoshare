package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
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

    // The phone stood still for a minute and the wall clock was stepped a
    // minute back in the middle of it. Counted on the wall clock the second
    // fix arrives before the first; counted since boot, a minute passed.
    @Test
    fun `a wall clock stepped back does not turn a standstill into a jump`() {
        val first = fixAt(60.9, 4.3, 60, sinceBoot = Duration.ofSeconds(600))
        val second = fixAt(60.9 + 0.0009, 4.3, 0, sinceBoot = Duration.ofSeconds(660))

        assertEquals(1.7, impliedSpeedMps(first, second)!!, 0.5)
        assertNull(whyAJump(first, second))
    }

    // The other way round: the clock was stepped forward an hour, which on
    // the wall clock makes a flight across the country a stroll.
    @Test
    fun `a wall clock stepped forward does not hide a jump`() {
        val first = fixAt(60.9, 4.3, 0, sinceBoot = Duration.ofSeconds(600))
        val second = fixAt(60.98, 4.56, 3600, sinceBoot = Duration.ofSeconds(615))

        assertEquals(JumpReason.Speed, whyAJump(first, second))
    }

    // A fix read back from the trail file carries no count since boot, and
    // neither does one taken before this run of the process. The wall clock
    // answers for those, as it did before there was a second clock at all.
    @Test
    fun `without the count since boot the wall clock still answers`() {
        val first = fixAt(60.9, 4.3, 0, sinceBoot = Duration.ofSeconds(600))
        val second = fixAt(60.9 + 0.0009, 4.3, 10)

        assertEquals(10.0, impliedSpeedMps(first, second)!!, 0.5)
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

    // The first false answer of a run has no trail behind it, which is what
    // every other rule here is read from. This one is read from the epoch.
    @Test
    fun `a fix built on the spoofed band alone is caught without anything to compare`() {
        assertEquals(
            JumpReason.OneBand,
            whyAJump(
                null,
                fixAt(61.11719, 5.39843, 0, accuracy = 57.0),
                restsOn = mapOf(
                    Constellation.Gps to Counted(heard = 9, usedInFix = 4),
                    Constellation.Glonass to Counted(heard = 5, usedInFix = 0),
                ),
            ),
        )
    }

    // The phone that hears the far band not at all rather than hears it and
    // leaves it out: the other rule stays silent there, because it asks for
    // GLONASS to have been heard. See OneTransmitter.kt for the measurements.
    @Test
    fun `a sky from one transmitter is caught where the one-band rule is silent`() {
        assertEquals(
            JumpReason.OneTransmitter,
            whyAJump(
                null,
                fixAt(61.11719, 5.39843, 0, accuracy = 57.0),
                restsOn = mapOf(Constellation.Gps to Counted(heard = 12, usedInFix = 11)),
                skySays = SkySays(shape = SkyShape.Flat, otherBandSilent = true),
            ),
        )
    }

    // The clock rule needs neither a flat sky nor a silent band: it rests on
    // the count of seconds since the phone was switched on, which no signal
    // reaches. See SatelliteClock.kt.
    @Test
    fun `a fix built while the satellites' time stepped is left out`() {
        val first = fixAt(61.03268, 4.40299, 0, accuracy = 60.0)
        val second = fixAt(61.03270, 4.40305, 60, accuracy = 80.0)

        assertEquals(
            JumpReason.TimeApart,
            whyAJump(first, second, skySays = SkySays(satelliteTimeApart = true)),
        )
    }

    @Test
    fun `a fix the satellites' time ran along with passes`() {
        val first = fixAt(61.03268, 4.40299, 0, accuracy = 60.0)
        val second = fixAt(61.03270, 4.40305, 60, accuracy = 80.0)

        assertNull(
            whyAJump(first, second, skySays = SkySays(satelliteTimeApart = false)),
        )
    }

    @Test
    fun `a flat sky with the far band still heard is not held against the fix`() {
        val first = fixAt(61.03268, 4.40299, 0, accuracy = 60.0)
        val second = fixAt(61.03270, 4.40305, 60, accuracy = 80.0)

        assertNull(
            whyAJump(
                first,
                second,
                skySays = SkySays(shape = SkyShape.Flat, otherBandSilent = false),
            ),
        )
    }

    @Test
    fun `a fix the other band is in passes, epoch and all`() {
        val first = fixAt(61.03268, 4.40299, 0, accuracy = 60.0)
        val second = fixAt(61.03270, 4.40305, 60, accuracy = 80.0)

        assertNull(
            whyAJump(
                first,
                second,
                restsOn = mapOf(
                    Constellation.Gps to Counted(heard = 8, usedInFix = 0),
                    Constellation.Glonass to Counted(heard = 5, usedInFix = 4),
                ),
            ),
        )
    }

    // The status is read as a statement about the fix beside it, so a caller
    // with no current status hands over nothing and the rule says nothing.
    @Test
    fun `no epoch to read is not an accusation`() {
        val first = fixAt(61.03268, 4.40299, 0, accuracy = 60.0)
        val second = fixAt(61.03270, 4.40305, 60, accuracy = 80.0)

        assertNull(whyAJump(first, second, restsOn = emptyMap()))
    }

    // The creep this whole rule exists for: every pair of answers is a few
    // metres apart, so the speed says nothing and the width says nothing,
    // while the place they have walked to is a kilometre from where the phone
    // is lying.
    @Test
    fun `answers creeping away from a phone nobody touched are a jump`() {
        val lying = fixAt(61.03268, 4.40300, 0, accuracy = 20.0)
        val crept = fixAt(61.04170, 4.40300, 1_800, accuracy = 40.0)
        val nearlyThere = fixAt(61.04168, 4.40300, 1_770, accuracy = 40.0)

        assertEquals(
            JumpReason.Drifted,
            whyAJump(
                nearlyThere,
                crept,
                putDown = PutDown(at = lying, since = lying.takenAt, steps = 1_000),
                carrying = Carrying(stirred = false, steps = 0),
            ),
        )
    }

    // The same pair with the phone having been carried: the sensors say it
    // went somewhere, and the rule has nothing to say about where.
    @Test
    fun `answers that moved with the phone are left alone`() {
        val lying = fixAt(61.03268, 4.40300, 0, accuracy = 20.0)
        val crept = fixAt(61.04170, 4.40300, 1_800, accuracy = 40.0)
        val nearlyThere = fixAt(61.04168, 4.40300, 1_770, accuracy = 40.0)

        assertNull(
            whyAJump(
                nearlyThere,
                crept,
                putDown = PutDown(at = lying, since = lying.takenAt, steps = 1_000),
                carrying = Carrying(stirred = true),
            ),
        )
    }

    private fun fixAt(
        latitude: Double,
        longitude: Double,
        second: Long,
        accuracy: Double? = null,
        sinceBoot: Duration? = null,
    ) = Fix(
        latitude = latitude,
        longitude = longitude,
        takenAt = Instant.parse("2026-09-13T12:00:00Z").plusSeconds(second),
        accuracyMetres = accuracy,
        sinceBoot = sinceBoot,
    )
}
