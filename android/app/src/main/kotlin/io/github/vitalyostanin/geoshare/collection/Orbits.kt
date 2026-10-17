package io.github.vitalyostanin.geoshare.collection

import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Where the satellites are, as their operators publish it rather than as the
// sky over the device says.
//
// Every satellite broadcasts its own orbit, and a receiver handed a false sky
// is handed false orbits with it: whatever the device decodes itself is the
// transmitter's to choose. The same orbits are collected by stations all over
// the world and published as a file a day, which no transmitter near the device
// can reach. Held against that file, a satellite the device claims to hear has
// to be above the horizon and at the distance its signal says.
//
// GPS and Galileo describe an orbit the same way, as an ellipse with
// corrections; GLONASS gives a position and a velocity to be followed forward,
// see GlonassOrbit.kt. GLONASS is read too because the devices hear it where
// they hear only two or three GPS satellites, and because a transmitter that
// imitates one system leaves the other honest to compare against.

/** Earth's gravitational constant as GPS defines it, in m³/s². */
private const val MU = 3.986005e14

/** How fast Earth turns, in radians a second. */
const val EARTH_TURNS_RADIANS_PER_SECOND: Double = 7.2921151467e-5

/** The relativistic term of a satellite's clock, in s/√m. */
private const val RELATIVITY = -4.442807633e-10

const val SECONDS_IN_A_WEEK: Long = 604_800L

/**
 * How far from the moment an orbit describes it is still used.
 *
 * Three days. Measured on the devices' own records: a day-old orbit puts the
 * satellite within a kilometre and a half of where a fresh one does, three
 * days within six, and a week puts it thousands of kilometres off — while the
 * check that reads it allows two hundred.
 */
const val ORBIT_USABLE_SECONDS: Long = 3L * 86_400L

/**
 * How far the GPS scale is ahead of UTC, in seconds.
 *
 * GLONASS records are dated in UTC. Eighteen since 2017; no leap second has
 * been added since, and none is planned before they are abolished.
 */
const val GPS_AHEAD_OF_UTC_SECONDS: Long = 18L

/** The GPS scale starts at midnight between the 5th and the 6th of January 1980. */
private val GPS_START = LocalDateTime.of(1980, 1, 6, 0, 0)

/** How many times the anomaly is refined; ten is far past what the orbits need. */
private const val KEPLER_ROUNDS = 10

/** An orbit of any system: whose it is and the moment it describes, on the GPS scale. */
sealed interface BroadcastOrbit {
    val constellation: Constellation
    val id: Int
    val orbitSeconds: Double
}

/**
 * One GPS or Galileo orbit as a satellite broadcast it, in the terms of the
 * RINEX file.
 *
 * The times are seconds on the GPS scale since its start rather than within a
 * week, so that orbits from both sides of a week's end compare directly.
 */
data class Orbit(
    override val constellation: Constellation,
    override val id: Int,
    /** The moment the clock terms are counted from. */
    val clockSeconds: Double,
    val clockBias: Double,
    val clockDrift: Double,
    val clockDriftRate: Double,
    /** The moment the orbit terms are counted from. */
    override val orbitSeconds: Double,
    val crs: Double,
    val deltaN: Double,
    val m0: Double,
    val cuc: Double,
    val eccentricity: Double,
    val cus: Double,
    val sqrtA: Double,
    val cic: Double,
    val omega0: Double,
    val cis: Double,
    val i0: Double,
    val crc: Double,
    val omega: Double,
    val omegaDot: Double,
    val iDot: Double,
) : BroadcastOrbit

/** A point in Earth-centred, Earth-fixed coordinates, in metres. */
data class Ecef(val x: Double, val y: Double, val z: Double) {
    operator fun minus(other: Ecef) = Ecef(x - other.x, y - other.y, z - other.z)

    fun length(): Double = sqrt(x * x + y * y + z * z)

    fun dot(other: Ecef): Double = x * other.x + y * other.y + z * other.z
}

/** Where a satellite was at a moment, and how far its clock was off then, in seconds. */
data class SatelliteAt(val position: Ecef, val clockSeconds: Double)

/** The orbits of a day, by satellite, ready to be asked about a moment. */
class Orbits(orbits: List<BroadcastOrbit>) {
    private val bySatellite = orbits.groupBy { it.constellation to it.id }

    /** How many orbits there are, for the log. */
    val size: Int = orbits.size

    /** The orbit described nearest to [gpsSeconds], or null where none is near enough. */
    fun nearest(constellation: Constellation, id: Int, gpsSeconds: Double): BroadcastOrbit? =
        bySatellite[constellation to id]
            ?.minByOrNull { abs(it.orbitSeconds - gpsSeconds) }
            ?.takeIf { abs(it.orbitSeconds - gpsSeconds) <= ORBIT_USABLE_SECONDS }

    /**
     * The GLONASS satellites on frequency [channel].
     *
     * Two at a time share a channel, on opposite sides of Earth, so that one
     * of them is always below the horizon of the other's listeners.
     */
    fun glonassOnChannel(channel: Int): List<Int> =
        glonassChannels.filterValues { it == channel }.keys.sorted()

