package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

/**
 * The measurements are made up from the published orbits of 15.09.2026: each
 * signal is given the flight it would have from a place, plus a receiver
 * clock error common to all of them. Made up from the place the device is at,
 * they are an honest sky; made up from another place, they are a false one.
 */
class OrbitCheckTest {
    private val orbits = orbitsOf(
        javaClass.classLoader!!.getResourceAsStream("orbits-2026-09-15.rnx")!!
            .bufferedReader()
            .lineSequence(),
    )

    private val greenwich = Place(51.4779, 0.0, 45.0)
    private val madrid = Place(40.4168, -3.7038, 650.0)
    private val receivedNanos = (2436L * SECONDS_IN_A_WEEK + 180_600L) * 1_000_000_000L
    private val at = Instant.parse("2026-09-15T02:10:00Z")
    private val clock = Clock(
        timeNanos = 0,
        leapSecond = 18,
        timeUncertaintyNanos = null,
        fullBiasNanos = -receivedNanos,
        biasNanos = 0.0,
        biasUncertaintyNanos = null,
        driftNanosPerSecond = null,
        driftUncertaintyNanosPerSecond = null,
        discontinuities = 0,
        sinceBootNanos = null,
        sinceBootUncertaintyNanos = null,
    )

    /** The GPS satellites at least 15° up from [place], by number. */
    private fun upFrom(place: Place): List<Int> = (1..32).filter { id ->
        val orbit =
            orbits.nearest(Constellation.Gps, id, receivedNanos / 1e9) ?: return@filter false

        elevation(satelliteAt(orbit, receivedNanos / 1e9).position, place) > 15.0
    }

    private fun elevation(satellite: Ecef, place: Place): Double {
        val here = ecefOf(place)
        val between = satellite - here

        return Math.toDegrees(asin(between.dot(here) / between.length() / here.length()))
    }

    /** What the receiver would report for [id] if its signal came to [from]. */
    private fun measured(
        id: Int,
        from: Place,
        clockErrorMetres: Double = 12_345.0,
        constellation: Constellation = Constellation.Gps,
        state: Int = 1 or (1 shl 3),
        reportedId: Int = id,
        glonassScaleOffNanos: Long = 0,
    ): Measurement {
        val orbit = orbits.nearest(constellation, id, receivedNanos / 1e9)!!
        val here = ecefOf(from)
        var flight = 0.07
        var there = satelliteAt(orbit, receivedNanos / 1e9 - flight)

        repeat(5) {
            there = satelliteAt(orbit, receivedNanos / 1e9 - flight)
            flight = (turned(there.position, flight) - here).length() / LIGHT
        }

        val metres = flight * LIGHT + clockErrorMetres - there.clockSeconds * LIGHT
        val glonass = orbit as? GlonassOrbit
        val sent = if (glonass != null) {
            Math.floorMod(receivedNanos - 18_000_000_000L + 3 * 3_600_000_000_000L, DAY_NANOS) -
                (metres / LIGHT * 1e9).toLong() - glonassScaleOffNanos
        } else {
            Math.floorMod(receivedNanos, WEEK_NANOS) - (metres / LIGHT * 1e9).toLong()
        }

        return Measurement(
            constellation = constellation,
            id = reportedId,
            timeOffsetNanos = 0.0,
            state = state,
            receivedSvTimeNanos = sent,
            receivedSvTimeUncertaintyNanos = 20,
            loudness = 30.0,
            basebandLoudness = null,
            pseudorangeRateMps = 0.0,
            pseudorangeRateUncertaintyMps = 0.1,
            deltaRangeState = 0,
            deltaRangeMetres = 0.0,
            deltaRangeUncertaintyMetres = 0.0,
            carrierHz = glonass?.let { 1_602e6 + it.channel * 562.5e3 } ?: 1_575_420_000.0,
            multipath = 0,
            gainDb = null,
            snrDb = null,
            codeType = "C",
            fullInterSignalBiasNanos = null,
            fullInterSignalBiasUncertaintyNanos = null,
            satelliteInterSignalBiasNanos = null,
            satelliteInterSignalBiasUncertaintyNanos = null,
            fullTracking = null,
        )
    }

