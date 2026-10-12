package io.github.vitalyostanin.geoshare.map

/** How far north and south the globe goes, and how far east and west. */
private const val LATITUDE_LIMIT = 90.0
private const val LONGITUDE_LIMIT = 180.0
private const val FULL_TURN = 360.0

/**
 * The same angle, written as the turn of at most half a circle either way.
 *
 * Arithmetic rather than a loop that takes the turn off step by step: the
 * number comes from another member's document, and a device that writes a
 * degree count in the quadrillions would have the drawing thread here for the
 * rest of the afternoon. The page does the same thing the same way.
 */
fun wrapDegrees(degrees: Double): Double {
    if (!degrees.isFinite()) {
        return 0.0
    }

    return (((degrees + LONGITUDE_LIMIT) % FULL_TURN + FULL_TURN) % FULL_TURN) - LONGITUDE_LIMIT
}

/**
 * Whether a pair of coordinates is a place on the globe.
 *
 * Written by another member's device, so it is worth as much as that device: a
 * missing field, a number no globe has room for and a value that is not a
 * number at all all arrive the same way, and a member left off the map is a
 * better answer than a map that cannot be drawn.
 */
fun placeable(latitude: Double?, longitude: Double?): Boolean = latitude != null &&
    longitude != null &&
    latitude.isFinite() &&
    longitude.isFinite() &&
    latitude in -LATITUDE_LIMIT..LATITUDE_LIMIT &&
    longitude in -LONGITUDE_LIMIT..LONGITUDE_LIMIT
