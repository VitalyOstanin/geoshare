package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class RescueTest {
    @Test
    fun `a mode that asks for nothing waits for nothing`() {
        assertNull(rescueAfter(CollectionMode.Idle))
    }

    @Test
    fun `the fast mode gives itself a minute before asking another way`() {
        assertEquals(Duration.ofMinutes(1), rescueAfter(CollectionMode.Viewing))
    }

    @Test
    fun `a slow mode does not wait longer than half the time a position stays fresh`() {
        assertEquals(STALE_AFTER.dividedBy(2), rescueAfter(CollectionMode.Standby))
    }

    @Test
    fun `the rescue of the fast mode lets the network answer in`() {
        assertEquals(Accuracy.Balanced, rescueAccuracy(CollectionMode.Viewing))
    }

    @Test
    fun `a mode that already asks coarsely keeps its own accuracy`() {
        assertEquals(Accuracy.LowPower, rescueAccuracy(CollectionMode.Night))
        assertEquals(Accuracy.Balanced, rescueAccuracy(CollectionMode.Standby))
    }

    @Test
    fun `no rescue asks more finely than the mode it rescues`() {
        CollectionMode.entries.forEach { mode ->
            assertTrue(
                "$mode would be rescued more finely than it asks",
                rescueAccuracy(mode).ordinal <= mode.accuracy.ordinal,
            )
        }
    }

    @Test
    fun `every mode that listens has room for the rescue before the position goes grey`() {
        val deadlines = CollectionMode.entries.mapNotNull { rescueAfter(it) }

        assertEquals(CollectionMode.entries.size - 1, deadlines.size)
        deadlines.forEach { assertTrue("$it is too late to help", it < STALE_AFTER) }
    }
}
