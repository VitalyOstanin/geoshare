// The card over the map: one member's detail, or the list of who stands under
// a marker that covers several of them.
//
// Split out of map.js because the card is a dialogue with the viewer rather
// than a drawing on the map — everything here builds plain DOM into the
// element the caller handed to createMap, and reads the map's own state
// (view.markers, view.selection) rather than drawing on it.

import { pinColour, shortName } from './members.js';
import { showFace } from './faces.js';
import { ageText, isStuck, positionAge, stuckCause, timeText } from './age.js';
import { isGrey } from './false-places.js';
import { openModeHelp } from './mode-help.js';
import { openReasonHelp } from './reason-help.js';
import { listShown, openMember } from './selection.js';
import { modeText } from './modes.js';
import { widthIsWide, widthText } from './width.js';
import { foundText, leftOutText } from './found.js';
import { distanceFromViewer, distanceText } from './distance.js';
import { percentText } from './percent.js';
import { SPEAKING, WORDS } from './words.js';
import { redrawSelection } from './map.js';
import { bringTheRingIntoSight, setFollowTarget } from './map-follow.js';
import { keep, readKept } from './kept.js';

/** Closes whatever card is open, and says whether there was one. */
export function closeCards(view) {
    if (view.selection === null) {
        return false;
    }

    view.selection = null;
    view.card.hidden = true;
    view.card.replaceChildren();

    return true;
}

/** What a marker for several members writes above itself. */
export function groupLabel(count) {
    return count === 1 ? WORDS.oneMember : WORDS.members(count);
}

/**
 * Draws the one card the whole page ever shows at once: one member's detail,
 * or the list of who is standing under a marker that covers several of them.
 *
 * Which of the group was meant is a question only the viewer can answer, so
 * the list comes first and the detail is one click further in. That choice
 * lives in view.selection rather than in the card itself, so that a tick that
 * redraws the card does not throw the viewer back to the list.
 *
 * Nothing here decides whether a card should be open at all — that is what
 * closeCards is for — only what the open one looks like.
 */
export function showCard(view, names, faces, now) {
    const entry = view.markers.get(view.selection.key);

    // The marker this card names is not on the map right now. draw() closes
    // a card left stranded like this once a drawing has actually happened;
    // reached from a click instead, a stale selection would otherwise be
    // rendered as a card about nobody.
    if (!entry) {
        closeCards(view);

        return;
    }

    const group = entry.members;
    const chosen = group.find((member) => member.positionId === view.selection.positionId);

    let content;

    // A member picked from a list stands on a pin of their own while the card
    // is open (see clusterMembers), so the way back is told by the list the
    // card remembers rather than by how many share this marker.
    const shown = chosen ?? (group.length === 1 ? group[0] : null);

    if (!shown) {
        content = groupList(view, entry, names, faces, now);
    } else {
        content = detail(view, shown, nameOf(shown, names), faces, now);
    }

    if (shown && (group.length > 1 || view.selection.list)) {
        content.prepend(backButton(view));
    }

    const close = cardCloseButton(view);

    // Scrolling lives on this wrapper rather than on #card itself: #card is
    // also the close button's positioned ancestor, and an ancestor that
    // scrolls its own box carries every descendant along with it, the close
    // button included — the corner it is meant to stay pinned to would drift
    // out of view with the rest of the card. A plain content box underneath
    // it, scrolling on its own, leaves the button where it was tapped from.
    const body = document.createElement('div');

    body.className = 'card-body';
    body.append(content);

    view.card.replaceChildren(close, body);
    view.card.hidden = false;
}

/** The way back from one member to the list of everybody under the marker. */
function backButton(view) {
    const back = document.createElement('button');

    back.type = 'button';
    back.className = 'group-back';
    // The arrow says which way the button goes before the words are read; an
    // arrow written as a character rather than drawn, because a set of icons
    // is a dependency of its own and this is the only one the page needs.
    back.textContent = WORDS.backToList;
    // Drawn again rather than only shown: the marker for the whole list
    // comes back only once the member is no longer kept apart from it.
    back.addEventListener('click', () => {
        view.selection = listShown(view.selection);
        redrawSelection(view);
        view.onCardChange();
    });

    return back;
}

/**
 * The cross in the card's corner.
 *
 * Named in the language the rest of the card is read in: a cross drawn
 * out of two lines has no text inside it for a reader who is listening to
 * the page rather than looking at it.
 */
