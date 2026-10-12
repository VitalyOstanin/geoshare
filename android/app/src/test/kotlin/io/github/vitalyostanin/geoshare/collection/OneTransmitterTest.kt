package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * That a sky from one transmitter is told from a thin one.
 *
 * The epochs below are the shape of the logs: under the transmitter the
 * receiver held orbits for nine GLONASS satellites and heard none of them
 * while a dozen of the 1575 MHz band carried the fix; on the honest morning
 * the same device heard GLONASS and built on it.
 */
class OneTransmitterTest {
    private fun satellite(
        constellation: Constellation,
        id: Int,
        loudness: Double,
        usedInFix: Boolean = loudness > 0.0,
        hasEphemeris: Boolean = true,
    ) = Satellite(
        constellation = constellation,
        id = id,
        loudness = loudness,
        basebandLoudness = null,
        elevationDegrees = 40.0,
        azimuthDegrees = 120.0,
        carrierHz = null,
        usedInFix = usedInFix,
        hasEphemeris = hasEphemeris,
        hasAlmanac = true,
    )

    private fun band(heard: Int, inFix: Int) =
        (1..heard).map { satellite(Constellation.Gps, it, 33.0, usedInFix = it <= inFix) }

    private fun glonass(expected: Int, heard: Int) = (1..expected).map {
        satellite(Constellation.Glonass, 70 + it, if (it <= heard) 24.0 else 0.0)
    }

    @Test
    fun `an epoch is counted over the whole of it, not over what was heard`() {
        val read = presenceOf(band(heard = 10, inFix = 9) + glonass(expected = 9, heard = 0))

        assertEquals(
            Presence(expected = 10, heard = 10, usedInFix = 9),
            read.getValue(Constellation.Gps),
        )
        assertEquals(
            Presence(expected = 9, heard = 0, usedInFix = 0),
            read.getValue(Constellation.Glonass),
        )
    }

    @Test
    fun `orbits held and not one satellite heard, while the near band carries the fix`() {
        val epoch = band(heard = 10, inFix = 9) + glonass(expected = 9, heard = 0)

        assertTrue(theOtherBandIsSilent(presenceOf(epoch)))
    }

    // One heard is enough to say the band is not silent: the rule is about a
    // band that cannot be reached at all, not about a weak one.
    @Test
    fun `a band with one satellite heard is not a silent band`() {
        val epoch = band(heard = 10, inFix = 9) + glonass(expected = 9, heard = 1)

        assertFalse(theOtherBandIsSilent(presenceOf(epoch)))
    }

    // A receiver that holds no GLONASS orbits has nothing to expect, so its
    // silence says nothing about what is transmitting: that is a device that
    // has just been switched on, or one that has never seen the system.
    @Test
    fun `a receiver holding no orbits of the far band says nothing`() {
        val epoch = band(heard = 10, inFix = 9) +
            glonass(expected = 9, heard = 0).map { it.copy(hasEphemeris = false) }

        assertFalse(theOtherBandIsSilent(presenceOf(epoch)))
    }

    @Test
    fun `a thin near band is not enough to hold the silence against`() {
        val thinlyHeard = band(heard = 5, inFix = 4) + glonass(expected = 9, heard = 0)
        val thinlyUsed = band(heard = 10, inFix = 2) + glonass(expected = 9, heard = 0)

        assertFalse(theOtherBandIsSilent(presenceOf(thinlyHeard)))
        assertFalse(theOtherBandIsSilent(presenceOf(thinlyUsed)))
    }

    // Measured rather than argued: the silence alone fires on a tenth to a
    // quarter of the honest morning's windows, and the flat sky alone says
    // nothing about where the signal comes from.
    @Test
    fun `neither half of the verdict stands on its own`() {
        assertFalse(fromOneTransmitter(SkySays(shape = SkyShape.Flat, otherBandSilent = false)))
        assertFalse(fromOneTransmitter(SkySays(shape = SkyShape.Sloped, otherBandSilent = true)))
        assertFalse(fromOneTransmitter(SkySays(shape = SkyShape.Unclear, otherBandSilent = true)))
        assertTrue(fromOneTransmitter(SkySays(shape = SkyShape.Flat, otherBandSilent = true)))
    }

    @Test
    fun `a phone with nothing to say leaves the answer alone`() {
        assertFalse(fromOneTransmitter(SkySays()))
        assertFalse(theOtherBandIsSilent(emptyMap()))
    }

    // The second way in: the far band can be heard and the verdict still
    // stand, where the satellites of the band that is heard move as one block.
    @Test
    fun `satellites moving as one block stand in for the silent band`() {
        assertTrue(
            fromOneTransmitter(
                SkySays(shape = SkyShape.Flat, drift = SkyDrift.Together, otherBandSilent = false),
            ),
        )
    }

    @Test
    fun `satellites moving as one block do not stand on their own`() {
        assertFalse(
            fromOneTransmitter(
                SkySays(
                    shape = SkyShape.Sloped,
                    drift = SkyDrift.Together,
                    otherBandSilent = false,
                ),
            ),
        )
        assertFalse(
            fromOneTransmitter(
                SkySays(shape = SkyShape.Flat, drift = SkyDrift.Apart, otherBandSilent = false),
            ),
        )
    }
}
