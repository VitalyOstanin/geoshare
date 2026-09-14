// The page: sign in, then everyone on a map with a panel to jump between them.

import { ageText, isStale, positionAge } from './lib/age.js';
import { showFace } from './lib/faces.js';
import {
    batteryText,
    centreOnce,
    createMap,
    lookAt,
    remeasure,
    showMembers,
    shownPositions,
} from './lib/map.js';
import {
    STALE_COLOUR,
    memberColour,
    memberName,
    orderedForNavigation,
    positionDocuments,
} from './lib/members.js';
import { applyTheme, readTheme, writeTheme } from './lib/theme.js';
import { watchingMembers } from './lib/watchers.js';
import { readLanguage, writeLanguage } from './lib/language.js';
import { widthIsWide, widthText } from './lib/width.js';
import { SPEAKING, WORDS } from './lib/words.js';
import {
    keepWatching,
    leave,
    publishProfile,
    signIn,
    watchPositions,
    watchGrants,
    watchProfiles,
    watchSignIn,
    watchWatchers,
} from './lib/store.js';

/** How often the ages on screen are recounted. Half a minute keeps "5 minutes ago" honest. */
const AGE_TICK_MS = 30 * 1000;

const elements = {
    signIn: document.querySelector('#sign-in'),
    signInExplanation: document.querySelector('#sign-in-explanation'),
    signInButton: document.querySelector('#sign-in-button'),
    signInError: document.querySelector('#sign-in-error'),
    map: document.querySelector('#map'),
    panel: document.querySelector('#panel'),
    panelNote: document.querySelector('#panel-note'),
    panelWatching: document.querySelector('#panel-watching'),
    members: document.querySelector('#members'),
    account: document.querySelector('#account'),
    accountButton: document.querySelector('#account-button'),
    accountPicture: document.querySelector('#account-picture'),
    accountInitial: document.querySelector('#account-initial'),
    accountMenu: document.querySelector('#account-menu'),
    accountName: document.querySelector('#account-name'),
    signOutButton: document.querySelector('#sign-out-button'),
    themeLabel: document.querySelector('#theme-label'),
    themeButtons: document.querySelectorAll('#theme-choice button'),
    languageLabel: document.querySelector('#language-label'),
    languageButtons: document.querySelectorAll('#language-choice button'),
};

const state = {
    member: null,
    positions: [],
    // Null until the first answer arrives, which is what tells "nobody has a
    // profile" apart from "the answer is still on its way". Writing before it
    // lands would put the same profile in again on every visit.
    profiles: null,
    // What the members give out, by member: the phones to subscribe to, and
    // nothing at all for a member who shares with somebody else.
    grants: new Map(),
    view: null,
    stopPositions: null,
    // The documents the current position subscriptions cover, joined into one
    // string: the profiles change for reasons that leave the devices alone,
    // and rebuilding the subscriptions for those would cost a read each time.
    positionDocuments: null,
    stopProfiles: null,
    stopGrants: null,
    // The members the current grant subscriptions cover, joined into one
    // string: a renamed account changes the profiles without changing who is
    // in the household.
    grantMembers: null,
    stopWatching: null,
    // The claims of the other members to be looking, as they arrived: the
    // names are worked out again on every draw, because a claim runs out with
    // the clock rather than with an answer from the database.
    watchers: [],
    stopWatchers: null,
    // How many of the documents the profiles name are being refused to us. A
    // refusal is a member no longer sharing, an expired permission or a rule
    // saying no, and the panel should say so rather than quietly lose a face.
    refused: 0,
    // Whether the group itself is out of reach: the profiles are what every
    // other subscription is built from, so their failure is the connection.
    reachable: true,
};

elements.signInButton.addEventListener('click', async () => {
    elements.signInError.textContent = '';

    try {
        await signIn();
    } catch (error) {
        elements.signInError.textContent = signInTrouble(error);
    }
});

/**
 * What to say when signing in did not happen.
 *
 * The same four answers the phone gives, because the same four things go
 * wrong: the viewer changed their mind, the network is not there, this build
 * is pointed at a project that does not want it, or something else.
 */
function signInTrouble(error) {
    // A closed popup is the viewer saying "not now"; answering it with an
    // error message would be arguing.
    if (error.code === 'auth/popup-closed-by-user' || error.code === 'auth/cancelled-popup-request') {
        return '';
    }

    if (error.code === 'auth/network-request-failed') {
        return WORDS.signInOffline;
    }

    if (error.code === 'auth/unauthorized-domain' || error.code === 'auth/operation-not-allowed') {
        return WORDS.signInMisconfigured;
    }

    return WORDS.signInFailed;
}

elements.signOutButton.addEventListener('click', () => leave());

elements.accountButton.addEventListener('click', (event) => {
    event.stopPropagation();
    showMenu(elements.accountMenu.hidden);
});

// Anywhere else means "not that": the menu is a corner of the map, and the map
// is what the viewer came for.
document.addEventListener('click', () => showMenu(false));

