package io.github.vitalyostanin.geoshare.collection

import java.time.Instant
import java.time.LocalTime

/** Everything the choice depends on, gathered by the caller. */
data class CollectionState(
    /** When the viewing markers of the other members expire. */
    val watchersUntil: List<Instant>,
    val now: Instant,
    val localTime: LocalTime,
    val insideGeofence: Boolean,
    val moving: Boolean,
    val batteryPercent: Int,
    /**
     * Whether this device's own map is open in front of somebody.
     *
     * Separate from [watchersUntil] because it never travels: the claim this
     * device writes for the others comes back to it filtered out, so without
     * this the one viewer the device can be certain of would be the only one it
     * never counted.
     */
    val ownMapOpen: Boolean,
)

/** Below this the device stops trying to be useful and starts trying to last. */
const val LOW_BATTERY_PERCENT: Int = 15

private val NIGHT_START: LocalTime = LocalTime.of(23, 0)
private val NIGHT_END: LocalTime = LocalTime.of(7, 0)

/**
 * The whole policy, as one function of its inputs.
 *
 * Time is passed in rather than read here, otherwise the night case could only
 * be tested by waiting for the night.
 */
fun chooseMode(state: CollectionState): CollectionMode {
    val watched = state.watchersUntil.any { it.isAfter(state.now) }

    return when {
        // A viewer outranks everything, including a flat battery: the point of
        // the application is to answer "where are you" while somebody asks.
        // The person holding this device is a viewer like any other, and the
        // question they are asking of their own map is the same question.
        watched || state.ownMapOpen -> CollectionMode.Viewing

        state.insideGeofence -> CollectionMode.Idle

        state.batteryPercent < LOW_BATTERY_PERCENT -> CollectionMode.Night

        isNight(state.localTime) -> CollectionMode.Night

        state.moving -> CollectionMode.Moving

        else -> CollectionMode.Standby
    }
}

/** The window wraps around midnight, so it is two comparisons joined by `or`. */
private fun isNight(time: LocalTime): Boolean = time >= NIGHT_START || time < NIGHT_END
