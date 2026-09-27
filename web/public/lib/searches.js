// The group looking for a member, out of the searches lying in the database.
//
// While a search stands, the rules open the member's place to everybody in the
// group; these are the pieces of the page that say so. The end is counted from
// the last renewal by the server's clock, which is what the rules count from
// too, so the page and the rules agree on when the place closes.

/** The lengths an administrator chooses from, in hours: six hours, a day, three days. */
export const SEARCH_HOURS = [6, 24, 72];

/** What the choice starts on: long enough to find somebody, short enough to be forgotten safely. */
export const DEFAULT_SEARCH_HOURS = 24;

const HOUR_MS = 60 * 60 * 1000;

/**
 * One search as the page holds it, out of what the document says, or null
 * for a document that is not one.
 *
 * The times are read the way the store hands them over, in milliseconds; a
 * renewal written a moment ago carries the server's estimate until the server
 * answers, which is close enough to draw an end by.
 */
export function searchOf(memberId, data) {
    const renewedAt = data?.renewedAt;
    const hours = data?.hours;

    if (
        typeof memberId !== 'string' ||
        typeof data?.openedBy !== 'string' ||
        !Number.isFinite(renewedAt) ||
        !Number.isInteger(hours) ||
        hours < 1
    ) {
        return null;
    }

    return {
        memberId,
        openedBy: data.openedBy,
        renewedBy: typeof data.renewedBy === 'string' ? data.renewedBy : data.openedBy,
        startedAt: Number.isFinite(data.startedAt) ? data.startedAt : renewedAt,
        endsAt: renewedAt + hours * HOUR_MS,
    };
}

/**
 * The searches still standing, by member.
 *
 * One that has run out is dropped here rather than when it is deleted:
 * nothing deletes it, and nothing arrives at the moment it runs out, so the
 * list is worked out again as the clock moves.
 */
export function standingSearches(searches, now) {
    return new Map([...searches].filter(([, search]) => search.endsAt > now));
}

/**
 * The positions with the search for their member laid on them, as `sought`,
 * so that the pin, the strip and the card mark the same people.
 */
export function withSearches(positions, searches, now) {
    const standing = standingSearches(searches, now);

    return positions.map((position) => {
        const search = standing.get(position.memberId) ?? null;

        return search === null && !position.sought ? position : { ...position, sought: search };
    });
}

/**
 * When the next of the standing searches runs out, or null when none does:
 * the moment to work the list out again, since nothing else will say so.
 */
export function nextSearchEnd(searches, now) {
    const ends = [...standingSearches(searches, now).values()].map((search) => search.endsAt);

    return ends.length === 0 ? null : Math.min(...ends);
}

/** The end a choice of hours would have, if the search were opened or renewed now. */
export function endIfChosen(hours, now) {
    return now + hours * HOUR_MS;
}
