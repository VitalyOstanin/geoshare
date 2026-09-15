package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    // The epoch beside the false fix of 22:26 on the Huawei: nine GPS
    // satellites heard and four of them in the fix, five GLONASS heard and
    // none of them in it, and the position fifty-six kilometres from the flat.
    @Test
    fun `a fix built on the spoofed band while the other was heard is one band's work`() {
        assertTrue(
            restsOnOneBand(
                mapOf(
                    Constellation.Gps to Counted(heard = 9, usedInFix = 4),
                    Constellation.Glonass to Counted(heard = 5, usedInFix = 0),
                    Constellation.Beidou to Counted(heard = 1, usedInFix = 0),
                ),
            ),
        )
    }

    // The epoch beside the true fix two minutes earlier, at home.
    @Test
    fun `a fix the other band is in is not one band's work`() {
        assertFalse(
            restsOnOneBand(
                mapOf(
                    Constellation.Gps to Counted(heard = 8, usedInFix = 0),
                    Constellation.Glonass to Counted(heard = 5, usedInFix = 4),
                    Constellation.Beidou to Counted(heard = 1, usedInFix = 1),
                ),
            ),
        )
    }

    // One satellite of the other band in the fix is the receiver having had
    // something to check the band against, which is the whole of the rule.
    @Test
    fun `a single satellite of the other band in the fix silences the rule`() {
        assertFalse(
            restsOnOneBand(
                mapOf(
                    Constellation.Gps to Counted(heard = 9, usedInFix = 8),
                    Constellation.Glonass to Counted(heard = 5, usedInFix = 1),
                ),
            ),
        )
    }

    // The HONOR phones, which hear no GLONASS at all: there is no denominator
    // to compare against and the rule has to stay silent rather than call
    // every fix a lie.
    @Test
    fun `a phone that hears one band is accused of nothing`() {
        assertFalse(
            restsOnOneBand(mapOf(Constellation.Gps to Counted(heard = 12, usedInFix = 9))),
        )
        assertFalse(restsOnOneBand(emptyMap()))
    }

    @Test
    fun `two satellites of the other band are too few to have been built on`() {
        assertFalse(
            restsOnOneBand(
                mapOf(
                    Constellation.Gps to Counted(heard = 9, usedInFix = 8),
                    Constellation.Glonass to Counted(heard = 2, usedInFix = 0),
                ),
            ),
        )
    }

    // Three satellites of the band cannot have produced a position on their
    // own, so a fix resting on that many rests on something else as well.
    @Test
    fun `a fix resting on too little of the band is not held to be its work`() {
        assertFalse(
            restsOnOneBand(
                mapOf(
                    Constellation.Gps to Counted(heard = 9, usedInFix = 3),
                    Constellation.Glonass to Counted(heard = 5, usedInFix = 0),
                ),
            ),
        )
    }

    // The three systems transmitting where GPS does are one band to a
    // transmitter and one number here.
    @Test
    fun `the band is counted as the whole of it, not as GPS alone`() {
        val counts =
            mapOf(
                Constellation.Gps to Counted(heard = 4, usedInFix = 2),
                Constellation.Galileo to Counted(heard = 3, usedInFix = 1),
                Constellation.Beidou to Counted(heard = 2, usedInFix = 1),
                Constellation.Glonass to Counted(heard = 6, usedInFix = 0),
            )

        assertEquals(FixMakeup(bandInFix = 4, glonassHeard = 6, glonassInFix = 0), makeupOf(counts))
        assertTrue(restsOnOneBand(counts))
    }
}