function cardCloseButton(view) {
    const close = document.createElement('button');

    close.type = 'button';
    close.className = 'card-close';
    close.setAttribute('aria-label', WORDS.close);
    close.title = WORDS.close;
    close.addEventListener('click', () => {
        if (closeCards(view)) {
            view.onCardChange();
            redrawSelection(view);
        }
    });

    return close;
}

/** The members under one marker, as rows that open their cards. */
function groupList(view, entry, names, faces, now) {
    const list = document.createElement('div');

    list.className = 'group';

    const heading = document.createElement('h2');

    heading.textContent = groupLabel(entry.members.length);
    list.append(heading);

    for (const member of entry.members) {
        const row = document.createElement('button');

        row.type = 'button';
        row.className = 'group-row';

        const stale = isGrey(member, now);
        const colour = pinColour(member.memberId, stale);
        const label = nameOf(member, names);

        // The same circle the panel and the pin carry, so that one member is
        // one drawing wherever they are named.
        const face = document.createElement('span');

        face.className = stale ? 'member-face stale' : 'member-face';
        face.style.setProperty('--ring', colour);
        showFace(face, faces.get(member.memberId), label, colour);

        const name = document.createElement('span');

        name.className = 'group-name';
        name.textContent = label;

        const age = document.createElement('span');

        age.className = stale ? 'group-age stale' : 'group-age';
        age.textContent = ageText(positionAge(member.positionAt, now));

        row.append(face, name, age);
        row.addEventListener('click', () => {
            view.selection = openMember(view.selection.key, member.positionId, view.selection.key);
            setFollowTarget(view, member.positionId);
            showCard(view, names, faces, now);
            view.onCardChange();
            redrawSelection(view);
            bringTheRingIntoSight(view);
        });

        list.append(row);
    }

    return list;
}

export function nameOf(member, names) {
    return names.get(member.positionId) ?? shortName(member.memberId);
}

/**
 * What the bubble says once the viewer asks.
 *
 * Everything the position document carries beyond the two coordinates: when it
 * was taken, how the device was collecting at the time and how much battery it
 * had left, which together answer "why is this old" without a second question.
 *
 * Built as elements rather than as text, so that a member id — written by
 * whoever holds the account — has no way of being read as markup.
 */
function detail(view, position, name, faces, now) {
    const card = document.createElement('div');
    card.className = 'detail';

    const heading = detailHeading(view, position, name, faces, now);

    // Right of the name, so the card is no taller for them; what they leave on
    // the device is a line under the heading.
    const actions = view.deviceActions?.(position) ?? null;

    if (actions) {
        heading.append(actions.buttons);
    }

    card.append(heading);

    if (actions?.status) {
        card.append(actions.status);
    }

    card.append(summary(view, position, now));

    const sought = view.soughtLine?.(position);

    if (sought) {
        card.append(sought);
    }

    const warnings = warningsOf(position, now);

    if (warnings) {
        card.append(warnings);
    }

    // Everything else behind one button, and the choice kept: a viewer who
    // opens the details wants them on the next card too, and one who closed
    // them opened the card for the summary.
    const details = detailTable(position);
    const more = moreButton(view, details);

    // Beside the name rather than under the details: opened, the details
    // reach the bottom of the card, and a button below them had to be
    // scrolled to.
    heading.append(more);
    card.append(details);

    return card;
}

/** The member's face and, beside it, the person and the device, each pressed to rename it. */
function detailHeading(view, position, name, faces, now) {
    // The same face the marker carries, so that a card opened from the panel
    // is recognisably the person whose pin is on the map.
    const heading = document.createElement('div');

    heading.className = 'detail-head';

    // Old means grey here as well as on the pin and in the strip: a card in
    // full colour beside a faded pin reads as a member who is there right now.
    const stale = isGrey(position, now);
    const colour = pinColour(position.memberId, stale);

    const face = document.createElement('span');

    face.className = stale ? 'detail-face stale' : 'detail-face';
    face.style.setProperty('--ring', colour);
    showFace(face, faces.get(position.memberId), name, colour);

    const titles = document.createElement('div');
    const title = document.createElement('h2');
    const device = document.createElement('span');
    const own = position.memberId === view.ownMemberId;

    // The person and the device on lines of their own, whatever the markers
    // say, because each of the two is what is pressed to rename it: a row of
    // its own for renaming took a line of a card that is short of them.
    // Oneself is named on the account, not here.
    const person = view.personName?.(position) ?? name;

    title.append(nameWord(view, person, own ? null : ['members', position.memberId]));
    device.className = 'detail-device';
    device.append(nameWord(view, position.deviceName || WORDS.unnamedDevice, ['devices', position.positionId]));
    titles.append(title, device);

    heading.append(face, titles);

    return heading;
}

