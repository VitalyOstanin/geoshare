package io.github.vitalyostanin.geoshare.collection

import io.github.vitalyostanin.geoshare.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

/**
 * The counts are those of phone 1's own sky files, cut down to what each
 * question needs: GLONASS heard in about half the epochs of an hour with no
 * ephemeris among them and nothing in a fix, which is the state the aiding
 * data had left it in, against the same constellation eleven minutes after
 * the data was erased.
 */
class AidingDoubtTest {
    @Test
    fun `a constellation heard with no orbit for any of it is being wasted`() {
        assertTrue(heardForNothing(ConstellationUse(heard = 4, withEphemeris = 0, usedInFix = 0)))
    }

    // What the receiver does once it is working again, and the one thing that
    // has to silence this.
    @Test
    fun `a constellation whose orbits are held is not`() {
        val working = ConstellationUse(heard = 4, withEphemeris = 4, usedInFix = 2)

        assertEquals(false, heardForNothing(working))
    }

    // Phone 1 reported single GLONASS satellites in a fix while saying it
    // held no orbit for them, a hundred and thirty times in one night. Read
    // as the constellation having come good, that would have cleared the
    // count twice an hour for as long as the fault lasted.
    @Test
    fun `a satellite in the fix without an orbit does not count as coming good`() {
        val odd = ConstellationUse(heard = 1, withEphemeris = 0, usedInFix = 1)

        assertTrue(heardForNothing(odd))
    }

    @Test
    fun `a constellation below the horizon says nothing either way`() {
        assertEquals(false, heardForNothing(ConstellationUse(0, withEphemeris = 0, usedInFix = 0)))
    }

    @Test
    fun `an hour of hearing a constellation for nothing is doubted`() {
        val doubt = AidingDoubt()

        assertEquals(setOf(Constellation.Glonass), wasted(doubt, Duration.ofHours(1)))
    }

    // Half an hour is the window, and the epochs are what tells a fault from
    // a constellation that is merely low: phone 2 heard BeiDou in fourteen
    // epochs over two and a half hours, which is a sky.
    @Test
    fun `an hour of hearing it in a handful of epochs is not`() {
        val doubt = AidingDoubt()
        val useless = mapOf(Constellation.Glonass to ConstellationUse(1, 0, 0))

        for (minute in 0..60 step 5) {
            doubt.read(useless, Duration.ofMinutes(minute.toLong()))
        }

        assertEquals(emptySet<Constellation>(), doubt.doubted(Duration.ofHours(1)))
    }

    @Test
    fun `the first half hour is left to the receiver to work in`() {
        val doubt = AidingDoubt()

        assertEquals(emptySet<Constellation>(), wasted(doubt, Duration.ofMinutes(29)))
    }

    // A receiver that decodes an orbit at the twenty-ninth minute is a
    // receiver doing its work, and the half hour starts again rather than
    // running out a minute later.
    @Test
    fun `a constellation that comes good is forgiven its whole count`() {
        val doubt = AidingDoubt()

        wasted(doubt, Duration.ofMinutes(29))
        doubt.read(
            mapOf(Constellation.Glonass to ConstellationUse(4, withEphemeris = 4, usedInFix = 0)),
            Duration.ofMinutes(29),
        )

        assertEquals(emptySet<Constellation>(), doubt.doubted(Duration.ofHours(2)))
    }

    // The constellations are watched apart from one another: on phone 1 GPS
    // was in every fix while GLONASS and BeiDou were being wasted.
    @Test
    fun `one constellation being wasted says nothing about the others`() {
        val doubt = AidingDoubt()
        val hour = Duration.ofHours(1)

        for (second in 0..3600 step 10) {
            doubt.read(
                mapOf(
                    Constellation.Glonass to ConstellationUse(4, withEphemeris = 0, usedInFix = 0),
                    Constellation.Gps to ConstellationUse(8, withEphemeris = 8, usedInFix = 6),
                ),
                Duration.ofSeconds(second.toLong()),
            )
        }

        assertEquals(setOf(Constellation.Glonass), doubt.doubted(hour))
    }

    // QZSS is heard a satellite or two at a time on these phones and was
    // never going to carry an answer, so a member is not told about it.
    @Test
    fun `the systems that were never going to carry an answer are left out`() {
        val doubt = AidingDoubt()

        for (second in 0..7200 step 10) {
            doubt.read(
                mapOf(Constellation.Qzss to ConstellationUse(4, withEphemeris = 0, usedInFix = 0)),
                Duration.ofSeconds(second.toLong()),
            )
        }

        assertEquals(emptySet<Constellation>(), doubt.doubted(Duration.ofHours(2)))
    }

    @Test
    fun `every constellation a member can be told about has a name`() {
        for (constellation in Constellation.entries) {
            assertEquals(
                "${constellation.name} is named twice or not at all",
                1,
                Constellation.entries.count {
                    constellationLabel(it) == constellationLabel(constellation)
                },
            )
        }

        assertEquals(R.string.constellation_glonass, constellationLabel(Constellation.Glonass))
    }

    /** An epoch a second of hearing GLONASS and making nothing of it. */
    private fun wasted(doubt: AidingDoubt, span: Duration): Set<Constellation> {
        val useless = mapOf(Constellation.Glonass to ConstellationUse(4, 0, 0))
        var said = emptySet<Constellation>()

        for (second in 0..span.seconds step 10) {
            said = doubt.read(useless, Duration.ofSeconds(second))
        }

        return said
    }
}
