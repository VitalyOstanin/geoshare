// Looking for a phone: the window that opens one, the line for the one sought, the list.

import { memberName } from '../lib/members.js';
import {
    DEFAULT_SEARCH_HOURS,
    endIfChosen,
    nextSearchEnd,
    standingSearches,
    SEARCH_HOURS,
} from '../lib/searches.js';
import { SPEAKING, WORDS } from '../lib/words.js';
import { ADMINISTRATOR, standingIn } from '../lib/membership.js';
import { endSearch, openSearch, renewSearch } from '../lib/store.js';
import { elements, state } from './state.js';
import { linkIsLost, troubleOf } from './link-ui.js';
import { followPositions, showPositions } from '../app.js';
import { drawGroup, tryGroupWrite } from './group-panel.js';
import { render } from './strip.js';

/**
 * What a change to the searches changes: which phones this viewer may read,
 * so the positions are followed again; who is marked on the map and in the
 * strip; what the panel offers; and what this viewer is told about their own
 * phones.
 *
 * Also run when the next standing search runs out, since nothing arrives from
 * the database at that moment.
 */
export function searchesChanged() {
    const now = Date.now();

    clearTimeout(state.searchEnds);

    const next = nextSearchEnd(state.searches, now);

    if (next !== null) {
        // A second past the end, so that the rules and this page agree the
        // search is over by the time the positions are asked for again.
        state.searchEnds = setTimeout(searchesChanged, next - now + 1000);
    }

    followPositions(state.grants);
    showPositions();
    drawSought();
    drawGroup();
    render();
}

/** A moment a search ends at, the way a person reads a date and a time. */
export function untilText(millis) {
    return new Date(millis).toLocaleString(SPEAKING, {
        day: '2-digit',
        month: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
    });
}

/** What the group calls a member, for the sentences about a search. */
function searchName(memberId) {
    return memberName(state.profiles?.get(memberId), memberId, false);
}

/**
 * The best name known for a search's phone: this viewer's own label and the
 * phone's own report both already won over the search's, inside the position
 * that being searched has just opened; failing that, what the search itself
 * remembers, and failing that, nothing to call it but "device".
 */
function phoneNameForSearch(search) {
    return state.positions.find((position) => position.positionId === search.phoneId)?.deviceName
        ?? search.phoneName
        ?? WORDS.unnamedDevice;
}

/** Who opened a search, said to the one reading it. */
function openedByText(search) {
    return search.openedBy === state.member?.memberId
        ? WORDS.searchOpenedByYou
        : WORDS.searchOpenedBy(searchName(search.openedBy));
}

function runningTheGroup() {
    return (
        state.member !== null &&
        standingIn(state.roster, state.member.memberId) === ADMINISTRATOR
    );
}

/**
 * The line a card about a member looked for carries: until when, who opened
 * the search, and to an administrator the button that ends it.
 */
export function soughtLine(position) {
    const search = position.sought;

    if (!search) {
        return null;
    }

    const line = document.createElement('div');

    line.className = 'detail-sought';

    const until = document.createElement('b');

    until.textContent = WORDS.soughtUntil(untilText(search.endsAt));

    const who = document.createElement('span');

    who.textContent = openedByText(search);
    line.append(until, who);

    if (runningTheGroup()) {
        const found = document.createElement('button');

        found.type = 'button';
        found.className = 'alarm-fill';
        found.textContent = WORDS.searchFound;
        found.addEventListener('click', () => tryGroupWrite(() => endSearch(search.phoneId)));
        line.append(found);
    }

    return line;
}

/**
 * Told to the member whose phone or phones are being looked for: which of
 * them, until when, and a way to end every one of them at once — more than
 * one may stand together, the family looking for two phones lost side by
 * side, and "found" answers for both.
 */
export function drawSought() {
    const own = state.member
        ? [...standingSearches(state.searches, Date.now()).values()]
            .filter((search) => search.memberId === state.member.memberId)
        : [];

    elements.soughtBanner.hidden = own.length === 0;

    if (own.length === 0) {
        return;
    }

    const phoneNames = own.map((search) => phoneNameForSearch(search)).join(', ');
    const until = Math.max(...own.map((search) => search.endsAt));
    const openers = [...new Set(own.map((search) => search.openedBy))]
        .map((memberId) => searchName(memberId))
        .join(', ');

    elements.soughtText.textContent = WORDS.soughtBanner(phoneNames, untilText(until));
    elements.soughtNote.textContent = WORDS.soughtBannerNote(openers);
    elements.soughtFound.textContent = WORDS.soughtFound;
}

