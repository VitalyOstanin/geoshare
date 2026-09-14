package io.github.vitalyostanin.geoshare.collection

import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sqrt

// Catching the jammer by the shape of the road it draws.
//
// Every other rule here asks whether one answer can be reconciled with
// another: how far apart they are, how fast the phone would have had to
// travel, what the other radios say. A transmitter that moves its false
// position slowly enough satisfies all of them — two hundred and fifty metres
// every seven seconds is a car on a motorway, and nothing in a pair of fixes
// says otherwise.
//
// What says otherwise is the whole set of them at once. The false position
// observed here went round a circle of one thousand two hundred metres at a
// steady thirty-five metres a second, a lap every three and a half minutes,
// for the better part of an hour. A phone carried by a person does not draw
// that: a car on a roundabout leaves it within a lap, a ring road is wider
// than any circle worth fitting, and a walk holds neither the radius nor the
// speed for ten answers running.

/**
 * How many answers in a row are looked at.
 *
 * Ten, because the question asked of them is whether they lie on one circle,
 * and three points lie on a circle always. Ten of them at the seven seconds
 * the fast mode answers in is a minute of road — long enough that fitting a
 * circle to it means something, short enough that the phone is told before
 * the next lap.
 */
const val CIRCLE_ANSWERS: Int = 10

/**
 * How long those answers may be spread over before they stop being a road.
 *
 * Answers minutes apart say nothing about the shape between them: any ten
 * places can be strung onto a circle if the phone had a quarter of an hour
 * between each of them to get there.
 */
val CIRCLE_WITHIN: Duration = Duration.ofMinutes(5)

/**
 * How far the answers may sit off the circle, as a share of its radius.
 *
 * Three per cent — thirty-six metres on the circle observed, against the two
 * to eight metres it actually held. The allowance is what leaves room for a
 * real arc to be recognised too; what it does not leave room for is a road
 * with corners in it.
 */
const val CIRCLE_ROUNDNESS: Double = 0.03

/**
 * The widest circle worth calling a circle.
 *
 * A straight line is a circle of infinite radius, and a motorway fitted with
 * one comes out at tens of kilometres. Five is past any roundabout and short
 * of every ring road, so what is left inside it is a phone going round
 * something small enough to be gone round.
 */
const val CIRCLE_WIDEST_METRES: Double = 5_000.0

/**
 * The tightest circle worth calling a circle.
 *
 * A hundred metres, which a roundabout is inside of: a car on one draws a
 * circle of twenty to fifty metres and is off it within a lap, and answers
 * jittering around a standing phone fit a tiny circle by accident.
 */
const val CIRCLE_TIGHTEST_METRES: Double = 100.0

/**
 * How much the speed may vary across the answers, as a share of its average.
 *
 * Fifteen per cent, against the one to three per cent the false road held. A
 * driver holding a speed that steadily through ten answers is holding it on
 * cruise control on a straight road, and a straight road is not a circle.
 */
const val CIRCLE_STEADINESS: Double = 0.15

/**
 * How far round the circle the answers have to have gone, in degrees.
 *
 * Sixty, and all of them the same way round. Less than that is an arc, and
 * every bend in every road is an arc.
 */
const val CIRCLE_TURNED_DEGREES: Double = 60.0

/**
 * The slowest the answers may move and still be a road at all.
 *
 * Three metres a second is a run. Below it the phone is standing or walking,
 * the movement between answers is mostly the doubt in them, and a circle
 * fitted to that is a circle fitted to noise.
 */
const val CIRCLE_SLOWEST_MPS: Double = 3.0

/** The circle a run of answers turned out to be drawing. */
data class Circle(
    /** How far the answers sat from its centre, in metres. */
    val radiusMetres: Double,
    /** How far off the circle they sat, in metres, taken together. */
    val offCircleMetres: Double,
    /** How fast the position moved along it, in metres a second. */
    val speedMps: Double,
    /** How far round it the answers went, in degrees, always one way. */
    val turnedDegrees: Double,
)

