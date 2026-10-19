package io.github.vitalyostanin.geoshare.collection

import java.time.Duration
import java.time.Instant
import java.util.Locale

// Whether the device was carried anywhere, asked of the one part of it a false
// signal cannot reach.
//
// A transmitter that draws its road slowly enough defeats every rule that
// compares one answer with another: a field measurement showed the answers
// creeping about thirty metres apiece while the device lay indoors, and no
// speed, width or shape catches a creep. The accelerometer is not on that
// signal; it answers about the device rather than about the sky.
//
// [movedWhileStanding] weighs a fix against the position written before it,
// so it catches a leap but not a creep: the previous position crept along with
// the lie. Here the place is kept instead, where the device was standing when
// it was last put down, and answers are weighed against that for as long as
// nothing has touched it.

/**
 * How many steps a device may count while still standing where it was put.
 *
 * Twenty, which is fifteen metres of walking. Some are counted by a device
 * being picked up off a table and put back; a person going anywhere at all
 * takes more than this before they reach the door.
 */
const val CARRIED_STEPS: Int = 20

/**
 * How far an answer may wander from where a put-down device is lying.
 *
 * A hundred and fifty metres. Measured rather than chosen: of 477 answers this
 * device gave over an hour and a half of lying indoors, nine in ten stood
 * within twenty metres of the middle of them, and the ones that did not stood
 * at 115, 313, 885 and 1025 metres — every one of them a place the device was
 * not. The threshold sits above the honest spread with room to spare and
 * below the nearest of the lies.
 *
 * The width of the two answers is added to it where they are wider than that,
 * so a device answering from a courtyard with a hundred metres of doubt on each
 * side is not accused of anything.
 */
const val PUT_DOWN_DRIFT_METRES: Double = 150.0

/**
 * How long the sensors have to stay quiet before the device counts as put down.
 *
 * Two minutes. A device set on a table is still being knocked about for a few
 * seconds after; waiting that out costs nothing, because what the wait delays
 * is the moment this rule starts speaking, and a creep takes longer than two
 * minutes to go anywhere interesting.
 */
val PUT_DOWN_AFTER: Duration = Duration.ofMinutes(2)

/**
 * What speed a put-down device may be told it is travelling at.
 *
 * Three metres a second. The receiver reports a speed of its own alongside
 * every position, and on a device lying still that number is not noisy — it is
 * zero. Of 6,405 answers the three devices gave from within two hundred metres
 * of home, the median was 0.00 m/s and so was the ninetieth percentile; of the
 * 5,381 answers given while the same devices, still lying where they were,
 * were being told they stood a kilometre or more away, the median was 33.00
 * m/s and four in five were above 20. The threshold sits an order of
 * magnitude below the lie and above anything a device on a table has ever been
 * seen to claim.
 *
 * Unlike the ordinary speed rule it needs no second position: the claim is
 * inside a single answer, so the first false fix is caught.
 */
const val PUT_DOWN_SPEED_MPS: Double = 3.0

/**
 * How long a stir is worth keeping after it arrives.
 *
 * Half an hour. What the stirs are asked is whether anything has touched the
 * device since the anchor was set, and an anchor older than this has been
 * replaced by a later one long since — a position is written every few minutes
 * even from a device standing still.
 */
val STIRS_KEPT: Duration = Duration.ofMinutes(30)

/**
 * A moment the device's own sensors noticed it being moved.
 *
 * One kind of event rather than two, because the difference between the
 * accelerometer's significant motion and a step being counted does not matter
 * to anything asked here: both of them mean the device is in somebody's hand or
 * pocket, and neither can be produced by a transmitter.
 */
data class Stir(
    /** When it happened, by the clock the fixes are stamped with. */
    val at: Instant,
    /** The count the step sensor stood at, where it was the step sensor. */
    val steps: Long? = null,
)

/**
 * Where the device was standing when it was last put down.
 *
 * The position is one this device wrote down and believed at the time. A lie
 * can become an anchor — a device put down inside a spoofed area anchors on
 * whatever it was told — and the rule is still worth having then: it catches
 * the answers walking away from that spot, which is what they do.
 */
data class PutDown(
    /** The position believed at the moment the device settled. */
    val at: Fix,
    /** When the sensors fell quiet. */
    val since: Instant,
    /**
     * What the step counter stood at then, where there is one.
     *
     * The counter counts from the last boot and never restarts, so the steps
     * taken since the device was put down are the difference from here. Kept
     * with the anchor rather than worked out from the stirs, because a batch
     * of steps arrives as one reading and a difference taken inside it would
     * be zero.
     */
    val steps: Long? = null,
)