/** The button that opens and closes the details, remembering the choice for the next card. */
function moreButton(view, details) {
    const more = document.createElement('button');

    view.detailsOpen ??= readDetailsOpen();
    details.id = 'card-details';
    more.type = 'button';
    more.className = 'detail-more';
    more.setAttribute('aria-controls', details.id);

    const showDetails = (open) => {
        details.hidden = !open;
        more.setAttribute('aria-expanded', String(open));
        more.textContent = open ? WORDS.fewerDetails : WORDS.moreDetails;
    };

    showDetails(view.detailsOpen);
    more.addEventListener('click', () => {
        view.detailsOpen = !view.detailsOpen;
        writeDetailsOpen(view.detailsOpen);
        showDetails(view.detailsOpen);
    });

    return more;
}

/**
 * The line under the name: how far, how old the position, how long since the
 * device was online, and the charge.
 *
 * The two ages side by side because under a spoof they are hours apart, and a
 * card naming only one of them is read as naming the other.
 */
function summary(view, position, now) {
    const line = document.createElement('div');

    line.className = 'detail-summary';

    const away = distanceFromViewer(position, view.last?.positions ?? [], view.ownMemberId, now);
    const parts = [
        ...(away === null ? [] : [WORDS.fromYou(distanceText(away))]),
        WORDS.positionAgo(ageText(positionAge(position.positionAt, now))),
        WORDS.onlineAgo(ageText(positionAge(position.knownAt, now))),
    ];

    for (const text of parts) {
        const part = document.createElement('span');

        part.textContent = text;
        line.append(part);
    }

    line.append(batteryText(position));

    // While the database is silent the ages above are of what the page last
    // heard, not of now; the page says as of when.
    const known = view.knownNote?.();

    if (known) {
        const part = document.createElement('span');

        part.textContent = known;
        line.append(part);
    }

    return line;
}

/**
 * What is wrong with the position, as one paragraph, or null when nothing is.
 *
 * Each sentence only while it is true: a marker left behind by a device that is
 * still online, answers being rejected, a receiver that has not settled. Put
 * together rather than as rows, because they are read as one account of why
 * the marker should not be trusted.
 */
function warningsOf(position, now) {
    // Each sentence is a list of pieces: the one about rejected answers holds
    // the reason as a word to press.
    const sentences = [];

    // The answers refused for lying in the same place are that place again,
    // so they are counted into its sentence rather than given one of their own.
    const refusedHere = position.inFalsePlace
        && position.leftOutCount > 0
        && position.leftOutReason === 'FalsePlace';

    if (refusedHere) {
        const place = reasonButton('FalsePlace');

        place.textContent = WORDS.falsePlaceWhere;
        sentences.push([WORDS.falsePlaceAndAfter(position.leftOutCount), ' ', place, '.']);
    } else if (position.inFalsePlace) {
        const place = reasonButton('FalsePlace');

        place.textContent = lowerFirst(place.textContent);
        sentences.push([WORDS.falsePlaceLead, ' ', place, '.']);
    }

    if (isStuck(position.positionAt, position.knownAt, now)) {
        const age = ageText(positionAge(position.knownAt, now));
        const words = stuckCause(position.leftOutCount) === 'rejected'
            ? WORDS.stuckSentence
            : WORDS.stuckSilentSentence;

        sentences.push([words(age)]);
    }

    if (position.leftOutCount > 0 && !refusedHere) {
        const reason = reasonButton(position.leftOutReason);

        reason.textContent = lowerFirst(reason.textContent);
        const lead = WORDS.leftOutLead(WORDS.leftOutAnswers(position.leftOutCount));

        sentences.push([lead, ' ', reason, '.']);
    }

    if (position.settling) {
        sentences.push([WORDS.settlingSentence]);
    }

    if (sentences.length === 0) {
        return null;
    }

    const paragraph = document.createElement('p');

    paragraph.className = 'detail-warn';
    sentences.forEach((pieces, index) => paragraph.append(...(index > 0 ? [' '] : []), ...pieces));

    return paragraph;
}

