package io.github.vitalyostanin.geoshare.collection

import java.time.Duration
import kotlin.math.abs

// Catching the transmitter by the route it goes round again.
//
// The circle was the first shape seen, and it turned out to be one shape out
// of several: the same neighbourhood produced, a day later, a closed route
// with corners in it, which no circle fits. What did not change is that the
// route closes — the position came back to where it had been, to within a
// hundred metres, every hundred and eighty-eight seconds, and went round
// again.
//
// That is the thing worth catching, because it is the thing a person cannot
// do. A device carried by somebody may go round a block twice; it does not
// retrace six kilometres of road to within a hundred metres on a fixed
// period, and it does not do it for an hour.
//
// This asks nothing about the shape, so a spoofer changing the figure it
// draws changes nothing here.

/** How long a route has to take before going round it again means anything. */
val REPEAT_SHORTEST: Duration = Duration.ofSeconds(60)

/**
 * The longest period looked for.
 *
 * Bounded by what the device remembers rather than by anything about
 * transmitters: two rounds have to fit inside the run of answers for a
 * repeat to be visible at all.
 */
val REPEAT_LONGEST: Duration = Duration.ofMinutes(10)

/**
 * How many pairs of answers have to line up across the period.
 *
 * Eight. Two answers a period apart happen wherever somebody walks back the
 * way they came; eight of them, spread along the whole run, are a route being
 * traced rather than a place being passed twice.
 */
const val REPEAT_PAIRS_NEEDED: Int = 8

/**
 * How far apart the answers of such a pair may sit, in metres, at the middle
 * of the run.
 *
 * A hundred and fifty. The false route observed held fifty-six metres at the
 * middle and ninety-seven at its worst, and a person retracing a walk holds
 * nothing like it.
 */
const val REPEAT_APART_METRES: Double = 150.0

/** How close two answers have to be to suggest a period between them. */
const val REPEAT_CLOSE_METRES: Double = 100.0

/**
 * How far across the route has to reach, in metres: the distance between its
 * two furthest answers.
 *
 * Five hundred, and it is what keeps a standing device and a running track out
 * of this. Both were tried against the rule and both were caught by it
 * without this: answers jittering around one place line up across every
 * period that fits, and their wandering adds up to a kilometre of apparent
 * road, while four hundred metres of running track is a lap somebody really
 * is going round. What tells those from a false route is not how much road
 * was covered but how far the route goes — a device jittering reaches sixty
 * metres, the track a hundred and twenty-seven, the false route two and a
 * half kilometres.
 */
const val REPEAT_REACH_METRES: Double = 500.0

/** How far off the period an answer may fall and still count as a pair. */
val REPEAT_NEAR_ENOUGH: Duration = Duration.ofSeconds(3)

/** How many candidate periods are weighed, the closest pairs first. */
const val REPEAT_PERIODS_TRIED: Int = 64

/** A route the answers turned out to be going round again. */
data class Repeated(
    /** How long one round takes. */
    val period: Duration,
    /** How far apart the answers of a round sit from the round before, at the middle. */
    val apartMetres: Double,
    /** How many pairs lined up across the period. */
    val pairs: Int,
    /** How far apart the two furthest answers of one round are, in metres. */
    val reachMetres: Double,
)

/** What counts as going round a route again, as this group has settled it. */
data class RepeatLimits(
    val shortest: Duration = REPEAT_SHORTEST,
    val longest: Duration = REPEAT_LONGEST,
    val pairsNeeded: Int = REPEAT_PAIRS_NEEDED,
    val apartMetres: Double = REPEAT_APART_METRES,
    val reachMetres: Double = REPEAT_REACH_METRES,
)

/**
 * The route the answers are going round again, or nothing where they are not.
 *
 * [recentAnswers] is every answer the satellites gave, newest last, whether or not it
 * was written out — the answers a rule has already refused are the ones that
 * draw the route, and they are refused for as long as the anchor still stands
 * where the device really is.
 */
fun routeRepeated(recentAnswers: List<Fix>, limits: RepeatLimits = RepeatLimits()): Repeated? {
    if (recentAnswers.size < limits.pairsNeeded * 2) {
        return null
    }

    // The moments are worked out once for every period tried, and kept as
    // plain nanoseconds: this runs on each answer, over up to two hundred
    // of them and sixty-four periods.
    val at = Moments(sinceFirst(recentAnswers).map { it.toNanos() }.toLongArray())
    var best: Repeated? = null

    for (period in periodsWorthTrying(recentAnswers, limits)) {
        val lined = linedUpAcross(recentAnswers, at, period, limits) ?: continue

        if (best == null || lined.apartMetres < best.apartMetres) {
            best = lined
        }
    }

    return best
}

