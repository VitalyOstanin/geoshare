package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionModeTest {
    // The grey on the map means "this phone has stopped saying where it is",
    // so no mode may take longer than that to say it again. A mode whose round
    // of asking and waiting outlasts the half hour paints a working phone as a
    // broken one, and the person looking has no way to tell which it is.
    @Test
    fun `every mode that asks for a position writes one again before it goes grey`() {
        for (mode in CollectionMode.entries) {
            val round = mode.asksAgainWithin() ?: continue

            assertTrue(
                "$mode asks again after $round, and the grey starts at $STALE_AFTER",
                round < STALE_AFTER,
            )
        }
    }

    // The expensive mode exists to answer somebody watching, and answering
    // means writing: collecting four times a minute and writing once every ten
    // is the battery of the whole group spent on nothing.
    @Test
    fun `the mode somebody is watching writes more readily than the quiet ones`() {
        assertTrue(CollectionMode.Viewing.writeSilence < CollectionMode.Standby.writeSilence)
        assertTrue(CollectionMode.Viewing.writeDistance < CollectionMode.Standby.writeDistance)
        assertTrue(
            CollectionMode.Viewing.writeSilence <= CollectionMode.Viewing.interval.multipliedBy(2),
        )
    }

    @Test
    fun `a mode that asks for nothing promises nothing`() {
        assertTrue(CollectionMode.Idle.asksAgainWithin() == null)
    }
}
