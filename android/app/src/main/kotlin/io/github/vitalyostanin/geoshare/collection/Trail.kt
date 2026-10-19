package io.github.vitalyostanin.geoshare.collection

import java.time.Duration
import java.time.Instant

// The places the device has actually been, as far as it was ever believed.
//
// The rules that catch a jump look at two fixes: the last one written and the
// one that just arrived. A spoofed signal is indistinguishable from the truth
// at that scale — it draws a plausible road — and differs only in not fitting
// with the rest of what the device knows. This is that rest.
//
// What goes in is positions that were written, never answers that arrived. The
// distinction is the whole safety of it: an answer left out as a jump never
// becomes part of what the device considers familiar, so an hour of spoofing
// does not teach the device that the false place is home.

/** The places the device has been, oldest first. */
data class Trail(val fixes: List<Fix> = emptyList())

/**
 * How many places are kept.
 *
 * Fifty, because what the trail is asked is whether a place is familiar, and
 * that question is answered by a handful of visits rather than by a record of
 * every one of them.
 */
const val TRAIL_LENGTH: Int = 50

/** How far back the trail reaches before a place stops counting as familiar. */
val TRAIL_REMEMBERS: Duration = Duration.ofDays(3)

/** How close to a remembered place a fix has to fall to be in the same place. */
const val FAMILIAR_METRES: Double = 300.0

/**
 * How many remembered visits make a place familiar.
 *
 * Three, and they have to be spread over [FAMILIAR_SPREAD]: one written
 * position proves only that the device once believed something, while three
 * across an hour is a place the device keeps coming back to.
 */
const val FAMILIAR_ENOUGH: Int = 3

/** Over how long those visits have to be spread. */
val FAMILIAR_SPREAD: Duration = Duration.ofHours(1)

/** The trail with one more place in it, the oldest and the stalest dropped. */
fun Trail.remembering(fix: Fix): Trail {
    val fresh =
        (fixes + fix).filter {
            Duration.between(it.takenAt, fix.takenAt) <= TRAIL_REMEMBERS
        }

    return Trail(fresh.takeLast(TRAIL_LENGTH))
}

/**
 * Whether the device has been at this place often enough, and over long enough,
 * for the place to be one of its own.
 */
fun Trail.knows(place: Fix): Boolean {
    val visits = fixes.filter { distanceMetres(it, place) <= FAMILIAR_METRES }

    if (visits.size < FAMILIAR_ENOUGH) {
        return false
    }

    // The fixes are in the order they were written, not the order they were
    // taken: a network answer carries the device clock and a satellite one the
    // GNSS time, so the first and the last of them may be neither oldest nor
    // newest.
    val oldest = visits.minOf { it.takenAt }
    val newest = visits.maxOf { it.takenAt }

    return Duration.between(oldest, newest) >= FAMILIAR_SPREAD
}

/**
 * Whether a fix the rules would drop is in fact the device coming home.
 *
 * The case this answers is the anchor left standing somewhere the device has
 * long left — the failure the jump filter causes by its own logic, since a fix
 * left out does not move the anchor, and a wrong anchor turns every true fix
 * that follows into a jump. Waiting it out works only where the rule that
 * caught the fix is the speed, which falls as the clock runs; a width does not
 * fall, and neither does an anchor sitting inside a false place.
 *
 * Coming back to a place the device has been to three times over an hour is not
 * something a spoofer can arrange, because those three times are positions
 * that were written — and while the spoofing runs, nothing is.
 */
fun comingHome(trail: Trail, last: Fix?, next: Fix): Boolean {
    if (last == null) {
        return false
    }

    if (distanceMetres(last, next) <= FAMILIAR_METRES) {
        return false
    }

    return trail.knows(next)
}

/**
 * How long a remembered position may stand in for the anchor after a restart.
 *
 * The anchor is what the speed rule measures against, and an old one weakens
 * it by arithmetic: seventy kilometres in a day is a walk. An hour keeps the
 * rule sharp — a device that really moved in that hour is believed as soon as
 * the journey has had time to happen — while covering the case this is for, a
 * process killed and restarted inside a spoofing episode.
 */
val ANCHOR_KEEPS: Duration = Duration.ofHours(1)

/**
 * How long an anchor may stand when it sits in a place the device keeps coming
 * back to.
 *
 * Half a day, against the hour a single position is worth. A lone written
 * position says where the device was once; three of them spread over an hour
 * say where the device lives, and a device that was at home last night is
 * overwhelmingly at home this morning.
 *
 * What an anchor this old is worth is worth being plain about: the speed rule
 * divides by the age, so half a day of it forgives any journey on earth, and
 * this buys nothing there. What it does buy is the rules that do not divide by
 * anything — the width of the answer, the two places at one time, and the
 * sensors saying the device was never carried — which have nothing to measure
 * against at all until some anchor stands.
 */
val FAMILIAR_ANCHOR_KEEPS: Duration = Duration.ofHours(12)

/**
 * The place to measure from after a restart, or nothing when it is too old to
 * trust.
 *
 * A position of the last hour stands on its own. Older than that it has to be
 * in a place the device counts as its own, and it stands then because the trail
 * says the device belongs there rather than because the position is fresh.
 */
fun anchorFrom(trail: Trail, now: Instant): Fix? {
    val last = trail.fixes.lastOrNull() ?: return null
    val age = Duration.between(last.takenAt, now)

    if (age <= ANCHOR_KEEPS) {
        return last
    }

    if (age > FAMILIAR_ANCHOR_KEEPS || !trail.knows(last)) {
        return null
    }

    return last
}
