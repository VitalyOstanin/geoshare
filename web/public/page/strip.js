// The strip of members under the map and the line of who is looking.

import { ageText, positionAge } from '../lib/age.js';
import { showFace } from '../lib/faces.js';
import { FOLLOW_ALL, FOLLOW_NONE, FOLLOW_ONE } from '../lib/following.js';
import { centreOnce, lookAt, showMembers, shownPositions } from '../lib/map.js';
import { batteryText } from '../lib/map-card.js';
import { awaitingFollowTarget, followMode, followPaused, followTarget } from '../lib/map-follow.js';
import { memberColour, memberName, orderedForNavigation, STALE_COLOUR } from '../lib/members.js';
import { isGrey } from '../lib/false-places.js';
import { watchingBesides, watchingMembers } from '../lib/watchers.js';
import { WORDS } from '../lib/words.js';
import { elements, state } from './state.js';
import { FOLLOW_ANNOUNCE_MS } from './menus.js';
import { untilText } from './search-ui.js';

/**
 * The account picture, or the first letter of the name when there is none.
 *
 * The picture is hosted by whoever the account is with; a load that fails
 * leaves the letter showing rather than an empty circle.
 */
export function showAccount(member) {
    elements.accountName.textContent = member.name ?? WORDS.signedIn;
    elements.accountInitial.textContent = (member.name ?? '?').slice(0, 1).toUpperCase();

    // The letter and the picture share one cell, the letter drawn over it:
    // shown together, the letter lands on the face as a dark mark.
    elements.accountPicture.hidden = !member.picture;
    elements.accountInitial.hidden = Boolean(member.picture);

    if (member.picture) {
        elements.accountPicture.src = member.picture;
        elements.accountPicture.onerror = () => {
            elements.accountPicture.hidden = true;
            elements.accountInitial.hidden = false;
        };
    }
}

export function render() {
    if (!state.member || !state.view) {
        return;
    }

    // A tab nobody can see is a tab whose ages nobody reads: the whole panel
    // and every card would be built again for a hidden window, and the answer
    // would be stale by the time it is looked at anyway. The redraw on the way
    // back is what makes this safe.
    if (document.hidden) {
        return;
    }

    const now = Date.now();

    const labels = names();
    const pictures = faces();

    showMembers(state.view, state.positions, labels, pictures, state.member.memberId, now);
    centreOnce(state.view, state.positions, state.member.memberId);

    // Whoever the open card is about is marked in the panel, so that a card
    // over the map and a card in the strip are visibly the same member.
    const shown = shownPositions(state.view);
    const ordered = orderedForNavigation(state.positions, state.member.memberId);
    const entries = ordered.map((position) => memberEntry(position, labels, pictures, shown, now));

    // Put back only when who is in the strip or their order has changed:
    // moving the same elements around on every tick is work for nothing.
    if (!sameElements(elements.members.children, entries)) {
        elements.members.replaceChildren(...entries);
    }

    state.stripEntries = new Map(
        ordered.map((position, index) => [position.positionId, entries[index]]),
    );
    scrollShownIntoView(ordered, entries, shown);

    showFollow(labels);
    showNote();
    showWatching();
    showStripEdge();
}

/**
 * Brings the entry the card is about into view, but only on the drawing that
 * actually opened, closed or moved it.
 *
 * The strip redraws on its own every half a minute as the ages tick over, and
 * scrolling it on each of those ticks would drag the strip out from under a
 * reader's thumb for a highlight that has not gone anywhere. Comparing the
 * set of shown ids against the one from the last drawing is what tells the
 * two apart.
 */
function scrollShownIntoView(ordered, entries, shown) {
    if (sameIds(shown, state.shownPositionIds)) {
        return;
    }

    state.shownPositionIds = shown;

    const index = ordered.findIndex((position) => shown.has(position.positionId));

    if (index !== -1) {
        entries[index].scrollIntoView({ block: 'nearest', inline: 'nearest' });
    }
}

/** Whether a parent already holds exactly these elements, in this order. */
function sameElements(children, entries) {
    return (
        children.length === entries.length &&
        entries.every((entry, index) => children[index] === entry)
    );
}

/** Whether two sets of ids name exactly the same members, in any order. */
function sameIds(a, b) {
    return a.size === b.size && [...a].every((id) => b.has(id));
}

