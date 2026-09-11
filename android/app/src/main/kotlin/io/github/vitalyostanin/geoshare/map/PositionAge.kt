package io.github.vitalyostanin.geoshare.map

import java.time.Duration
import java.time.Instant

/**
 * Past this, a position is old enough that the map says so in grey.
 *
 * Well beyond the standby interval of fifteen minutes, so an ordinary quiet
 * phone never looks broken, and well short of an hour, so a phone that has
 * actually stopped sharing is noticed within one.
 */
val STALE_AFTER: Duration = Duration.ofMinutes(30)

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
