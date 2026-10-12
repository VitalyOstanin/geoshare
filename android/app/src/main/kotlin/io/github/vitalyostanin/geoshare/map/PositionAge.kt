package io.github.vitalyostanin.geoshare.map

import io.github.vitalyostanin.geoshare.collection.STALE_AFTER
import java.time.Duration
import java.time.Instant

/**
 * How old a position is, in the unit worth naming.
 *
 * A count and a unit rather than a finished string: the wording belongs to the
 * resources, which have the plural rules, and this way the choice of unit can
 * be tested without them.
 */
sealed interface PositionAge {
    /** Younger than a minute, or taken by a device whose clock runs ahead. */
    data object JustNow : PositionAge

    data class Minutes(val count: Long) : PositionAge

    data class Hours(val count: Long) : PositionAge

    data class Days(val count: Long) : PositionAge
}

fun positionAge(knownAt: Instant, now: Instant): PositionAge {
    val age = Duration.between(knownAt, now)

    return when {
        age < Duration.ofMinutes(1) -> PositionAge.JustNow
        age < Duration.ofHours(1) -> PositionAge.Minutes(age.toMinutes())
        age < Duration.ofDays(1) -> PositionAge.Hours(age.toHours())
        else -> PositionAge.Days(age.toDays())
    }
}

fun isStale(knownAt: Instant, now: Instant): Boolean = Duration.between(knownAt, now) >= STALE_AFTER

/**
 * Whether the device is still reaching the server but has had no position it
 * believed for as long as it takes a marker to go grey.
 *
 * That is a marker stuck where it last was: every answer since has been left
 * out, and the card should say so rather than leave the viewer to compare two
 * clock times. A device gone quiet is not stuck, only old, and the grey already
 * says that. The same mark as the grey on purpose, so that the page and this
 * screen need no second number to agree on.
 */
fun isStuck(positionAt: Instant, knownAt: Instant, now: Instant): Boolean =
    !isStale(knownAt, now) && Duration.between(positionAt, knownAt) >= STALE_AFTER
