package io.github.vitalyostanin.geoshare.collection

import java.time.Duration

// Telling a move from a jump.
//
// Where the satellites are jammed, the provider goes on answering: the answers
// are simply wrong, and they land kilometres from where the phone is. On a map
// that reads as a member teleporting across the city and back every few
// minutes. What gives such an answer away is not the distance but the speed it
// implies, and the width the provider itself puts on it.

/**
 * The fastest a phone plausibly travels between two fixes, in metres a second.
 *
 * Two hundred kilometres an hour: faster than a car on a motorway and than any
 * train the members ride, slower than the hundreds of metres a second a jammed
 * answer implies. A flight would cross it, and a phone in one is in flight
 * mode anyway.
 */
const val PLAUSIBLE_SPEED_MPS: Double = 55.0

/**
 * The width past which an answer says nothing about where the phone is.
 *
 * Half a kilometre is a neighbourhood: a marker placed by such an answer is
 * not a position, it is a guess that happens to have coordinates.
 */
const val USELESS_ACCURACY_METRES: Double = 500.0

/**
 * How fast the phone would have had to travel, or nothing when the clock of
 * the two fixes does not run forward.
 */
fun impliedSpeedMps(last: Fix, next: Fix): Double? {
    val seconds = Duration.between(last.takenAt, next.takenAt).toMillis() / MILLIS_IN_A_SECOND

    if (seconds <= 0.0) {
        return null
    }

    return distanceMetres(last, next) / seconds
}

/**
 * What gives a jump away, so that the log says which rule caught the fix
 * rather than leaving the numbers to be compared by hand.
 */
enum class JumpReason {
    /** The provider put a width on the answer that covers a neighbourhood. */
    Width,

    /** Reaching the place would have taken a speed no member travels at. */
    Speed,

    /** Two places carrying one time: the provider contradicting itself. */
    Contradiction,
}

/**
 * Why a fix is a jump rather than a move, or nothing when it is a move.
 *
 * The first fix of a session is never a jump: there is nothing to have jumped
 * from. A fix no wider than the doubt in the two answers put together has not
 * moved at all in any sense worth arguing about.
 */
fun whyAJump(last: Fix?, next: Fix): JumpReason? {
    if (last == null) {
        return null
    }

    if ((next.accuracyMetres ?: 0.0) >= USELESS_ACCURACY_METRES) {
        return JumpReason.Width
    }

    return whyOutOfReach(last, next)
}

/**
 * Whether two fixes agree about where the phone is.
 *
 * How wide the answers are is not asked here, and that is the point: a pair of
 * wide answers that agree with each other is still the phone saying where it
 * is. This is what a fix left out has to pass to be believed after all — one
 * answer nobody can reach is a jump, two answers that agree are a phone that
 * has moved and a last position that has gone stale.
 */
fun fixesAgree(one: Fix, other: Fix): Boolean = whyOutOfReach(one, other) == null

/** What stands between two fixes once the width of the answer is granted. */
private fun whyOutOfReach(last: Fix, next: Fix): JumpReason? {
    val doubt = (last.accuracyMetres ?: 0.0) + (next.accuracyMetres ?: 0.0)

    if (distanceMetres(last, next) <= doubt) {
        return null
    }

    // A fix stamped at or before the last one, from somewhere else: two
    // positions at one time is the provider contradicting itself, not a phone
    // that moved.
    val speed = impliedSpeedMps(last, next) ?: return JumpReason.Contradiction

    return if (speed > PLAUSIBLE_SPEED_MPS) JumpReason.Speed else null
}

private const val MILLIS_IN_A_SECOND = 1_000.0