/**
 * The follow button's mark and label, and whether it needs to say its member
 * has gone quiet.
 *
 * The map only remembers a target by position id, which stops resolving to
 * anything the moment that member's phone drops out of the set. The name is
 * kept here instead, refreshed whenever the id still resolves and held onto
 * once it does not, so a viewer following somebody out of the strip keeps
 * reading their name rather than watching the button go blank.
 */
function showFollow(labels) {
    const mode = followMode(state.view);
    const targetId = followTarget(state.view);
    const known = targetId !== null && labels.has(targetId);

    if (known) {
        state.followTargetName = labels.get(targetId);
    }

    const missing = targetId !== null && !known;

    const caption = followCaption(mode);

    // An svg has no hidden property, only the attribute the page's [hidden]
    // rule reads.
    for (const mark of elements.followButton.querySelectorAll('svg')) {
        mark.toggleAttribute('hidden', mark.dataset.mode !== mode);
    }

    elements.followCaption.textContent = caption;
    elements.followButton.setAttribute(
        'aria-label',
        missing ? `${caption}. ${WORDS.followTargetMissing}` : caption,
    );
    elements.followButton.classList.toggle('active', mode !== FOLLOW_NONE);
    elements.followButton.classList.toggle('paused', followPaused(state.view));
    elements.followButton.classList.toggle('missing', missing);

    // Said once per change, not on every drawing: the first drawing after a
    // sign-in is the mode the map starts in, which nobody chose.
    if (state.shownFollowMode !== null && state.shownFollowMode !== mode) {
        announceFollow();
    }

    state.shownFollowMode = mode;

    const awaiting = awaitingFollowTarget(state.view);

    elements.followWait.textContent = WORDS.followWait;
    elements.followWait.hidden = !awaiting;
    elements.members.classList.toggle('awaiting', awaiting);

    if (!awaiting) {
        clearTimeout(state.followWaitEnds);
    }
}

/** Unfolds the mode's name beside its mark for a moment. */
function announceFollow() {
    elements.followButton.classList.add('announce');
    clearTimeout(state.followAnnounceEnds);
    state.followAnnounceEnds = setTimeout(
        () => elements.followButton.classList.remove('announce'),
        FOLLOW_ANNOUNCE_MS,
    );
}

/**
 * The words of the follow button's mode: its label for a screen reader and
 * the title a pointer shows over the mark.
 *
 * The member's name is said inside a phrase rather than on its own: a button
 * reading only "Anna" among captions like "Following everyone" says who
 * without saying what is being done about them.
 */
function followCaption(mode) {
    if (mode === FOLLOW_ONE && state.followTargetName) {
        return WORDS.followOne(state.followTargetName);
    }

    return mode === FOLLOW_ALL ? WORDS.followAll : WORDS.followNone;
}

/**
 * Says, by fading the right edge, that the strip holds more than it shows.
 *
 * Measured rather than guessed: the strip is as wide as the window, and how
 * many members fit depends on the length of their names.
 */
export function showStripEdge() {
    const more = elements.members.scrollWidth > elements.members.clientWidth + 1;

    elements.members.classList.toggle('scrollable', more);
}

/**
 * The one line under the strip, for what the strip itself cannot say.
 *
 * The order is by how much it matters: a group out of reach explains every
 * other emptiness, a refusal explains a missing face, and an empty strip on a
 * reachable group is the honest "nobody yet" the phone shows in the middle of
 * its map. Waiting is told apart from empty by the profiles never having
 * arrived.
 */
function showNote() {
    const note = noteText();

    elements.panelNote.textContent = note;
    elements.panelNote.hidden = note === '';
}

/**
 * Who else has the map open, under the strip.
 *
 * Said of the household rather than of a member, because one open map puts
 * every phone of the group into the same way of collecting; and said by name,
 * because "somebody is looking" leaves the reader wondering which of the
 * household it was.
 */
function showWatching() {
    const looking = watchingBesides(
        watchingMembers(state.watchers, Date.now()),
        state.member?.memberId,
        state.pageLooking,
    );
    const others = looking.map((memberId) =>
        memberName(
            state.profiles?.get(memberId),
            memberId,
            memberId === state.member?.memberId,
        ),
    );
    const names = state.pageLooking ? [WORDS.thisPage, ...others] : others;

    elements.panelWatching.textContent = names.length === 0 ? '' : WORDS.mapOpen(names);
    elements.panelWatching.hidden = names.length === 0;
}

/** Keeps the line of who is looking in step with this page's own claim. */
export function notePageLooking(looking) {
    state.pageLooking = looking;
    showWatching();
}

