package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.collection.Fix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** What is said when the last position is said again. */
class WrittenTest {
    @Test
    fun `the last position written is said again in preference to the last known`() {
        val said = sayAgainOf(WRITTEN_FIX, KNOWN_FIX, null, charging = false)

        assertEquals(Written(WRITTEN_FIX, charging = false, settling = false), said)
    }

    @Test
    fun `the last known position is said when nothing has been written yet`() {
        val said = sayAgainOf(null, KNOWN_FIX, null, charging = true)

        assertEquals(Written(KNOWN_FIX, charging = true, settling = false), said)
    }

    @Test
    fun `nothing is said when the device has no position at all`() {
        assertNull(sayAgainOf(null, null, null, charging = false))
    }

    // The note about the receiver belongs to the answer, which has not changed.
    @Test
    fun `a position written while the receiver was settling is said again as settling`() {
        val before = Written(WRITTEN_FIX, charging = false, settling = true)

        assertTrue(sayAgainOf(WRITTEN_FIX, null, before, charging = false)!!.settling)
    }

    @Test
    fun `the cable is read afresh when a position is said again`() {
        val before = Written(WRITTEN_FIX, charging = false)

        assertTrue(sayAgainOf(WRITTEN_FIX, null, before, charging = true)!!.charging)
        assertFalse(sayAgainOf(WRITTEN_FIX, null, before, charging = false)!!.charging)
    }

    @Test
    fun `the same place with the cable newly in is not the same write`() {
        assertFalse(
            Written(WRITTEN_FIX, charging = false) == Written(WRITTEN_FIX, charging = true),
        )
    }

    private companion object {
        val WRITTEN_FIX = Fix(61.03268, 4.40300, Instant.parse("2026-10-17T12:00:00Z"))
        val KNOWN_FIX = Fix(61.03270, 4.40310, Instant.parse("2026-10-17T09:00:00Z"))
    }
}
