package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CarriedTest {
    private val noon: Instant = Instant.parse("2026-09-15T12:00:00Z")

    // Where the phone actually is, with the width an honest answer indoors
    // carries.
    private val home = fix(61.03268, 4.40300, noon, width = 20.0)

    private val standing = PutDown(at = home, since = noon, steps = 1_000)

    private val untouched = Carrying(stirred = false, steps = 0)

    @Test
    fun `an answer a kilometre from a phone nobody touched has walked away`() {
        // The one this phone actually gave at 11:00 on 15.09.2026: four
        // satellites, a quarter of a kilometre of doubt, and a place a
        // kilometre from the flat it was lying in.
        val elsewhere = fix(61.04170, 4.40300, noon.plusSeconds(60), width = 246.0)

        assertTrue(driftedWhilePutDown(standing, elsewhere, untouched))
    }

    @Test
    fun `an answer wandering inside the doubt of a put-down phone is left alone`() {
        val nearby = fix(61.03340, 4.40300, noon.plusSeconds(60), width = 30.0)

        assertFalse(driftedWhilePutDown(standing, nearby, untouched))
    }

    // Two answers wide enough to cover the distance between them are not a
    // contradiction: a phone in a courtyard says where it is with several
    // hundred metres of doubt, and both answers hold the same flat.
    @Test
    fun `two wide answers that overlap are not a wandering`() {
        val wideAnchor = PutDown(at = fix(61.03268, 4.40300, noon, width = 200.0), since = noon)
        val wide = fix(61.03600, 4.40300, noon.plusSeconds(60), width = 300.0)

        assertFalse(driftedWhilePutDown(wideAnchor, wide, untouched))
    }

    @Test
    fun `a phone that was stirred is not accused of anything`() {
        val elsewhere = fix(61.04170, 4.40300, noon.plusSeconds(60), width = 246.0)

        assertFalse(driftedWhilePutDown(standing, elsewhere, Carrying(stirred = true)))
    }

    @Test
    fun `a walk of twenty steps is a phone that went somewhere`() {
        val elsewhere = fix(61.04170, 4.40300, noon.plusSeconds(60), width = 246.0)

        assertFalse(
            driftedWhilePutDown(standing, elsewhere, Carrying(stirred = false, steps = 20)),
        )
    }

    // A few steps are a phone picked up off a table and put back, or a knock
    // against the table it is lying on.
    @Test
    fun `a handful of steps leaves the answer to explain itself`() {
        val elsewhere = fix(61.04170, 4.40300, noon.plusSeconds(60), width = 246.0)

        assertTrue(driftedWhilePutDown(standing, elsewhere, Carrying(stirred = false, steps = 5)))
    }

    @Test
    fun `a phone with no sensors to ask says nothing`() {
        val elsewhere = fix(61.04170, 4.40300, noon.plusSeconds(60), width = 246.0)

        assertFalse(
            driftedWhilePutDown(standing, elsewhere, Carrying(stirred = false, told = false)),
        )
    }

    @Test
    fun `an answer from before the phone settled is about another part of the day`() {
        val earlier = fix(61.04170, 4.40300, noon.minusSeconds(60), width = 246.0)

        assertFalse(driftedWhilePutDown(standing, earlier, untouched))
    }

    @Test
    fun `without an anchor there is nothing to have walked away from`() {
        val elsewhere = fix(61.04170, 4.40300, noon.plusSeconds(60), width = 246.0)

        assertFalse(driftedWhilePutDown(null, elsewhere, untouched))
    }

    @Test
    fun `a phone still being knocked about is not put down yet`() {
        assertNull(putDownAt(home, noon.minusSeconds(30), told = true, now = noon))
    }

    @Test
    fun `a phone quiet for two minutes counts as put down when it fell quiet`() {
        val anchor = putDownAt(home, noon.minusSeconds(180), told = true, now = noon, steps = 1_000)

        assertEquals(noon.minusSeconds(180).plus(PUT_DOWN_AFTER), anchor?.since)
        assertEquals(1_000L, anchor?.steps)
    }

    // Nothing has stirred since the phone was switched on, which is a phone
    // that has been lying there the whole time.
    @Test
    fun `a phone that never stirred is anchored from now`() {
        assertEquals(noon, putDownAt(home, null, told = true, now = noon)?.since)
    }

    @Test
    fun `without sensors or a position there is no anchor`() {
        assertNull(putDownAt(home, null, told = false, now = noon))
        assertNull(putDownAt(null, null, told = true, now = noon))
    }

    @Test
    fun `stirs from before the phone settled say nothing about it`() {
        val carrying =
            carryingSince(standing, listOf(Stir(noon.minusSeconds(30)), Stir(noon.minusSeconds(5))))

        assertFalse(carrying.stirred)
    }

    @Test
    fun `a stir inside the anchor's watch undoes it however late it arrives`() {
        val carrying = carryingSince(standing, listOf(Stir(noon.plusSeconds(30))))

        assertTrue(carrying.stirred)
    }

    // The counter runs from the last boot, so what matters is the distance
    // from where it stood when the phone was put down — not the difference
    // inside one batch of readings, which is nothing.
    @Test
    fun `steps are counted from where the counter stood at the anchor`() {
        val carrying =
            carryingSince(
                standing,
                listOf(Stir(noon.plusSeconds(60), steps = 1_050)),
            )

        assertEquals(50, carrying.steps)
        assertFalse(carrying.stirred)
    }

    @Test
    fun `without an anchor the sensors are asked about nothing`() {
        val carrying = carryingSince(null, listOf(Stir(noon, steps = 2_000)))

        assertFalse(carrying.stirred)
        assertNull(carrying.steps)
    }

    private fun fix(latitude: Double, longitude: Double, takenAt: Instant, width: Double) = Fix(
        latitude = latitude,
        longitude = longitude,
        takenAt = takenAt,
        accuracyMetres = width,
        from = "fused",
    )
}