    private fun turned(position: Ecef, seconds: Double): Ecef {
        val angle = EARTH_TURNS_RADIANS_PER_SECOND * seconds

        return Ecef(
            position.x * cos(angle) + position.y * sin(angle),
            -position.x * sin(angle) + position.y * cos(angle),
            position.z,
        )
    }

    private fun check(measurements: List<Measurement>, place: Place = greenwich) =
        holdAgainstOrbits(at, clock, measurements, orbits, place)

    @Test
    fun `an honest sky is where the orbits put it`() {
        val epoch = check(upFrom(greenwich).map { measured(it, greenwich) })!!

        assertTrue(epoch.held.size >= 6)
        assertFalse(epoch.suspicious)
        epoch.held.forEach { assertEquals(0.0, it.offMetres, 50.0) }
    }

    @Test
    fun `a sky made up for another place is off its orbits`() {
        val sky = upFrom(greenwich).intersect(upFrom(madrid).toSet()).map { measured(it, madrid) }
        val epoch = check(sky)!!

        assertTrue(epoch.suspicious)
        assertTrue(epoch.off.any { it.distanceOff })
    }

    @Test
    fun `the place the device is told it is at sees nothing wrong`() {
        val sky = upFrom(madrid).map { measured(it, madrid) }

        assertFalse(check(sky, madrid)!!.suspicious)
    }

    @Test
    fun `a satellite heard from below the horizon is off its orbits`() {
        val below = (1..32).first { id ->
            val orbit = orbits.nearest(Constellation.Gps, id, receivedNanos / 1e9)

            orbit != null &&
                elevation(satelliteAt(orbit, receivedNanos / 1e9).position, greenwich) < -10.0
        }
        val epoch = check(
            upFrom(greenwich).map {
                measured(it, greenwich)
            } + measured(below, greenwich),
        )!!

        assertTrue(epoch.off.single { it.id == below }.belowTheHorizon)
        assertTrue(epoch.suspicious)
    }

    @Test
    fun `a millisecond the device got wrong is the device's, not the sky's`() {
        val sky = upFrom(greenwich).map { measured(it, greenwich) }.toMutableList()

        sky[0] = sky[0].copy(receivedSvTimeNanos = sky[0].receivedSvTimeNanos - 1_000_000L)

        val epoch = check(sky)!!

        assertEquals(LIGHT / 1_000.0, epoch.held[0].offMetres, 100.0)
        assertFalse(epoch.suspicious)
    }

    @Test
    fun `Galileo is held against its orbits too`() {
        val galileo = (1..36).filter { id ->
            val orbit =
                orbits.nearest(Constellation.Galileo, id, receivedNanos / 1e9)
                    ?: return@filter false

            elevation(satelliteAt(orbit, receivedNanos / 1e9).position, greenwich) > 5.0
        }
        val sky = galileo.map { measured(it, greenwich, constellation = Constellation.Galileo) }
        val epoch = check(sky)

        assertEquals("Galileo up: $galileo", galileo.size, epoch?.held?.size)
        assertFalse(epoch!!.suspicious)
    }

    /** The healthy GLONASS satellites at least 10° up from [place], by number. */
    private fun glonassUpFrom(place: Place): List<Int> = (1..27).filter { id ->
        val orbit = orbits.nearest(Constellation.Glonass, id, receivedNanos / 1e9)
            ?: return@filter false

        elevation(satelliteAt(orbit, receivedNanos / 1e9).position, place) > 10.0
    }

    private fun glonassMeasured(id: Int, from: Place, reportedId: Int = id, scaleOff: Long = 0) =
        measured(
            id,
            from,
            constellation = Constellation.Glonass,
            state = 1 or (1 shl 7),
            reportedId = reportedId,
            glonassScaleOffNanos = scaleOff,
        )

