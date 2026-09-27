package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * That an epoch is read as loudness against height, and that the two skies
 * the logs hold are told apart by it.
 *
 * The numbers in the two epochs below are not invented: the flat one is the
 * GPS band of the phone under a false signal, every satellite between 32,8 and
 * 34,3 dB-Hz whether it sat at seven degrees or at seventy-six; the sloped one
 * is the other phone the same evening, where the low satellites are ten
 * decibels down on the high ones.
 */
class SlantTest {
    private fun satellite(
        id: Int,
        loudness: Double,
        elevationDegrees: Double?,
        azimuthDegrees: Double? = 120.0,
        constellation: Constellation = Constellation.Gps,
    ) = Satellite(
        constellation = constellation,
        id = id,
        loudness = loudness,
        basebandLoudness = null,
        elevationDegrees = elevationDegrees,
        azimuthDegrees = azimuthDegrees,
        carrierHz = null,
        usedInFix = true,
        hasEphemeris = true,
        hasAlmanac = true,
    )

    private val flatSky =
        listOf(
            satellite(1, 33.9, 7.0),
            satellite(2, 32.8, 19.0),
            satellite(3, 34.3, 31.0),
            satellite(4, 33.1, 48.0),
            satellite(5, 33.8, 62.0),
            satellite(6, 33.9, 76.0),
        )

    private val slopedSky =
        listOf(
            satellite(1, 21.0, 7.0),
            satellite(2, 24.5, 19.0),
            satellite(3, 28.0, 31.0),
            satellite(4, 33.0, 48.0),
            satellite(5, 36.5, 62.0),
            satellite(6, 38.0, 76.0),
        )

    @Test
    fun `a sky at one strength whatever the height reads as flat`() {
        val slant = slantOf(flatSky).getValue(Constellation.Gps)

        assertEquals(6, slant.satellites)
        assertTrue("spread ${slant.spreadDb}", slant.spreadDb < FLAT_SPREAD_DB)
        assertTrue("slope ${slant.slopeDb}", abs(slant.slopeDb ?: 0.0) < FLAT_SLOPE_DB)
        assertEquals(SkyShape.Flat, shapeOf(List(EPOCHS_READ_TOGETHER) { slant }))
    }

    @Test
    fun `a sky heard through air is louder overhead`() {
        val slant = slantOf(slopedSky).getValue(Constellation.Gps)

        assertTrue("spread ${slant.spreadDb}", slant.spreadDb > SLOPED_SPREAD_DB)
        assertTrue("slope ${slant.slopeDb}", (slant.slopeDb ?: 0.0) > SLOPED_SLOPE_DB)
        assertEquals(SkyShape.Sloped, shapeOf(List(EPOCHS_READ_TOGETHER) { slant }))
    }

    // The plainer of the two measures, and the one a person checks by hand:
    // the satellites above forty-five degrees against those below twenty.
    @Test
    fun `the high satellites are held against the low ones`() {
        val flat = slantOf(flatSky).getValue(Constellation.Gps)
        val sloped = slantOf(slopedSky).getValue(Constellation.Gps)

        assertEquals(0.25, flat.highLessLowDb ?: 0.0, 0.01)
        assertEquals(13.08, sloped.highLessLowDb ?: 0.0, 0.01)
    }

    @Test
    fun `an epoch with no satellite near the horizon holds nothing against nothing`() {
        val overhead = flatSky.filter { (it.elevationDegrees ?: 0.0) > LOW_DEGREES }

        assertNull(slantOf(overhead).getValue(Constellation.Gps).highLessLowDb)
    }

    // Satellites gathered in one quarter of the sky give a line whose slope is
    // the noise of the epoch divided by a small number, which is how a thin
    // sky comes to look steep. Said as nothing rather than as a number.
    @Test
    fun `a line is not drawn through satellites all at one height`() {
        val together =
            listOf(
                satellite(1, 31.0, 40.0),
                satellite(2, 35.0, 41.0),
                satellite(3, 30.0, 42.0),
                satellite(4, 36.0, 43.0),
                satellite(5, 32.0, 44.0),
            )

        val slant = slantOf(together).getValue(Constellation.Gps)

        assertNull(slant.slopeDb)
        assertEquals(SkyShape.Unclear, shapeOf(List(EPOCHS_READ_TOGETHER) { slant }))
    }

    // A receiver lists the satellites it expects as well as the ones it has,
    // and reports a satellite whose place it does not know as sitting at zero
    // and zero, which is not a place a satellite can be.
    @Test
    fun `the unheard and the unplaced are left out`() {
        val epoch =
            flatSky +
                satellite(7, 0.0, 55.0) +
                satellite(8, 44.0, 0.0, azimuthDegrees = 0.0) +
                satellite(9, 44.0, null)

        assertEquals(6, slantOf(epoch).getValue(Constellation.Gps).satellites)
    }

    // GLONASS arrives some decibels quieter than GPS on these phones, and the
    // two averaged together would show the difference between the systems
    // rather than the shape of the sky.
    @Test
    fun `the systems are read apart`() {
        val epoch =
            flatSky +
                listOf(
                    satellite(20, 24.0, 12.0, constellation = Constellation.Glonass),
                    satellite(21, 25.0, 44.0, constellation = Constellation.Glonass),
                )

        val read = slantOf(epoch)

        assertEquals(6, read.getValue(Constellation.Gps).satellites)
        assertEquals(2, read.getValue(Constellation.Glonass).satellites)
        assertTrue(read.getValue(Constellation.Gps).spreadDb < FLAT_SPREAD_DB)
    }

    @Test
    fun `too few satellites and too few epochs say nothing`() {
        val thin = slantOf(flatSky.take(4)).getValue(Constellation.Gps)

        assertEquals(SkyShape.Unclear, shapeOf(List(EPOCHS_READ_TOGETHER) { thin }))

        val enough = slantOf(flatSky).getValue(Constellation.Gps)

        assertEquals(SkyShape.Unclear, shapeOf(List(EPOCHS_READ_TOGETHER - 1) { enough }))
    }

    // One epoch in three of a false sky still reads as sloped, and one in ten
    // of a true one reads as flat: the verdict is the middle of ten epochs so
    // that neither of those decides it.
    @Test
    fun `one epoch out of ten does not decide the verdict`() {
        val flat = slantOf(flatSky).getValue(Constellation.Gps)
        val sloped = slantOf(slopedSky).getValue(Constellation.Gps)
        val mostlyFlat = List(EPOCHS_READ_TOGETHER - 1) { flat } + sloped

        assertEquals(SkyShape.Flat, shapeOf(mostlyFlat))
    }
}
