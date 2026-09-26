package io.github.vitalyostanin.geoshare.map

import java.time.Instant
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** The radius the distance is measured on: the mean sphere the ring uses. */
private const val EARTH_RADIUS_METRES = 6_371_008.8

/**
 * How far the member on a card is from the viewer.
 *
 * Near rather than a number while the two answers lie inside each other's
 * width: forty metres between two answers each sixty metres wide is two
 * answers about one flat, and a figure would say the member is across the
 * street. The page holds the same rules.
 */
sealed interface ViewerDistance {
    data object Near : ViewerDistance

    data class Away(val metres: Double) : ViewerDistance
}

/**
 * Where the viewer is: the freshest place any of their own phones has written.
 *
 * All of their phones rather than this one alone, so that a member reading the
 * map on a second phone left at home is measured from the one in their pocket.
 *
 * A place that has gone grey is no place to measure from: the viewer has had
 * half an hour to walk off, and a distance from where they were is read as a
 * distance from where they are. A marker left behind by a phone that is still
 * online is the same place gone grey, only without the colour.
 */
fun ownPlace(positions: List<MemberPosition>, ownMemberId: String, now: Instant): MemberPosition? =
    positions
        .filter {
            it.memberId == ownMemberId &&
                !isStale(it.knownAt, now) &&
                !isStuck(it.positionAt, it.knownAt, now)
        }.maxByOrNull { it.positionAt }

/** Haversine: good to a few metres at any distance a family is spread over. */
fun metresBetween(from: MemberPosition, to: MemberPosition): Double {
    val fromLatitude = Math.toRadians(from.latitude)
    val toLatitude = Math.toRadians(to.latitude)
    val northwards = toLatitude - fromLatitude
    val eastwards = Math.toRadians(to.longitude - from.longitude)

    val a =
        sin(northwards / 2).pow(2) +
            cos(fromLatitude) * cos(toLatitude) * sin(eastwards / 2).pow(2)

    return 2 * EARTH_RADIUS_METRES * asin(sqrt(min(a, 1.0)))
}

/**
 * How far [position] is from the viewer, or null when the card has nothing to
 * say about it: on the card of the very place measured from, and when the
 * viewer has no place of their own to measure from.
 */
fun distanceFromViewer(
    position: MemberPosition,
    positions: List<MemberPosition>,
    ownMemberId: String,
    now: Instant,
): ViewerDistance? {
    val own = ownPlace(positions, ownMemberId, now)

    if (own == null || own.positionId == position.positionId) {
        return null
    }

    val metres = metresBetween(own, position)
    val widest = max(own.accuracyMetres ?: 0.0, position.accuracyMetres ?: 0.0)

    return if (metres <= widest) ViewerDistance.Near else ViewerDistance.Away(metres)
}