// Inside the menu is not anywhere else: the theme buttons live there, and a
// menu that shuts on the first press hides the change it was pressed for.
elements.accountMenu.addEventListener('click', (event) => event.stopPropagation());

/**
 * What the page is painted in. Kept here rather than in the account: it
 * answers for the screen the map is read on, not for the member reading it.
 */
let theme = readTheme();

applyTheme(theme);
showTheme();

for (const button of elements.themeButtons) {
    button.addEventListener('click', () => {
        theme = button.dataset.theme;

        applyTheme(theme);
        writeTheme(theme);
        showTheme();
    });
}

function showTheme() {
    for (const button of elements.themeButtons) {
        button.setAttribute('aria-pressed', String(button.dataset.theme === theme));
    }
}

function showMenu(open) {
    elements.accountMenu.hidden = !open;
    elements.accountButton.setAttribute('aria-expanded', String(open));
}

/**
 * The language the page is read in. The answer is the viewer's, the language
 * it comes out as is theirs or their browser's, and the two are not the same:
 * "system" is an answer that follows the browser rather than a language.
 */
const language = readLanguage();

sayMarkup();
showLanguage();

for (const button of elements.languageButtons) {
    button.addEventListener('click', () => {
        if (button.dataset.language === language) {
            return;
        }

        writeLanguage(button.dataset.language);

        // Loading the page again rather than redrawing it: every word on
        // screen comes from the dictionary, including the ones the markup was
        // painted with, and a redraw that misses one corner is worse than the
        // moment this costs. Nothing is lost — the map is drawn from the
        // documents, and the subscriptions open again on their own.
        window.location.reload();
    });
}

/**
 * Says the words of index.html in the language the page settled on.
 *
 * The markup is written in English and so is painted in English; this is what
 * a viewer reading in the other language sees instead, as soon as the modules
 * have run. The alternative — markup with no words in it — starts the page
 * blank and fills it in, which is worse for everybody to save a moment for
 * some.
 */
function sayMarkup() {
    const themes = {
        light: WORDS.themeLight,
        dark: WORDS.themeDark,
        system: WORDS.themeSystem,
    };
    const languages = {
        ru: WORDS.languageRussian,
        en: WORDS.languageEnglish,
        system: WORDS.languageSystem,
    };

    document.documentElement.lang = SPEAKING;

    elements.signInExplanation.textContent = WORDS.signInExplanation;
    elements.signInButton.textContent = WORDS.signInAction;
    elements.signOutButton.textContent = WORDS.signOut;
    elements.themeLabel.textContent = WORDS.appearance;
    elements.languageLabel.textContent = WORDS.language;

    for (const button of elements.themeButtons) {
        button.textContent = themes[button.dataset.theme];
    }

    for (const button of elements.languageButtons) {
        button.textContent = languages[button.dataset.language];
    }
}

function showLanguage() {
    for (const button of elements.languageButtons) {
        button.setAttribute('aria-pressed', String(button.dataset.language === language));
    }
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
        state.view = createMap(elements.map, render);
    }

    // The map measures its element when it is created, and until the sign-in
    // screen goes away that measurement is of a hidden box.
    remeasure(state.view);

    state.stopProfiles ??= watchProfiles(
        (profiles) => {
            state.profiles = profiles;
            state.reachable = true;
            publishProfile(state.member, profiles.get(state.member.memberId), () => {});
            followGrants(profiles);
            render();
        },
        () => {
            state.reachable = false;
            render();
        },
    );

    state.stopWatching ??= keepWatching(state.member.memberId, () => {});

    state.stopWatchers ??= watchWatchers(state.member.memberId, (watchers) => {
        state.watchers = watchers;
        render();
    });
}

/**
 * Subscribes to what each member gives out, and again whenever the household
 * itself changes.
 *
 * The grants cannot be listed, so who to ask about comes from the profiles,
 * which every member may read. A member who shares with this viewer answers
 * with their phones; one who does not is simply absent, and the positions
 * subscription is built from what is left.
 */
function followGrants(profiles) {
    const memberIds = [...profiles.keys()].sort();
    const wanted = memberIds.join(',');

    if (wanted === state.grantMembers) {
        return;
    }

    state.stopGrants?.();
    state.grantMembers = wanted;
    state.stopGrants = watchGrants(memberIds, (grants) => {
        state.grants = grants;
        followPositions(grants);
        render();
    });
}

/**
 * Subscribes to the positions the grants point at, and again whenever they
 * point somewhere else.
 *
 * The positions are read a document at a time, so the list of documents is
 * what a subscription is made of: a member who picks up a second phone adds it
 * to their grant, and this is what notices.
 */
function followPositions(grants) {
    const documents = positionDocuments(grants);
    const wanted = documents.join(',');

    // The grants change for reasons that have nothing to do with the devices —
    // a permission given to somebody else — and rebuilding every subscription
    // for those would cost a read each time.
    if (wanted === state.positionDocuments) {
        return;
    }

    state.stopPositions?.();
    state.positionDocuments = wanted;
    state.stopPositions = watchPositions(
        documents,
        (positions) => {
            state.positions = positions;
            render();
        },
        (refused) => {
            state.refused = refused;
            render();
        },
    );
}

