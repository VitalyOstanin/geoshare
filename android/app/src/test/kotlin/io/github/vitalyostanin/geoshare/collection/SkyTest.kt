package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyTest {
    private fun satellite(
        constellation: Constellation,
        id: Int,
        loudness: Double,
        usedInFix: Boolean = false,
    ) = Satellite(
        constellation = constellation,
        id = id,
        loudness = loudness,
        basebandLoudness = null,
        elevationDegrees = null,
        azimuthDegrees = null,
        carrierHz = null,
        usedInFix = usedInFix,
        hasEphemeris = false,
        hasAlmanac = false,
    )

    private fun gain(constellation: Constellation, levelDb: Double) =
        Gain(constellation = constellation, carrierHz = null, levelDb = levelDb)

    @Test
    fun `a band the receiver is not listening to is not a gain of minus three hundred`() {
        val epoch =
            listOf(
                gain(Constellation.Gps, -12.0),
                gain(Constellation.Glonass, -327.67),
            )

        assertEquals(listOf(gain(Constellation.Gps, -12.0)), reported(epoch))
    }

    @Test
    fun `the idle bands left in would drag the average down by hundreds`() {
        val epoch =
            listOf(
                gain(Constellation.Gps, -12.0),
                gain(Constellation.Galileo, -14.0),
                gain(Constellation.Glonass, -327.67),
            )

        val read = gainPerBand(epoch)

        assertEquals(-12.0, read.getValue(Constellation.Gps), 0.001)
        assertEquals(-14.0, read.getValue(Constellation.Galileo), 0.001)
        assertTrue(Constellation.Glonass !in read)
    }

    @Test
    fun `each band's gain is its own, several frequencies averaged within it`() {
        val epoch =
            listOf(
                gain(Constellation.Gps, -10.0),
                gain(Constellation.Gps, -14.0),
                gain(Constellation.Glonass, -20.0),
            )

        val read = gainPerBand(epoch)

        assertEquals(-12.0, read.getValue(Constellation.Gps), 0.001)
        assertEquals(-20.0, read.getValue(Constellation.Glonass), 0.001)
    }

    @Test
    fun `a satellite that is listed but not heard is not silence`() {
        val epoch =
            listOf(
                satellite(Constellation.Gps, id = 1, loudness = 30.0),
                satellite(Constellation.Gps, id = 2, loudness = 0.0),
            )

        assertEquals(30.0, loudnessPerBand(epoch).getValue(Constellation.Gps), 0.001)
        assertEquals(1, counted(epoch).getValue(Constellation.Gps).heard)
    }

    @Test
    fun `how many the fix rests on is counted apart from how many are heard`() {
        val epoch =
            listOf(
                satellite(Constellation.Gps, id = 1, loudness = 30.0, usedInFix = true),
                satellite(Constellation.Gps, id = 2, loudness = 28.0, usedInFix = true),
                satellite(Constellation.Gps, id = 3, loudness = 22.0),
                satellite(Constellation.Glonass, id = 7, loudness = 33.0),
            )

        val counts = counted(epoch)

        assertEquals(Counted(heard = 3, usedInFix = 2), counts.getValue(Constellation.Gps))
        assertEquals(Counted(heard = 1, usedInFix = 0), counts.getValue(Constellation.Glonass))
    }

    @Test
    fun `the systems are named in one order whichever of them answered`() {
        assertEquals(
            listOf(Constellation.Gps, Constellation.Glonass, Constellation.Beidou),
            inOrder(setOf(Constellation.Beidou, Constellation.Gps, Constellation.Glonass)),
        )
    }

    // The regional systems used to arrive as one word, "other", which threw
    // away which of them was heard: QZSS transmits in the GPS band and IRNSS
    // does not, and a file that calls them the same thing cannot be asked
    // which band went quiet.
    @Test
    fun `the regional systems are named apart from each other`() {
        assertEquals("QZSS", Constellation.Qzss.word)
        assertEquals("SBAS", Constellation.Sbas.word)
        assertEquals("IRNSS", Constellation.Irnss.word)
        assertEquals("other", Constellation.Other.word)
    }

    @Test
    fun `every system has a word of its own`() {
        val words = Constellation.entries.map { it.word }

        assertEquals(words.size, words.toSet().size)
    }

    @Test
    fun `the regional systems take their place in the one order`() {
        assertEquals(
            listOf(
                Constellation.Gps,
                Constellation.Qzss,
                Constellation.Sbas,
                Constellation.Irnss,
            ),
            inOrder(
                setOf(
                    Constellation.Irnss,
                    Constellation.Sbas,
                    Constellation.Qzss,
                    Constellation.Gps,
                ),
            ),
        )
    }
}
