// Who is looking, out of the claims lying in the database.

/**
 * The members still looking, each of them named once.
 *
 * A member watching from two places — a phone in one hand and this page on a
 * laptop — leaves two claims behind, and the strip has one person to name for
 * them. Claims that have run out are dropped here rather than as they arrive,
 * because nothing arrives at the moment one expires: the list is worked out
 * again as the clock moves.
 *
 * The Android client works the same list out the same way, so that one
 * household reads the same on both.
 */
export function watchingMembers(watchers, now) {
    const live = watchers.filter((watcher) => watcher.until > now);

    return [...new Set(live.map((watcher) => watcher.memberId))].sort();
}
