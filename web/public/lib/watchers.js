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

/**
 * The members to name besides this page, when this page is looking itself.
 *
 * The page holding the phones in the viewing mode used to go unnamed, and a
 * viewer who saw every phone of the group viewing with nobody listed could not
 * tell what was holding them. Named first and on its own; the viewer's own
 * account watching from somewhere else as well is folded into it, because
 * either claim holds the phones and one "you" says so.
 */
export function watchingBesides(members, ownMemberId, pageLooking) {
    return pageLooking ? members.filter((memberId) => memberId !== ownMemberId) : members;
}
