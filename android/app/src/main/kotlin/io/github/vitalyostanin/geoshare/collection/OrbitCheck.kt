package io.github.vitalyostanin.geoshare.collection

import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

// The satellites the phone hears, held against where their published orbits
// put them as seen from a place no false sky can move.
//
// A transmitter that moves a phone has to make every satellite it imitates
// agree with the place it is moving the phone to: each signal arrives as late
// as the distance from that satellite to that place. From anywhere else the
// same delays do not fit. So the check asks two things of an epoch, from the
// place the towers and the Wi-Fi put the phone:
//
// whether any satellite heard is below the horizon there, where no real
// signal comes from;
//
// and whether, once the receiver's own clock is taken out of them, the
// distances the signals measured differ from the distances to the published
// positions by more than any honest error does.
//
// Measured on the phones' own records: under the transmitter the distances
// were off by a thousand kilometres and more, in two epochs of three or more; on
// clean records nothing was off by more than seventy.

/** The speed of light, in metres a second. */
private const val LIGHT_MPS = 299_792_458.0

private const val NANOS_IN_A_SECOND = 1e9

/** WGS 84: the equatorial radius and the flattening. */
private const val WGS84_A = 6_378_137.0
private const val WGS84_F = 1 / 298.257223563

/** The receiver has the time only within a millisecond, not the whole of it. */
private const val MSEC_AMBIGUOUS = 1 shl 4

/** The band GPS L1 and Galileo E1 share, which is the one every phone hears. */
private const val L1_HZ = 1_575.42e6
private const val BAND_HALF_WIDTH_HZ = 1e6

/**
 * GLONASS L1: each satellite on its own channel, 1602 MHz plus 562.5 kHz a
 * channel, channels -7 to +6.
 */
private const val GLONASS_L1_HZ = 1_602e6
private const val GLONASS_CHANNEL_HZ = 562.5e3
private val GLONASS_CHANNELS = -7..6

/** A phone that does not know a GLONASS satellite's number gives its channel plus this. */
private const val GLONASS_CHANNEL_ID_OFFSET = 100

/** GLONASS keeps Moscow time: UTC and three hours. */
private const val GLONASS_AHEAD_OF_UTC_NANOS = 3L * 3_600L * 1_000_000_000L

/** No satellite of either system is nearer than this or farther than the next. */
private const val NEAREST_METRES = 1.5e7
private const val FARTHEST_METRES = 5e7

/** Fewer satellites than this leave the receiver's clock and nothing to check. */
const val ORBIT_CHECK_MIN_SATELLITES: Int = 4

/**
 * How far below the horizon a satellite may be and still be heard honestly.
 *
 * Three degrees, because the place it is measured from is known only to a few
 * hundred metres or a few kilometres: a degree of arc is a hundred and eleven
 * kilometres, so a place off by a couple of hundred still leaves a satellite
 * truly on the horizon above this line.
 */
const val BELOW_THE_HORIZON_DEGREES: Double = -3.0

/**
 * How far a satellite's distance may be off before it is not an honest error.
 *
 * Two hundred kilometres. On clean records the worst was sixty-eight, with
 * the phone indoors; under the transmitter the middle of the epochs was a
 * thousand four hundred. Thirty fired on clean records.
 */
const val DISTANCE_OFF_METRES: Double = 200_000.0

/**
 * A whole millisecond of light, and how near to a whole number of them an
 * error has to be to count as one.
 *
 * The phones get the millisecond wrong now and then — the time they decode is
 * ambiguous by one — and that error is a whole number of these to within a
 * few kilometres. It is the phone's, not the sky's, and is not counted.
 */
private const val MILLISECOND_METRES = LIGHT_MPS / 1_000.0
private const val WHOLE_MILLISECOND_SLACK_METRES = 3_000.0

/**
 * How many checked epochs in a row have to be off before the sky is said to
 * be false.
 *
 * Two, so that a single bad epoch — a satellite tracked wrongly for a moment —
 * does not leave fixes out.
 */
const val ORBIT_EPOCHS_IN_A_ROW: Int = 2

/**
 * How old the last checked epoch may be and still say anything.
 *
 * The receiver reports once a second; an answer older than this is about a
 * sky the phone has stopped reporting.
 */
val ORBIT_CHECK_STILL_CURRENT: Duration = Duration.ofSeconds(10)

/**
 * How old a place may be and still be read from.
 *
 * Half an hour: at a hundred kilometres an hour that is fifty kilometres,
 * which moves a satellite half a degree and a distance by at most as much —
 * both well inside what the check allows.
 */
val ORBIT_REFERENCE_FRESH: Duration = Duration.ofMinutes(30)

/** A place on Earth: degrees and metres above the ellipsoid. */
data class Place(val latitude: Double, val longitude: Double, val altitudeMetres: Double = 0.0)

