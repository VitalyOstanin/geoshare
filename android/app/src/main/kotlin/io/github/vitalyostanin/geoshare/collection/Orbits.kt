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

private const val METRES_IN_A_KILOMETRE = 1_000.0

private const val FIELD = 19
private const val FIRST_FIELD = 23
private const val NEXT_FIELD = 4
private const val ID_END = 3
private const val EPOCH_END = 23

/** The clock and orbit terms come first, the week two fields after them. */
private const val PARAMETERS = 20
private const val WEEK_FIELD = 21

private fun orbitOf(record: List<String>): Orbit? {
    val first = record[0]
    val constellation = if (first[0] == 'G') Constellation.Gps else Constellation.Galileo
    val id = first.substring(1, ID_END).trim().toIntOrNull() ?: return null
    val clockAt = gpsSecondsOf(first.substring(ID_END, EPOCH_END)) ?: return null

    val values = (0 until 3).map { field(first, FIRST_FIELD + it * FIELD) } +
        (1..5).flatMap { line -> (0 until 4).map { field(record[line], NEXT_FIELD + it * FIELD) } }

    if (values.take(PARAMETERS).any { it == null }) {
        return null
    }

    val v = values.map { it ?: 0.0 }
    val week = values.getOrNull(WEEK_FIELD)?.toLong() ?: return null

    return Orbit(
        constellation = constellation,
        id = id,
        clockSeconds = clockAt,
        clockBias = v[0],
        clockDrift = v[1],
        clockDriftRate = v[2],
        crs = v[4],
        deltaN = v[5],
        m0 = v[6],
        cuc = v[7],
        eccentricity = v[8],
        cus = v[9],
        sqrtA = v[10],
        orbitSeconds = week * SECONDS_IN_A_WEEK + v[11],
        cic = v[12],
        omega0 = v[13],
        cis = v[14],
        i0 = v[15],
        crc = v[16],
        omega = v[17],
        omegaDot = v[18],
        iDot = v[19],
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
    val axes = (1..3).map { line ->
        (0 until 4).map { field(record[line], NEXT_FIELD + it * FIELD) ?: return null }
    }

    if (axes[0][3] != 0.0) {
        return null
    }

    fun vector(column: Int) = Ecef(
        axes[0][column] * METRES_IN_A_KILOMETRE,
        axes[1][column] * METRES_IN_A_KILOMETRE,
        axes[2][column] * METRES_IN_A_KILOMETRE,
    )

    return GlonassOrbit(
        id = id,
        channel = axes[1][3].toInt(),
        orbitSeconds = at,
        clockBias = clockBias,
        frequencyBias = frequencyBias,
        position = vector(0),
        velocity = vector(1),
        pull = vector(2),
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
