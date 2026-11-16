package io.github.vitalyostanin.geoshare.collection

import java.time.Duration
import java.time.Instant
import java.time.LocalTime

/**
 * The least time between two alarms the platform lets through.
 *
 * In deep sleep an alarm that may fire while idle is delivered about once in
 * nine minutes per application, and one asked for sooner is simply held back
 * until then. Asking for no less says the same thing the platform will do
 * anyway, and keeps the schedule from pretending to a precision it cannot have.
 */
val MINIMUM_WAKE_GAP: Duration = Duration.ofMinutes(9)

/**
 * How long from [now] the device should be woken to look at itself again, or
 * null when nothing needs it.
 *
 * Both of the timers the collection keeps — the silence that ends in a rescue,
 * and the mode changing when the night begins or ends — are coroutine delays,
 * and those stand still while the processor sleeps. The device then neither
 * rescues nor changes mode until something else wakes it, which on a phone left
 * on a shelf overnight is the morning. This says when an alarm has to do it
 * instead: the nearer of the moment the silence reaches [rescueAfter] and the
 * next edge of the night.
 *
 * The fast mode has no use for it: it is held by an open map, and a screen
 * that is on is a processor that is awake.
 *
 * The clock of the day is passed in alongside the instant so that the edge of
 * the night, which is a local time, can be tested without waiting for it.
 */
fun nextWake(
    now: Instant,
    heardAt: Instant,
    mode: CollectionMode,
    localTime: LocalTime,
): Duration? {
    if (mode == CollectionMode.Viewing) {
        return null
    }

    val silent = rescueAfter(mode)?.let { Duration.between(now, heardAt.plus(it)) }
    val soonest = listOfNotNull(silent, untilTheNightChanges(localTime)).min()

    return soonest.coerceAtLeast(MINIMUM_WAKE_GAP)
}

/** Time to the nearer edge of the night, wrapping past midnight. */
private fun untilTheNightChanges(time: LocalTime): Duration =
    minOf(until(time, NIGHT_START), until(time, NIGHT_END))

/** Time from [from] to the next [edge]; an edge standing right on [from] is a day away. */
private fun until(from: LocalTime, edge: LocalTime): Duration {
    val gap = Duration.between(from, edge)

    return if (gap.isZero || gap.isNegative) gap.plusDays(1) else gap
}