elements.soughtFound.addEventListener('click', async () => {
    const own = [...standingSearches(state.searches, Date.now()).values()]
        .filter((search) => search.memberId === state.member.memberId);

    elements.soughtFound.disabled = true;

    try {
        await Promise.all(own.map((search) => endSearch(search.phoneId)));
    } catch {
        // The banner stays up, which says a search is still on; the next
        // press tries again.
    } finally {
        elements.soughtFound.disabled = false;
    }
});

/** The searches standing now, as the panel lists them under the rows. */
export function adminSearch(search) {
    const block = document.createElement('div');

    block.className = 'admin-search';

    const title = document.createElement('b');

    title.textContent = WORDS.searchSummary(
        phoneNameForSearch(search),
        searchName(search.memberId),
        untilText(search.endsAt),
    );

    const who = document.createElement('small');

    who.textContent = openedByText(search);

    const buttons = document.createElement('div');
    const renew = document.createElement('button');

    renew.type = 'button';
    renew.textContent = WORDS.searchRenew;
    renew.addEventListener('click', () => {
        askSearch(search.phoneId, search.memberId, phoneNameForSearch(search), true);
    });

    const found = document.createElement('button');

    found.type = 'button';
    found.className = 'alarm-fill';
    found.textContent = WORDS.searchFound;
    found.addEventListener('click', () => tryGroupWrite(() => endSearch(search.phoneId)));

    buttons.append(renew, found);
    block.append(title, who, buttons);

    return block;
}

/**
 * Opens the window that opens a search for one phone, or renews one: what
 * happens, for how long, and the end the length picked comes to.
 */
export function askSearch(phoneId, memberId, phoneName, renew) {
    state.searchAsk = { phoneId, memberId, phoneName, renew, hours: DEFAULT_SEARCH_HOURS };
    drawSearchAsk();

    if (!elements.search.open) {
        elements.search.showModal();
    }
}

export function drawSearchAsk() {
    const ask = state.searchAsk;

    if (!ask) {
        return;
    }

    const phoneLabel = ask.phoneName ?? WORDS.unnamedDevice;
    const memberLabel = searchName(ask.memberId);

    elements.searchTitle.textContent = ask.renew
        ? WORDS.searchRenewTitle(phoneLabel, memberLabel)
        : WORDS.searchOpenTitle(phoneLabel, memberLabel);
    elements.searchNote.textContent = WORDS.searchNote;
    elements.searchHours.replaceChildren(
        ...SEARCH_HOURS.map((hours) => {
            const button = document.createElement('button');

            button.type = 'button';
            button.textContent = WORDS.searchHours(hours);
            button.setAttribute('aria-pressed', String(hours === ask.hours));
            button.addEventListener('click', () => {
                ask.hours = hours;
                drawSearchAsk();
            });

            return button;
        }),
    );
    elements.searchUntil.textContent = WORDS.searchUntil(
        untilText(endIfChosen(ask.hours, Date.now())),
    );
    elements.searchGo.textContent = ask.renew ? WORDS.searchRenew : WORDS.searchGo;
    elements.searchCancel.textContent = WORDS.searchCancel;
    // Asked again whenever the link changes, so a window left open follows it.
    elements.searchGo.disabled = linkIsLost() || ask.sending === true;
    elements.searchError.textContent = linkIsLost() ? WORDS.linkLostAction : ask.trouble ?? '';
    elements.searchError.hidden = elements.searchError.textContent === '';
}

export function closeSearch() {
    state.searchAsk = null;
    elements.searchError.hidden = true;
    elements.searchGo.disabled = false;

    if (elements.search.open) {
        elements.search.close();
    }
}

/**
 * Writes the search and waits for the database: a refusal stays on the
 * window, where the administrator is still looking, rather than closing it
 * on a search that never started.
 */
elements.searchGo.addEventListener('click', async () => {
    const ask = state.searchAsk;

    if (!ask) {
        return;
    }

    const adminId = state.member.memberId;

    if (linkIsLost()) {
        return;
    }

    ask.sending = true;
    ask.trouble = null;
    drawSearchAsk();

    try {
        await (ask.renew
            ? renewSearch(ask.phoneId, adminId, ask.hours, ask.phoneName)
            : openSearch(ask.phoneId, adminId, ask.hours, ask.phoneName));

        if (state.searchAsk === ask) {
            closeSearch();
        }
    } catch (error) {
        ask.sending = error?.code === 'unanswered';
        ask.trouble = troubleOf(error);
        drawSearchAsk();
    }
});

elements.searchCancel.addEventListener('click', closeSearch);
elements.search.addEventListener('close', () => {
    state.searchAsk = null;
});
