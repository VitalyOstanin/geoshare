package io.github.vitalyostanin.geoshare.map

import io.github.vitalyostanin.geoshare.collection.haversineMetres
import java.time.Instant

/**
 * Where the viewer is: the freshest place any of their own devices has written.
 *
 * All of their devices rather than this one alone, so that a member reading the
 * map on a second device left at home is measured from the one in their pocket.
 *
 * A place that has gone grey is no place to measure from: the viewer has had
 * half an hour to walk off, and a distance from where they were is read as a
 * distance from where they are. A marker left behind by a device that is still
 * in touch is the same place gone grey, only without the colour, and so is one
 * inside a place a false signal is known to put devices at.
 */
fun ownPlace(positions: List<MemberPosition>, ownMemberId: String, now: Instant): MemberPosition? =
    positions
        .filter {
            it.memberId == ownMemberId &&
                !it.inFalsePlace &&
                !isStale(it.knownAt, now) &&
                !isStuck(it.positionAt, it.knownAt, now)
        }.maxByOrNull { it.positionAt }

/** The metres between two positions. */
fun metresBetween(from: MemberPosition, to: MemberPosition): Double =
    haversineMetres(from.latitude, from.longitude, to.latitude, to.longitude)

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
 * Unlike a width, nought metres is an answer: two devices on one table.
 */
fun distanceUnits(metres: Double): PositionWidth = metresOrKilometres(metres, 0)
