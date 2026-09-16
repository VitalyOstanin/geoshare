package io.github.vitalyostanin.geoshare.collection

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
 *
 * The time between them is counted by [apart], which asks the count since
 * boot before the wall clock: a phone whose clock is stepped between two
 * fixes must not be read as having travelled the distance in the difference.
 */
fun impliedSpeedMps(last: Fix, next: Fix): Double? {
    val seconds = apart(last, next).toMillis() / MILLIS_IN_A_SECOND

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

    /**
     * The answers have walked away from a phone lying untouched.
     *
     * The same evidence as [Unmoved] held against a place instead of against
     * the answer before: a creep of thirty metres an answer never trips a
     * comparison between neighbours, and every one of those metres is distance
     * from where the phone was put down. See [driftedWhilePutDown].
     */
    Drifted,

    /**
     * The towers and the Wi-Fi place the phone somewhere else entirely.
     *
     * The one rule that does not compare a position with an earlier position,
     * and so the one a slowly drawn false road does not defeat.
     */
    Contradicted,

    /**
     * The answers are going round a circle at a steady speed.
     *
     * Read off the run of answers rather than off a pair of them: every pair
     * of a false road is a plausible stretch of driving, and what gives the
     * road away is that it closes.
     */
    Circling,

    /**
     * The fix was built out of the band a false signal is transmitted in,
     * while the band it cannot reach was heard and left out.
     *
     * Read off one epoch rather than off a run of them, and so the one rule
     * that speaks on the first false answer: see [restsOnOneBand].
     */
    OneBand,

    /**
     * The sky the answer was built out of came from one transmitter.
     *
     * The other half of [OneBand], for the phone that hears the far band not
     * at all rather than hears it and leaves it out: the receiver holds the
     * orbits of those satellites and hears none of them, while the near band
     * arrives at one strength however high a satellite claims to be. See
     * [fromOneTransmitter], and Slant.kt for what a flat sky is.
     */
    OneTransmitter,

    /**
     * The answers are tracing a route they have already been round.
     *
     * The same reading as the circle, without asking what shape the route is:
     * a transmitter that changes its figure still has to close it, and a
     * person retraces nothing on a fixed period for an hour.
     */
    Repeating,

    /**
     * The receiver had not settled since the phone was switched on.
     *
     * The only rule whose limits are not the limits of the others: what it
     * catches would pass every one of them, being a walking pace and a width
     * a courtyard fits in. See Settling.kt for the minutes this covers and
     * for what the platform answers during them.
     */
    Settling,
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
 * The places themselves come from the group's own settings rather than from
 * the build, and [falsePlaces] is empty wherever none have been named or the
 * phone has not reached them yet.
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
 *
 * Where the phone was put down is asked about in the same spirit and needs no
 * anchor of its own beyond that place: [putDown] is the spot the phone was
 * standing at when its sensors last fell quiet, and [carrying] is what they
 * have said since. This is the only rule here that a creep does not defeat —
 * every other comparison is against the position written before, which crept
 * along with the lie. Both are absent wherever the phone has no such sensors,
 * the permission was refused, or something has touched it since.
 *
 * The shape of the run is asked about next, and it is asked of the answers
 * the phone was given rather than of the ones it wrote down: while the speed
 * rule is still refusing a false road, the answers it refuses are the ones
 * that show the shape. [lately] is empty wherever there is no run to look at.
 *
 * The other radios are asked before any of that, and before the anchor, for
 * the same reason the false places are: their answer needs nothing earlier to
 * be worth something. A phone switched on inside a spoofed area has no anchor,
 * no sensor history and no list of false places covering a new one — and still
 * has towers around it that the transmitter never reached. [networkSays] is
 * null wherever the other radios were not asked or had nothing to say.
 *
 * What the fix rests on is asked in the same breath and for the same reason:
 * one epoch of the receiver's own status, needing no trail behind it. It is
 * asked after the other radios because they are the stronger statement — a
 * tower naming another district contradicts the position itself, where this
 * contradicts only the way it was built. [restsOn] is empty wherever there is
 * nothing to ask: no status, a status too old to be about this fix, or a phone
 * that hears one band and has nothing to compare.
 *
 * Whether the receiver has settled since the phone was switched on is asked
 * last of the rules that need no anchor, and it is asked before the anchor is
 * required because the first answer after a boot is the one with nothing
 * behind it. [settling] is false wherever the phone has been up long enough or
 * its answers are built out of satellites; see Settling.kt for both.
 */
fun whyAJump(
    last: Fix?,
    next: Fix,
    stillSince: Instant? = null,
    networkSays: Fix? = null,
    now: Instant = next.takenAt,
    lately: List<Fix> = emptyList(),
    falsePlaces: List<FalsePlace> = emptyList(),
    circleLimits: CircleLimits = CircleLimits(),
    repeatLimits: RepeatLimits = RepeatLimits(),
    restsOn: Map<Constellation, Counted> = emptyMap(),
    bandLimits: OneBandLimits = OneBandLimits(),
    skySays: SkySays = SkySays(),
    putDown: PutDown? = null,
    carrying: Carrying = Carrying(stirred = false, told = false),
    settling: Boolean = false,
): JumpReason? {
    if (falsePlaceFor(next, falsePlaces) != null) {
        return JumpReason.FalsePlace
    }

    if (secondOpinion(next, networkSays, now)?.disagrees == true) {
        return JumpReason.Contradicted
    }

    if (restsOnOneBand(restsOn, bandLimits)) {
        return JumpReason.OneBand
    }

    if (fromOneTransmitter(skySays)) {
        return JumpReason.OneTransmitter
    }

    if (goingInCircles(lately, circleLimits)) {
        return JumpReason.Circling
    }

    if (goingRoundAgain(lately, repeatLimits)) {
        return JumpReason.Repeating
    }

    if (driftedWhilePutDown(putDown, next, carrying)) {
        return JumpReason.Drifted
    }

    if (settling && settlingJump(last, next)) {
        return JumpReason.Settling
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

/**
 * The widest an answer may be and still be confirmable by an agreeing one.
 *
 * Two kilometres. What two answers agreeing means depends entirely on how wide
 * they are, because agreement is measured against the doubt in them: a pair
 * twenty kilometres wide agrees with itself across a whole region, and a phone
 * was watched being carried out of its own city by exactly that — two answers
 * of twenty-one and nineteen kilometres, forty kilometres from the flat,
 * agreeing with each other because their circles of doubt overlapped. Past two
 * kilometres an answer names a district, and two districts overlapping is not
 * a phone saying where it is.
 */
const val CONFIRMABLE_WIDTH_METRES: Double = 2_000.0

/**
 * Whether a second, agreeing answer really does overrule the width that caught
 * the first.
 *
 * Three things are asked, and the last of them is why this is not simply two
 * answers agreeing. The rule the agreement overrules is the one saying the
 * answer is too wide to place the phone; letting the same wide answers vouch
 * for each other therefore means trusting the very thing that was doubted. So
 * something the transmitter cannot reach has to put the phone there too — not
 * merely fail to contradict it, which a wide answer is never contradicted by,
 * its doubt being wider than any disagreement.
 *
 * A phone out of reach of the towers and the Wi-Fi around it confirms nothing
 * here, and that is intended: its way back is the one that does not depend on
 * a radio at all, a place it has answered from three times over an hour.
 */
fun agreementStands(
    jump: JumpReason,
    candidate: Fix?,
    next: Fix,
    networkSays: Fix? = null,
    now: Instant = next.takenAt,
): Boolean {
    if (!mayBeConfirmed(jump) || candidate == null || !fixesAgree(candidate, next)) {
        return false
    }

    val widest = maxOf(candidate.accuracyMetres ?: 0.0, next.accuracyMetres ?: 0.0)

    if (widest > CONFIRMABLE_WIDTH_METRES) {
        return false
    }

    return backedUpBy(next, networkSays, now)
}

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
