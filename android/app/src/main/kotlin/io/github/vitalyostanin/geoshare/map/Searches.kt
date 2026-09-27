package io.github.vitalyostanin.geoshare.map

import java.time.Duration
import java.time.Instant

// The group looking for a member, out of the searches lying in the database.
//
// While a search stands, the rules open the member's place to everybody in the
// group; these are the pieces of the application that say so. The end is
// counted from the last renewal by the server's clock, which is what the rules
// count from too, so the phone and the rules agree on when the place closes.

/** The lengths an administrator chooses from, in hours: six hours, a day, three days. */
val SEARCH_HOURS: List<Int> = listOf(6, 24, 72)

/** What the choice starts on: long enough to find somebody, short enough to be forgotten safely. */
const val DEFAULT_SEARCH_HOURS: Int = 24

/**
 * One search as the phone holds it, out of what the document says.
 *
 * [endsAt] rather than the hours themselves, because that is what every screen
 * actually reads: worked out once here instead of by each of them out of
 * [renewedBy]'s own renewal.
 */
data class Search(
    val memberId: String,
    val openedBy: String,
    val renewedBy: String,
    val startedAt: Instant,
    val endsAt: Instant,
)

/**
 * A document turns into a search, or into nothing.
 *
 * The times are read the way the subscription hands them over: a renewal
 * written a moment ago carries the server's estimate until the server
 * answers, which is close enough to draw an end by.
 */
fun searchOf(
    memberId: String,
    openedBy: String?,
    renewedBy: String?,
    startedAt: Instant?,
    renewedAt: Instant?,
    hours: Int?,
): Search? {
    if (openedBy == null || renewedAt == null || hours == null || hours < 1) {
        return null
    }

    return Search(
        memberId = memberId,
        openedBy = openedBy,
        renewedBy = renewedBy ?: openedBy,
        startedAt = startedAt ?: renewedAt,
        endsAt = renewedAt.plus(Duration.ofHours(hours.toLong())),
    )
}

/** Whether a search still stands, at [now]. */
fun Search.stands(now: Instant): Boolean = endsAt.isAfter(now)

/**
 * The searches still standing, by member.
 *
 * One that has run out is dropped here rather than when it is deleted: nothing
 * deletes it, and nothing arrives at the moment it runs out, so the list is
 * worked out again as the clock moves.
 */
fun standingSearches(searches: Map<String, Search>, now: Instant): Map<String, Search> =
    searches.filterValues { it.stands(now) }

/** The members the group is looking for right now, out of every search it holds. */
fun soughtMembers(searches: Map<String, Search>, now: Instant): Set<String> =
    standingSearches(searches, now).keys

/**
 * When the next of the standing searches runs out, or null when none does: the
 * moment to look at the list again, since nothing else will say so.
 */
fun nextSearchEnd(searches: Map<String, Search>, now: Instant): Instant? =
    standingSearches(searches, now).values.minOfOrNull { it.endsAt }

/** The end a choice of hours would have, if the search were opened or renewed now. */
fun endIfChosen(hours: Int, now: Instant): Instant = now.plus(Duration.ofHours(hours.toLong()))
