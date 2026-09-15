package io.github.vitalyostanin.geoshare.collection

import java.time.Duration
import java.time.Instant

// Whether the phone was carried anywhere, asked of the one part of it a false
// signal cannot reach.
//
// Every rule that compares one answer with another is arguing with a
// transmitter about geometry, and a transmitter that draws its road slowly
// enough wins that argument: the answers that came out of the phone on
// 13.09.2026 crept about thirty metres apiece while the phone lay indoors, and
// no speed, width or shape catches a creep. The accelerometer is not on that
// signal. It answers a smaller question than the receiver does — was this
// thing picked up and taken somewhere — and answers it about the phone rather
// than about the sky.
//
// The other half of the same idea is already here in [movedWhileStanding],
// which weighs a fix against the position written before it. That catches a
// leap and lets a creep through, because the position written before it crept
// along with the lie: every pair of answers is a metre or two apart, and the
// place they walk away from is nowhere in the comparison. So the place is kept
// instead — where the phone was standing when it was last put down — and the
// answers are weighed against that, for as long as nothing has touched it.

/**
 * How many steps a phone may count while still standing where it was put.
 *
 * Twenty, which is fifteen metres of walking. Some are counted by a phone
 * being picked up off a table and put back; a person going anywhere at all
 * takes more than this before they reach the door.
 */
const val CARRIED_STEPS: Int = 20

/**
 * How far an answer may wander from where a put-down phone is lying.
 *
 * A hundred and fifty metres. Measured rather than chosen: of 477 answers this
 * phone gave over an hour and a half of lying indoors on 15.09.2026, nine in
 * ten stood within twenty metres of the middle of them, and the ones that did
 * not stood at 115, 313, 885 and 1025 metres — every one of them a place the
 * phone was not. The threshold sits above the honest spread with room to
 * spare and below the nearest of the lies.
 *
 * The width of the two answers is added to it where they are wider than that,
 * so a phone answering from a courtyard with a hundred metres of doubt on each
 * side is not accused of anything.
 */
const val PUT_DOWN_DRIFT_METRES: Double = 150.0

/**
 * How long the sensors have to stay quiet before the phone counts as put down.
 *
 * Two minutes. A phone set on a table is still being knocked about for a few
 * seconds after; waiting that out costs nothing, because what the wait delays
 * is the moment this rule starts speaking, and a creep takes longer than two
 * minutes to go anywhere interesting.
 */
val PUT_DOWN_AFTER: Duration = Duration.ofMinutes(2)

/**
 * How long a stir is worth keeping after it arrives.
 *
 * Half an hour. What the stirs are asked is whether anything has touched the
 * phone since the anchor was set, and an anchor older than this has been
 * replaced by a later one long since — a position is written every few minutes
 * even from a phone standing still.
 */
val STIRS_KEPT: Duration = Duration.ofMinutes(30)

/**
 * A moment the phone's own sensors noticed it being moved.
 *
 * One kind of event rather than two, because the difference between the
 * accelerometer's significant motion and a step being counted does not matter
 * to anything asked here: both of them mean the phone is in somebody's hand or
 * pocket, and neither can be produced by a transmitter.
 */
data class Stir(
    /** When it happened, by the clock the fixes are stamped with. */
    val at: Instant,
    /** The count the step sensor stood at, where it was the step sensor. */
    val steps: Long? = null,
)

/**
 * Where the phone was standing when it was last put down.
 *
 * The position is one this phone wrote down and believed at the time. A lie
 * can become an anchor — a phone put down inside a spoofed area anchors on
 * whatever it was told — and the rule is still worth having then: it catches
 * the answers walking away from that spot, which is what they do.
 */
data class PutDown(
    /** The position believed at the moment the phone settled. */
    val at: Fix,
    /** When the sensors fell quiet. */
    val since: Instant,
    /**
     * What the step counter stood at then, where there is one.
     *
     * The counter counts from the last boot and never restarts, so the steps
     * taken since the phone was put down are the difference from here. Kept
     * with the anchor rather than worked out from the stirs, because a batch
     * of steps arrives as one reading and a difference taken inside it would
     * be zero.
     */
    val steps: Long? = null,
)

/**
 * What the sensors have to say about the time since the phone was put down.
 *
 * [told] separates "the sensors say nothing happened" from "there are no
 * sensors, or the permission was refused": the two read the same and mean
 * opposite things, and only the first of them is evidence.
 */
data class Carrying(
    /** Whether the phone has been stirred since it was put down. */
    val stirred: Boolean,
    /** Steps counted since then, where the counter answers. */
    val steps: Int? = null,
    /** Whether any of this is the sensors talking. */
    val told: Boolean = true,
)

/**
 * Whether the phone counts as put down, given the last stir and the position
 * it is standing at.
 *
 * Null wherever it does not: the sensors have not spoken, something touched
 * the phone within the last [PUT_DOWN_AFTER], or there is no position to
 * anchor on. A null anchor is the rule saying nothing, which is what it should
 * say wherever it does not know.
 */
fun putDownAt(
    where: Fix?,
    lastStir: Instant?,
    told: Boolean,
    now: Instant,
    steps: Long? = null,
): PutDown? {
    if (!told || where == null) {
        return null
    }

    val quietSince = lastStir ?: return PutDown(where, now, steps)

    if (Duration.between(quietSince, now) < PUT_DOWN_AFTER) {
        return null
    }

    return PutDown(where, quietSince.plus(PUT_DOWN_AFTER), steps)
}

/**
 * Whether an answer has walked away from a phone nobody has touched.
 *
 * The anchor holds only while the sensors go on saying nothing: a stir, or
 * enough steps to have gone anywhere, and the rule falls silent rather than
 * accusing the first honest fix after somebody picked the phone up. It falls
 * silent as well for a fix taken before the phone settled — that answer is
 * about a different part of the day.
 */
fun driftedWhilePutDown(putDown: PutDown?, next: Fix, carrying: Carrying): Boolean {
    if (putDown == null || !carrying.told || carrying.stirred) {
        return false
    }

    if ((carrying.steps ?: 0) >= CARRIED_STEPS) {
        return false
    }

    if (next.takenAt.isBefore(putDown.since)) {
        return false
    }

    val doubt = (putDown.at.accuracyMetres ?: 0.0) + (next.accuracyMetres ?: 0.0)

    return distanceMetres(putDown.at, next) > maxOf(PUT_DOWN_DRIFT_METRES, doubt)
}

/**
 * What the sensors say about the time since the phone was put down, worked out
 * from the stirs that have arrived.
 *
 * Stirs arrive late: the step counter is read in batches so that the chip can
 * stay asleep between them, and a batch can land a minute after the steps in
 * it were taken. That is why the moment of each stir is carried rather than
 * the fact of it — a stir that happened inside the anchor's watch undoes the
 * anchor when it arrives, however late that is.
 */
fun carryingSince(putDown: PutDown?, stirs: List<Stir>, told: Boolean = true): Carrying {
    if (putDown == null) {
        return Carrying(stirred = false, told = told)
    }

    val after = stirs.filter { !it.at.isBefore(putDown.since) }
    val counted = after.mapNotNull { it.steps }.maxOrNull()

    return Carrying(
        stirred = after.any { it.steps == null },
        steps =
        if (counted == null || putDown.steps == null) {
            null
        } else {
            (counted - putDown.steps).coerceAtLeast(0).toInt()
        },
        told = told,
    )
}