/** The same place as a point in Earth-centred coordinates. */
fun ecefOf(place: Place): Ecef {
    val lat = Math.toRadians(place.latitude)
    val lon = Math.toRadians(place.longitude)
    val e2 = WGS84_F * (2 - WGS84_F)
    val n = WGS84_A / sqrt(1 - e2 * sin(lat) * sin(lat))

    return Ecef(
        x = (n + place.altitudeMetres) * cos(lat) * cos(lon),
        y = (n + place.altitudeMetres) * cos(lat) * sin(lon),
        z = (n * (1 - e2) + place.altitudeMetres) * sin(lat),
    )
}

/** One signal of an epoch: when it arrived and how far it says it came. */
data class Ranged(
    val constellation: Constellation,
    val id: Int,
    /** The moment it arrived, on the GPS scale, in seconds. */
    val receivedSeconds: Double,
    /** How far it travelled, receiver's clock error included, in metres. */
    val metres: Double,
)

/**
 * How far a signal says it travelled, or nothing where it cannot say.
 *
 * GPS, Galileo and GLONASS on the L1 band only, with the time decoded whole —
 * of the week for the first two, of the day for GLONASS: anything less is a
 * distance modulo a millisecond or a hundred, which is not a distance.
 */
fun rangedOf(clock: Clock, measurement: Measurement): Ranged? {
    val glonass = measurement.constellation == Constellation.Glonass

    if (measurement.constellation != Constellation.Gps &&
        measurement.constellation != Constellation.Galileo &&
        !glonass
    ) {
        return null
    }

    val carrier = measurement.carrierHz

    if (carrier != null && !onL1(carrier, glonass)) {
        return null
    }

    if (!sentTimeIsKnown(measurement) || measurement.state and MSEC_AMBIGUOUS != 0) {
        return null
    }

    val fullBias = clock.fullBiasNanos ?: return null
    val received =
        (clock.timeNanos - fullBias) +
            (measurement.timeOffsetNanos - (clock.biasNanos ?: 0.0)).toLong()
    // GLONASS counts the time of day in Moscow; the others, the time of week
    // on their own scale, which is the GPS one.
    val (period, here) = if (glonass) {
        DAY_NANOS to
            received - GPS_AHEAD_OF_UTC_SECONDS * NANOS_IN_A_SECOND.toLong() +
            GLONASS_AHEAD_OF_UTC_NANOS
    } else {
        WEEK_NANOS to received
    }
    var flight = Math.floorMod(here, period) - measurement.receivedSvTimeNanos

    if (flight < -period / 2) {
        flight += period
    }

    val metres = flight / NANOS_IN_A_SECOND * LIGHT_MPS

    if (metres !in NEAREST_METRES..FARTHEST_METRES) {
        return null
    }

    return Ranged(measurement.constellation, measurement.id, received / NANOS_IN_A_SECOND, metres)
}

private fun onL1(carrier: Double, glonass: Boolean): Boolean = if (glonass) {
    val channel = (carrier - GLONASS_L1_HZ) / GLONASS_CHANNEL_HZ

    abs(channel - Math.round(channel)) * GLONASS_CHANNEL_HZ < BAND_HALF_WIDTH_HZ &&
        Math.round(channel).toInt() in GLONASS_CHANNELS
} else {
    abs(carrier - L1_HZ) <= BAND_HALF_WIDTH_HZ
}

/**
 * The orbits a signal may be from.
 *
 * GLONASS satellites are numbered 1 to 24; a phone that has not yet decoded
 * the number gives the channel plus a hundred instead, which two satellites
 * share — the caller takes the one above the horizon.
 */
private fun candidatesFor(signal: Ranged, orbits: Orbits): List<BroadcastOrbit> {
    val ids = if (signal.constellation == Constellation.Glonass &&
        signal.id - GLONASS_CHANNEL_ID_OFFSET in GLONASS_CHANNELS
    ) {
        orbits.glonassOnChannel(signal.id - GLONASS_CHANNEL_ID_OFFSET)
    } else {
        listOf(signal.id)
    }

    return ids.mapNotNull { orbits.nearest(signal.constellation, it, signal.receivedSeconds) }
}

/** What the orbits say about one satellite of an epoch. */
data class Held(
    val constellation: Constellation,
    val id: Int,
    /** How high it stands over the place, in degrees. */
    val elevationDegrees: Double,
    /** How far its distance is off, the receiver's clock taken out, in metres. */
    val offMetres: Double,
) {
    val belowTheHorizon: Boolean get() = elevationDegrees < BELOW_THE_HORIZON_DEGREES

    val distanceOff: Boolean
        get() {
            if (abs(offMetres) <= DISTANCE_OFF_METRES) {
                return false
            }

            val millis = offMetres / MILLISECOND_METRES

            return abs(millis - millis.roundToLong()) * MILLISECOND_METRES >
                WHOLE_MILLISECOND_SLACK_METRES
        }
}

