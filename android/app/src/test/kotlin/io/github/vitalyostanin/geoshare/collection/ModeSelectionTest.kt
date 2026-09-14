package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalTime

class ModeSelectionTest {
    private val now: Instant = Instant.parse("2026-09-11T12:00:00Z")

    private fun state(
        watchersUntil: List<Instant> = emptyList(),
        localTime: LocalTime = LocalTime.of(12, 0),
        insideGeofence: Boolean = false,
        moving: Boolean = false,
        batteryPercent: Int = 80,
        ownMapOpen: Boolean = false,
    ) = CollectionState(
        watchersUntil = watchersUntil,
        now = now,
        localTime = localTime,
        insideGeofence = insideGeofence,
        moving = moving,
        batteryPercent = batteryPercent,
        ownMapOpen = ownMapOpen,
    )

    @Test
    fun `standby when nothing is happening`() {
        assertEquals(CollectionMode.Standby, chooseMode(state()))
    }

    @Test
    fun `movement raises the rate`() {
        assertEquals(CollectionMode.Moving, chooseMode(state(moving = true)))
    }

    @Test
    fun `the geofence outranks movement`() {
        val mode = chooseMode(state(moving = true, insideGeofence = true))

        assertEquals(CollectionMode.Idle, mode)
    }

    @Test
    fun `a live watcher outranks the geofence`() {
        val mode =
            chooseMode(
                state(
                    watchersUntil = listOf(now.plusSeconds(60)),
                    insideGeofence = true,
                ),
            )

        assertEquals(CollectionMode.Viewing, mode)
    }

    @Test
    fun `a live watcher outranks a flat battery`() {
        val mode =
            chooseMode(
                state(
                    watchersUntil = listOf(now.plusSeconds(60)),
                    batteryPercent = 5,
                ),
            )

        assertEquals(CollectionMode.Viewing, mode)
    }

    @Test
    fun `an expired marker is not a watcher`() {
        val mode = chooseMode(state(watchersUntil = listOf(now.minusSeconds(1))))

        assertEquals(CollectionMode.Standby, mode)
    }

    @Test
    fun `one live marker among expired ones is enough`() {
        val mode =
            chooseMode(
                state(watchersUntil = listOf(now.minusSeconds(600), now.plusSeconds(5))),
            )

        assertEquals(CollectionMode.Viewing, mode)
    }

    @Test
    fun `night starts at 23 and outranks movement`() {
        val mode = chooseMode(state(localTime = LocalTime.of(23, 0), moving = true))

        assertEquals(CollectionMode.Night, mode)
    }

    @Test
    fun `night holds past midnight`() {
        assertEquals(CollectionMode.Night, chooseMode(state(localTime = LocalTime.of(3, 0))))
    }

    @Test
    fun `night ends at 7`() {
        assertEquals(CollectionMode.Standby, chooseMode(state(localTime = LocalTime.of(7, 0))))
    }

    @Test
    fun `a flat battery is treated as night at any hour`() {
        val mode = chooseMode(state(batteryPercent = LOW_BATTERY_PERCENT - 1, moving = true))

        assertEquals(CollectionMode.Night, mode)
    }

    @Test
    fun `the threshold itself is not flat`() {
        val mode = chooseMode(state(batteryPercent = LOW_BATTERY_PERCENT, moving = true))

        assertEquals(CollectionMode.Moving, mode)
    }

    @Test
    fun `the map open on this phone outranks the geofence`() {
        val mode = chooseMode(state(insideGeofence = true, ownMapOpen = true))

        assertEquals(CollectionMode.Viewing, mode)
    }

    @Test
    fun `the map open on this phone outranks the night`() {
        val mode = chooseMode(state(localTime = LocalTime.of(3, 0), ownMapOpen = true))

        assertEquals(CollectionMode.Viewing, mode)
    }

    @Test
    fun `a closed map on this phone changes nothing`() {
        assertEquals(CollectionMode.Standby, chooseMode(state(ownMapOpen = false)))
    }
}
