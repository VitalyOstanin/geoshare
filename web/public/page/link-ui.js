import { linkFrom, linkLost, linkLostAt, nextLink } from '../lib/link.js';
import { watchLink } from '../lib/store.js';
import { SPEAKING, WORDS } from '../lib/words.js';
import { elements, state } from './state.js';

/**
 * Follows the link to the database while signed in, and calls [onChange]
 * whenever it is lost or back: the banner, the grey markers and the notes in
 * the rows all follow from that one change.
 */
export function followLink(onChange) {
    if (state.stopLink) {
        return;
    }

    state.link = linkFrom(Date.now());
    state.linkShown = false;
    state.linkChanged = onChange;
    state.stopLink = watchLink((fromCache) => {
        state.link = nextLink(state.link, fromCache, Date.now());
        judgeLink();
    });
    judgeLink();
}

export function unfollowLink() {
    state.stopLink?.();
    state.stopLink = null;
    clearTimeout(state.linkTimer);
    state.link = null;
    state.linkShown = false;
    drawLink();
}

/** Whether the database has been silent long enough to say so. */
export function linkIsLost() {
    return state.link !== null && linkLost(state.link, Date.now());
}

/**
 * What a row adds to an age while the database is silent: the ages are of
 * what the page last heard, not of now. Empty while the link holds.
 */
export function knownNote() {
    if (!linkIsLost() || state.link.confirmedAt === null) {
        return '';
    }

    return WORDS.knownAt(clockText(state.link.confirmedAt));
}

/** What a failed write says: a silent database, or one that refused. */
export function troubleOf(error) {
    return error?.code === 'unanswered' ? WORDS.unanswered : WORDS.groupRefused;
}

function judgeLink() {
    clearTimeout(state.linkTimer);

    const at = linkLostAt(state.link);

    if (at !== null && at > Date.now()) {
        state.linkTimer = setTimeout(judgeLink, at - Date.now());
    }

    const lost = linkIsLost();

    if (lost !== state.linkShown) {
        state.linkShown = lost;
        drawLink();
        state.linkChanged?.();
    }
}

function drawLink() {
    const lost = state.linkShown;

    elements.linkBanner.hidden = !lost;
    document.documentElement.classList.toggle('link-lost', lost);

    if (!lost) {
        return;
    }

    elements.linkText.textContent = WORDS.linkLost(clockText(state.link.cachedSince));
    elements.linkNote.textContent = state.link.confirmedAt === null
        ? WORDS.linkKnownNever
        : WORDS.linkKnownAt(clockText(state.link.confirmedAt));
}

/** A time of day, with the date as well once it is not today's. */
function clockText(millis) {
    const then = new Date(millis);
    const today = then.toDateString() === new Date().toDateString();

    return then.toLocaleString(SPEAKING, today
        ? { hour: '2-digit', minute: '2-digit' }
        : { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
}
