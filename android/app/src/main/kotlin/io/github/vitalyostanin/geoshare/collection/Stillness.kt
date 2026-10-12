package io.github.vitalyostanin.geoshare.collection

import java.time.Instant

// What the device's own sensors say about a fix.
//
// Every other rule in this package weighs one answer of the receiver against
// another, and a receiver being fed a false signal answers consistently: the
// road it draws is plausible, and drawn slowly enough it breaks none of them.
// The step counter and the accelerometer are not on that signal. They are the
// one thing here that a spoofer cannot write to, and all they are asked is
// whether the device went anywhere at all.

/**
 * How far a device may appear to move while its own sensors say it is standing
 * still.
 *
 * A kilometre. Short of that the answer is somebody walking round a building
 * with a device the recogniser has not called a walk yet, or a receiver being
 * imprecise in a courtyard; past it, the device would have had to be carried,
 * and being carried is the one thing the sensors do notice.
 */
const val STILL_DRIFT_METRES: Double = 1_000.0

/**
 * Whether a fix says the device has travelled while its sensors say it has not
 * moved at all.
 *
 * The stillness has to cover the whole way from the last position to this one:
 * a device that was told it stopped moving a minute ago says nothing about
 * where it was ten minutes before that, and the rule would then throw away the
 * first true fix after a journey.
 *
 * [stillSince] is null whenever the answer is not the sensors talking — the
 * permission was refused, or nothing has been reported yet — and the rule then
 * says nothing rather than guessing.
 */
fun movedWhileStanding(stillSince: Instant?, last: Fix, next: Fix): Boolean {
    if (stillSince == null || stillSince.isAfter(last.takenAt)) {
        return false
    }

    val doubt = (last.accuracyMetres ?: 0.0) + (next.accuracyMetres ?: 0.0)

    return distanceMetres(last, next) > maxOf(STILL_DRIFT_METRES, doubt)
}