/**
 * The circle the last answers are drawing, or nothing when they are not.
 *
 * [lately] is every answer the satellites gave, newest last, whether or not it
 * was written out. That it is not filtered by the other rules is the point:
 * while the speed rule is still refusing the false road, the answers it
 * refuses are the ones that show the shape, and a filtered run would show the
 * phone the shape only once the anchor had already been lost to it.
 */
fun circleDrawn(lately: List<Fix>): Circle? {
    if (lately.size < CIRCLE_ANSWERS) {
        return null
    }

    val run = lately.takeLast(CIRCLE_ANSWERS)

    if (Duration.between(run.first().takenAt, run.last().takenAt) > CIRCLE_WITHIN) {
        return null
    }

    val spots = spotsOf(run)
    val centre = centreOf(spots) ?: return null

    if (centre.radius > CIRCLE_WIDEST_METRES || centre.radius < CIRCLE_TIGHTEST_METRES) {
        return null
    }

    val offCircle = offCircle(spots, centre)

    if (offCircle > centre.radius * CIRCLE_ROUNDNESS) {
        return null
    }

    val speed = speedAlong(spots) ?: return null

    if (speed.average < CIRCLE_SLOWEST_MPS || speed.spread > speed.average * CIRCLE_STEADINESS) {
        return null
    }

    val turned = turnedAround(spots, centre) ?: return null

    if (abs(turned) < CIRCLE_TURNED_DEGREES) {
        return null
    }

    return Circle(
        radiusMetres = centre.radius,
        offCircleMetres = offCircle,
        speedMps = speed.average,
        turnedDegrees = turned,
    )
}

/** Whether the answers are going round in a circle rather than anywhere. */
fun goingInCircles(lately: List<Fix>): Boolean = circleDrawn(lately) != null

/**
 * The run with one more answer in it, the oldest dropped.
 *
 * Twice the length that is looked at is kept, so that the shape is still
 * there to be seen after a few answers have arrived from somewhere else.
 */
fun List<Fix>.andThen(fix: Fix): List<Fix> = (this + fix).takeLast(CIRCLE_ANSWERS * 2)

/** A place in metres east and north of where the run began. */
private data class Spot(val east: Double, val north: Double, val at: Instant)

/** The centre a run of places turns out to be going round, and how far off it. */
private data class Centre(val east: Double, val north: Double, val radius: Double)

/** How fast the position moved, and how much that varied. */
private data class Speed(val average: Double, val spread: Double)

/**
 * The run as plain metres.
 *
 * A flat sheet laid on the earth at the first answer. Over the few kilometres
 * a run of answers covers, the error of doing so is centimetres, and what is
 * being measured here is the shape rather than the place.
 */
private fun spotsOf(run: List<Fix>): List<Spot> {
    val first = run.first()
    val shrink = cos(Math.toRadians(first.latitude))
    val perDegree = Math.toRadians(1.0) * EARTH_RADIUS_METRES

    return run.map {
        Spot(
            east = (it.longitude - first.longitude) * perDegree * shrink,
            north = (it.latitude - first.latitude) * perDegree,
            at = it.takenAt,
        )
    }
}

/**
 * The circle that fits the places best, or nothing where none does.
 *
 * The plain least-squares fit: the circle is written as
 * `x² + y² + Dx + Ey + F = 0`, which is linear in its three unknowns, and the
 * three normal equations are solved by elimination. It pulls slightly towards
 * the inside on a short arc, which costs nothing here — the arc is asked to be
 * a long one, and the fit is asked only whether the places sit on it.
 */
