package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConstellationsTest {
    private fun moving(by: Double) = Epochs(recent = 40.0 + by, earlier = 40.0)

    private val nothing = Epochs(recent = null, earlier = null)

    @Test
    fun `a band gone loud while the other holds still is a false signal`() {
        assertEquals(
            Interference.Spoofing,
            whatTheBandsSay(sharedBand = moving(6.0), glonass = moving(0.2)),
        )
    }

    @Test
    fun `a sky that got louder everywhere is a sky, not an attack`() {
        assertNull(whatTheBandsSay(sharedBand = moving(6.0), glonass = moving(5.0)))
    }

    @Test
    fun `a band buried while the other holds still is jamming`() {
        assertEquals(
            Interference.Jamming,
            whatTheBandsSay(sharedBand = moving(-7.0), glonass = moving(-0.5)),
        )
    }

    @Test
    fun `a phone going indoors loses every band at once and is told nothing`() {
        assertNull(whatTheBandsSay(sharedBand = moving(-7.0), glonass = moving(-6.0)))
    }

    @Test
    fun `one band cannot disagree with a band the phone never heard`() {
        assertNull(whatTheBandsSay(sharedBand = moving(9.0), glonass = nothing))
        assertNull(whatTheBandsSay(sharedBand = nothing, glonass = moving(9.0)))
    }

    @Test
    fun `a move too small to mean anything is left unspoken`() {
        assertNull(whatTheBandsSay(sharedBand = moving(1.5), glonass = moving(0.0)))
    }

    @Test
    fun `the systems sharing the band are averaged into one`() {
        val band =
            acrossTheBand(
                listOf(
                    Epochs(recent = 44.0, earlier = 40.0),
                    Epochs(recent = 36.0, earlier = 30.0),
                ),
            )

        assertEquals(40.0, band.recent!!, 0.001)
        assertEquals(35.0, band.earlier!!, 0.001)
        assertEquals(5.0, band.moved!!, 0.001)
    }

    @Test
    fun `a system heard for the first time does not join the band it landed in`() {
        val band = acrossTheBand(listOf(moving(4.0), Epochs(recent = 48.0, earlier = null)))

        assertEquals(4.0, band.moved!!, 0.001)
    }

    @Test
    fun `a band nothing has a history in says nothing`() {
        assertNull(acrossTheBand(listOf(nothing, Epochs(recent = 30.0, earlier = null))).moved)
    }

    @Test
    fun `the systems on the GPS band are the three that transmit there`() {
        assertEquals(
            setOf(Constellation.Gps, Constellation.Galileo, Constellation.Beidou),
            SHARE_THE_GPS_BAND,
        )
    }
}
