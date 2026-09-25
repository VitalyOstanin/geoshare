package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class GeofenceTest {
    private fun fix(latitude: Double, longitude: Double) =
        Fix(latitude, longitude, Instant.parse("2026-09-13T12:00:00Z"))

    @Test
    fun `arriving somewhere is not yet standing there`() {
        assertFalse(dwellingAfter(listOf(Fencing.Entered)))
    }

    @Test
    fun `staying put is`() {
        assertTrue(dwellingAfter(listOf(Fencing.Entered, Fencing.Dwelt)))
    }

    @Test
    fun `leaving ends it`() {
        assertFalse(dwellingAfter(listOf(Fencing.Left), before = true))
    }

    @Test
    fun `the last crossing of a batch is the one that holds`() {
        val changes = listOf(Fencing.Dwelt, Fencing.Left, Fencing.Entered, Fencing.Dwelt)

        assertTrue(dwellingAfter(changes))
    }

    @Test
    fun `an entry on its own leaves the answer as it was`() {
        assertTrue(dwellingAfter(listOf(Fencing.Entered), before = true))
    }

    @Test
    fun `the first position written has no fence to compare with`() {
        assertTrue(fenceMoves(null, fix(60.93, 4.31)))
    }

    @Test
    fun `a position inside the fence leaves it where it is`() {
        // Some fifty metres north of the centre.
        assertFalse(fenceMoves(fix(60.93, 4.31), fix(60.9305, 4.31)))
    }

    @Test
    fun `a position beyond the fence moves it`() {
        // Some four hundred metres north of the centre.
        assertTrue(fenceMoves(fix(60.93, 4.31), fix(60.9336, 4.31)))
    }

    @Test
    fun `the quiet mode speaks up before its position goes grey`() {
        assertTrue(IDLE_REPEAT < STALE_AFTER)
        assertTrue(GEOFENCE_SETTLING < IDLE_REPEAT)
    }
}
