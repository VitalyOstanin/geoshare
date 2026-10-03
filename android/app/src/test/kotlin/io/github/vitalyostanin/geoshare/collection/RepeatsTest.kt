package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.math.cos
import kotlin.math.sin

class RepeatsTest {
    private val at = Instant.parse("2026-09-14T17:23:00Z")

    /** Latitude, longitude and seconds since the run began, three at a time. */
    private fun road(flat: DoubleArray): List<Fix> = (flat.indices step PARTS_OF_AN_ANSWER).map {
        Fix(
            latitude = flat[it],
            longitude = flat[it + 1],
            takenAt = at.plusMillis((flat[it + 2] * 1_000).toLong()),
            accuracyMetres = 20.0,
            from = "gps",
        )
    }

    private fun around(radiusMetres: Double, speedMps: Double, answers: Int): List<Fix> {
        val metresPerDegree = Math.toRadians(1.0) * EARTH_RADIUS_METRES
        val shrink = cos(Math.toRadians(61.03))
        val flat = mutableListOf<Double>()

        for (step in 0 until answers) {
            val angle = step * EVERY_SEVEN_SECONDS * speedMps / radiusMetres

            flat += 61.03 + radiusMetres * cos(angle) / metresPerDegree
            flat += 4.57 + radiusMetres * sin(angle) / (metresPerDegree * shrink)
            flat += step * EVERY_SEVEN_SECONDS
        }

        return road(flat.toDoubleArray())
    }

    private fun straight(speedMps: Double, answers: Int): List<Fix> {
        val flat = mutableListOf<Double>()

        for (step in 0 until answers) {
            flat += 61.03 + step * EVERY_SEVEN_SECONDS * speedMps / 111_320.0
            flat += 4.57
            flat += step * EVERY_SEVEN_SECONDS
        }

        return road(flat.toDoubleArray())
    }