    @Test
    fun `GLONASS on Moscow time is where its orbits put it`() {
        val glonass = glonassUpFrom(greenwich)
        val sky = upFrom(greenwich).take(2).map { measured(it, greenwich) } +
            glonass.map { glonassMeasured(it, greenwich) }
        val epoch = check(sky)!!

        assertEquals(2 + glonass.size, epoch.held.size)
        assertFalse(epoch.suspicious)
        epoch.held.forEach { assertEquals(0.0, it.offMetres, 50.0) }
    }

    @Test
    fun `GLONASS made up for another place is off its orbits`() {
        val shared = glonassUpFrom(greenwich).intersect(glonassUpFrom(madrid).toSet())
        val sky = upFrom(greenwich).map { measured(it, greenwich) } +
            shared.map { glonassMeasured(it, madrid) }
        val epoch = check(sky)!!

        assertTrue(epoch.off.any { it.constellation == Constellation.Glonass })
        assertTrue(epoch.off.none { it.constellation == Constellation.Gps })
    }

    @Test
    fun `a GLONASS satellite named by its channel is the one above the horizon`() {
        val id = glonassUpFrom(greenwich).first()
        val channel = (
            orbits.nearest(
                Constellation.Glonass,
                id,
                receivedNanos / 1e9,
            ) as GlonassOrbit
            ).channel
        val sky = upFrom(greenwich).take(3).map { measured(it, greenwich) } +
            glonassMeasured(id, greenwich, reportedId = 100 + channel)
        val epoch = check(sky)!!

        val held = epoch.held.single { it.constellation == Constellation.Glonass }

        assertEquals(100 + channel, held.id)
        assertTrue(held.elevationDegrees > 10.0)
        assertEquals(0.0, held.offMetres, 50.0)
    }

    @Test
    fun `GLONASS on another scale than Moscow time is not a distance`() {
        val sky = glonassUpFrom(greenwich).map {
            glonassMeasured(it, greenwich, scaleOff = (3 * 3_600L - 18L) * 1_000_000_000L)
        }

        assertNull(check(sky))
    }

    @Test
    fun `a time known only within the millisecond is not a distance`() {
        val sky = upFrom(greenwich).map { measured(it, greenwich, state = 1 or (1 shl 4)) }

        assertNull(check(sky))
    }

    @Test
    fun `too few satellites say nothing`() {
        assertNull(check(upFrom(greenwich).take(3).map { measured(it, greenwich) }))
        assertNotNull(check(upFrom(greenwich).take(4).map { measured(it, greenwich) }))
    }

    @Test
    fun `two suspicious epochs in a row are needed, the newer one recent`() {
        val honest = check(upFrom(greenwich).map { measured(it, greenwich) })!!
        val off = honest.copy(held = honest.held + honest.held[0].copy(elevationDegrees = -20.0))
        val later = off.copy(at = at.plusSeconds(1))

        assertFalse(offTheOrbits(listOf(off), at))
        assertFalse(
            offTheOrbits(listOf(off, honest.copy(at = at.plusSeconds(1))), at.plusSeconds(1)),
        )
        assertTrue(offTheOrbits(listOf(honest, off, later), at.plusSeconds(1)))
        assertFalse(offTheOrbits(listOf(off, later), at.plusSeconds(60)))
    }

    @Test
    fun `the towers are read from first, a written position failing them, nothing stale`() {
        val now = Instant.parse("2026-09-15T02:10:00Z")
        val towers = Fix(60.9, 4.3, now.minusSeconds(600))
        val written = Fix(55.7, 37.6, now.minusSeconds(60))

        assertEquals(Place(60.9, 4.3), orbitReference(towers, written, now))
        assertEquals(
            Place(55.7, 37.6),
            orbitReference(towers.copy(takenAt = now.minusSeconds(3_600)), written, now),
        )
        assertNull(orbitReference(null, written.copy(takenAt = now.minusSeconds(3_600)), now))
    }

    private companion object {
        const val LIGHT = 299_792_458.0
    }
}
