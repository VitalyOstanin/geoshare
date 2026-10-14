package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.Moving
import io.github.vitalyostanin.geoshare.collection.PUT_DOWN_AFTER
import io.github.vitalyostanin.geoshare.collection.Stir
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class BeingCarriedTest {
    private val start = Instant.parse("2026-10-04T10:00:00Z")

    @Test
    fun `stillness is dated from its first report`() {
        val carried = BeingCarried(sensorsTell = true)

        carried.hear(Moving(going = false, told = true), start)
        carried.hear(Moving(going = false, told = true), start.plusSeconds(300))

        assertEquals(start, carried.standingSince)
    }

    @Test
    fun `going anywhere ends the stillness`() {
        val carried = BeingCarried(sensorsTell = true)

        carried.hear(Moving(going = false, told = true), start)
        carried.hear(Moving(going = true, told = true), start.plusSeconds(60))

        assertNull(carried.standingSince)
    }

    @Test
    fun `stillness nobody reported is no stillness`() {
        val carried = BeingCarried(sensorsTell = true)

        carried.hear(Moving(going = false, told = false), start)

        assertNull(carried.standingSince)
    }

    @Test
    fun `a device left alone is anchored where it lies`() {
        val carried = BeingCarried(sensorsTell = true)
        val here = Fix(latitude = 1.0, longitude = 2.0, takenAt = start, accuracyMetres = 5.0)

        carried.hear(Stir(start), start)
        carried.anchorWhereItLies(here, start.plus(PUT_DOWN_AFTER).plusSeconds(1))

        assertEquals(here, carried.putDown?.at)
        assertFalse(carried.carrying().stirred)
    }

    @Test
    fun `a stir after the anchor says the device was picked up`() {
        val carried = BeingCarried(sensorsTell = true)
        val here = Fix(latitude = 1.0, longitude = 2.0, takenAt = start, accuracyMetres = 5.0)
        val settled = start.plus(PUT_DOWN_AFTER).plusSeconds(1)

        carried.hear(Stir(start), start)
        carried.anchorWhereItLies(here, settled)
        carried.hear(Stir(settled.plusSeconds(10)), settled.plusSeconds(10))

        assertTrue(carried.carrying().stirred)
    }
}