/**
 * What the sensors have to say about the time since the device was put down.
 *
 * [told] separates "the sensors say nothing happened" from "there are no
 * sensors, or the permission was refused": the two read the same and mean
 * opposite things, and only the first of them is evidence.
 */
data class Carrying(
    /** Whether the device has been stirred since it was put down. */
    val stirred: Boolean,
    /** Steps counted since then, where the counter answers. */
    val steps: Int? = null,
    /** Whether any of this is the sensors talking. */
    val told: Boolean = true,
)

/**
 * Whether the device counts as put down, given the last stir and the position
 * it is standing at.
 *
 * Null wherever it does not: the sensors have not spoken, something touched
 * the device within the last [PUT_DOWN_AFTER], or there is no position to
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
 * The anchor, if it still stands as a claim that nobody has touched the device.
 *
 * The one place the terms are written down for both rules that read the anchor,
 * [driftedWhilePutDown] and [hurriedWhilePutDown]: no anchor, a stir, enough
 * steps to have gone anywhere, or a fix taken before the device settled, and
 * there is nothing to judge an answer against.
 */
private fun stillPutDown(putDown: PutDown?, next: Fix, carrying: Carrying): PutDown? {
    if (putDown == null || !carrying.told || carrying.stirred) {
        return null
    }

    if ((carrying.steps ?: 0) >= CARRIED_STEPS) {
        return null
    }

    if (next.takenAt.isBefore(putDown.since)) {
        return null
    }

    return putDown
}

/**
 * Whether an answer has walked away from a device nobody has touched.
 *
 * The anchor holds only while the sensors go on saying nothing: a stir, or
 * enough steps to have gone anywhere, and the rule falls silent rather than
 * accusing the first honest fix after somebody picked the device up. It falls
 * silent as well for a fix taken before the device settled — that answer is
 * about a different part of the day.
 */
fun driftedWhilePutDown(putDown: PutDown?, next: Fix, carrying: Carrying): Boolean {
    val anchor = stillPutDown(putDown, next, carrying) ?: return false

    val doubt = (anchor.at.accuracyMetres ?: 0.0) + (next.accuracyMetres ?: 0.0)

    return distanceMetres(anchor.at, next) > maxOf(PUT_DOWN_DRIFT_METRES, doubt)
}

/**
 * Whether an answer says the device is moving while nobody has touched it.
 *
 * The other reading of the anchor used by [driftedWhilePutDown]: that one asks
 * where the answer puts the device, this one what it says the device is doing.
 * A receiver drawing a false road has to draw a speed along it even while the
 * drawn place is still near the device, so this catches a lie that has not yet
 * gone anywhere. It falls silent on the same terms as the distance rule, and
 * also where the provider reported no speed.
 */
fun hurriedWhilePutDown(putDown: PutDown?, next: Fix, carrying: Carrying): Boolean {
    stillPutDown(putDown, next, carrying) ?: return false

    return (next.reported?.speedMps ?: 0.0) > PUT_DOWN_SPEED_MPS
}

/**
 * What the sensors say about the time since the device was put down, worked out
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

/**
 * How many steps must be walked, without a break, before a position that has
 * not moved looks pinned in place.
 *
 * Four hundred, which is about three hundred metres. The number is deliberately
 * far beyond anything a room allows: what this rule has to survive is a person
 * moving about indoors, where the steps are real and the position honestly does
 * not change, and three hundred metres of walking is not indoors.
 */
const val PINNED_STEPS: Int = 400

/**
 * How long a gap in the counting ends the stretch of walking being measured.
 *
 * Three minutes. The counter is read in batches about once a minute, so a
 * couple of missed readings are the chip staying asleep rather than the person
 * stopping; a gap longer than this is somebody who sat down, and the walk
 * after it is a new one.
 */
val WALKING_BREAK: Duration = Duration.ofMinutes(3)

/**
 * How far a position must move over such a walk to count as following it.
 *
 * Sixty metres. Three hundred metres of walking rarely ends within sixty
 * metres of where it started — and where it does, the walk was a circle around
 * a building, which is exactly the case the width test below throws out
 * anyway.
 */
const val PINNED_MOVE_METRES: Double = 60.0

/**
 * How narrow the answers have to be for their standing still to mean anything.
 *
 * Fifty metres. A place worked out from the towers honestly does not move
 * while its owner walks a few streets, so the rule speaks only for answers
 * measured off the sky and known to within a few tens of metres: one that
 * narrow, standing still through three hundred metres of walking, is about
 * somewhere the device is not.
 */
