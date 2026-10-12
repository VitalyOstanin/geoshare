package io.github.vitalyostanin.geoshare.collection

/**
 * A change the device noticed in what its owner is doing.
 *
 * Two plain facts rather than the platform's pair of numbers: the choice of
 * mode cares only whether the activity carries the device somewhere, and
 * whether it has begun or ended. The service turns the platform's constants
 * into these on the way in, so that everything decided in this package still
 * runs on a plain JVM.
 */
data class ActivityChange(
    /** Whether this is a way of getting somewhere rather than of staying put. */
    val travelling: Boolean,
    /** True for the start of the activity, false for its end. */
    val begun: Boolean,
)

/**
 * What the device's own sensors say about whether it is going anywhere.
 *
 * The answer is carried with a note of whether it is an answer at all: before
 * the first transition arrives, and where the permission was refused, standing
 * still is what the modes are told, because the slow mode is the safe one to
 * be in — but nothing may be concluded from it about a fix. The two cases read
 * the same and have to be told apart by whoever asks.
 */
data class Moving(
    /** Whether the device is being carried somewhere. */
    val going: Boolean,
    /** Whether the sensors have said so, rather than this being the answer given until they do. */
    val told: Boolean,
)

/**
 * Whether the device counts as moving once these changes have arrived.
 *
 * The end of standing still is not the beginning of going somewhere: the
 * platform can report leaving one activity before it says which one was
 * entered, and answering that first half alone would put every device picked up
 * off a table into the faster mode.
 *
 * Changes arrive in order and the last one wins, because that is what the
 * device is doing now; several of them can come in a single message after a
 * spell with the screen off.
 */
fun movingAfter(changes: List<ActivityChange>, before: Boolean = false): Boolean =
    changes.fold(before) { moving, change ->
        when {
            change.begun -> change.travelling

            // Travelling has ended and nothing has been entered yet: standing
            // still is the safe answer, since the slow mode costs a late fix
            // while the fast one costs the battery of somebody sitting down.
            change.travelling -> false

            else -> moving
        }
    }
