package io.github.vitalyostanin.geoshare.service

import com.google.android.gms.location.Priority
import io.github.vitalyostanin.geoshare.collection.Accuracy
import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.github.vitalyostanin.geoshare.collection.rescueAccuracy

/**
 * What to ask the position provider for, in its own terms.
 *
 * The mode says how hard the device should be working; this turns that into the
 * three numbers the provider takes. Kept apart from the provider itself so the
 * translation can be checked without a device.
 */
data class RequestSpec(
    val priority: Int,
    val intervalMillis: Long,
    /**
     * How long fixes may pile up in the chip before they are handed over. The
     * radio stays asleep in between, which is where most of the saving is.
     */
    val maxDelayMillis: Long,
)

/** The request for a mode, or null for a mode that asks for nothing. */
fun requestSpec(mode: CollectionMode): RequestSpec? {
    val priority = priorityFor(mode.accuracy) ?: return null

    return RequestSpec(
        priority = priority,
        intervalMillis = mode.interval.toMillis(),
        maxDelayMillis = mode.maxUpdateDelay.toMillis(),
    )
}

/**
 * What a rescue asks the provider for, or null for a mode that asks for
 * nothing and so has nothing to rescue.
 */
fun rescuePriority(mode: CollectionMode): Int? = priorityFor(rescueAccuracy(mode))

/** Play Services' name for an accuracy, or null where it asks for nothing. */
private fun priorityFor(accuracy: Accuracy): Int? = when (accuracy) {
    Accuracy.None -> null
    Accuracy.LowPower -> Priority.PRIORITY_LOW_POWER
    Accuracy.Balanced -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
    Accuracy.High -> Priority.PRIORITY_HIGH_ACCURACY
}
