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
    /** Younger than a minute, or taken by a phone whose clock runs ahead. */
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
