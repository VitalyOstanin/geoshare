package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The file is a cut of the broadcast orbits published for 15.09.2026: every
 * GPS and Galileo satellite's record for 02:00. The positions to match were
 * worked out from the same records by the script the check was first tried
 * with.
 */
class OrbitsTest {
    private val orbits = orbitsOf(
        javaClass.classLoader!!.getResourceAsStream("orbits-2026-09-15.rnx")!!
            .bufferedReader()
            .lineSequence(),
    )

    private val week = 2436L * SECONDS_IN_A_WEEK

    @Test
    fun `every GPS and Galileo record is read`() {
        assertEquals(49, orbits.size)
    }

    @Test
    fun `a GPS satellite stands where the published orbit puts it`() {
        val orbit = orbits.nearest(Constellation.Gps, 5, week + 181_000.0)!!
        val at = satelliteAt(orbit, week + 181_000.0)

        assertEquals(-25_233_194.762838896, at.position.x, 0.01)
        assertEquals(5_768_259.257735785, at.position.y, 0.01)
        assertEquals(5_817_834.81400981, at.position.z, 0.01)
        assertEquals(-0.00023679078533928564, at.clockSeconds, 1e-12)
    }

    @Test
    fun `a Galileo satellite is read the same way`() {
        val orbit = orbits.nearest(Constellation.Galileo, 5, week + 181_000.0)!!
        val at = satelliteAt(orbit, week + 181_000.0)

        assertEquals(-17_285_001.616670597, at.position.x, 0.01)
        assertEquals(-19_301_665.877525758, at.position.y, 0.01)
        assertEquals(14_312_585.45717629, at.position.z, 0.01)
        assertEquals(6.339247421565626e-05, at.clockSeconds, 1e-12)
    }

    @Test
    fun `an orbit is not used three days away from its moment`() {
        assertNotNull(orbits.nearest(Constellation.Gps, 5, week + 180_000.0 + 2 * 86_400.0))
        assertNull(orbits.nearest(Constellation.Gps, 5, week + 180_000.0 + 4 * 86_400.0))
    }

    @Test
    fun `GLONASS and the header are passed over`() {
        val read = orbitsOf(
            sequenceOf(
                "     3.05           N: GNSS NAV DATA    M: MIXED            RINEX VERSION / TYPE",
                "                                                            END OF HEADER",
                "R05 2026 09 15 02 15 00 1.000000000000e-05 0.000000000000e+00 1.000000000000e+04",
                "     1.000000000000e+04 1.000000000000e+00 0.000000000000e+00 0.000000000000e+00",
            ),
        )

        assertEquals(0, read.size)
    }
}
