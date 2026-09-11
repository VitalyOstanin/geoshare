// The page: sign in, then everyone on a map with a panel to jump between them.

import { ageText, isStale, positionAge } from './lib/age.js';
import { centreOnce, createMap, lookAt, remeasure, showMembers } from './lib/map.js';
import { memberColour, memberName, orderedForNavigation } from './lib/members.js';
import {
    keepWatching,
    leave,
    publishProfile,
    signIn,
    watchPositions,
    watchProfiles,
    watchSignIn,
} from './lib/store.js';

/** How often the ages on screen are recounted. Half a minute keeps "5 minutes ago" honest. */
const AGE_TICK_MS = 30 * 1000;

const elements = {
    signIn: document.querySelector('#sign-in'),
    signInButton: document.querySelector('#sign-in-button'),
    signInError: document.querySelector('#sign-in-error'),
    map: document.querySelector('#map'),
    panel: document.querySelector('#panel'),
    members: document.querySelector('#members'),
    account: document.querySelector('#account'),
    accountButton: document.querySelector('#account-button'),
    accountPicture: document.querySelector('#account-picture'),
    accountInitial: document.querySelector('#account-initial'),
    accountMenu: document.querySelector('#account-menu'),
    accountName: document.querySelector('#account-name'),
    signOutButton: document.querySelector('#sign-out-button'),
};

const state = {
    member: null,
    positions: [],
    // Null until the first answer arrives, which is what tells "nobody has a
    // profile" apart from "the answer is still on its way". Writing before it
    // lands would put the same profile in again on every visit.
    profiles: null,
    view: null,
    stopPositions: null,
    stopProfiles: null,
    stopWatching: null,
};

elements.signInButton.addEventListener('click', async () => {
    elements.signInError.textContent = '';

    try {
        await signIn();
    } catch (error) {
        // A closed popup is the viewer saying "not now"; answering it with an
        // error message would be arguing.
        if (error.code !== 'auth/popup-closed-by-user' && error.code !== 'auth/cancelled-popup-request') {
            elements.signInError.textContent = 'Sign-in did not go through. Try again.';
        }
    }
});

elements.signOutButton.addEventListener('click', () => leave());

elements.accountButton.addEventListener('click', (event) => {
    event.stopPropagation();
    showMenu(elements.accountMenu.hidden);
});

// Anywhere else means "not that": the menu is a corner of the map, and the map
// is what the viewer came for.
document.addEventListener('click', () => showMenu(false));

function showMenu(open) {
    elements.accountMenu.hidden = !open;
    elements.accountButton.setAttribute('aria-expanded', String(open));
}

watchSignIn((member) => {
    state.member = member;

    if (member) {
        start();
    } else {
        stop();
    }

    render();
});

function start() {
    elements.signIn.hidden = true;
    elements.map.hidden = false;
    elements.panel.hidden = false;
    elements.account.hidden = false;

    showAccount(state.member);

    if (!state.view) {
        state.view = createMap(elements.map);
    }

    // The map measures its element when it is created, and until the sign-in
    // screen goes away that measurement is of a hidden box.
    remeasure(state.view);

    state.stopPositions ??= watchPositions((positions) => {
        state.positions = positions;
        render();
    }, () => {});

    state.stopProfiles ??= watchProfiles((profiles) => {
        state.profiles = profiles;
        publishProfile(state.member, profiles.get(state.member.memberId), () => {});
        render();
    }, () => {});

    state.stopWatching ??= keepWatching(state.member.memberId, () => {});
}

function stop() {
    state.stopPositions?.();
    state.stopPositions = null;

    state.stopProfiles?.();
    state.stopProfiles = null;
    state.profiles = null;

    state.stopWatching?.();
    state.stopWatching = null;

    state.positions = [];

    elements.signIn.hidden = false;
    elements.map.hidden = true;
    elements.panel.hidden = true;
    elements.account.hidden = true;
    showMenu(false);
}

/**
 * The account picture, or the first letter of the name when there is none.
 *
 * The picture is hosted by whoever the account is with; a load that fails
 * leaves the letter showing rather than an empty circle.
 */
function showAccount(member) {
    elements.accountName.textContent = member.name ?? 'Signed in';
    elements.accountInitial.textContent = (member.name ?? '?').slice(0, 1).toUpperCase();

    elements.accountPicture.hidden = !member.picture;

    if (member.picture) {
        elements.accountPicture.src = member.picture;
        elements.accountPicture.onerror = () => {
            elements.accountPicture.hidden = true;
        };
    }
}

function render() {
    if (!state.member || !state.view) {
        return;
    }

    const now = Date.now();

    showMembers(state.view, state.positions, names(), state.member.memberId, now);
    centreOnce(state.view, state.positions, state.member.memberId);

    elements.members.replaceChildren(
        ...orderedForNavigation(state.positions, state.member.memberId).map((position) =>
            memberEntry(position, now),
        ),
    );
}

/**
 * What every member on screen is called, worked out once per drawing.
 *
 * One's own marker says "you" rather than one's own name: the name is on the
 * account button already, and the page exists to answer where everybody else
 * is.
 */
function names() {
    return new Map(
        state.positions.map((position) => [
            position.memberId,
            memberName(
                state.profiles?.get(position.memberId),
                position.memberId,
                position.memberId === state.member.memberId,
            ),
        ]),
    );
}

function memberEntry(position, now) {
    const entry = document.createElement('button');
    entry.className = 'member';
    entry.addEventListener('click', () => lookAt(state.view, position));

    const dot = document.createElement('span');
    dot.className = 'dot';
    dot.style.background = memberColour(position.memberId);

    const name = document.createElement('span');
    name.className = 'name';
    name.textContent = memberName(
        state.profiles?.get(position.memberId),
        position.memberId,
        position.memberId === state.member.memberId,
    );

    const age = document.createElement('span');
    age.className = isStale(position.knownAt, now) ? 'age stale' : 'age';
    age.textContent = ageText(positionAge(position.knownAt, now));

    const battery = document.createElement('span');
    battery.className = 'battery';
    battery.textContent = position.batteryPercent === null ? '' : `${position.batteryPercent}%`;

    entry.append(dot, name, age, battery);

    return entry;
}

// The ages have to move on their own: a member who has gone quiet — the case
// worth noticing — is exactly the one whose entry no write would ever refresh.
setInterval(render, AGE_TICK_MS);

// Leaving the claim behind would hold every phone in its fifteen-second mode
// for a tab nobody has open any more.
window.addEventListener('pagehide', () => state.stopWatching?.());
