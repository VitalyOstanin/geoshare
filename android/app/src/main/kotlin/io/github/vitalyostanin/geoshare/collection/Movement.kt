package io.github.vitalyostanin.geoshare.collection

/**
 * A change the phone noticed in what its owner is doing.
 *
 * Two plain facts rather than the platform's pair of numbers: the choice of
 * mode cares only whether the activity carries the phone somewhere, and
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
 * Whether the phone counts as moving once these changes have arrived.
 *
 * The end of standing still is not the beginning of going somewhere: the
 * platform can report leaving one activity before it says which one was
 * entered, and answering that first half alone would put every phone picked up
 * off a table into the faster mode.
 *
 * Changes arrive in order and the last one wins, because that is what the
 * phone is doing now; several of them can come in a single message after a
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
