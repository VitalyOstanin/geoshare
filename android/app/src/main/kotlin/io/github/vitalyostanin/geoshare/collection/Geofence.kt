package io.github.vitalyostanin.geoshare.collection

import java.time.Duration

/**
 * How far from the place it last wrote down a phone may drift and still count
 * as standing there.
 *
 * The platform asks for a hundred metres at the very least and recommends a
 * hundred and fifty where the fence is meant to hold: a crossing is decided by
 * whatever fix the system happens to have, and a fence no wider than the doubt
 * in that fix would report a phone on a table leaving and coming back all
 * afternoon.
 */
const val GEOFENCE_RADIUS_METRES: Double = 150.0

/**
 * How long the phone has to stay inside the fence before it counts as settled.
 *
 * Entering is not standing still: a phone carried through the yard is inside
 * for a minute and out again, and switching the collection off for it would
 * lose the walk. The mode that asks for nothing is entered only after the
 * phone has stayed put for this long.
 */
val GEOFENCE_SETTLING: Duration = Duration.ofMinutes(5)

/**
 * How long the platform may sit on a crossing before it tells us.
 *
 * The platform's own recommendation for saving battery: a looser deadline lets
 * it fold the check into work it was doing anyway. Five minutes of lateness
 * costs nothing here, because what waits on the answer is the choice between
 * two slow modes.
 */
val GEOFENCE_RESPONSIVENESS: Duration = Duration.ofMinutes(5)

/**
 * How often a phone that asks for nothing says it is still there.
 *
 * [CollectionMode.Idle] collects no positions at all, so nothing would be
 * written and the card would go grey on everybody else's map within
 * [STALE_AFTER] — the phone would look stopped precisely because it was
 * sitting quietly at home. The last known position is written again on this
 * beat instead, with five minutes to spare before the grey.
 */
val IDLE_REPEAT: Duration = STALE_AFTER.minus(Duration.ofMinutes(5))

/** What the platform noticed about the fence around the last written position. */
enum class Fencing {
    Entered,
    Dwelt,
    Left,
}

/**
 * Whether the phone counts as standing where it last wrote, once these
 * crossings have arrived.
 *
 * Several can arrive together after a spell with the screen off, and the last
 * one is what holds now. Entering changes nothing by itself — only staying
 * does, which is what [Fencing.Dwelt] says.
 */
fun dwellingAfter(changes: List<Fencing>, before: Boolean = false): Boolean =
    changes.fold(before) { dwelling, change ->
        when (change) {
            Fencing.Dwelt -> true
            Fencing.Left -> false
            Fencing.Entered -> dwelling
        }
    }

/**
 * Whether a freshly written position calls for the fence to be moved.
 *
 * Every write would otherwise rebuild the fence, and rebuilding it throws away
 * the very thing being waited for: the phone would have to settle again from
 * the start, and a member walking about the house would never reach the quiet
 * mode. A position still inside the fence is what the fence already says.
 */
fun fenceMoves(centre: Fix?, written: Fix): Boolean =
    centre == null || distanceMetres(centre, written) >= GEOFENCE_RADIUS_METRES
