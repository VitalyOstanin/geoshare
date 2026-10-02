package io.github.vitalyostanin.geoshare.collection

import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Where the satellites are, as their operators publish it rather than as the
// sky over the phone says.
//
// Every satellite broadcasts its own orbit, and a receiver handed a false sky
// is handed false orbits with it: whatever the phone decodes itself is the
// transmitter's to choose. The same orbits are collected by stations all over
// the world and published as a file a day, which no transmitter near the phone
// can reach. Held against that file, a satellite the phone claims to hear has
// to be above the horizon and at the distance its signal says.
//
// GPS and Galileo only. Both describe an orbit the same way, as an ellipse
// with corrections, which is a page of arithmetic; GLONASS gives a position and
// a velocity to be integrated forward, which is not worth it for a check that
// two systems already carry.

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
 * Three days. Measured on the phones' own records: a day-old orbit puts the
 * satellite within a kilometre and a half of where a fresh one does, three
 * days within six, and a week puts it thousands of kilometres off — while the
 * check that reads it allows two hundred.
 */
const val ORBIT_USABLE_SECONDS: Long = 3L * 86_400L

/** The GPS scale starts at midnight between the 5th and the 6th of January 1980. */
private val GPS_START = LocalDateTime.of(1980, 1, 6, 0, 0)

/** How many times the anomaly is refined; ten is far past what the orbits need. */
private const val KEPLER_ROUNDS = 10

/**
 * One orbit as a satellite broadcast it, in the terms of the RINEX file.
 *
 * The times are seconds on the GPS scale since its start rather than within a
 * week, so that orbits from both sides of a week's end compare directly.
 */
data class Orbit(
    val constellation: Constellation,
    val id: Int,
    /** The moment the clock terms are counted from. */
    val clockSeconds: Double,
    val clockBias: Double,
    val clockDrift: Double,
    val clockDriftRate: Double,
    /** The moment the orbit terms are counted from. */
    val orbitSeconds: Double,
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
)

/** A point in Earth-centred, Earth-fixed coordinates, in metres. */
data class Ecef(val x: Double, val y: Double, val z: Double) {
    operator fun minus(other: Ecef) = Ecef(x - other.x, y - other.y, z - other.z)

    fun length(): Double = sqrt(x * x + y * y + z * z)

    fun dot(other: Ecef): Double = x * other.x + y * other.y + z * other.z
}

/** Where a satellite was at a moment, and how far its clock was off then, in seconds. */
data class SatelliteAt(val position: Ecef, val clockSeconds: Double)

/** The orbits of a day, by satellite, ready to be asked about a moment. */
class Orbits(orbits: List<Orbit>) {
    private val bySatellite = orbits.groupBy { it.constellation to it.id }

    /** How many orbits there are, for the log. */
    val size: Int = orbits.size

    /** The orbit described nearest to [gpsSeconds], or null where none is near enough. */
    fun nearest(constellation: Constellation, id: Int, gpsSeconds: Double): Orbit? =
        bySatellite[constellation to id]
            ?.minByOrNull { abs(it.orbitSeconds - gpsSeconds) }
            ?.takeIf { abs(it.orbitSeconds - gpsSeconds) <= ORBIT_USABLE_SECONDS }
}

/**
 * The orbits of GPS and Galileo out of a RINEX 3 navigation file.
 *
 * Read line by line, so a file of eight megabytes is never held whole. The
 * other systems are passed over, and so is anything that does not parse: a
 * broken record costs one satellite an orbit, not the day its file.
 */
fun orbitsOf(lines: Sequence<String>): Orbits {
    val orbits = mutableListOf<Orbit>()
    var header = true
    var record = mutableListOf<String>()

    fun flush() {
        if (record.size == RECORD_LINES) {
            orbitOf(record)?.let(orbits::add)
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

            if (line[0] != 'G' && line[0] != 'E') {
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

private const val FIELD = 19
private const val FIRST_FIELD = 23
private const val NEXT_FIELD = 4
private const val ID_END = 3
private const val EPOCH_END = 23

/** The clock and orbit terms come first, the week two fields after them. */
private const val PARAMETERS = 20
private const val WEEK_FIELD = 21

@Suppress("MagicNumber")
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

    @Suppress("MagicNumber")
    if (parts.size != 6) {
        return null
    }

    @Suppress("MagicNumber")
    val at = LocalDateTime.of(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5])

    return (at.toEpochSecond(ZoneOffset.UTC) - GPS_START.toEpochSecond(ZoneOffset.UTC)).toDouble()
}

/**
 * Where the satellite was at [gpsSeconds], in the frame of Earth at that
 * moment, and how far its clock was off.
 *
 * The broadcast model as the interface documents of both systems give it.
 */
fun satelliteAt(orbit: Orbit, gpsSeconds: Double): SatelliteAt {
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
