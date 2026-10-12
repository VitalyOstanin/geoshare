package io.github.vitalyostanin.geoshare.map

import java.time.Duration
import java.time.Instant
import kotlin.math.abs

/**
 * What the map's camera does about the members on it.
 *
 * [NONE] leaves the camera wherever the last gesture put it. [ONE] keeps a
 * chosen member centred. [ALL] keeps every member's marker inside the view.
 * Picked from the follow button's own menu, one at a time, rather than
 * stepped through in order.
 */
enum class FollowMode { NONE, ONE, ALL }

/**
 * How long the camera leaves off following after a hand drags the map.
 *
 * Twenty seconds. Long enough to read a marker the follow mode had covered up
 * without the camera snapping back mid-glance; short enough that forgetting
 * about the map for a minute returns to a camera that follows again rather
 * than one stuck wherever a thumb last left it.
 */
val PAUSE_AFTER_A_GESTURE: Duration = Duration.ofSeconds(20)

/**
 * Whether a gesture at [pausedAt] still holds the camera still at [now].
 *
 * Null means no gesture has happened, which is never a pause. The boundary
 * itself is not a pause either: a duration exactly [PAUSE_AFTER_A_GESTURE]
 * old has finished waiting out its own allowance, not still inside it.
 */
fun isPaused(pausedAt: Instant?, now: Instant): Boolean {
    if (pausedAt == null) {
        return false
    }

    return Duration.between(pausedAt, now) < PAUSE_AFTER_A_GESTURE
}

/** A rectangle in latitude and longitude, the shape the camera is asked to fit. */
data class MapBounds(val west: Double, val south: Double, val east: Double, val north: Double)

/**
 * The smallest rectangle holding every position in [positions], or null for an
 * empty list — nothing for the camera to fit.
 *
 * Read west-to-east and south-to-north without wrapping: a household spread
 * across an antimeridian would need its width measured the other way round,
 * and nobody this map has ever shown lives on both sides of one. Handling
 * that crossing honestly is separate work, not a case this function guesses
 * at.
 */
fun boundsOf(positions: List<MemberPosition>): MapBounds? {
    if (positions.isEmpty()) {
        return null
    }

    return MapBounds(
        west = positions.minOf { it.longitude },
        south = positions.minOf { it.latitude },
        east = positions.maxOf { it.longitude },
        north = positions.maxOf { it.latitude },
    )
}

/**
 * How far two rectangles' edges may drift apart and still count as the same
 * view.
 *
 * Roughly twenty metres of latitude. A household standing still hands the
 * map a fresh, slightly different rectangle with every batch of
 * positions — the last few metres a receiver's estimate wanders by — and
 * moving the camera for that is a jitter nobody asked for. Twenty metres is
 * well inside what a device lying on a table wanders between two positions,
 * and well outside a step actually worth re-centring for.
 */
const val BOUNDS_TOLERANCE_DEGREES: Double = 0.0002

/**
 * Whether [one] and [other] are close enough that the camera should treat
 * them as the same view.
 *
 * Compared edge by edge rather than by area or centre: a household that grew
 * by one member at the rectangle's corner should move the camera even where
 * the centre barely shifts, and this catches that while still absorbing the
 * ordinary jitter of positions that have not gone anywhere.
 */
fun boundsAreClose(
    one: MapBounds,
    other: MapBounds,
    tolerance: Double = BOUNDS_TOLERANCE_DEGREES,
): Boolean = abs(one.west - other.west) < tolerance &&
    abs(one.south - other.south) < tolerance &&
    abs(one.east - other.east) < tolerance &&
    abs(one.north - other.north) < tolerance

/**
 * The position of [target] among [positions], or null where it is not there.
 *
 * A chosen member's [MemberPosition.positionId] can go missing between
 * batches — the device stopped sending, or the position it last wrote was
 * rejected — and this is how a caller finds out rather than following a
 * marker that no longer exists.
 */
fun targetPosition(positions: List<MemberPosition>, target: String?): MemberPosition? =
    positions.firstOrNull { it.positionId == target }