/** One epoch held against the orbits. */
data class OrbitEpoch(val at: Instant, val held: List<Held>) {
    /** The satellites the orbits cannot account for. */
    val off: List<Held> get() = held.filter { it.belowTheHorizon || it.distanceOff }

    val suspicious: Boolean get() = off.isNotEmpty()
}

/**
 * One epoch held against the orbits, as seen from [place], or nothing where it
 * cannot be: too few satellites with a whole time and an orbit.
 *
 * Each satellite is put where it was when its signal left, which is the
 * moment of arrival less the flight, and then turned with Earth for as long as
 * the flight took. What is left of a distance after the satellite's clock is
 * the receiver's clock error, the same for every satellite of the epoch; the
 * middle of them is taken as that error, so that a few false satellites
 * among honest ones do not move it.
 */
fun holdAgainstOrbits(
    at: Instant,
    clock: Clock,
    measurements: List<Measurement>,
    orbits: Orbits,
    place: Place,
): OrbitEpoch? {
    val here = ecefOf(place)
    val ranged = measurements
        .mapNotNull { rangedOf(clock, it) }
        .distinctBy { it.constellation to it.id }

    val raw = ranged.mapNotNull { signal ->
        candidatesFor(signal, orbits)
            .map { orbit -> seen(signal, orbit, here) }
            .maxByOrNull { it.second }
    }

    if (raw.size < ORBIT_CHECK_MIN_SATELLITES) {
        return null
    }

    val middle = median(raw.map { it.third })

    return OrbitEpoch(
        at = at,
        held = raw.map { (signal, elevation, off) ->
            Held(signal.constellation, signal.id, elevation, off - middle)
        },
    )
}

/** The satellite of [orbit] as seen from [here]: its elevation and how far the signal's distance is off. */
private fun seen(
    signal: Ranged,
    orbit: BroadcastOrbit,
    here: Ecef,
): Triple<Ranged, Double, Double> {
    val flight = signal.metres / LIGHT_MPS
    var sent = signal.receivedSeconds - flight
    var there = satelliteAt(orbit, sent)

    sent = signal.receivedSeconds - flight - there.clockSeconds
    there = satelliteAt(orbit, sent)

    val satellite = turnedWithEarth(there.position, flight)
    val between = satellite - here
    val distance = between.length()
    val up = between.dot(here) / distance / here.length()

    return Triple(
        signal,
        Math.toDegrees(asin(up)),
        signal.metres - distance + there.clockSeconds * LIGHT_MPS,
    )
}

/** Earth turns under a signal while it flies; the satellite's place is turned back with it. */
private fun turnedWithEarth(position: Ecef, seconds: Double): Ecef {
    val angle = EARTH_TURNS_RADIANS_PER_SECOND * seconds

    return Ecef(
        x = position.x * cos(angle) + position.y * sin(angle),
        y = -position.x * sin(angle) + position.y * cos(angle),
        z = position.z,
    )
}

private fun median(values: List<Double>): Double {
    val sorted = values.sorted()
    val half = sorted.size / 2

    return if (sorted.size % 2 == 1) sorted[half] else (sorted[half - 1] + sorted[half]) / 2
}

/**
 * Whether the last checked epochs say the sky is not the one the orbits
 * describe: [ORBIT_EPOCHS_IN_A_ROW] of them in a row, the newest recent.
 *
 * Epochs that could not be checked are not in [checked] at all, so they
 * neither break a run nor make one.
 */
fun offTheOrbits(checked: List<OrbitEpoch>, now: Instant): Boolean {
    val last = checked.takeLast(ORBIT_EPOCHS_IN_A_ROW)

    return last.size == ORBIT_EPOCHS_IN_A_ROW &&
        last.all { it.suspicious } &&
        Duration.between(last.last().at, now) <= ORBIT_CHECK_STILL_CURRENT
}

/**
 * The place the orbits are read from, or null where there is none recent
 * enough.
 *
 * The towers and the Wi-Fi first: a false sky cannot move them. Failing that,
 * the last position the jump filter wrote, which it believed at the time and
 * which a sky judged false does not move.
 */
fun orbitReference(networkSaid: Fix?, lastWritten: Fix?, now: Instant): Place? =
    listOfNotNull(networkSaid, lastWritten)
        .firstOrNull { Duration.between(it.takenAt, now) <= ORBIT_REFERENCE_FRESH }
        ?.let { Place(it.latitude, it.longitude, it.reported?.altitudeMetres ?: 0.0) }