    private val glonassChannels: Map<Int, Int> = orbits
        .filterIsInstance<GlonassOrbit>()
        .associate { it.id to it.channel }
}

/**
 * The orbits of GPS, Galileo and GLONASS out of a RINEX 3 navigation file.
 *
 * Read line by line, so a file of eight megabytes is never held whole. The
 * other systems are passed over, and so is anything that does not parse: a
 * broken record costs one satellite an orbit, not the day its file.
 */
fun orbitsOf(lines: Sequence<String>): Orbits {
    val orbits = mutableListOf<BroadcastOrbit>()
    var header = true
    var record = mutableListOf<String>()

    fun flush() {
        when {
            record.isEmpty() -> Unit

            record[0][0] == 'R' && record.size == GLONASS_RECORD_LINES ->
                glonassOrbitOf(record)?.let(orbits::add)

            record[0][0] != 'R' && record.size == RECORD_LINES -> orbitOf(record)?.let(orbits::add)
        }
        record = mutableListOf()
    }

    for (line in lines) {
        if (header) {
            header = !line.contains("END OF HEADER")
            continue
        }

        if (line.isNotEmpty() && line[0] != ' ') {
            flush()

            if (line[0] != 'G' && line[0] != 'E' && line[0] != 'R') {
                continue
            }
        } else if (record.isEmpty()) {
            continue
        }

        record += line
    }

    flush()

    return Orbits(orbits)
}

/** A GPS or Galileo record is its first line and seven more. */
private const val RECORD_LINES = 8

/** A GLONASS record is its first line and three more, one per axis. */
private const val GLONASS_RECORD_LINES = 4

private const val FIELD = 19
private const val FIRST_FIELD = 23
private const val NEXT_FIELD = 4
private const val ID_END = 3
private const val EPOCH_END = 23

/** The first line of a record carries three fields after the epoch: the clock terms. */
private const val FIELDS_ON_FIRST_LINE = 3

/** Every following line carries four fields. */
private const val FIELDS_PER_LINE = 4

/** A GPS or Galileo record continues for five lines, a GLONASS one for three. */
private const val ORBIT_LINES = 5
private const val GLONASS_AXIS_LINES = 3

/**
 * The fields of a GPS or Galileo record in the order RINEX lays them out,
 * counted from the first clock term. Index 3 is the issue of data (IODE),
 * which is not used and so has no name here, and after the last orbit term
 * come the codes, then the week.
 */
private const val CLOCK_BIAS = 0
private const val CLOCK_DRIFT = 1
private const val CLOCK_DRIFT_RATE = 2
private const val CRS = 4
private const val DELTA_N = 5
private const val M0 = 6
private const val CUC = 7
private const val ECCENTRICITY = 8
private const val CUS = 9
private const val SQRT_A = 10
private const val TIME_OF_EPHEMERIS = 11
private const val CIC = 12
private const val OMEGA0 = 13
private const val CIS = 14
private const val I0 = 15
private const val CRC = 16
private const val OMEGA = 17
private const val OMEGA_DOT = 18
private const val I_DOT = 19

/** The clock and orbit terms come first, the week two fields after them. */
private const val PARAMETERS = 20
private const val WEEK_FIELD = 21

/** The columns of a GLONASS axis line, the same on all three. */
private const val POSITION_COLUMN = 0
private const val VELOCITY_COLUMN = 1
private const val PULL_COLUMN = 2

/** The fourth column is the health on the first axis and the channel on the second. */
private const val HEALTH_COLUMN = 3
private const val CHANNEL_COLUMN = 3
private const val HEALTHY = 0.0
private const val FIRST_AXIS = 0
private const val SECOND_AXIS = 1
private const val THIRD_AXIS = 2

private fun orbitOf(record: List<String>): Orbit? {
    val first = record[0]
    val constellation = if (first[0] == 'G') Constellation.Gps else Constellation.Galileo
    val id = first.substring(1, ID_END).trim().toIntOrNull() ?: return null
    val clockAt = gpsSecondsOf(first.substring(ID_END, EPOCH_END)) ?: return null

    val values = (0 until FIELDS_ON_FIRST_LINE).map { field(first, FIRST_FIELD + it * FIELD) } +
        (1..ORBIT_LINES).flatMap { line ->
            (0 until FIELDS_PER_LINE).map { field(record[line], NEXT_FIELD + it * FIELD) }
        }

    if (values.take(PARAMETERS).any { it == null }) {
        return null
    }

    val v = values.map { it ?: 0.0 }
    val week = values.getOrNull(WEEK_FIELD)?.toLong() ?: return null

    return Orbit(
        constellation = constellation,
        id = id,
        clockSeconds = clockAt,
        clockBias = v[CLOCK_BIAS],
        clockDrift = v[CLOCK_DRIFT],
        clockDriftRate = v[CLOCK_DRIFT_RATE],
        crs = v[CRS],
        deltaN = v[DELTA_N],
        m0 = v[M0],
        cuc = v[CUC],
        eccentricity = v[ECCENTRICITY],
        cus = v[CUS],
        sqrtA = v[SQRT_A],
        orbitSeconds = week * SECONDS_IN_A_WEEK + v[TIME_OF_EPHEMERIS],
        cic = v[CIC],
        omega0 = v[OMEGA0],
        cis = v[CIS],
        i0 = v[I0],
        crc = v[CRC],
        omega = v[OMEGA],
        omegaDot = v[OMEGA_DOT],
        iDot = v[I_DOT],
    )
}

