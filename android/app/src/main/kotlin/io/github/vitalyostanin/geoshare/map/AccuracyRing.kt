package io.github.vitalyostanin.geoshare.map

import io.github.vitalyostanin.geoshare.collection.EARTH_RADIUS_METRES
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * How many corners the ring is drawn with.
 *
 * Sixty-four is where the corners stop being visible at the zoom a circle is
 * looked at: a ring of a hundred pixels then has a corner every ten, and the
 * line between two of them falls a tenth of a pixel short of the arc.
 */
private const val RING_CORNERS = 64

/** How flat the cosine is allowed to get before the ring stops widening. */
private const val NARROWEST_PARALLEL = 0.01

private const val DEGREES_IN_A_RADIAN = 180.0 / PI

/**
 * The ring a width draws around a position: a closed list of points.
 *
 * A ring of points rather than a circle of a fixed number of pixels, because
 * the width is a claim in metres: a circle drawn in pixels would say the same
 * thing about a fix eleven metres wide and one seventeen hundred metres wide as
 * soon as the map was zoomed.
 *
 * The parallel narrows towards the poles, so a step east is a wider turn there
 * than at the equator; dividing by its cosine is what keeps the ring round
 * rather than an ellipse squashed along the latitude. The cosine is held off
 * nought so that a position at the pole itself gives a ring rather than a
 * division that runs away.
 *
 * Points are (longitude, latitude), which is the order the map and the page
 * both read them in. The first point is repeated at the end, because a ring is
 * closed and the readers of it expect to be told so rather than to work it out.
 */
fun accuracyRing(
    latitude: Double,
    longitude: Double,
    metres: Double,
    corners: Int = RING_CORNERS,
): List<Pair<Double, Double>> {
    val northwards = metres / EARTH_RADIUS_METRES * DEGREES_IN_A_RADIAN
    val eastwards = northwards / max(cos(abs(latitude) / DEGREES_IN_A_RADIAN), NARROWEST_PARALLEL)

    return (0..corners).map { corner ->
        val turn = corner.toDouble() / corners * 2.0 * PI

        // Clamped rather than wrapped: a ring that reaches over the pole is
        // not a ring any drawing of a map can show, and stopping at the pole
        // is the honest end of it.
        val north = (latitude + northwards * cos(turn)).coerceIn(-POLE, POLE)
        val east = wrapDegrees(longitude + eastwards * sin(turn))

        east to north
    }
}

/** How far north and south a drawn point may go. */
private const val POLE = 90.0
