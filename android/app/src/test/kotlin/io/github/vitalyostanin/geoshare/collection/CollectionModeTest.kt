package io.github.vitalyostanin.geoshare.collection

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class CollectionModeTest {
    // The ceiling is global, so a test that sets it must not leave it for the next.
    @After
    fun clearCeiling() {
        PaceCeiling.current = null
    }

    private fun minutes(count: Long): Duration = Duration.ofMinutes(count)

    private fun assertPace(
        mode: CollectionMode,
        interval: Duration,
        delay: Duration,
        silence: Duration,
    ) {
        assertEquals("$mode interval", interval, mode.interval)
        assertEquals("$mode maxUpdateDelay", delay, mode.maxUpdateDelay)
        assertEquals("$mode writeSilence", silence, mode.writeSilence)
    }

    @Test
    fun `without a ceiling every mode keeps its own pace`() {
        assertPace(CollectionMode.Idle, Duration.ZERO, Duration.ZERO, minutes(10))
        assertPace(CollectionMode.Standby, minutes(15), minutes(10), minutes(10))
        assertPace(CollectionMode.Moving, minutes(5), minutes(5), minutes(10))
        assertPace(CollectionMode.Viewing, Duration.ofSeconds(30), Duration.ZERO, minutes(3))
        assertPace(CollectionMode.Night, minutes(20), minutes(5), minutes(10))
    }

    @Test
    fun `a ceiling of five minutes asks and writes at least that often`() {
        PaceCeiling.current = minutes(5)

        assertPace(CollectionMode.Standby, minutes(5), Duration.ZERO, minutes(5))
        assertPace(CollectionMode.Moving, minutes(5), Duration.ZERO, minutes(5))
        assertPace(CollectionMode.Night, minutes(5), Duration.ZERO, minutes(5))
    }

    @Test
    fun `a ceiling never slows viewing and shortens its stillness`() {
        PaceCeiling.current = minutes(1)

        assertPace(CollectionMode.Viewing, Duration.ofSeconds(30), Duration.ZERO, minutes(1))

        PaceCeiling.current = minutes(20)

        assertPace(CollectionMode.Viewing, Duration.ofSeconds(30), Duration.ZERO, minutes(3))
    }

    @Test
    fun `a ceiling of twenty minutes leaves room for a batch window`() {
        PaceCeiling.current = minutes(20)

        assertPace(CollectionMode.Standby, minutes(15), minutes(5), minutes(10))
        assertPace(CollectionMode.Moving, minutes(5), minutes(5), minutes(10))
        assertPace(CollectionMode.Night, minutes(20), Duration.ZERO, minutes(10))
    }

    @Test
    fun `with a ceiling every collecting mode asks again within it`() {
        for (ceiling in 1L..20L) {
            PaceCeiling.current = minutes(ceiling)

            for (mode in CollectionMode.entries) {
                val round = mode.asksAgainWithin() ?: continue

                // Viewing is faster than any ceiling; the rest are bounded by it.
                val bound = maxOf(minutes(ceiling), mode.interval)

                assertTrue("$mode asks again after $round, ceiling $ceiling", round <= bound)
            }
        }
    }

    // The grey on the map means "this device has stopped saying where it is",
    // so no mode may take longer than that to say it again. A mode whose round
    // of asking and waiting outlasts the half hour paints a working device as a
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
    }

    // A still device keeps quiet longer than it collects, because every device
    // of the group is in this mode while anybody looks; but not longer than one
    // claim lasts, or a watcher could look at a still device and never see it
    // say anything.
    @Test
    fun `a still device somebody is watching speaks within one claim`() {
        assertTrue(CollectionMode.Viewing.writeSilence < WATCH_AHEAD)
    }

    @Test
    fun `a mode that asks for nothing promises nothing`() {
        assertTrue(CollectionMode.Idle.asksAgainWithin() == null)
    }
}
