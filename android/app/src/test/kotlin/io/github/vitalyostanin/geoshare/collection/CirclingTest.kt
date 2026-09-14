package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class CirclingTest {
    private val at = Instant.parse("2026-09-13T16:29:00Z")

    private fun road(places: List<Pair<Double, Double>>, everySeconds: Long = 7): List<Fix> =
        places.mapIndexed { step, place ->
            Fix(
                latitude = place.first,
                longitude = place.second,
                takenAt = at.plusSeconds(step * everySeconds),
                accuracyMetres = 30.0,
                from = "gps",
            )
        }

    // Ten answers of the false road, as a phone wrote them down: a lap of one
    // thousand two hundred metres every three and a half minutes, held for the
    // better part of an hour. Every pair of them passes the speed rule.
    private val falseRoad =
        road(
            listOf(
                61.0449412 to 4.5452172,
                61.0445808 to 4.5409185,
                61.0434411 to 4.5373280,
                61.0421806 to 4.5335657,
                61.0405947 to 4.5305065,
                61.0386342 to 4.5283568,
                61.0365063 to 4.5270747,
                61.0342928 to 4.5266344,
                61.0322262 to 4.5268668,
                61.0301633 to 4.5280736,
            ),
        )

    private fun around(radiusMetres: Double, speedMps: Double, turns: Int = 1): List<Fix> {
        val centre = 61.03 to 4.57
        val metresPerDegree = Math.toRadians(1.0) * EARTH_RADIUS_METRES
        val shrink = cos(Math.toRadians(centre.first))

        return road(
            (0 until 10).map { step ->
                val angle = turns * step * 7.0 * speedMps / radiusMetres

                (centre.first + radiusMetres * cos(angle) / metresPerDegree) to
                    (centre.second + radiusMetres * sin(angle) / (metresPerDegree * shrink))
            },
        )
    }

    @Test
    fun `a circle held at a steady speed is caught`() {
        val circle = circleDrawn(falseRoad)

        assertNotNull(circle)
        assertEquals(1_209.0, circle!!.radiusMetres, 20.0)
        assertEquals(34.7, circle.speedMps, 1.0)
        assertTrue("a quarter of the lap", circle.turnedDegrees > 90.0)
        assertTrue("and it hugs the circle", circle.offCircleMetres < 10.0)
    }

    @Test
    fun `a straight road is not a circle`() {
        val straight = road((0 until 10).map { (61.0300 + it * 0.00126) to 4.5700 })

        assertNull(circleDrawn(straight))
    }

    @Test
    fun `a standing phone is not a circle`() {
        val jitter =
            road(
                listOf(
                    61.030000 to 4.570000, 61.030090 to 4.570150, 61.029950 to 4.569800,
                    61.030030 to 4.570240, 61.029900 to 4.569900, 61.030110 to 4.570100,
                    61.029960 to 4.570300, 61.030070 to 4.569750, 61.029930 to 4.570050,
                    61.030100 to 4.569950,
                ),
            )

        assertFalse(goingInCircles(jitter))
    }

    @Test
    fun `a road that bends both ways is not a circle`() {
        val bendingBack =
            around(radiusMetres = 400.0, speedMps = 20.0).mapIndexed { step, fix ->
                if (step < 5) fix else fix.copy(latitude = 61.06 - (fix.latitude - 61.03))
            }

        assertNull(circleDrawn(bendingBack))
    }

    @Test
    fun `a roundabout is too tight to count`() {
        assertNull(circleDrawn(around(radiusMetres = 25.0, speedMps = 8.0)))
    }

    @Test
    fun `a walk round a park is too slow to count`() {
        assertNull(circleDrawn(around(radiusMetres = 300.0, speedMps = 1.4)))
    }

    @Test
    fun `answers spread over too long say nothing about the shape`() {
        val sparse =
            road(falseRoad.map { it.latitude to it.longitude }, everySeconds = 60)

        assertNull(circleDrawn(sparse))
    }

    @Test
    fun `fewer answers than the run are not judged`() {
        assertNull(circleDrawn(falseRoad.dropLast(1)))
    }

    @Test
    fun `only the last answers are looked at`() {
        val straight = road((0 until 10).map { (61.0300 + it * 0.00126) to 4.5700 })

        assertNull(circleDrawn(straight + falseRoad.take(3)))
        assertNotNull(circleDrawn(straight.take(3) + falseRoad))
    }

    @Test
    fun `the run keeps the answers it is given, newest last`() {
        var run = emptyList<Fix>()

        repeat(CIRCLE_ANSWERS * 3) { step ->
            run = run.andThen(falseRoad[step % falseRoad.size])
        }

        assertEquals(CIRCLE_ANSWERS * 2, run.size)
        assertEquals(falseRoad[(CIRCLE_ANSWERS * 3 - 1) % falseRoad.size], run.last())
    }

    @Test
    fun `a full turn at a steady speed is caught wherever it is drawn`() {
        val circle = circleDrawn(around(radiusMetres = 900.0, speedMps = 20.0))

        assertNotNull(circle)
        assertEquals(900.0, circle!!.radiusMetres, 10.0)
    }

    @Test
    fun `going round in circles is what the phone is told`() {
        assertTrue(goingInCircles(falseRoad))
        assertFalse(goingInCircles(falseRoad.take(4)))
    }

    @Test
    fun `the circle is a jump of its own, with nothing to compare against`() {
        val onTheCircle = falseRoad.last()

        assertNull(whyAJump(falseRoad[8], onTheCircle))
        assertEquals(
            JumpReason.Circling,
            whyAJump(falseRoad[8], onTheCircle, lately = falseRoad),
        )
    }

    @Test
    fun `the turn is counted the short way round between answers`() {
        // Nine steps of sixteen degrees: a hundred and forty-four of the lap,
        // and the sign says only which way round it went.
        val circle = circleDrawn(around(radiusMetres = 1_000.0, speedMps = 40.0))

        assertNotNull(circle)
        assertEquals(144.0, abs(circle!!.turnedDegrees), 2.0)
    }

    @Test
    fun `answers carrying one time say nothing about speed`() {
        val atOnce = falseRoad.map { it.copy(takenAt = at) }

        assertNull(circleDrawn(atOnce))
    }

    // What a group buys by naming the shape of its own transmitter: the same
    // circle is recognised in fewer answers, because the size and the speed
    // are known rather than guessed.
    @Test
    fun `a group narrows what counts as going round in circles`() {
        val narrowed =
            CircleLimits(
                answers = 6,
                within = Duration.ofMinutes(2),
                tightestMetres = 900.0,
                widestMetres = 1_500.0,
                slowestMps = 25.0,
                turnedDegrees = 40.0,
            )

        assertNull(circleDrawn(falseRoad.take(6)))
        assertNotNull(circleDrawn(falseRoad.take(6), narrowed))
    }

    @Test
    fun `a circle outside the size a group named is left alone`() {
        val narrowed = CircleLimits(tightestMetres = 2_000.0)

        assertNotNull(circleDrawn(falseRoad))
        assertNull(circleDrawn(falseRoad, narrowed))
    }

    @Test
    fun `limits have to make sense to be weighed against`() {
        assertTrue(circleLimitsStand(CircleLimits()))
        assertFalse(circleLimitsStand(CircleLimits(answers = 3)))
        assertFalse(circleLimitsStand(CircleLimits(within = Duration.ZERO)))
        assertFalse(circleLimitsStand(CircleLimits(roundness = 0.0)))
        assertFalse(circleLimitsStand(CircleLimits(widestMetres = 50.0)))
        assertFalse(circleLimitsStand(CircleLimits(tightestMetres = 0.0)))
        assertFalse(circleLimitsStand(CircleLimits(steadiness = 0.0)))
        assertFalse(circleLimitsStand(CircleLimits(turnedDegrees = 0.0)))
        assertFalse(circleLimitsStand(CircleLimits(slowestMps = -1.0)))
    }

    @Test
    fun `a quarter turn is not enough to be a lap`() {
        val quarter = around(radiusMetres = 2_000.0, speedMps = 20.0)
        val turned = circleDrawn(quarter)

        // Ten answers seven seconds apart at twenty metres a second go round a
        // two kilometre circle by thirty-six degrees: a bend of a road.
        assertNull(turned)
    }
}
