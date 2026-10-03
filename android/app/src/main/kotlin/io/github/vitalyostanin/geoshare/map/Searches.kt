package io.github.vitalyostanin.geoshare.map

import java.time.Duration
import java.time.Instant

// The group looking for one phone of a member, out of the searches lying in
// the database.
//
// While a search stands, the rules open that phone's place to everybody in
// the group; these are the pieces of the application that say so. A search is
// kept by its own phone's id rather than by the member it belongs to, because
// several may stand at once: different phones of different members, or two
// phones of one member lost together. The end is counted from the last
// renewal by the server's clock, which is what the rules count from too, so
// the phone and the rules agree on when the place closes.

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
    /** The phone this search stands for: the document's own id. */
    val phoneId: String,
    /** Whose phone it is, the part of [phoneId] before the dot. */
    val memberId: String,
    /** The phone's name as the administrator saw it when they opened or renewed the search. */
    val phoneName: String?,
    val openedBy: String,
    val renewedBy: String,
    val startedAt: Instant,
    val endsAt: Instant,
)

/**
 * A document turns into a search, or into nothing.
 *
 * A [phoneId] with no dot in it names no phone — an old, per-member document
 * from before a search moved to one per phone — and is read the same as one
 * the rules would refuse outright: dropped rather than guessed at.
 *
 * The times are read the way the subscription hands them over: a renewal
 * written a moment ago carries the server's estimate until the server
 * answers, which is close enough to draw an end by.
 */
fun searchOf(
    phoneId: String,
    phoneName: String?,
    openedBy: String?,
    renewedBy: String?,
    startedAt: Instant?,
    renewedAt: Instant?,
    hours: Int?,
): Search? {
    val memberId = phoneId.substringBefore('.', missingDelimiterValue = "")

    if (memberId.isEmpty() || openedBy == null || renewedAt == null || hours == null || hours < 1) {
        return null
    }

    return Search(
        phoneId = phoneId,
        memberId = memberId,
        phoneName = phoneName,
        openedBy = openedBy,
        renewedBy = renewedBy ?: openedBy,
        startedAt = startedAt ?: renewedAt,
        endsAt = renewedAt.plus(Duration.ofHours(hours.toLong())),
    )
}

/** Whether a search still stands, at [now]. */
fun Search.stands(now: Instant): Boolean = endsAt.isAfter(now)

/**
 * The searches still standing, by phone.
 *
 * One that has run out is dropped here rather than when it is deleted: nothing
 * deletes it, and nothing arrives at the moment it runs out, so the list is
 * worked out again as the clock moves.
 */
fun standingSearches(searches: Map<String, Search>, now: Instant): Map<String, Search> =
    searches.filterValues { it.stands(now) }

/** The phones the group is looking for right now, out of every search it holds. */
fun soughtPhones(searches: Map<String, Search>, now: Instant): Set<String> =
    standingSearches(searches, now).keys

/** The end a choice of hours would have, if the search were opened or renewed now. */
fun endIfChosen(hours: Int, now: Instant): Instant = now.plus(Duration.ofHours(hours.toLong()))