/** Whether the answers are going round a route again rather than going anywhere. */
fun goingRoundAgain(recentAnswers: List<Fix>, limits: RepeatLimits = RepeatLimits()): Boolean =
    routeRepeated(recentAnswers, limits) != null

/**
 * The periods worth weighing: those between two answers that fell in the same
 * place.
 *
 * A route that closes puts two answers within a few dozen metres of each
 * other, and the time between them is how long the round took. Trying every
 * second between a minute and ten instead would be five hundred and forty
 * passes over the run for nothing.
 */
private fun periodsWorthTrying(recentAnswers: List<Fix>, limits: RepeatLimits): List<Duration> {
    val found = mutableListOf<Pair<Double, Duration>>()

    for (later in recentAnswers.indices) {
        for (earlier in 0 until later) {
            val gap = apart(recentAnswers[earlier], recentAnswers[later])

            if (gap < limits.shortest || gap > limits.longest) {
                continue
            }

            val metres = distanceMetres(recentAnswers[earlier], recentAnswers[later])

            if (metres <= REPEAT_CLOSE_METRES) {
                found += metres to gap
            }
        }
    }

    return found.sortedBy { it.first }.take(REPEAT_PERIODS_TRIED).map { it.second }
}

/**
 * How well the run lines up with itself one period back, or nothing where it
 * does not line up at all.
 */
private fun linedUpAcross(
    recentAnswers: List<Fix>,
    at: Moments,
    period: Duration,
    limits: RepeatLimits,
): Repeated? {
    val periodNanos = period.toNanos()
    val nearEnough = REPEAT_NEAR_ENOUGH.toNanos()
    val apart = mutableListOf<Double>()

    for (answer in recentAnswers.indices) {
        val then = at.nanos[answer] - periodNanos
        val before = at.nearestTo(then) ?: continue

        if (abs(at.nanos[before] - then) > nearEnough) {
            continue
        }

        apart += distanceMetres(recentAnswers[before], recentAnswers[answer])
    }

    if (apart.size < limits.pairsNeeded) {
        return null
    }

    val middle = apart.sorted()[apart.size / 2]

    if (middle > limits.apartMetres) {
        return null
    }

    val reach = reachOfTheRound(recentAnswers, at, periodNanos)

    if (reach < limits.reachMetres) {
        return null
    }

    return Repeated(
        period = period,
        apartMetres = middle,
        pairs = apart.size,
        reachMetres = reach,
    )
}

/**
 * The moments of a run, in nanoseconds since its first answer.
 *
 * The answers arrive newest last, so the moments normally only grow, and the
 * nearest one is then found by halving rather than by walking all of them. A
 * run that goes back somewhere — a clock set mid-run with no boot time to go
 * by — is walked the slow way, which gives the same answer.
 */
private class Moments(val nanos: LongArray) {
    private val ordered = (1 until nanos.size).all { nanos[it - 1] <= nanos[it] }

    /** Which moment is closest to [moment], the earliest on a tie, or nothing where there are none. */
    fun nearestTo(moment: Long): Int? {
        if (nanos.isEmpty()) {
            return null
        }

        if (!ordered) {
            return nanos.indices.minBy { abs(nanos[it] - moment) }
        }

        val found = nanos.binarySearch(moment)

        if (found >= 0) {
            return firstOf(found)
        }

        val after = -found - 1

        return when {
            after == 0 -> 0
            after == nanos.size -> firstOf(nanos.size - 1)
            moment - nanos[after - 1] <= nanos[after] - moment -> firstOf(after - 1)
            else -> after
        }
    }

    /** The earliest index holding the same moment as [index]. */
    private fun firstOf(index: Int): Int {
        var first = index

        while (first > 0 && nanos[first - 1] == nanos[index]) {
            first -= 1
        }

        return first
    }
}

/**
 * How far across the last round reaches: the distance between its two
 * furthest answers.
 *
 * Not the road covered, which a standing device produces by jittering, and not
 * the radius, which asks the route to be a circle.
 */
private fun reachOfTheRound(recentAnswers: List<Fix>, at: Moments, periodNanos: Long): Double {
    val since = at.nanos.last() - periodNanos
    val round = recentAnswers.filterIndexed { index, _ -> at.nanos[index] >= since }
    var furthest = 0.0

    for (one in round.indices) {
        for (other in one + 1 until round.size) {
            furthest = maxOf(furthest, distanceMetres(round[one], round[other]))
        }
    }

    return furthest
}