const val PINNED_WIDTH_METRES: Double = 50.0

/**
 * An unbroken stretch of walking, as the step counter reports it.
 *
 * [since] is when the stretch began, [steps] how many have been counted since.
 * The stretch is what the rule measures against, rather than the whole history
 * of the counter, because a person who walked to the shop an hour ago and has
 * been sitting since has not moved and their position should not move either.
 */
data class Walk(val since: Instant, val steps: Long)

/**
 * The stretch of walking the device is in the middle of, or nothing where it is
 * not walking.
 *
 * A reading that added nothing, or a gap longer than [WALKING_BREAK], starts
 * the stretch again. The count then runs from the reading before the rest and
 * the moment from the reading that ended it: the steps in that batch were taken
 * after the rest, but when exactly is not known any closer than the batch
 * itself, so the rule waits for a position taken after it.
 *
 * A count lower than the one before it is a counter restarted by a reboot; the
 * stretch starts after that reading. A stretch whose last reading is more than
 * [WALKING_BREAK] before [now] is over, since the counter falls silent once the
 * device stops.
 */
fun walkingNow(stirs: List<Stir>, now: Instant): Walk? {
    // The moment of each reading that carries a count, with the count.
    val counted = stirs.mapNotNull { stir ->
        stir.steps?.let { stir.at to it }
    }.sortedBy { it.first }

    if (counted.size < 2 || Duration.between(counted.last().first, now) > WALKING_BREAK) {
        return null
    }

    var (since, base) = counted.first()

    for (index in 1 until counted.size) {
        val (beforeAt, beforeSteps) = counted[index - 1]
        val (nextAt, nextSteps) = counted[index]
        val walked = nextSteps > beforeSteps
        val rested = Duration.between(beforeAt, nextAt) > WALKING_BREAK

        if (nextSteps < beforeSteps) {
            base = nextSteps
            since = nextAt
        } else if (!walked || rested) {
            base = beforeSteps
            since = nextAt
        }
    }

    return Walk(since = since, steps = counted.last().second - base)
}

/**
 * Whether the position has stayed put while the device was carried somewhere.
 *
 * The converse of [driftedWhilePutDown]: there the device was still and the
 * answer moved, here the device moved and the answer is still. A receiver held
 * on a false position reports it steadily however far its owner walks, and the
 * answers agree with one another; only the step counter, which no transmitter
 * reaches, disagrees.
 *
 * [from] is the position the device gave when this stretch of walking began.
 * Both it and [next] have to be narrow claims measured off the sky, for which
 * see [PINNED_WIDTH_METRES]; where the caller cannot say that much about them,
 * it should not call.
 */
fun pinnedWhileWalking(walk: Walk?, from: Fix?, next: Fix, told: Boolean = true): Boolean {
    if (!told || walk == null || from == null) {
        return false
    }

    if (walk.steps < PINNED_STEPS) {
        return false
    }

    if (next.takenAt.isBefore(walk.since)) {
        return false
    }

    val widest = maxOf(from.accuracyMetres ?: 0.0, next.accuracyMetres ?: 0.0)

    if (widest > PINNED_WIDTH_METRES) {
        return false
    }

    return distanceMetres(from, next) < PINNED_MOVE_METRES
}

/**
 * One line about an answer that arrived while the device was being walked
 * somewhere, for the log kept apart.
 *
 * Written for every such answer, not only the accused ones: a log of accusations
 * alone reads the same whether the rule found nothing wrong or was never asked,
 * and [PINNED_STEPS] cannot be measured off silence. The line carries how far
 * the walk had got, how the answer was arrived at and how far it stands from
 * where the walk began, but not where that was.
 *
 * [from] is the answer the walk is measured from; without it this answer is
 * the one the walk will be measured from, or, if it did not come off the sky,
 * one that is not measured at all.
 */
fun walkNote(walk: Walk, foundBy: FoundBy, from: Fix?, next: Fix): String {
    val width = next.accuracyMetres ?: 0.0
    val head = "walking ${walk.steps} steps; an answer by $foundBy, ±%.0f m".format(
        Locale.ROOT,
        width,
    )

    return when {
        foundBy != FoundBy.Satellites -> "$head, not measured against the walk"

        from == null -> "$head, the walk is measured from it"

        else ->
            "$head, %.0f m from where the walk began, ±%.0f m"
                .format(Locale.ROOT, distanceMetres(from, next), from.accuracyMetres ?: 0.0)
    }
}
