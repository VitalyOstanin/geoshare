package io.github.vitalyostanin.geoshare.collection

import kotlin.math.abs
import kotlin.math.sqrt

// GLONASS describes an orbit differently from GPS and Galileo: not as an
// ellipse with corrections but as where the satellite is, how fast it moves
// and how the Sun and the Moon pull it, every half hour. The position at any
// other moment is found by following the satellite through Earth's gravity
// from there, step by step, as the interface document of the system describes.
//
// Followed for a day, which is what yesterday's file asks for, the position
// stays within a few kilometres of the truth: measured on the phones' own
// records, against the two hundred the check allows.

/** PZ-90: Earth's gravitational constant, its radius, its flattening term and turn. */
private const val MU_GLONASS = 3.9860044e14
private const val RADIUS_GLONASS = 6_378_136.0
private const val J2_GLONASS = 1.0826257e-3
private const val TURN_GLONASS = 7.292115e-5

/** One step of the integration, in seconds: a minute, as the interface document does. */
private const val STEP_SECONDS = 60.0

/** Where a satellite is and how fast it moves, in metres and metres a second. */
private data class Motion(val at: Double, val p: Ecef, val v: Ecef)

/**
 * One orbit as a GLONASS satellite broadcast it.
 *
 * Not a data class: it remembers how far it was last followed, so that the
 * next epoch, a second later, costs one step rather than a day of them.
 */
class GlonassOrbit(
    override val id: Int,
    /** The frequency channel, -7 to +6: the satellite's carrier is 1602 MHz plus this many 562.5 kHz. */
    val channel: Int,
    /** The moment the record describes, on the GPS scale. */
    override val orbitSeconds: Double,
    /** How far the satellite's clock is off at that moment, in seconds (-τn in the file). */
    val clockBias: Double,
    /** How fast its frequency is off, relative (+γn in the file). */
    val frequencyBias: Double,
    val position: Ecef,
    val velocity: Ecef,
    /** The pull of the Sun and the Moon, taken as steady over the record. */
    val pull: Ecef,
) : BroadcastOrbit {
    override val constellation: Constellation get() = Constellation.Glonass

    @Volatile
    private var followed = Motion(orbitSeconds, position, velocity)

    /** Where the satellite was at [gpsSeconds] and how far its clock was off. */
    fun at(gpsSeconds: Double): SatelliteAt {
        val known = followed
        val start = if (abs(known.at - gpsSeconds) < abs(orbitSeconds - gpsSeconds)) {
            known
        } else {
            Motion(orbitSeconds, position, velocity)
        }
        val reached = follow(start, gpsSeconds)

        followed = reached

        return SatelliteAt(
            position = reached.p,
            clockSeconds = clockBias + frequencyBias * (gpsSeconds - orbitSeconds),
        )
    }

    private fun follow(from: Motion, to: Double): Motion {
        var motion = from

        while (abs(to - motion.at) > 1e-9) {
            val left = to - motion.at
            val h = if (abs(left) > STEP_SECONDS) STEP_SECONDS * Math.signum(left) else left

            motion = step(motion, h)
        }

        return motion
    }

    /** One step of Runge-Kutta, the fourth order. */
    private fun step(m: Motion, h: Double): Motion {
        val (a1, b1) = m.v to pullAt(m.p, m.v)
        val (a2, b2) = (m.v + b1 * (h / 2)) to pullAt(m.p + a1 * (h / 2), m.v + b1 * (h / 2))
        val (a3, b3) = (m.v + b2 * (h / 2)) to pullAt(m.p + a2 * (h / 2), m.v + b2 * (h / 2))
        val (a4, b4) = (m.v + b3 * h) to pullAt(m.p + a3 * h, m.v + b3 * h)

        return Motion(
            at = m.at + h,
            p = m.p + (a1 + a2 * 2.0 + a3 * 2.0 + a4) * (h / 6),
            v = m.v + (b1 + b2 * 2.0 + b3 * 2.0 + b4) * (h / 6),
        )
    }

    /** The acceleration in Earth's turning frame: gravity with its flattening, the turn itself, the Sun and the Moon. */
    private fun pullAt(p: Ecef, v: Ecef): Ecef {
        val r2 = p.dot(p)
        val r = sqrt(r2)
        val a = 1.5 * J2_GLONASS * MU_GLONASS * RADIUS_GLONASS * RADIUS_GLONASS / (r2 * r2 * r)
        val b = 5.0 * p.z * p.z / r2
        val c = -MU_GLONASS / (r2 * r) - a * (1.0 - b)
        val w2 = TURN_GLONASS * TURN_GLONASS

        return Ecef(
            x = (c + w2) * p.x + 2 * TURN_GLONASS * v.y + pull.x,
            y = (c + w2) * p.y - 2 * TURN_GLONASS * v.x + pull.y,
            z = (c - 2 * a) * p.z + pull.z,
        )
    }
}

private operator fun Ecef.plus(other: Ecef) = Ecef(x + other.x, y + other.y, z + other.z)

private operator fun Ecef.times(k: Double) = Ecef(x * k, y * k, z * k)
