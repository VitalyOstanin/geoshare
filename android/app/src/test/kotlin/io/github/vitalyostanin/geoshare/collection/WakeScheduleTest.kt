package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

class WakeScheduleTest {
    private val now: Instant = Instant.parse("2026-09-11T12:00:00Z")

    private fun wake(
        mode: CollectionMode,
        heardAgo: Duration = Duration.ZERO,
        at: LocalTime = LocalTime.of(12, 0),
    ): Duration? = nextWake(now, now.minus(heardAgo), mode, at)

    @Test
    fun `the fast mode needs no alarm`() {
        assertNull(wake(CollectionMode.Viewing))
    }

    @Test
    fun `silence is the nearer when the night is far`() {
        val silent = rescueAfter(CollectionMode.Standby)!!

        assertEquals(silent, wake(CollectionMode.Standby))
    }

    @Test
    fun `time already spent in silence is taken off the wait`() {
        val silent = rescueAfter(CollectionMode.Standby)!!

        assertEquals(
            silent.minus(Duration.ofMinutes(4)),
            wake(CollectionMode.Standby, heardAgo = Duration.ofMinutes(4)),
        )
    }

    @Test
    fun `the edge of the night wins when it is nearer than the silence`() {
        assertEquals(
            Duration.ofMinutes(12),
            wake(CollectionMode.Standby, at = LocalTime.of(6, 48)),
        )
    }

    @Test
    fun `the edge is found across midnight`() {
        assertEquals(
            Duration.ofMinutes(12),
            wake(CollectionMode.Standby, at = LocalTime.of(6, 48)),
        )
        assertEquals(
            Duration.ofMinutes(12),
            wake(CollectionMode.Standby, at = LocalTime.of(22, 48)),
        )
    }

    @Test
    fun `an alarm is never asked for sooner than the platform gives one`() {
        assertEquals(MINIMUM_WAKE_GAP, wake(CollectionMode.Standby, at = LocalTime.of(22, 59)))
        assertEquals(
            MINIMUM_WAKE_GAP,
            wake(CollectionMode.Standby, heardAgo = Duration.ofHours(3)),
        )
    }

    @Test
    fun `a mode that asks for nothing is woken only for the night`() {
        assertEquals(Duration.ofHours(11), wake(CollectionMode.Idle, at = LocalTime.of(12, 0)))
        assertEquals(Duration.ofHours(8), wake(CollectionMode.Idle, at = LocalTime.of(23, 0)))
    }

    @Test
    fun `after the evening edge the next one is the morning`() {
        assertEquals(Duration.ofHours(7), wake(CollectionMode.Idle, at = LocalTime.of(0, 0)))
    }
}