/**
 * One GLONASS record: on each axis line the position, the velocity and the
 * pull in kilometres, then the health, the channel and the age of the data.
 * A satellite its operators mark unhealthy is left out.
 */
private fun glonassOrbitOf(record: List<String>): GlonassOrbit? {
    val first = record[0]
    val id = first.substring(1, ID_END).trim().toIntOrNull() ?: return null
    val at = gpsSecondsOf(first.substring(ID_END, EPOCH_END))
        ?.plus(GPS_AHEAD_OF_UTC_SECONDS) ?: return null
    val clockBias = field(first, FIRST_FIELD) ?: return null
    val frequencyBias = field(first, FIRST_FIELD + FIELD) ?: return null
    val axes = (1..GLONASS_AXIS_LINES).map { line ->
        (0 until FIELDS_PER_LINE).map {
            field(record[line], NEXT_FIELD + it * FIELD) ?: return null
        }
    }

    if (axes[FIRST_AXIS][HEALTH_COLUMN] != HEALTHY) {
        return null
    }

    fun vector(column: Int) = Ecef(
        axes[FIRST_AXIS][column] * METRES_IN_A_KILOMETRE,
        axes[SECOND_AXIS][column] * METRES_IN_A_KILOMETRE,
        axes[THIRD_AXIS][column] * METRES_IN_A_KILOMETRE,
    )

    return GlonassOrbit(
        id = id,
        channel = axes[SECOND_AXIS][CHANNEL_COLUMN].toInt(),
        orbitSeconds = at,
        clockBias = clockBias,
        frequencyBias = frequencyBias,
        position = vector(POSITION_COLUMN),
        velocity = vector(VELOCITY_COLUMN),
        pull = vector(PULL_COLUMN),
    )
}

private fun field(line: String, from: Int): Double? {
    if (line.length <= from) {
        return null
    }

    return line.substring(from, minOf(line.length, from + FIELD))
        .trim()
        .replace('D', 'E')
        .toDoubleOrNull()
}

/** " 2026 09 15 02 00 00" as seconds on the GPS scale. */
private fun gpsSecondsOf(epoch: String): Double? {
    val parts = epoch.trim().split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }

    if (parts.size != 6) {
        return null
    }

    val at = LocalDateTime.of(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5])

    return (at.toEpochSecond(ZoneOffset.UTC) - GPS_START.toEpochSecond(ZoneOffset.UTC)).toDouble()
}

/**
 * Where the satellite was at [gpsSeconds], in the frame of Earth at that
 * moment, and how far its clock was off.
 */
fun satelliteAt(orbit: BroadcastOrbit, gpsSeconds: Double): SatelliteAt = when (orbit) {
    is Orbit -> keplerAt(orbit, gpsSeconds)
    is GlonassOrbit -> orbit.at(gpsSeconds)
}

/** The broadcast model as the interface documents of GPS and Galileo give it. */
private fun keplerAt(orbit: Orbit, gpsSeconds: Double): SatelliteAt {
    val a = orbit.sqrtA * orbit.sqrtA
    val tk = gpsSeconds - orbit.orbitSeconds
    val n = sqrt(MU / (a * a * a)) + orbit.deltaN
    val m = orbit.m0 + n * tk
    var e = m

    repeat(KEPLER_ROUNDS) { e = m + orbit.eccentricity * sin(e) }

    val ecc = orbit.eccentricity
    val nu = atan2(sqrt(1 - ecc * ecc) * sin(e), cos(e) - ecc)
    val phi = nu + orbit.omega
    val u = phi + orbit.cus * sin(2 * phi) + orbit.cuc * cos(2 * phi)
    val r = a * (1 - ecc * cos(e)) + orbit.crs * sin(2 * phi) + orbit.crc * cos(2 * phi)
    val i = orbit.i0 + orbit.cis * sin(2 * phi) + orbit.cic * cos(2 * phi) + orbit.iDot * tk
    val x = r * cos(u)
    val y = r * sin(u)
    val week = Math.floorMod(orbit.orbitSeconds.toLong(), SECONDS_IN_A_WEEK).toDouble()
    val node = orbit.omega0 +
        (orbit.omegaDot - EARTH_TURNS_RADIANS_PER_SECOND) * tk -
        EARTH_TURNS_RADIANS_PER_SECOND * week
    val tc = gpsSeconds - orbit.clockSeconds
    val clock = orbit.clockBias + orbit.clockDrift * tc + orbit.clockDriftRate * tc * tc +
        RELATIVITY * ecc * orbit.sqrtA * sin(e)

    return SatelliteAt(
        position = Ecef(
            x = x * cos(node) - y * cos(i) * sin(node),
            y = x * sin(node) + y * cos(i) * cos(node),
            z = y * sin(i),
        ),
        clockSeconds = clock,
    )
}
