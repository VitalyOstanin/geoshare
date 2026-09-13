package io.github.vitalyostanin.geoshare.collection

import java.time.Duration
import java.time.Instant

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

    /** The answer landed where a false signal is known to put phones. */
    FalsePlace,

    /** The phone's own sensors say it has not been carried anywhere. */
    Unmoved,
}

/**
 * Why a fix is a jump rather than a move, or nothing when it is a move.
 *
 * The known false places are asked about first, and that order is the whole
 * point of them: every other rule compares a fix with the one before it, so a
 * phone switched on inside a spoofed area has nothing to catch its first
 * answer with and takes the lie as its anchor. A place needs no anchor to be
 * recognised.
 *
 * Past that, the first fix of a session is never a jump: there is nothing to
 * have jumped from. A fix no wider than the doubt in the two answers put
 * together has not moved at all in any sense worth arguing about.
 *
 * The sensors are asked last, and only about what the other rules let through:
 * they answer about the phone rather than about the answer, and a road drawn
 * slowly enough to pass the speed is still a road the phone was never carried
 * along. [stillSince] is null wherever there is nothing to ask — the
 * permission was refused, or the sensors have not reported yet.
 */
fun whyAJump(last: Fix?, next: Fix, stillSince: Instant? = null): JumpReason? {
    if (falsePlaceFor(next) != null) {
        return JumpReason.FalsePlace
    }

    if (last == null) {
        return null
    }

    if ((next.accuracyMetres ?: 0.0) >= USELESS_ACCURACY_METRES) {
        return JumpReason.Width
    }

    whyOutOfReach(last, next)?.let { return it }

    return if (movedWhileStanding(stillSince, last, next)) JumpReason.Unmoved else null
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

/**
 * Whether a second, agreeing fix may overrule the rule that caught the first.
 *
 * Only the width rule, and the reason is what a spoofed signal looks like. A
 * receiver fed a false signal does not jitter: it draws a road, and any two of
 * its fixes agree with each other perfectly. Letting agreement overrule the
 * speed rule therefore hands the anchor to the spoofer in the time between two
 * fixes — seventeen seconds, in the case this was written for, to move the
 * phone seventy-three kilometres.
 *
 * Against the speed rule nothing needs to be done anyway: the speed it
 * measures falls as the clock runs, so a phone that really did travel is
 * believed as soon as the journey has had time to happen. A width has no such
 * clock — an answer half a kilometre wide stays half a kilometre wide however
 * long the phone waits — which is why agreement is left to overrule that one.
 *
 * A known false place is not up for confirmation either, and for the same
 * reason as the speed: agreement is exactly what a spoofer produces. Neither
 * is a phone that was never carried anywhere — two answers agreeing about a
 * place a kilometre away are two answers of the same jammed receiver.
 */
fun mayBeConfirmed(jump: JumpReason): Boolean = jump == JumpReason.Width

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