    // Thirteen minutes of a transmitter at work, as the phone's own logger
    // wrote them down: thinned to one answer every seven seconds, and moved
    // off the longitudes the family lives at. The route closes and is traced
    // again, and no circle fits it.
    private val falseRoute =
        road(
            doubleArrayOf(
                61.11607, 5.37775, 0.0,
                61.11597, 5.38167, 7.0,
                61.11606, 5.38581, 14.0,
                61.11629, 5.39006, 21.0,
                61.11653, 5.39424, 28.0,
                61.11718, 5.39832, 35.0,
                61.11801, 5.39650, 42.0,
                61.11811, 5.38991, 49.0,
                61.11825, 5.38452, 56.0,
                61.11832, 5.38019, 63.0,
                61.11793, 5.37614, 70.0,
                61.11710, 5.37227, 77.0,
                61.11636, 5.37010, 84.0,
                61.11586, 5.37814, 91.0,
                61.11595, 5.38301, 98.0,
                61.11600, 5.38717, 105.0,
                61.11622, 5.39132, 112.0,
                61.11645, 5.39550, 119.0,
                61.11771, 5.39876, 126.0,
                61.11945, 5.40094, 133.0,
                61.11821, 5.38962, 140.0,
                61.11821, 5.38536, 147.0,
                61.11824, 5.38116, 154.0,
                61.11809, 5.37705, 161.0,
                61.11727, 5.37319, 168.0,
                61.11633, 5.36969, 175.0,
                61.11457, 5.36827, 182.0,
                61.11582, 5.38123, 189.0,
                61.11587, 5.38589, 196.0,
                61.11611, 5.39018, 203.0,
                61.11633, 5.39438, 210.0,
                61.11727, 5.39797, 217.0,
                61.11858, 5.39869, 224.0,
                61.11853, 5.39274, 231.0,
                61.11848, 5.38821, 238.0,
                61.11839, 5.38313, 245.0,
                61.11837, 5.37839, 252.0,
                61.11755, 5.37439, 259.0,
                61.11673, 5.37055, 266.0,
                61.11615, 5.37446, 273.0,
                61.11597, 5.37938, 280.0,
                61.11597, 5.38392, 287.0,
                61.11612, 5.38853, 294.0,
                61.11635, 5.39315, 301.0,
                61.11679, 5.39725, 308.0,
                61.11832, 5.39910, 315.0,
                61.11835, 5.39755, 322.0,
                61.11842, 5.38843, 329.0,
                61.11840, 5.38396, 336.0,
                61.11838, 5.37939, 343.0,
                61.11789, 5.37530, 350.0,
                61.11704, 5.37139, 357.0,
                61.11646, 5.37110, 364.0,
                61.11587, 5.37843, 371.0,
                61.11590, 5.38343, 378.0,
                61.11599, 5.38780, 385.0,
                61.11624, 5.39205, 392.0,
                61.11646, 5.39622, 399.0,
                61.11794, 5.39919, 406.0,
                61.11840, 5.39776, 413.0,
                61.11838, 5.39767, 420.0,
                61.11841, 5.38466, 427.0,
                61.11833, 5.38028, 434.0,
                61.11792, 5.37621, 441.0,
                61.11706, 5.37234, 448.0,
                61.11665, 5.37097, 455.0,
                61.11576, 5.37761, 462.0,
                61.11585, 5.38252, 469.0,
                61.11590, 5.38682, 476.0,
                61.11613, 5.39110, 483.0,
                61.11635, 5.39532, 490.0,
                61.11758, 5.39870, 497.0,
                61.11774, 5.39372, 504.0,
                61.11811, 5.38965, 511.0,
                61.11807, 5.38544, 518.0,
                61.11812, 5.38124, 525.0,
                61.11799, 5.37713, 532.0,
                61.11718, 5.37328, 539.0,
                61.11653, 5.37039, 546.0,
                61.11592, 5.37543, 553.0,
                61.11580, 5.38055, 560.0,
                61.11597, 5.38589, 567.0,
                61.11615, 5.39017, 574.0,
                61.11636, 5.39437, 581.0,
                61.11727, 5.39792, 588.0,
                61.11814, 5.39920, 595.0,
                61.11833, 5.39198, 602.0,
                61.11824, 5.38673, 609.0,
                61.11828, 5.38245, 616.0,
                61.11832, 5.37823, 623.0,
                61.11753, 5.37433, 630.0,
                61.11674, 5.37055, 637.0,
                61.11591, 5.37307, 644.0,
                61.11579, 5.37822, 651.0,
                61.11579, 5.38267, 658.0,
                61.11596, 5.38703, 665.0,
                61.11619, 5.39138, 672.0,
                61.11650, 5.39560, 679.0,
                61.11798, 5.39855, 686.0,
                61.11822, 5.39401, 693.0,
                61.11820, 5.38831, 700.0,
                61.11825, 5.38361, 707.0,
                61.11830, 5.37932, 714.0,
                61.11776, 5.37532, 721.0,
                61.11690, 5.37146, 728.0,
                61.11619, 5.37161, 735.0,
                61.11586, 5.37749, 742.0,
                61.11598, 5.38371, 749.0,
                61.11606, 5.38796, 756.0,
                61.11627, 5.39219, 763.0,
            ),
        )

    /**
     * [run] as a phone whose wall clock was set a minute and a half forwards
     * halfway through it would carry it, with the count since boot running on.
     */
    private fun clockSetHalfway(run: List<Fix>): List<Fix> = run.mapIndexed { index, fix ->
        val awake = Duration.between(at, fix.takenAt)
        val shifted = if (index < run.size / 2) fix.takenAt else fix.takenAt.plusSeconds(90)

        fix.copy(takenAt = shifted, sinceBoot = awake)
    }

    @Test
    fun `a route traced again is caught`() {
        val round = routeRepeated(falseRoute)

        assertNotNull(round)
        assertTrue("a round takes minutes", round!!.period.seconds in 60..600)
        assertTrue("and lands on itself", round.apartMetres < REPEAT_APART_METRES)
        assertTrue("with the whole run lining up", round.pairs >= REPEAT_PAIRS_NEEDED)
        assertTrue("across a route worth the name", round.reachMetres > 1_000.0)
    }

