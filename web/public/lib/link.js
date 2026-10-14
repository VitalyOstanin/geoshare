// Whether the database is answering, out of what the store says of each
// snapshot of one document the page keeps watching.
//
// Firestore says nothing of a lost connection: a listener goes on being
// called, only with what the page held before, marked as coming from the
// cache. A network that is up is no answer either, since a device behind a
// tunnel can reach the network and not the database. So the link is judged by
// the snapshots themselves: the server confirming one is the database
// answering, and LINK_GRACE_MS of nothing but the cache is its silence.

/** How long the page waits on the cache alone before saying the database is silent. */
export const LINK_GRACE_MS = 10_000;

/** How long an action waits for the database before saying it did not answer. */
const ANSWER_WAIT_MS = 15_000;

/**
 * The link before any snapshot: the page has asked and heard nothing, so the
 * waiting starts at once and a database that never answers is told of too.
 */
export function linkFrom(now) {
    return { confirmedAt: null, cachedSince: now };
}

/** The link after one more snapshot, served from the cache or confirmed by the server. */
export function nextLink(link, fromCache, now) {
    if (!fromCache) {
        return { confirmedAt: now, cachedSince: null };
    }

    return { confirmedAt: link.confirmedAt, cachedSince: link.cachedSince ?? now };
}

/** Whether the database has been silent for longer than LINK_GRACE_MS. */
export function linkLost(link, now) {
    return link.cachedSince !== null && now - link.cachedSince >= LINK_GRACE_MS;
}

/** When linkLost turns true for this link, or null when nothing is being waited for. */
export function linkLostAt(link) {
    return link.cachedSince === null ? null : link.cachedSince + LINK_GRACE_MS;
}

/**
 * The write, or a refusal with the code 'unanswered' once ANSWER_WAIT_MS have
 * passed without the server acknowledging it.
 *
 * The write itself is not called off — Firestore keeps it and sends it when
 * the database is back — only the screen stops waiting for it.
 */
export function withinAnswerWait(write, wait = ANSWER_WAIT_MS) {
    let timer;
    const late = new Promise((_, reject) => {
        timer = setTimeout(() => {
            const error = new Error('the database did not answer');
            error.code = 'unanswered';
            reject(error);
        }, wait);
    });

    return Promise.race([write, late]).finally(() => clearTimeout(timer));
}
