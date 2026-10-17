package io.github.vitalyostanin.geoshare.collection

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The radius the distances here are measured on, in metres: the mean sphere,
 * the same one the page measures on.
 */
const val EARTH_RADIUS_METRES: Double = 6_371_008.8

/** Where metres stop being the unit worth writing and kilometres start. */
const val METRES_IN_A_KILOMETRE = 1_000.0

/** How a tenth of a kilometre is arrived at: hundreds of metres, then back. */
const val METRES_IN_A_TENTH = 100.0
const val TENTHS_IN_A_KILOMETRE = 10.0

/** Haversine: good to a few metres at any distance a family is spread over. */
fun haversineMetres(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val fromLatitude = Math.toRadians(lat1)
    val toLatitude = Math.toRadians(lat2)
    val deltaLatitude = toLatitude - fromLatitude
    val deltaLongitude = Math.toRadians(lon2 - lon1)

    val a =
        sin(deltaLatitude / 2).pow(2) +
            cos(fromLatitude) * cos(toLatitude) * sin(deltaLongitude / 2).pow(2)

    // Rounding carries a past one for points on opposite sides of the Earth;
    // capped, so that asin is never asked for what it has no answer to.
    return 2 * EARTH_RADIUS_METRES * asin(sqrt(min(a, 1.0)))
}
