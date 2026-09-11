// The page: sign in, then everyone on a map with a panel to jump between them.

import { ageText, isStale, positionAge } from './lib/age.js';
import { centreOnce, createMap, lookAt, remeasure, showMembers } from './lib/map.js';
import { memberColour, orderedForNavigation, shortName } from './lib/members.js';
import { keepWatching, leave, signIn, watchPositions, watchSignIn } from './lib/store.js';

/** How often the ages on screen are recounted. Half a minute keeps "5 minutes ago" honest. */
const AGE_TICK_MS = 30 * 1000;

const elements = {
    signIn: document.querySelector('#sign-in'),
    signInButton: document.querySelector('#sign-in-button'),
    signInError: document.querySelector('#sign-in-error'),
    map: document.querySelector('#map'),
    panel: document.querySelector('#panel'),
    members: document.querySelector('#members'),
    signOutButton: document.querySelector('#sign-out-button'),
};

const state = {
    member: null,
    positions: [],
    view: null,
    stopPositions: null,
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

    state.stopWatching ??= keepWatching(state.member.memberId, () => {});
}

function stop() {
    state.stopPositions?.();
    state.stopPositions = null;

    state.stopWatching?.();
    state.stopWatching = null;

    state.positions = [];

    elements.signIn.hidden = false;
    elements.map.hidden = true;
    elements.panel.hidden = true;
}

function render() {
    if (!state.member || !state.view) {
        return;
    }

    const now = Date.now();

    showMembers(state.view, state.positions, state.member.memberId, now);
    centreOnce(state.view, state.positions, state.member.memberId);

    elements.members.replaceChildren(
        ...orderedForNavigation(state.positions, state.member.memberId).map((position) =>
            memberEntry(position, now),
        ),
    );
}

function memberEntry(position, now) {
    const own = position.memberId === state.member.memberId;

    const entry = document.createElement('button');
    entry.className = 'member';
    entry.addEventListener('click', () => lookAt(state.view, position));

    const dot = document.createElement('span');
    dot.className = 'dot';
    dot.style.background = memberColour(position.memberId);

    const name = document.createElement('span');
    name.className = 'name';
    name.textContent = own ? 'You' : shortName(position.memberId);

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