private fun centreOf(spots: List<Spot>): Centre? {
    var sumEast = 0.0
    var sumNorth = 0.0
    var sumEastEast = 0.0
    var sumNorthNorth = 0.0
    var sumEastNorth = 0.0
    var sumFar = 0.0
    var sumEastFar = 0.0
    var sumNorthFar = 0.0

    for (spot in spots) {
        val far = spot.east * spot.east + spot.north * spot.north

        sumEast += spot.east
        sumNorth += spot.north
        sumEastEast += spot.east * spot.east
        sumNorthNorth += spot.north * spot.north
        sumEastNorth += spot.east * spot.north
        sumFar += far
        sumEastFar += spot.east * far
        sumNorthFar += spot.north * far
    }

    val solved =
        solve(
            arrayOf(
                doubleArrayOf(sumEastEast, sumEastNorth, sumEast, -sumEastFar),
                doubleArrayOf(sumEastNorth, sumNorthNorth, sumNorth, -sumNorthFar),
                doubleArrayOf(sumEast, sumNorth, spots.size.toDouble(), -sumFar),
            ),
        ) ?: return null

    val east = -solved[0] / 2
    val north = -solved[1] / 2
    val squared = east * east + north * north - solved[2]

    if (squared <= 0.0) {
        return null
    }

    return Centre(east = east, north = north, radius = sqrt(squared))
}

/** Three equations in three unknowns, or nothing where they do not decide. */
private fun solve(rows: Array<DoubleArray>): DoubleArray? {
    for (step in rows.indices) {
        val pivot = (step until rows.size).maxByOrNull { abs(rows[it][step]) } ?: return null

        rows[step] = rows[pivot].also { rows[pivot] = rows[step] }

        if (abs(rows[step][step]) < TOO_FLAT) {
            return null
        }

        for (other in rows.indices) {
            if (other == step) {
                continue
            }

            val share = rows[other][step] / rows[step][step]

            for (column in step..rows.size) {
                rows[other][column] -= share * rows[step][column]
            }
        }
    }

    return DoubleArray(rows.size) { rows[it][rows.size] / rows[it][it] }
}

/** How far the places sit off the circle, taken together. */
private fun offCircle(spots: List<Spot>, centre: Centre): Double {
    val squared =
        spots.sumOf {
            val off = hypot(it.east - centre.east, it.north - centre.north) - centre.radius

            off * off
        }

    return sqrt(squared / spots.size)
}

/** How fast the position moved between the places, and how steadily. */
private fun speedAlong(spots: List<Spot>): Speed? {
    val speeds = mutableListOf<Double>()

    for (step in 1 until spots.size) {
        val was = spots[step - 1]
        val now = spots[step]
        val seconds = Duration.between(was.at, now.at).toMillis() / A_SECOND

        if (seconds <= 0.0) {
            return null
        }

        speeds += hypot(now.east - was.east, now.north - was.north) / seconds
    }

    if (speeds.isEmpty()) {
        return null
    }

    val average = speeds.average()
    val spread = sqrt(speeds.sumOf { (it - average) * (it - average) } / speeds.size)

    return Speed(average = average, spread = spread)
}

/**
 * How far round the centre the places went, in degrees, or nothing where they
 * turned back on themselves.
 *
 * Turning back is what tells an arc of a road from a lap: a car that goes one
 * way round a bend and then the other way round the next has driven a road,
 * whatever the two arcs fit.
 */
private fun turnedAround(spots: List<Spot>, centre: Centre): Double? {
    var turned = 0.0
    var forwards = false
    var backwards = false

    for (step in 1 until spots.size) {
        val was = spots[step - 1]
        val now = spots[step]
        val from = atan2(was.north - centre.north, was.east - centre.east)
        val to = atan2(now.north - centre.north, now.east - centre.east)
        val by = wrapped(to - from)

        turned += by
        forwards = forwards || by > 0.0
        backwards = backwards || by < 0.0
    }

    if (forwards && backwards) {
        return null
    }

    return Math.toDegrees(turned)
}

/** An angle brought back inside half a turn either way. */
private fun wrapped(angle: Double): Double {
    var brought = angle

    while (brought > Math.PI) {
        brought -= 2 * Math.PI
    }

    while (brought < -Math.PI) {
        brought += 2 * Math.PI
    }

    return brought
}

private const val TOO_FLAT = 1e-9
private const val A_SECOND = 1_000.0