function noteText() {
    if (state.outsideGroup) {
        return WORDS.outsideGroup;
    }

    if (!state.reachable) {
        return WORDS.unreachable;
    }

    if (state.profiles === null) {
        return WORDS.looking;
    }

    if (state.refused > 0 && state.positions.length === 0) {
        return WORDS.nobodySharing;
    }

    if (state.refused > 0) {
        return state.refused === 1 ? WORDS.oneMoreRefused : WORDS.moreRefused(state.refused);
    }

    return state.positions.length === 0 ? WORDS.nobodyYet : '';
}

/**
 * What every marker on screen is called, worked out once per drawing.
 *
 * One's own marker says "you" rather than one's own name: the name is on the
 * account button already, and the page exists to answer where everybody else
 * is.
 *
 * A member carrying two phones gets two markers, and a pair of markers with
 * the same name says nothing about which phone is where; the name of the
 * phone is added only then, because most members have one.
 */
function names() {
    const carried = new Map();

    for (const position of state.positions) {
        carried.set(position.memberId, (carried.get(position.memberId) ?? 0) + 1);
    }

    return new Map(
        state.positions.map((position) => [position.positionId, nameOf(position, carried)]),
    );
}

function nameOf(position, carried) {
    const name = personName(position);

    if ((carried.get(position.memberId) ?? 1) > 1 && position.deviceName) {
        return `${name} · ${position.deviceName}`;
    }

    return name;
}

/** Who carries the phone, without the phone: the head of a card says it apart. */
export function personName(position) {
    return memberName(
        state.profiles?.get(position.memberId),
        position.memberId,
        position.memberId === state.member.memberId,
    );
}

/**
 * Where every member's picture is, for those who have one.
 *
 * The pin draws it; a member without a picture, or one whose profile has not
 * arrived, is drawn with the first letter of their name instead.
 */
function faces() {
    return new Map(
        state.positions
            .map((position) => [
                position.memberId,
                state.profiles?.get(position.memberId)?.photoUrl,
            ])
            .filter(([, pictureUrl]) => pictureUrl),
    );
}

/**
 * One member's entry in the strip, the one from the last drawing brought up
 * to date when there is one.
 */
function memberEntry(position, labels, pictures, shown, now) {
    const entry =
        state.stripEntries.get(position.positionId) ?? newMemberEntry(position.positionId);
    const [face, name, age, battery, sought] = entry.children;

    entry.className = shown.has(position.positionId) ? 'member shown' : 'member';
    entry.classList.toggle('sought', Boolean(position.sought));
    sought.textContent = position.sought
        ? WORDS.soughtUntil(untilText(position.sought.endsAt))
        : '';

    const stale = isGrey(position, now);
    // The same expression the pin and the card use, rather than colour here
    // and a filter in the stylesheet: one rule written two ways is one rule
    // that can be half-changed.
    const colour = stale ? STALE_COLOUR : memberColour(position.memberId);
    const label = labels.get(position.positionId) ?? position.memberId;

    // The same face the pin on the map carries, ringed in the colour of that
    // pin: a card and a marker then say the same thing about the same member,
    // and the colour the dot used to carry is not lost.
    face.className = stale ? 'member-face stale' : 'member-face';
    face.style.setProperty('--ring', colour);
    showFace(face, pictures.get(position.memberId), label, colour);

    name.textContent = label;

    age.className = stale ? 'age stale' : 'age';
    age.textContent = ageText(positionAge(position.positionAt, now));

    if (position.batteryPercent === null) {
        // Emptied as well as left empty: the entry may have held a
        // percentage on the drawing before.
        battery.textContent = '';
        battery.classList.remove('charging');
    } else {
        batteryText(position, battery);
    }

    return entry;
}

/**
 * The elements of an entry, with nothing in them yet.
 *
 * The click looks the position up when it happens rather than holding the
 * one the entry was built with: the entry outlives many drawings, and the
 * member will have moved since.
 */
function newMemberEntry(positionId) {
    const entry = document.createElement('button');

    entry.addEventListener('click', () => {
        const position = state.positions.find((one) => one.positionId === positionId);

        if (position) {
            lookAt(state.view, position);
        }
    });

    const face = document.createElement('span');
    const name = document.createElement('span');
    name.className = 'name';
    const age = document.createElement('span');
    const battery = document.createElement('span');
    battery.className = 'battery';
    const sought = document.createElement('span');
    sought.className = 'sought-until';

    entry.append(face, name, age, battery, sought);

    return entry;
}
