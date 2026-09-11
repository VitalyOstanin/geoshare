package io.github.vitalyostanin.geoshare.collection

import java.time.Duration
import java.time.Instant
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class Fix(val latitude: Double, val longitude: Double, val takenAt: Instant)

/** Below this a fix is jitter rather than movement. */
const val WRITE_DISTANCE_METRES: Double = 75.0

/** However still the phone is, it says so this often. */
val WRITE_SILENCE: Duration = Duration.ofMinutes(10)

/**
 * Whether a fix is worth a write.
 *
 * Writes are what wake the other side's subscription, so the threshold is not
 * only about the free quota: a phone lying on a table would otherwise keep
 * everyone's screen busy redrawing the same marker.
 */
fun shouldWrite(last: Fix?, next: Fix): Boolean {
    if (last == null) {
        return true
    }

    if (distanceMetres(last, next) >= WRITE_DISTANCE_METRES) {
        return true
    }

    return Duration.between(last.takenAt, next.takenAt) >= WRITE_SILENCE
}

private const val EARTH_RADIUS_METRES = 6_371_000.0

/** Haversine: the fixes compared here are metres apart, not continents. */
fun distanceMetres(from: Fix, to: Fix): Double {
    val fromLatitude = Math.toRadians(from.latitude)
    val toLatitude = Math.toRadians(to.latitude)
    val deltaLatitude = toLatitude - fromLatitude
    val deltaLongitude = Math.toRadians(to.longitude - from.longitude)

    val a =
        sin(deltaLatitude / 2).pow(2) +
            cos(fromLatitude) * cos(toLatitude) * sin(deltaLongitude / 2).pow(2)

    return 2 * EARTH_RADIUS_METRES * asin(sqrt(a))
}