/** The first letter made small, for a reason read in the middle of a sentence. */
function lowerFirst(text) {
    return text.charAt(0).toLocaleLowerCase(SPEAKING) + text.slice(1);
}

/** The rows behind the button: the two moments, and how the answer was arrived at. */
function detailTable(position) {
    const table = document.createElement('table');

    const rows = [
        [WORDS.at, timeText(position.positionAt)],
        [WORDS.online, timeText(position.knownAt)],
        [WORDS.mode, modeButton(position.mode)],
        [WORDS.foundBy, foundText(position.foundBy)],
        [WORDS.width, widthValue(position.accuracyMetres)],
        [WORDS.position, `${position.latitude.toFixed(5)}, ${position.longitude.toFixed(5)}`],
    ];

    for (const [title, value] of rows) {
        const row = document.createElement('tr');
        const head = document.createElement('th');
        const cell = document.createElement('td');

        head.textContent = title;

        if (value instanceof Node) {
            cell.append(value);
        } else {
            cell.textContent = value;
        }

        row.append(head, cell);
        table.append(row);
    }

    return table;
}

/** Where the choice of details is kept, in this browser only. */
const DETAILS_KEY = 'geoshare.cardDetails';

function readDetailsOpen() {
    return readKept(DETAILS_KEY) === 'open';
}

function writeDetailsOpen(open) {
    keep(DETAILS_KEY, open ? 'open' : 'closed');
}

/**
 * A name at the head of the card: a word to press where the page has asked to
 * be told and the name is one the viewer may give, plain text otherwise.
 */
function nameWord(view, text, naming) {
    if (!view.onName || !naming) {
        return document.createTextNode(text);
    }

    const button = document.createElement('button');

    button.type = 'button';
    button.className = 'detail-rename';
    button.textContent = text;
    button.addEventListener('click', () => view.onName(...naming));

    return button;
}

/**
 * The width as the card writes it, ochre once the answer names a block rather
 * than a building.
 *
 * The colour rather than a word beside the number, and only here now that the
 * strip has stopped carrying the figure: the card is where a width is read at
 * all, and the whole point of the colour is that the eye stops on it. The same
 * hundred metres the application colours at, because the two clients share a
 * database and nothing else, and one width read two ways is read as two.
 */
function widthValue(metres) {
    if (metres === null) {
        return WORDS.unknown;
    }

    if (!widthIsWide(metres)) {
        return widthText(metres);
    }

    const wide = document.createElement('span');

    wide.className = 'detail-width wide';
    wide.textContent = widthText(metres);

    return wide;
}

/**
 * The mode as a thing to press, because a name alone answers nothing.
 *
 * "Standby" says what the device was doing only to somebody who already knows
 * the five; the help behind it is where they are read against each other, and
 * a card is the one place a member meets a mode at all.
 */
function modeButton(mode) {
    const button = document.createElement('button');

    button.className = 'mode-help-open';
    button.type = 'button';
    button.textContent = modeText(mode);
    button.addEventListener('click', () => openModeHelp());

    return button;
}

/**
 * The reason as a thing to press, for the same reason the mode is one.
 *
 * "Two positions at one time" names what the device saw without saying what it
 * means, and a card is the one place a member meets a reason at all. The word
 * shown is the one the card would have shown anyway, the switched-off source
 * named among it.
 */
function reasonButton(reason) {
    const button = document.createElement('button');

    button.className = 'mode-help-open left-out';
    button.type = 'button';
    button.textContent = leftOutText(reason);
    button.addEventListener('click', () => openReasonHelp());

    return button;
}

/**
 * What is left in the device, written green while it is filling: a low
 * percentage on a charger and the same one off it mean opposite things, and
 * the colour says which without taking any room on the card.
 */
export function batteryText(position, into = document.createElement('span')) {
    into.replaceChildren();
    into.classList.toggle('charging', position.charging && position.batteryPercent !== null);
    into.textContent = position.batteryPercent === null
        ? WORDS.unknown
        : percentText(position.batteryPercent, SPEAKING);

    return into;
}