function stop() {
    state.stopPositions?.();
    state.stopPositions = null;
    state.positionDocuments = null;
    state.stopGrants?.();
    state.stopGrants = null;
    state.grantMembers = null;
    state.grants = new Map();

    state.stopProfiles?.();
    state.stopProfiles = null;
    state.profiles = null;

    state.stopWatching?.();
    state.stopWatching = null;

    state.stopWatchers?.();
    state.stopWatchers = null;
    state.watchers = [];

    state.positions = [];
    state.refused = 0;
    state.reachable = true;

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
    elements.accountName.textContent = member.name ?? WORDS.signedIn;
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

    elements.members.replaceChildren(
        ...orderedForNavigation(state.positions, state.member.memberId).map((position) =>
            memberEntry(position, labels, pictures, shown, now),
        ),
    );

    showNote();
    showWatching();
    showStripEdge();
}

/**
 * Says, by fading the right edge, that the strip holds more than it shows.
 *
 * Measured rather than guessed: the strip is as wide as the window, and how
 * many members fit depends on the length of their names.
 */
function showStripEdge() {
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
    const looking = watchingMembers(state.watchers, Date.now());
    const names = looking.map((memberId) =>
        memberName(
            state.profiles?.get(memberId),
            memberId,
            memberId === state.member?.memberId,
        ),
    );

    elements.panelWatching.textContent = names.length === 0 ? '' : WORDS.mapOpen(names);
    elements.panelWatching.hidden = names.length === 0;
}

function noteText() {
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
    const name = memberName(
        state.profiles?.get(position.memberId),
        position.memberId,
        position.memberId === state.member.memberId,
    );

    if ((carried.get(position.memberId) ?? 1) > 1 && position.deviceName) {
        return `${name} · ${position.deviceName}`;
    }

    return name;
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

function memberEntry(position, labels, pictures, shown, now) {
    const entry = document.createElement('button');
    entry.className = shown.has(position.positionId) ? 'member shown' : 'member';
    entry.addEventListener('click', () => lookAt(state.view, position));

    const stale = isStale(position.knownAt, now);
    // The same expression the pin and the card use, rather than colour here
    // and a filter in the stylesheet: one rule written two ways is one rule
    // that can be half-changed.
    const colour = stale ? STALE_COLOUR : memberColour(position.memberId);
    const label = labels.get(position.positionId) ?? position.memberId;

    // The same face the pin on the map carries, ringed in the colour of that
    // pin: a card and a marker then say the same thing about the same member,
    // and the colour the dot used to carry is not lost.
    const face = document.createElement('span');

    face.className = stale ? 'member-face stale' : 'member-face';
    face.style.setProperty('--ring', colour);
    showFace(face, pictures.get(position.memberId), label, colour);

    const name = document.createElement('span');
    name.className = 'name';
    name.textContent = label;

    const age = document.createElement('span');
    age.className = stale ? 'age stale' : 'age';
    age.textContent = ageText(positionAge(position.positionAt, now));

    // How wide the answer behind the marker is, in a column of its own: the
    // widths then line up down the strip and are read against each other,
    // which is the question being asked — whether one marker is standing where
    // a phone is and another somewhere in its district. A position whose phone
    // named no width leaves the column empty rather than inventing a number.
    const width = document.createElement('span');

    width.className =
        position.accuracyMetres !== null && widthIsWide(position.accuracyMetres)
            ? 'width wide'
            : 'width';
    width.textContent =
        position.accuracyMetres === null ? '' : widthText(position.accuracyMetres);

    const battery = document.createElement('span');
    battery.className = 'battery';

    if (position.batteryPercent !== null) {
        batteryText(position, battery);
    }

    entry.append(face, name, age, width, battery);

    return entry;
}

// The ages have to move on their own: a member who has gone quiet — the case
// worth noticing — is exactly the one whose entry no write would ever refresh.
setInterval(render, AGE_TICK_MS);

// The strip holds a different number of members at a different width, and the
// fade at its edge has to follow.
window.addEventListener('resize', showStripEdge);

// A hidden tab is not redrawn, so the first thing to do on the way back is to
// catch the ages up with the clock.
document.addEventListener('visibilitychange', () => {
    if (!document.hidden) {
        render();
    }
});

// Leaving the claim behind would hold every phone in its fifteen-second mode
// for a tab nobody has open any more. The field is cleared with it: a page put
// aside and taken back out again is restored rather than loaded, and the claim
// has to be made anew — a field still holding the stopped one would keep the
// page a viewer in name only.
window.addEventListener('pagehide', () => {
    state.stopWatching?.();
    state.stopWatching = null;
});

// Restored from the back-forward cache, which is what going back to the page
// does on a phone. Nothing else runs then: the account has not changed, so the
// sign-in listener stays quiet.
window.addEventListener('pageshow', () => {
    if (state.member) {
        state.stopWatching ??= keepWatching(state.member.memberId, () => {});
        render();
    }
});
