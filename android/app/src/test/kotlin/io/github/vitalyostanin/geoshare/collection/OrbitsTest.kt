package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Random

/**
 * The file is a cut of the broadcast orbits published for 15.09.2026: every
 * GPS and Galileo satellite's record for 02:00 and every GLONASS one for 01:45
 * and 02:15. The positions to match were worked out from the same records by
 * the script the check was first tried with.
 */
class OrbitsTest {
    private val orbits = orbitsOf(
        javaClass.classLoader!!.getResourceAsStream("orbits-2026-09-15.rnx")!!
            .bufferedReader()
            .lineSequence(),
    )

    private val week = 2436L * SECONDS_IN_A_WEEK

    @Test
    fun `every record is read but the GLONASS satellites marked unhealthy`() {
        assertEquals(49 + 42, orbits.size)
        assertNull(orbits.nearest(Constellation.Glonass, 20, week + 181_000.0))
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
    fun `a GLONASS satellite is followed from the record nearest the moment`() {
        val orbit = orbits.nearest(Constellation.Glonass, 16, week + 181_000.0)!!
        val at = satelliteAt(orbit, week + 181_000.0)

        assertEquals(week + 180_918.0, orbit.orbitSeconds, 1e-9)
        assertEquals(23_527_210.296, at.position.x, 1.0)
        assertEquals(-9_332_350.850, at.position.y, 1.0)
        assertEquals(3_003_540.545, at.position.z, 1.0)
        assertEquals(5.525816e-05, at.clockSeconds, 1e-10)
    }

    @Test
    fun `a GLONASS satellite followed again is where it was followed to before`() {
        val orbit = orbits.nearest(Constellation.Glonass, 1, week + 181_000.0)!!
        val first = satelliteAt(orbit, week + 181_000.0)

        satelliteAt(orbit, week + 185_000.0)

        val again = satelliteAt(orbit, week + 181_000.0)

        assertEquals(-13_355_004.051, first.position.x, 1.0)
        assertEquals(1_989_012.856, first.position.y, 1.0)
        assertEquals(21_636_247.556, first.position.z, 1.0)
        assertEquals(first.position.x, again.position.x, 0.01)
        assertEquals(first.position.z, again.position.z, 0.01)
    }

    @Test
    fun `the satellites sharing a GLONASS channel are both named`() {
        assertEquals(listOf(1, 5), orbits.glonassOnChannel(1))
    }

    @Test
    fun `an incomplete GLONASS record and the header are passed over`() {
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

    private val fixture = javaClass.classLoader!!.getResourceAsStream("orbits-2026-09-15.rnx")!!
        .bufferedReader()
        .readLines()

    // The first record after the header, a Galileo one, which the tests
    // below break and put after the header again.
    private val galileoRecord = fixture.subList(4, 12)

    private fun headerAnd(vararg records: List<String>): Sequence<String> =
        (fixture.subList(0, 4) + records.flatMap { it }).asSequence()

    private fun withFirstLine(record: List<String>, first: String) = listOf(first) + record.drop(1)

    @Test
    fun `a record whose first line ends before its epoch is passed over`() {
        val read = orbitsOf(
            headerAnd(
                withFirstLine(galileoRecord, "G05 2026 09 15"),
                withFirstLine(galileoRecord, "G"),
                galileoRecord,
            ),
        )

        assertEquals(1, read.size)
    }

    @Test
    fun `a record dated in a thirteenth month is passed over`() {
        val first = galileoRecord[0].replaceRange(9, 11, "13")

        val read = orbitsOf(headerAnd(withFirstLine(galileoRecord, first), galileoRecord))

        assertEquals(1, read.size)
    }

    @Test
    fun `a record dated at hour 25 or on 31 September is passed over`() {
        val lateHour = galileoRecord[0].replaceRange(15, 17, "25")
        val noSuchDay = galileoRecord[0].replaceRange(12, 14, "31")
        val read = orbitsOf(
            headerAnd(
                withFirstLine(galileoRecord, lateHour),
                withFirstLine(galileoRecord, noSuchDay),
                galileoRecord,
            ),
        )

        assertEquals(1, read.size)
    }

    // The file comes off the network, so whatever arrives must read as some
    // orbits, never as an exception that loses the whole day.
    @Test
    fun `any broken file reads as orbits`() {
        val random = Random(635L)
        val records = fixture.indices.filter { it >= 4 && fixture[it].firstOrNull() != ' ' }

        repeat(500) { case ->
            val lines = fixture.toMutableList()

            repeat(1 + random.nextInt(6)) { change ->
                val picked = if (random.nextBoolean()) {
                    records[random.nextInt(records.size)]
                } else {
                    4 + random.nextInt(lines.size - 4)
                }
                val at = picked.coerceAtMost(lines.lastIndex)

                broken(lines, at, (case + change) % 5, random)
            }

            try {
                orbitsOf(lines.asSequence())
            } catch (thrown: Exception) {
                throw AssertionError("case $case threw", thrown)
            }
        }
    }

    /** One of five ways a line goes wrong in transit: cut, garbled, lost, doubled or misdated. */
    private fun broken(lines: MutableList<String>, at: Int, kind: Int, random: Random) {
        val line = lines[at]
        val awkward = "0123456789 -+.DEexR"

        when (kind) {
            0 -> lines[at] = line.take(random.nextInt(line.length + 1))

            1 -> if (line.isNotEmpty()) {
                val chars = StringBuilder(line)

                repeat(1 + random.nextInt(4)) {
                    val column = random.nextInt(minOf(chars.length, 80))
                    chars.setCharAt(column, awkward[random.nextInt(awkward.length)])
                }
                lines[at] = chars.toString()
            }

            2 -> lines.removeAt(at)

            3 -> lines.add(at, line)

            else -> if (line.length >= 17) {
                val value = if (random.nextBoolean()) "13" else "99"
                val column = if (random.nextBoolean()) 9 else 15

                lines[at] = line.replaceRange(column, column + 2, value)
            }
        }
    }
}
