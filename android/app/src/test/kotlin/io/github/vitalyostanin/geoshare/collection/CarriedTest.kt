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

    private val putDown = PutDown(at = home, since = noon, steps = 1_000)

    private val untouched = Carrying(stirred = false, steps = 0)

    @Test
    fun `an answer a kilometre from a phone nobody touched has walked away`() {
        // The one this phone actually gave at 11:00 that morning: four
        // satellites, a quarter of a kilometre of doubt, and a place a
        // kilometre from the flat it was lying in.
        val elsewhere = fix(61.04170, 4.40300, noon.plusSeconds(60), width = 246.0)

        assertTrue(driftedWhilePutDown(putDown, elsewhere, untouched))
    }

    // The answer the towers and the Wi-Fi handed a phone at 13:35, while it
    // lay at home: five metres of claimed doubt, and within two metres of the
    // false satellite answer rejected a second earlier. The rescue branch
    // writes such an answer past the rest of the filter, so this rule is the
    // only one that stands in its way.
    @Test
    fun `an answer from the other radios is measured from the anchor as well`() {
        val anchor = PutDown(at = fix(61.03268, 4.40299, noon, width = 100.0), since = noon)
        val fromTheRadios = fix(61.04905, 4.46380, noon.plusSeconds(60), width = 5.0)

        assertTrue(driftedWhilePutDown(anchor, fromTheRadios, untouched))
    }

    @Test
    fun `an answer wandering inside the doubt of a put-down phone is left alone`() {
        val nearby = fix(61.03340, 4.40300, noon.plusSeconds(60), width = 30.0)

        assertFalse(driftedWhilePutDown(putDown, nearby, untouched))
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

        assertFalse(driftedWhilePutDown(putDown, elsewhere, Carrying(stirred = true)))
    }

    @Test
    fun `a walk of twenty steps is a phone that went somewhere`() {
        val elsewhere = fix(61.04170, 4.40300, noon.plusSeconds(60), width = 246.0)

        assertFalse(
            driftedWhilePutDown(putDown, elsewhere, Carrying(stirred = false, steps = 20)),
        )
    }

    // A few steps are a phone picked up off a table and put back, or a knock
    // against the table it is lying on.
    @Test
    fun `a handful of steps leaves the answer to explain itself`() {
        val elsewhere = fix(61.04170, 4.40300, noon.plusSeconds(60), width = 246.0)

        assertTrue(driftedWhilePutDown(putDown, elsewhere, Carrying(stirred = false, steps = 5)))
    }

    @Test
    fun `a phone with no sensors to ask says nothing`() {
        val elsewhere = fix(61.04170, 4.40300, noon.plusSeconds(60), width = 246.0)

        assertFalse(
            driftedWhilePutDown(putDown, elsewhere, Carrying(stirred = false, told = false)),
        )
    }

    @Test
    fun `an answer from before the phone settled is about another part of the day`() {
        val earlier = fix(61.04170, 4.40300, noon.minusSeconds(60), width = 246.0)

        assertFalse(driftedWhilePutDown(putDown, earlier, untouched))
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
            carryingSince(putDown, listOf(Stir(noon.minusSeconds(30)), Stir(noon.minusSeconds(5))))

        assertFalse(carrying.stirred)
    }

    @Test
    fun `a stir inside the anchor's watch undoes it however late it arrives`() {
        val carrying = carryingSince(putDown, listOf(Stir(noon.plusSeconds(30))))

        assertTrue(carrying.stirred)
    }

    // The counter runs from the last boot, so what matters is the distance
    // from where it stood when the phone was put down — not the difference
    // inside one batch of readings, which is nothing.
    @Test
    fun `steps are counted from where the counter stood at the anchor`() {
        val carrying =
            carryingSince(
                putDown,
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

    // The walk this rule is about: a person carrying the phone three hundred
    // metres down a street while the receiver goes on reporting the same
    // doorway to within ten metres.
    @Test
    fun `a narrow answer putDown still through a walk is pinned in place`() {
        val walked = walk(steps = 500)
        val doorway = fix(61.03268, 4.40300, noon, width = 10.0)
        val same = fix(61.03270, 4.40302, noon.plusSeconds(300), width = 12.0)

        assertTrue(pinnedWhileWalking(walked, doorway, same))
    }

    @Test
    fun `an answer that followed the walking says nothing`() {
        val walked = walk(steps = 500)
        val doorway = fix(61.03268, 4.40300, noon, width = 10.0)
        val away = fix(61.03500, 4.40300, noon.plusSeconds(300), width = 12.0)

        assertFalse(pinnedWhileWalking(walked, doorway, away))
    }

    // Moving about a flat is walking too, and the position honestly does not
    // change; the rule has to stay quiet through it.
    @Test
    fun `walking about indoors is too short to accuse anything`() {
        val walked = walk(steps = 120)
        val doorway = fix(61.03268, 4.40300, noon, width = 10.0)
        val same = fix(61.03269, 4.40301, noon.plusSeconds(300), width = 12.0)

        assertFalse(pinnedWhileWalking(walked, doorway, same))
    }

    // A place worked out from the towers does not move while its owner walks a
    // few streets, and there is nothing wrong with that.
    @Test
    fun `a wide answer putDown still is not accused of anything`() {
        val walked = walk(steps = 500)
        val district = fix(61.03268, 4.40300, noon, width = 700.0)
        val same = fix(61.03270, 4.40302, noon.plusSeconds(300), width = 700.0)

        assertFalse(pinnedWhileWalking(walked, district, same))
    }

    @Test
    fun `an answer taken before the walking began is about another part of the day`() {
        val walked = Walk(since = noon.plusSeconds(600), steps = 500)
        val doorway = fix(61.03268, 4.40300, noon, width = 10.0)
        val earlier = fix(61.03268, 4.40300, noon.plusSeconds(60), width = 10.0)

        assertFalse(pinnedWhileWalking(walked, doorway, earlier))
    }

    @Test
    fun `a phone without sensors is not asked about being carried`() {
        val walked = walk(steps = 500)
        val doorway = fix(61.03268, 4.40300, noon, width = 10.0)
        val same = fix(61.03270, 4.40302, noon.plusSeconds(300), width = 12.0)

        assertFalse(pinnedWhileWalking(walked, doorway, same, told = false))
    }

    @Test
    fun `the walking is counted from the last rest, not from the first step`() {
        // Five hundred steps to the shop, a quarter of an hour sitting, then
        // fifty steps back out: what is being walked now is the fifty.
        val stirs = listOf(
            Stir(noon, steps = 1_000),
            Stir(noon.plusSeconds(60), steps = 1_500),
            Stir(noon.plusSeconds(60 * 16), steps = 1_550),
        )

        assertEquals(50L, walkingNow(stirs)?.steps)
    }

    @Test
    fun `a reading that added no steps starts the count again`() {
        val stirs = listOf(
            Stir(noon, steps = 1_000),
            Stir(noon.plusSeconds(60), steps = 1_000),
            Stir(noon.plusSeconds(120), steps = 1_400),
        )

        val walked = walkingNow(stirs)

        assertEquals(400L, walked?.steps)
        assertEquals(noon.plusSeconds(60), walked?.since)
    }

    @Test
    fun `a phone whose counter has been read once is not walking yet`() {
        assertNull(walkingNow(listOf(Stir(noon, steps = 1_000))))
    }

    @Test
    fun `stirs without a step count are not walking`() {
        assertNull(walkingNow(listOf(Stir(noon), Stir(noon.plusSeconds(60)))))
    }

    private fun walk(steps: Long) = Walk(since = noon.minusSeconds(60), steps = steps)

    // On a phone left on a table overnight, when the answers it was handed
    // stood fifty-five kilometres away, the speed it was given was
    // thirty-three metres a second, which is a hundred and twenty kilometres
    // an hour.
    @Test
    fun `a phone nobody touched is not travelling at the speed of a car`() {
        val moving = fix(61.11864, 5.38881, noon.plusSeconds(60), width = 15.0, speedMps = 33.3)

        assertTrue(hurriedWhilePutDown(putDown, moving, untouched))
    }

    // What the distance rule cannot do: the place is still inside the anchor's
    // allowance, so nothing has walked away yet, and the claim is caught all
    // the same because it sits inside the single answer.
    @Test
    fun `a speed is caught before the answer has gone anywhere`() {
        val nearby = fix(61.03300, 4.40300, noon.plusSeconds(60), width = 15.0, speedMps = 33.3)

        assertFalse(driftedWhilePutDown(putDown, nearby, untouched))
        assertTrue(hurriedWhilePutDown(putDown, nearby, untouched))
    }

    // Of 6,405 answers the phones gave from home over three days, nine in
    // ten claimed exactly this.
    @Test
    fun `a phone lying still is told it is going nowhere`() {
        val resting = fix(61.03268, 4.40300, noon.plusSeconds(60), width = 15.0, speedMps = 0.0)

        assertFalse(hurriedWhilePutDown(putDown, resting, untouched))
    }

    @Test
    fun `a phone somebody picked up may honestly be travelling`() {
        val moving = fix(61.11864, 5.38881, noon.plusSeconds(60), width = 15.0, speedMps = 33.3)

        assertFalse(hurriedWhilePutDown(putDown, moving, Carrying(stirred = true)))
    }

    @Test
    fun `an answer that named no speed is not judged on one`() {
        val silent = fix(61.11864, 5.38881, noon.plusSeconds(60), width = 15.0)

        assertFalse(hurriedWhilePutDown(putDown, silent, untouched))
    }

    @Test
    fun `without an anchor a speed is nobody's business`() {
        val moving = fix(61.11864, 5.38881, noon.plusSeconds(60), width = 15.0, speedMps = 33.3)

        assertFalse(hurriedWhilePutDown(null, moving, untouched))
    }

    private fun fix(
        latitude: Double,
        longitude: Double,
        takenAt: Instant,
        width: Double,
        speedMps: Double? = null,
    ) = Fix(
        latitude = latitude,
        longitude = longitude,
        takenAt = takenAt,
        accuracyMetres = width,
        from = "fused",
        reported = speedMps?.let { Reported(speedMps = it) },
    )
}
