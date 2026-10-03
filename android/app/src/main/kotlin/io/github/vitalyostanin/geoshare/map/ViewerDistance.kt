package io.github.vitalyostanin.geoshare.map

import io.github.vitalyostanin.geoshare.collection.EARTH_RADIUS_METRES
import java.time.Instant
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/** Where metres stop being the unit worth writing and kilometres start. */
private const val METRES_IN_A_KILOMETRE = 1_000.0

/** How a tenth of a kilometre is arrived at: hundreds of metres, then back. */
private const val METRES_IN_A_TENTH = 100.0
private const val TENTHS_IN_A_KILOMETRE = 10.0

/**
 * Where the viewer is: the freshest place any of their own phones has written.
 *
 * All of their phones rather than this one alone, so that a member reading the
 * map on a second phone left at home is measured from the one in their pocket.
 *
 * A place that has gone grey is no place to measure from: the viewer has had
 * half an hour to walk off, and a distance from where they were is read as a
 * distance from where they are. A marker left behind by a phone that is still
 * online is the same place gone grey, only without the colour, and so is one
 * inside a place a false signal is known to put phones at.
 */
fun ownPlace(positions: List<MemberPosition>, ownMemberId: String, now: Instant): MemberPosition? =
    positions
        .filter {
            it.memberId == ownMemberId &&
                !it.inFalsePlace &&
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
 * How far [position] is from the viewer, in metres, or null when the card has
 * nothing to say about it: on the card of the very place measured from, on the
 * card of a position a false signal is known to have put there, and when the
 * viewer has no place of their own to measure from. The page holds
 * the same rules.
 */
fun distanceFromViewer(
    position: MemberPosition,
    positions: List<MemberPosition>,
    ownMemberId: String,
    now: Instant,
): Double? {
    val own = ownPlace(positions, ownMemberId, now)

    if (own == null || own.positionId == position.positionId || position.inFalsePlace) {
        return null
    }

    return metresBetween(own, position)
}

/**
 * The distance in the units the width is written in: whole metres up to a
 * kilometre and kilometres to one place above it.
 *
 * Unlike a width, nought metres is an answer: two phones on one table.
 */
fun distanceUnits(metres: Double): PositionWidth {
    val whole = metres.roundToLong()

    if (whole < METRES_IN_A_KILOMETRE) {
        return PositionWidth.Metres(whole)
    }

    return PositionWidth.Kilometres(
        (metres / METRES_IN_A_TENTH).roundToLong() / TENTHS_IN_A_KILOMETRE,
    )
}