    @Test
    fun `a wall clock set halfway through does not change the round`() {
        val round = routeRepeated(clockSetHalfway(falseRoute))

        assertEquals(routeRepeated(falseRoute)?.period, round?.period)
    }

    // The shape changed between the two days this was written from: the
    // circle rule catches the first and nothing of the second, and this one
    // catches both, because both close.
    @Test
    fun `the route is caught even where no circle fits it`() {
        assertFalse(goingInCircles(falseRoute))
        assertTrue(goingRoundAgain(falseRoute))
    }

    @Test
    fun `the rule stands on its own, with nothing to compare against`() {
        val landed = falseRoute.last()

        assertNull(whyAJump(falseRoute[falseRoute.size - 2], landed))
        assertEquals(
            JumpReason.Repeating,
            whyAJump(falseRoute[falseRoute.size - 2], landed, lately = falseRoute),
        )
    }

    // Answers jittering around a standing phone line up across every period
    // that fits, and their wandering adds up to a kilometre of apparent road.
    // What tells them from a route is that they go nowhere.
    @Test
    fun `a standing phone is not a route`() {
        val jitter = mutableListOf<Double>()
        var wander = 0

        for (step in 0 until 170) {
            wander = (wander * 37 + 11) % 97

            jitter += 61.03 + (wander - 48) * 0.0000040
            jitter += 4.57 + (48 - wander) * 0.0000080
            jitter += step * EVERY_SEVEN_SECONDS
        }

        assertNull(routeRepeated(road(jitter.toDoubleArray())))
    }

    @Test
    fun `a straight road is not a route traced again`() {
        assertNull(routeRepeated(straight(speedMps = 20.0, answers = 170)))
    }

    // Four hundred metres of running track, gone round again and again on a
    // fixed period, which is exactly what this rule looks for — and is
    // somebody really going round it. What keeps it out is how far it reaches.
    @Test
    fun `a running track is gone round for real`() {
        val track = around(radiusMetres = 63.7, speedMps = 4.0, answers = 170)

        assertNull(routeRepeated(track))
    }

    @Test
    fun `a route has to reach across more than a park`() {
        val narrowed = RepeatLimits(reachMetres = 100.0)

        assertNotNull(routeRepeated(around(63.7, 4.0, 170), narrowed))
    }

    @Test
    fun `a walk out and back is not a route traced again`() {
        val there = straight(speedMps = 15.0, answers = 86)
        val back =
            there.reversed().mapIndexed { step, fix ->
                fix.copy(takenAt = at.plusSeconds((86 + step) * EVERY_SEVEN_SECONDS.toLong()))
            }

        assertNull(routeRepeated(there + back))
    }

    @Test
    fun `a run too short to hold two rounds says nothing`() {
        assertNull(routeRepeated(falseRoute.take(REPEAT_PAIRS_NEEDED * 2 - 1)))
    }

    @Test
    fun `a group narrows what counts as going round again`() {
        val narrowed = RepeatLimits(apartMetres = 5.0)

        assertNotNull(routeRepeated(falseRoute))
        assertNull(routeRepeated(falseRoute, narrowed))
    }

    // The run is kept by age rather than by count, because what the rules ask
    // of it is about time.
    @Test
    fun `the run reaches twenty minutes back and no further`() {
        var run = emptyList<Fix>()

        for (step in 0 until 400) {
            run = run.andThen(falseRoute.first().copy(takenAt = at.plusSeconds(step * 10L)))
        }

        val oldest = Duration.between(run.first().takenAt, run.last().takenAt)

        assertTrue("nothing older than twenty minutes", oldest <= ANSWERS_REMEMBERED)
        assertTrue("and no more than two hundred answers", run.size <= ANSWERS_KEPT)
    }

    private companion object {
        const val PARTS_OF_AN_ANSWER = 3
        const val EVERY_SEVEN_SECONDS = 7.0
    }
}
