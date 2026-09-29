// The page: sign in, then everyone on a map with a panel to jump between them.

import { ageText, positionAge } from './lib/age.js';
import { showFace } from './lib/faces.js';
import { FOLLOW_ALL, FOLLOW_NONE, FOLLOW_ONE } from './lib/following.js';
import { groupOverview, phoneLines } from './lib/group-lines.js';
import { outsideTheBox } from './lib/mode-help.js';
import {
    awaitingFollowTarget,
    batteryText,
    centreOnce,
    chooseFollow,
    createMap,
    followMode,
    followPaused,
    followTarget,
    lookAt,
    remeasure,
    showMembers,
    shownPositions,
    stopAwaitingFollowTarget,
} from './lib/map.js';
import {
    STALE_COLOUR,
    memberColour,
    memberName,
    orderedForNavigation,
    positionDocuments,
    shortName,
} from './lib/members.js';
import { compareIds } from './lib/order.js';
import {
    NO_LABELS,
    keepLabels,
    labelText,
    labelledPositions,
    labelledProfiles,
    readKeptLabels,
} from './lib/labels.js';
import { isGrey, withFalsePlaces } from './lib/false-places.js';
import { withPhones } from './lib/phones.js';
import {
    DEFAULT_SEARCH_HOURS,
    SEARCH_HOURS,
    endIfChosen,
    nextSearchEnd,
    standingSearches,
    withSearches,
} from './lib/searches.js';
import { sirenState } from './lib/sirens.js';
import { applyTheme, readTheme, writeTheme } from './lib/theme.js';
import { watchingBesides, watchingMembers } from './lib/watchers.js';
import { readLanguage, writeLanguage } from './lib/language.js';
import { SPEAKING, WORDS } from './lib/words.js';
import {
    ADMINISTRATOR,
    UNGROUPED,
    UNRUN,
    invitationFrom,
    runsTheGroup,
    standingIn,
} from './lib/membership.js';
import {
    foundGroup,
    joinGroup,
    keepWatching,
    leave,
    makeInvitation,
    publishProfile,
    removeMember,
    callSiren,
    endSearch,
    forgetPhone,
    openSearch,
    renewSearch,
    setRunning,
    signIn,
    stopSiren,
    signInReturned,
    watchFalsePlaces,
    watchPositions,
    watchGrants,
    watchLabels,
    watchProfiles,
    watchRoster,
    watchSearches,
    watchSignIn,
    watchSiren,
    watchWatchers,
    writeLabel,
} from './lib/store.js';

/** Where the page remembers a sign-in; boot-sign-in.js spells it out too. */
const SIGNED_IN_KEY = 'geoshare.signedIn';

/** How often the ages on screen are recounted. Half a minute keeps "5 minutes ago" honest. */
const AGE_TICK_MS = 30 * 1000;

const elements = {
    signIn: document.querySelector('#sign-in'),
    signInExplanation: document.querySelector('#sign-in-explanation'),
    signInButton: document.querySelector('#sign-in-button'),
    signInWaiting: document.querySelector('#sign-in-waiting-text'),
    signInError: document.querySelector('#sign-in-error'),
    map: document.querySelector('#map'),
    card: document.querySelector('#card'),
    followButton: document.querySelector('#follow-button'),
    followCaption: document.querySelector('#follow-caption'),
    followMenu: document.querySelector('#follow-menu'),
    followMenuButtons: document.querySelectorAll('#follow-menu button'),
    followHint: document.querySelector('#follow-hint'),
    fullScreenButton: document.querySelector('#fullscreen-button'),
    followWait: document.querySelector('#follow-wait'),
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
    settings: document.querySelector('#settings'),
    settingsButton: document.querySelector('#settings-button'),
    settingsTitle: document.querySelector('#settings-title'),
    settingsClose: document.querySelector('#settings-close'),
    groupLabel: document.querySelector('#group-label'),
    groupNote: document.querySelector('#group-note'),
    groupMembers: document.querySelector('#group-members'),
    groupFound: document.querySelector('#group-found'),
    groupAdmin: document.querySelector('#group-admin'),
    groupPhones: document.querySelector('#group-phones'),
    phones: document.querySelector('#phones'),
    phonesClose: document.querySelector('#phones-close'),
    phonesHelp: document.querySelector('#phones-help'),
    help: document.querySelector('#help'),
    helpTitle: document.querySelector('#help-title'),
    helpList: document.querySelector('#help-list'),
    helpClose: document.querySelector('#help-close'),
    mapHelp: document.querySelector('#map-help'),
    phonesTitle: document.querySelector('#phones-title'),
    phonesList: document.querySelector('#phones-list'),
    groupJoin: document.querySelector('#group-join'),
    groupTyped: document.querySelector('#group-typed'),
    groupJoinButton: document.querySelector('#group-join-button'),
    groupSaid: document.querySelector('#group-said'),
    groupError: document.querySelector('#group-error'),
    admin: document.querySelector('#admin'),
    adminClose: document.querySelector('#admin-close'),
    adminHelp: document.querySelector('#admin-help'),
    adminTitle: document.querySelector('#admin-title'),
    adminMembers: document.querySelector('#admin-members'),
    adminInvitation: document.querySelector('#admin-invitation'),
    adminCode: document.querySelector('#admin-code'),
    adminCodeLasts: document.querySelector('#admin-code-lasts'),
    adminMakeInvitation: document.querySelector('#admin-make-invitation'),
    adminCopy: document.querySelector('#admin-copy'),
    adminSaid: document.querySelector('#admin-said'),
    adminError: document.querySelector('#admin-error'),
    naming: document.querySelector('#naming'),
    namingTitle: document.querySelector('#naming-title'),
    namingNote: document.querySelector('#naming-note'),
    namingTyped: document.querySelector('#naming-typed'),
    namingSave: document.querySelector('#naming-save'),
    namingReset: document.querySelector('#naming-reset'),
    namingCancel: document.querySelector('#naming-cancel'),
    adminSearches: document.querySelector('#admin-searches'),
    search: document.querySelector('#search'),
    searchTitle: document.querySelector('#search-title'),
    searchNote: document.querySelector('#search-note'),
    searchHours: document.querySelector('#search-hours'),
    searchUntil: document.querySelector('#search-until'),
    searchError: document.querySelector('#search-error'),
    searchGo: document.querySelector('#search-go'),
    searchCancel: document.querySelector('#search-cancel'),
    siren: document.querySelector('#siren'),
    sirenTitle: document.querySelector('#siren-title'),
    sirenPhones: document.querySelector('#siren-phones'),
    sirenNote: document.querySelector('#siren-note'),
    sirenError: document.querySelector('#siren-error'),
    sirenGo: document.querySelector('#siren-go'),
    sirenCancel: document.querySelector('#siren-cancel'),
    confirm: document.querySelector('#confirm'),
    confirmTitle: document.querySelector('#confirm-title'),
    confirmNote: document.querySelector('#confirm-note'),
    confirmGo: document.querySelector('#confirm-go'),
    confirmCancel: document.querySelector('#confirm-cancel'),
    soughtBanner: document.querySelector('#sought-banner'),
    soughtText: document.querySelector('#sought-text'),
    soughtNote: document.querySelector('#sought-note'),
    soughtFound: document.querySelector('#sought-found'),
};

const state = {
    member: null,
    positions: [],
    // Null until the first answer arrives, which is what tells "nobody has a
    // profile" apart from "the answer is still on its way". Writing before it
    // lands would put the same profile in again on every visit.
    profiles: null,
    // The profiles as the members wrote them, before this viewer's own names
    // for them are put in: the profile this viewer publishes is compared with
    // their own words, not with what they call themselves here.
    accountProfiles: null,
    // What this viewer calls the phones and the people, for themselves.
    labels: NO_LABELS,
    stopLabels: null,
    // The name being asked for: which of the two maps, and under what key.
    naming: null,
    // What the members give out, by member: the phones to subscribe to, and
    // nothing at all for a member who shares with somebody else.
    grants: new Map(),
    // The positions as the database holds them, before what their phones said
    // about themselves since is laid over them.
    written: [],
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
    // Whether this page itself is claiming to look, and so holding the
    // phones of the group in the viewing mode.
    pageLooking: false,
    stopWatchers: null,
    // How many of the documents the profiles name are being refused to us. A
    // refusal is a member no longer sharing, an expired permission or a rule
    // saying no, and the panel should say so rather than quietly lose a face.
    refused: 0,
    // Whether the group itself is out of reach: the profiles are what every
    // other subscription is built from, so their failure is the connection.
    reachable: true,
    // Whether the database refused the group outright, which is an account
    // on no roster rather than a connection gone.
    outsideGroup: false,
    // The name of whoever the follow button last had a target for. The map
    // only remembers a position id, which stops meaning anything the moment
    // that member goes quiet; the name is kept here so the button still has
    // something to say about them once their position drops out of the set.
    followTargetName: null,
    // The follow mode last drawn, to tell a change from a redrawing; null
    // before the first drawing after a sign-in.
    shownFollowMode: null,
    followAnnounceEnds: null,
    followWaitEnds: null,
    // Whose cards the strip highlighted on the last drawing, so that render
    // can tell a highlight that has actually changed from the same one drawn
    // again — the strip redraws on every clock tick regardless, and scrolling
    // it on every one of those would drag the view out from under a reader's
    // thumb for no reason at all.
    shownPositionIds: new Set(),
    // The strip's entries by position id, kept from one drawing to the next:
    // a clock tick changes an age and nothing else, and building every entry
    // again for that would throw away the elements and their listeners with
    // it.
    stripEntries: new Map(),
    // Who is in the group, or nothing at all — which is both a group that has
    // not been made and a group this account is not on, since neither can be
    // read.
    roster: null,
    stopRoster: null,
    // Where a false signal is known to put phones: a position there is drawn
    // grey and measured from by nobody.
    falsePlaces: [],
    stopFalsePlaces: null,
    // The invitation just made, held until the menu is closed. Nothing reads
    // it back from the database: nobody may read that collection at all.
    invitation: null,
    // What a refused write said, in a sentence. The rules weigh several things
    // at once and a refusal names none of them, so the page says what it can.
    groupTrouble: null,
    // What went right and is worth one line: the invitation reaching the
    // clipboard, which leaves nothing on screen to show for itself.
    groupSaid: null,
    // The group's searches by phone, as the database holds them, standing or
    // run out: which of them still stand is worked out against the clock.
    searches: new Map(),
    stopSearches: null,
    // The moment the next standing search runs out, when nothing arrives to
    // say so.
    searchEnds: null,
    // The search being opened or renewed in the window: which phone, whose,
    // the name it goes by, whether this renews a standing one, and the
    // length picked.
    searchAsk: null,
    // The phone this page last rang, and what its document says, followed
    // until another is rung so that its row can say whether it answered.
    sirenPhoneId: null,
    siren: null,
    stopSirenWatch: null,
    sirenTick: null,
    // The call being asked about in the window: whose, their phones, the one
    // picked.
    sirenAsk: null,
};

elements.signInButton.addEventListener('click', async () => {
    elements.signInError.textContent = '';

    try {
        await signIn();
    } catch (error) {
        console.warn('sign-in failed', error);
        elements.signInError.textContent = signInTrouble(error);
    }
});

/**
 * What to say when signing in did not happen.
 *
 * The answers the phone gives, less the one for a viewer who changed their
 * mind: leaving Google's page brings them back with no error at all. What is
 * left is the network not being there, this build pointed at a project that
 * does not want it, and something else.
 */
function signInTrouble(error) {
    if (error.code === 'auth/network-request-failed') {
        return WORDS.signInOffline;
    }

    if (error.code === 'auth/unauthorized-domain' || error.code === 'auth/operation-not-allowed') {
        return WORDS.signInMisconfigured;
    }

    // The code is what tells one unexpected failure from another, and the
    // viewer who reads it out is often the only way it reaches anybody.
    return error.code ? `${WORDS.signInFailed} (${error.code})` : WORDS.signInFailed;
}

// A sign-in that went to Google and came back failed is said here, on the
// page it came back to.
signInReturned().catch((error) => {
    elements.signInError.textContent = signInTrouble(error);
});

elements.signOutButton.addEventListener('click', () => leave());

// How long the name of a newly chosen mode stands beside its mark.
const FOLLOW_ANNOUNCE_MS = 2500;

// How long the page waits for a member to be picked after "follow a member"
// was asked for with nobody to follow. Long enough to find somebody in the
// strip, short enough that the strip does not stay outlined all afternoon.
const FOLLOW_WAIT_MS = 15000;

elements.followButton.addEventListener('click', (event) => {
    event.stopPropagation();
    showMenu(false);
    showFollowMenu(elements.followMenu.hidden);
});

elements.followMenu.addEventListener('click', (event) => event.stopPropagation());

for (const button of elements.followMenuButtons) {
    // Each entry carries the same mark as the button does in that mode.
    const marks = elements.followButton.querySelectorAll('svg');
    const mark = [...marks].find((each) => each.dataset.mode === button.dataset.mode);

    button.prepend(mark.cloneNode(true));
    button.querySelector('svg').removeAttribute('hidden');

    // chooseFollow moves the map itself and calls the map's own
    // onCardChange, which is render; the wait it may start is timed here.
    button.addEventListener('click', () => {
        showFollowMenu(false);

        clearTimeout(state.followWaitEnds);

        if (!chooseFollow(state.view, button.dataset.mode)) {
            state.followWaitEnds = setTimeout(
                () => stopAwaitingFollowTarget(state.view),
                FOLLOW_WAIT_MS,
            );
        }
    });
}

document.addEventListener('keydown', (event) => {
    if (event.key !== 'Escape') {
        return;
    }

    if (!elements.followMenu.hidden) {
        showFollowMenu(false);
        elements.followButton.focus();
    } else if (state.view !== null && awaitingFollowTarget(state.view)) {
        stopAwaitingFollowTarget(state.view);
    }
});

function showFollowMenu(open) {
    if (open) {
        sayFollowMenu();
    }

    elements.followMenu.hidden = !open;
    elements.followButton.setAttribute('aria-expanded', String(open));
}

/** The menu's words, with the current mode marked and the member named. */
function sayFollowMenu() {
    const mode = followMode(state.view);
    const titles = {
        [FOLLOW_NONE]: WORDS.followMenuNone,
        [FOLLOW_ONE]: state.followTargetName
            ? WORDS.followMenuOneNamed(state.followTargetName)
            : WORDS.followMenuOne,
        [FOLLOW_ALL]: WORDS.followMenuAll,
    };
    const notes = {
        [FOLLOW_NONE]: WORDS.followMenuNoneNote,
        [FOLLOW_ONE]: WORDS.followMenuOneNote,
        [FOLLOW_ALL]: WORDS.followMenuAllNote,
    };

    for (const button of elements.followMenuButtons) {
        button.querySelector('.follow-item-title').textContent = titles[button.dataset.mode];
        button.querySelector('.follow-item-note').textContent = notes[button.dataset.mode];
        button.setAttribute('aria-checked', String(button.dataset.mode === mode));
    }

    elements.followHint.textContent = WORDS.followMenuHint;
}

elements.accountButton.addEventListener('click', (event) => {
    event.stopPropagation();
    showMenu(elements.accountMenu.hidden);
});

// Anywhere else means "not that": the menu is a corner of the map, and the map
// is what the viewer came for.
document.addEventListener('click', () => {
    showMenu(false);
    showFollowMenu(false);
});

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
    // The roster is drawn when it changes, but which of its names lead to a
    // card depends on the positions, which change all the time: drawn again
    // as the menu opens rather than on every position.
    if (open) {
        drawGroup();
    }

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

elements.settingsButton.addEventListener('click', () => {
    showMenu(false);
    elements.settings.showModal();
});
elements.settingsClose.addEventListener('click', () => elements.settings.close());

/**
 * The map on the whole screen, and back. Offered only where the browser has a
 * full screen to give: a phone's browser that has none would leave a button
 * that does nothing. The mark follows the browser rather than the press, since
 * the browser leaves full screen on its own gestures too.
 */
const FULL_SCREEN = document.fullscreenEnabled === true;

elements.fullScreenButton.addEventListener('click', () => {
    const asked = document.fullscreenElement
        ? document.exitFullscreen()
        : document.documentElement.requestFullscreen();

    // A refusal leaves the page as it was, and the mark says so already.
    asked.catch(() => {});
});
document.addEventListener('fullscreenchange', () => showFullScreen());

function showFullScreen() {
    const full = document.fullscreenElement !== null;
    const words = full ? WORDS.fullScreenLeave : WORDS.fullScreen;

    for (const mark of elements.fullScreenButton.querySelectorAll('svg')) {
        mark.hidden = (mark.dataset.full === 'on') !== full;
    }

    elements.fullScreenButton.setAttribute('aria-pressed', String(full));
    elements.fullScreenButton.setAttribute('aria-label', words);
    elements.fullScreenButton.title = words;
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
    elements.signInWaiting.textContent = WORDS.signInWaiting;
    elements.signOutButton.textContent = WORDS.signOut;
    elements.namingSave.textContent = WORDS.nameSave;
    elements.namingReset.textContent = WORDS.nameReset;
    elements.namingCancel.textContent = WORDS.nameCancel;
    elements.themeLabel.textContent = WORDS.appearance;
    elements.languageLabel.textContent = WORDS.language;
    elements.settingsButton.textContent = WORDS.settings;
    elements.settingsTitle.textContent = WORDS.settings;
    elements.settingsClose.textContent = WORDS.close;
    elements.mapHelp.title = WORDS.helpOpen;
    elements.mapHelp.setAttribute('aria-label', WORDS.helpOpen);
    // The group section says its own words, since what stands there depends on
    // the roster rather than on the markup alone.
    drawGroup();

    // The theme buttons are marks: their words are what a screen reader says
    // and what a pointer resting on one shows.
    for (const button of elements.themeButtons) {
        button.setAttribute('aria-label', themes[button.dataset.theme]);
        button.title = themes[button.dataset.theme];
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
    rememberSignIn(member !== null);

    if (member) {
        start();
    } else {
        stop();
    }

    render();
});

/**
 * Keeps whether this browser is signed in for boot-sign-in.js to read on the
 * next load, and ends the waiting it started: the answer is in either way.
 */
function rememberSignIn(signedIn) {
    try {
        if (signedIn) {
            localStorage.setItem(SIGNED_IN_KEY, 'yes');
        } else {
            localStorage.removeItem(SIGNED_IN_KEY);
        }
    } catch {
        // Only the next load is worse off: it shows the button while waiting.
    }

    delete document.documentElement.dataset.signIn;
}

function start() {
    elements.signIn.hidden = true;
    elements.map.hidden = false;
    elements.followButton.hidden = false;
    elements.mapHelp.hidden = false;
    elements.fullScreenButton.hidden = !FULL_SCREEN;
    showFullScreen();
    elements.panel.hidden = false;
    elements.account.hidden = false;

    showAccount(state.member);
    drawGroup();

    if (!state.view) {
        state.view = createMap(elements.map, render, elements.card);
        state.view.onName = askName;
        state.view.personName = personName;
        state.view.soughtLine = soughtLine;
    }

    // The map measures its element when it is created, and until the sign-in
    // screen goes away that measurement is of a hidden box.
    remeasure(state.view);

    state.stopProfiles ??= watchProfiles(
        (profiles) => {
            state.accountProfiles = profiles;
            state.profiles = labelledProfiles(profiles, state.labels);
            state.reachable = true;
            state.outsideGroup = false;
            publishProfile(state.member, profiles.get(state.member.memberId), () => {});
            followGrants(profiles);
            // The names in the menu and in the panel are read from the
            // profiles, so a member signing in for the first time changes both.
            drawGroup();
            render();
        },
        (error) => {
            // A refusal is not a lost connection: it is the database saying
            // this account is on no roster of the group it asked about —
            // one that has not joined any, or one taken off — and "trying
            // again" would promise something no retry will bring.
            if (error?.code === 'permission-denied') {
                state.outsideGroup = true;
            } else {
                state.reachable = false;
            }
            render();
        },
    );

    if (!state.stopLabels) {
        const memberId = state.member.memberId;

        state.labels = readKeptLabels(memberId);
        state.stopLabels = watchLabels(memberId, (labels) => {
            keepLabels(memberId, labels);
            state.labels = labels;
            state.profiles = labelledProfiles(state.accountProfiles, labels);
            showPositions();
            drawGroup();
            render();
        });
    }

    state.stopRoster ??= watchRoster((roster) => {
        state.roster = roster;
        drawGroup();
    });

    state.stopSearches ??= watchSearches((searches) => {
        state.searches = searches;
        searchesChanged();
    });

    state.stopFalsePlaces ??= watchFalsePlaces((places) => {
        state.falsePlaces = places;
        showPositions();
        render();
    });

    state.stopWatching ??= keepWatching(state.member.memberId, () => {}, notePageLooking);

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
        showPositions();
        followPositions(grants);
        // What the panel counts beside each member is how many of their phones
        // this viewer may watch, which is exactly what has just changed.
        drawGroup();
        render();
    });
}

/**
 * Subscribes to the positions the grants point at, together with those a
 * standing search opens regardless of any grant, and again whenever either
 * points somewhere else.
 *
 * The positions are read a document at a time, so the list of documents is
 * what a subscription is made of: a member who picks up a second phone adds it
 * to their grant, a search starting or ending adds or drops its own phone,
 * and this is what notices both.
 */
function followPositions(grants) {
    const documents = [...new Set([
        ...positionDocuments(grants),
        ...standingSearches(state.searches, Date.now()).keys(),
    ])].sort(compareIds);
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
            state.written = positions;
            showPositions();
            render();
        },
        (refused) => {
            state.refused = refused;
            render();
        },
    );
}

/**
 * The positions as the strip and the map show them: what the phones said about
 * themselves since laid over what they wrote, and each phone called what this
 * viewer or its owner called it.
 */
function showPositions() {
    state.positions = withSearches(
        labelledPositions(
            withFalsePlaces(withPhones(state.written, state.grants), state.falsePlaces),
            state.labels,
            state.grants,
        ),
        state.searches,
        Date.now(),
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
    state.accountProfiles = null;

    state.stopLabels?.();
    state.stopLabels = null;
    state.labels = NO_LABELS;
    closeNaming();

    state.stopWatching?.();
    state.stopWatching = null;

    state.stopWatchers?.();
    state.stopWatchers = null;
    state.watchers = [];

    state.stopRoster?.();
    state.stopRoster = null;
    state.roster = null;

    state.stopFalsePlaces?.();
    state.stopFalsePlaces = null;
    state.falsePlaces = [];

    state.stopSearches?.();
    state.stopSearches = null;
    state.searches = new Map();
    clearTimeout(state.searchEnds);
    closeSearch();
    drawSought();
    state.invitation = null;
    state.groupTrouble = null;
    state.groupSaid = null;

    state.written = [];
    state.positions = [];
    state.refused = 0;
    state.reachable = true;
    state.outsideGroup = false;
    // A new sign-in is a different group, and its members are not the one
    // this name belonged to.
    state.followTargetName = null;
    state.shownFollowMode = null;
    state.shownPositionIds = new Set();
    state.stripEntries = new Map();

    elements.signIn.hidden = false;
    elements.map.hidden = true;
    elements.followButton.hidden = true;
    elements.mapHelp.hidden = true;
    elements.fullScreenButton.hidden = true;
    showFollowMenu(false);
    clearTimeout(state.followWaitEnds);
    elements.panel.hidden = true;
    elements.account.hidden = true;
    showMenu(false);
    closeAdmin();
}

/**
 * The group, as the menu shows it.
 *
 * Four positions to stand in and a different thing in each: somebody with no
 * group is offered the two ways into one, a member sees the roster, the one
 * running the group is offered the panel that runs it, and a group nobody runs
 * says so rather than offering controls whose every write would be refused.
 *
 * Drawn apart from the map and the strip, and on its own schedule: the roster
 * changes when somebody joins or leaves, which is rare, while the strip is
 * built again on every clock tick.
 */
function drawGroup() {
    if (!state.member) {
        return;
    }

    const standing = standingIn(state.roster, state.member.memberId);
    const ungrouped = standing === UNGROUPED;
    const running = standing === ADMINISTRATOR;

    elements.groupLabel.textContent = WORDS.group;

    elements.groupNote.hidden = !ungrouped && standing !== UNRUN;
    elements.groupNote.textContent = ungrouped ? WORDS.groupNone : WORDS.groupUnrun;

    elements.groupMembers.replaceChildren(
        ...(state.roster?.members ?? []).map((memberId) => memberRow(memberId)),
    );

    elements.groupFound.hidden = !ungrouped;
    elements.groupFound.textContent = WORDS.groupFound;

    elements.groupAdmin.hidden = !running;
    elements.groupAdmin.textContent = WORDS.adminTitle;

    elements.groupPhones.hidden = !(state.roster?.members ?? []).includes(state.member.memberId);
    elements.groupPhones.textContent = WORDS.myPhonesTitle;

    elements.groupJoin.hidden = !ungrouped;
    elements.groupTyped.placeholder = WORDS.groupInvitation;
    elements.groupJoinButton.textContent = WORDS.groupJoin;

    elements.groupSaid.hidden = state.groupSaid === null;
    elements.groupSaid.textContent = state.groupSaid ?? '';

    elements.groupError.hidden = state.groupTrouble === null;
    elements.groupError.textContent = state.groupTrouble ?? '';

    drawAdmin(running);
    drawPhones();
}

/**
 * This member's phones, drawn whenever the panel is open and something it shows
 * has changed. Every one of them may be rung from here: this page is not a
 * phone, so none of them is the one in hand.
 */
function drawPhones() {
    if (!elements.phones.open) {
        return;
    }

    const memberId = state.member.memberId;
    const own = state.grants.get(memberId)?.phones ?? new Map();
    const phones = phoneLines(memberId, own, state.labels);
    const now = Date.now();
    const running = standingIn(state.roster, memberId) === ADMINISTRATOR;
    const standing = standingSearches(state.searches, now);

    elements.phonesTitle.textContent = WORDS.myPhonesTitle;
    elements.phonesHelp.title = WORDS.helpOpen;
    elements.phonesHelp.setAttribute('aria-label', WORDS.helpOpen);

    if (phones.length === 0) {
        const none = document.createElement('p');

        none.textContent = WORDS.myPhonesNone;
        elements.phonesList.replaceChildren(none);

        return;
    }

    elements.phonesList.replaceChildren(
        ...phones.map((phone) => {
            const row = document.createElement('div');
            const about = document.createElement('div');
            const name = document.createElement('span');
            const said = document.createElement('span');
            const actions = document.createElement('div');

            row.className = 'admin-member';
            name.className = 'admin-name';
            name.textContent = phone.name;
            said.className = 'admin-about';
            // The name stands just above, so the line does not say it again.
            said.textContent = phoneText(phone, now, false);
            about.append(name, said);
            actions.className = 'admin-actions';
            actions.append(sirenButtonFor(phone, memberId, now, about));

            const search = standing.get(phone.phoneId) ?? null;

            // Only whoever runs the group opens a search, and here the phone
            // is one's own, the one lost.
            if (running) {
                actions.append(searchButton(phone, memberId, search));
            }

            if (search) {
                const until = document.createElement('span');

                until.className = 'admin-about sought-until';
                until.textContent = WORDS.soughtUntil(untilText(search.endsAt));
                about.append(until);
            }

            // Last in the row: a phone handed on, reset or lost stays in every
            // list until its member takes it out, and only they may.
            const remove = document.createElement('button');

            remove.type = 'button';
            remove.className = 'phone-remove';
            remove.title = WORDS.phoneRemove;
            remove.setAttribute('aria-label', WORDS.phoneRemove);
            remove.innerHTML = BIN_ICON;
            remove.addEventListener('click', () => {
                askConfirm(
                    {
                        title: WORDS.phoneRemoveTitle(phone.name),
                        note: WORDS.phoneRemoveNote,
                        go: WORDS.phoneRemoveGo,
                    },
                    () => forgetPhone(phone.phoneId),
                );
            });
            actions.append(remove);
            iconsInRow(actions);

            row.append(about, actions);

            return row;
        }),
    );
}

/**
 * Gathers the round buttons of a row's [actions] into one line at its top:
 * a column of them made every row three circles tall, while the worded
 * buttons, too wide to share a line, stay one under another beneath it.
 * A siren being sounded keeps its place in the line with its stop button.
 */
function iconsInRow(actions) {
    const icons = [...actions.children].filter((button) =>
        button.matches('.siren-ring, .siren-stop, .search-mark, .phone-remove'),
    );

    if (icons.length === 0) {
        return;
    }

    const line = document.createElement('div');

    line.className = 'admin-icons';
    line.append(...icons);
    actions.prepend(line);
}

/**
 * The round button that opens a search for [phone], outlined in red like the
 * siren beside it, and ends it once it is found: filled and blinking while
 * the search stands, so a row being looked for is seen before it is read.
 * The words it used to carry are its hint and its label.
 */
function searchButton(phone, memberId, search) {
    const button = document.createElement('button');
    const words = search ? WORDS.searchFound : WORDS.searchStart;

    button.type = 'button';
    button.className = search ? 'search-mark standing' : 'search-mark';
    button.title = words;
    button.setAttribute('aria-label', words);
    button.innerHTML = ALARM_ICON;
    button.addEventListener('click', () => {
        if (search) {
            tryGroupWrite(() => endSearch(phone.phoneId));
        } else {
            askSearch(phone.phoneId, memberId, phone.name, false);
        }
    });

    return button;
}

/**
 * The status of a call to [phone], laid onto [about], and the round button
 * that starts or stops it: shared between a row of the panel that runs the
 * group and one of "Мои устройства", since a call is rung and read the same
 * way in both, one phone at a time.
 */
function sirenButtonFor(phone, memberId, now, about) {
    const siren = state.siren?.phoneId === phone.phoneId ? state.siren : null;
    const sirenNow = siren === null ? null : sirenState(siren, now);

    if (siren !== null) {
        const told = sirenText(siren, sirenNow, [phone]);

        if (told !== null) {
            const status = document.createElement('span');

            status.className = 'admin-about siren-status';
            status.textContent = told;
            about.append(status);
        }
    }

    if (sirenNow?.kind === 'waiting' || sirenNow?.kind === 'sounding') {
        const stop = document.createElement('button');

        stop.type = 'button';
        stop.className = 'siren-stop';
        stop.innerHTML = MUTE_ICON;
        stop.append(WORDS.sirenStop);
        stop.addEventListener('click', () => {
            tryGroupWrite(() => stopSiren(phone.phoneId, state.member.memberId));
        });

        return stop;
    }

    const ring = document.createElement('button');

    ring.type = 'button';
    ring.className = 'siren-ring';
    ring.title = WORDS.sirenHint;
    ring.setAttribute('aria-label', WORDS.sirenHint);
    ring.innerHTML = SPEAKER_ICON;
    ring.addEventListener('click', () => {
        askSiren({ displayName: phone.name, memberId, phones: [phone] });
    });

    return ring;
}

elements.groupPhones.addEventListener('click', () => {
    if (!elements.phones.open) {
        elements.phones.showModal();
    }

    drawPhones();
});
elements.phonesClose.addEventListener('click', () => elements.phones.close());

/**
 * Opens the help of one screen: each of its buttons drawn the way the screen
 * draws it — [look] is 'plain', 'alarm' or 'filled' — and what it does.
 * [mark] is the markup of the icon, or an svg of the page to copy.
 */
function showHelp(title, entries) {
    elements.helpTitle.textContent = title;
    elements.helpClose.textContent = WORDS.helpClose;
    elements.helpList.replaceChildren(
        ...entries.flatMap(({ mark, look, text }) => {
            const shown = document.createElement('span');
            const said = document.createElement('span');

            shown.className = `help-look ${look}`;

            if (typeof mark === 'string') {
                shown.innerHTML = mark;
            } else {
                const copy = mark.cloneNode(true);

                copy.removeAttribute('hidden');
                shown.append(copy);
            }

            said.textContent = text;

            return [shown, said];
        }),
    );

    if (!elements.help.open) {
        elements.help.showModal();
    }
}

/** The siren and the search, as a row of the panel or of My devices draws them. */
function alarmHelp() {
    return [
        { mark: SPEAKER_ICON, look: 'alarm', text: WORDS.helpSiren },
        { mark: ALARM_ICON, look: 'alarm', text: WORDS.helpSearch },
        { mark: ALARM_ICON, look: 'filled', text: WORDS.helpSearchStanding },
    ];
}

elements.phonesHelp.addEventListener('click', () => {
    const running = standingIn(state.roster, state.member.memberId) === ADMINISTRATOR;
    // Only whoever runs the group has the search in these rows.
    const alarms = alarmHelp().filter((entry) => running || entry.mark !== ALARM_ICON);

    showHelp(WORDS.helpTitleScreen, [
        ...alarms,
        { mark: BIN_ICON, look: 'alarm', text: WORDS.helpRemove },
    ]);
});
elements.adminHelp.addEventListener('click', () => showHelp(WORDS.helpTitleScreen, alarmHelp()));
elements.mapHelp.addEventListener('click', () => {
    const follow = (mode) => elements.followButton.querySelector(`svg[data-mode="${mode}"]`);
    const theme = (name) => document.querySelector(`#theme-choice [data-theme="${name}"] svg`);
    const entries = [
        { mark: follow('none'), look: 'plain', text: WORDS.helpFollowNone },
        { mark: follow('one'), look: 'plain', text: WORDS.helpFollowOne },
        { mark: follow('all'), look: 'plain', text: WORDS.helpFollowAll },
    ];

    // Only where the browser has a full screen to give, as with the button.
    if (!elements.fullScreenButton.hidden) {
        entries.push({
            mark: elements.fullScreenButton.querySelector('svg[data-full="off"]'),
            look: 'plain',
            text: WORDS.helpFullScreen,
        });
    }

    entries.push(
        { mark: theme('light'), look: 'plain', text: WORDS.helpThemeLight },
        { mark: theme('dark'), look: 'plain', text: WORDS.helpThemeDark },
        { mark: theme('system'), look: 'plain', text: WORDS.helpThemeSystem },
    );
    showHelp(WORDS.helpTitleMap, entries);
});
elements.helpClose.addEventListener('click', () => elements.help.close());
closeOnClickOutside(elements.help, () => elements.help.close());

/**
 * A click on the dark behind a window closes it, as a tap beside a sheet does
 * on a phone; the same rule as the panel's, for every other window of the
 * page. The click is kept from the menu underneath, which stays as it was.
 */
function closeOnClickOutside(dialog, close) {
    dialog.addEventListener('click', (event) => {
        event.stopPropagation();

        if (event.target === dialog && outsideTheBox(dialog.getBoundingClientRect(), event)) {
            close();
        }
    });
}

closeOnClickOutside(elements.phones, () => elements.phones.close());
closeOnClickOutside(elements.siren, () => elements.siren.close());
closeOnClickOutside(elements.confirm, () => elements.confirm.close());
closeOnClickOutside(elements.search, () => closeSearch());
closeOnClickOutside(elements.settings, () => elements.settings.close());

/**
 * One person on the roster: their name, and whether they run the group.
 *
 * Nobody is taken off the roster from here — that, and the invitations, are in
 * the panel the button below opens. What is left is a list a member reads to
 * see who else is in this, and a way to their card: the name of somebody whose
 * position this page has leads to it, as a press on their marker would. A
 * member who shares nothing with this viewer is only a name.
 */
function memberRow(memberId) {
    const row = document.createElement('div');

    row.className = 'group-member';

    const position = freshestPositionOf(memberId);
    const name = document.createElement(position ? 'button' : 'span');

    if (position) {
        name.type = 'button';
        name.className = 'group-member-link';
        name.addEventListener('click', () => {
            showMenu(false);

            // Looked up again: the menu may have stood open a while.
            const now = freshestPositionOf(memberId);

            if (now && state.view) {
                lookAt(state.view, now);
            }
        });
    }

    name.textContent = memberName(
        state.profiles?.get(memberId),
        memberId,
        memberId === state.member.memberId,
    );
    row.append(name);

    if (state.roster && runsTheGroup(state.roster, memberId)) {
        const runs = document.createElement('span');

        runs.className = 'group-runs';
        runs.textContent = WORDS.groupRunsIt;
        row.append(runs);
    }

    return row;
}

/**
 * The position a member was last heard from at, of all their phones: the one
 * a viewer asking for them by name most likely means.
 */
function freshestPositionOf(memberId) {
    let freshest = null;

    for (const position of state.positions) {
        if (position.memberId === memberId
            && (freshest === null || position.knownAt > freshest.knownAt)) {
            freshest = position;
        }
    }

    return freshest;
}

/**
 * The panel that runs the group, drawn whenever what it shows has changed.
 *
 * The right to be here is weighed on every drawing rather than at the opening:
 * an administrator can be taken off the roster while the panel stands in front
 * of them, and a refused subscription leaves this page with no roster at all.
 * Either way the panel closes itself rather than offering writes that the rules
 * would refuse.
 */
function drawAdmin(running) {
    if (!running || state.roster === null) {
        closeAdmin();

        return;
    }

    elements.adminTitle.textContent = WORDS.adminTitle;
    elements.adminClose.setAttribute('aria-label', WORDS.close);
    elements.adminHelp.title = WORDS.helpOpen;
    elements.adminHelp.setAttribute('aria-label', WORDS.helpOpen);

    elements.adminMembers.replaceChildren(
        ...groupOverview(
            state.roster,
            state.profiles,
            state.grants,
            state.member.memberId,
            state.labels,
        ).map(adminRow),
    );

    elements.adminSearches.replaceChildren(
        ...[...standingSearches(state.searches, Date.now()).values()].map(adminSearch),
    );

    elements.adminMakeInvitation.textContent =
        state.invitation === null ? WORDS.groupMakeInvitation : WORDS.groupMakeAnotherInvitation;

    elements.adminInvitation.hidden = state.invitation === null;
    elements.adminCopy.hidden = state.invitation === null;
    elements.adminCopy.textContent = WORDS.groupCopy;
    elements.adminCode.textContent = state.invitation ?? '';
    elements.adminCodeLasts.textContent = WORDS.groupCodeLasts;

    elements.adminSaid.hidden = state.groupSaid === null;
    elements.adminSaid.textContent = state.groupSaid ?? '';

    elements.adminError.hidden = state.groupTrouble === null;
    elements.adminError.textContent = state.groupTrouble ?? '';
}

/**
 * One member in the panel: their name, what they are to this group, and the
 * way to take them off it.
 *
 * Nobody removes themselves here — leaving is not the same act as taking
 * somebody out, and one row holding both is how a click in the wrong place
 * empties a group — and the last administrator cannot be removed at all,
 * because the rules refuse a group nobody runs.
 */
function adminRow(line) {
    const row = document.createElement('div');

    row.className = 'admin-group';

    const head = document.createElement('div');

    head.className = 'admin-group-head';

    const name = document.createElement('span');

    name.className = 'admin-name';
    name.textContent = line.displayName ?? shortName(line.memberId);

    const said = document.createElement('span');

    said.className = 'admin-about';
    said.textContent = aboutThem(line);

    head.append(name, said);
    row.append(head);

    // One row per phone, each with its own round buttons: the family may be
    // looking for one of two phones lost, or ringing the other, and neither
    // act is on the member as a whole.
    const now = Date.now();

    for (const phone of line.phones) {
        row.append(adminPhoneRow(phone, line, now));
    }

    const tail = document.createElement('div');

    tail.className = 'admin-actions';

    if (line.mayBeMadeAdmin) {
        tail.append(
            adminButton(WORDS.groupMakeAdmin, () => setRunning(line.memberId, true)),
        );
    }

    if (line.mayStopRunning) {
        tail.append(
            adminButton(WORDS.groupStopRunning, () => setRunning(line.memberId, false), {
                // Giving up one's own role cannot be undone from here: only
                // another administrator can hand it back.
                asked: line.isYou
                    ? { title: WORDS.groupGiveUpRunning, go: WORDS.groupStopRunning }
                    : null,
            }),
        );
    }

    if (line.removable) {
        tail.append(
            adminButton(WORDS.groupRemove, () => removeMember(state.roster, line.memberId), {
                asked: {
                    title: WORDS.groupRemoveTitle(name.textContent),
                    note: WORDS.groupRemoveNote,
                    go: WORDS.groupRemoveGo,
                },
            }),
        );
    }

    if (tail.childElementCount > 0) {
        row.append(tail);
    }

    return row;
}

/**
 * One phone of a member, in the panel that runs the group: what it last
 * said on the left, its own siren and, for anybody but its owner's own row,
 * its own search on the right. Every phone of the member may be rung, one's
 * own included — this page is not a phone, and the phone looked for is most
 * often one's other one — but the search is offered only for somebody else's:
 * one's own phones are searched from "Мои устройства".
 */
function adminPhoneRow(phone, line, now) {
    const row = document.createElement('div');

    row.className = 'admin-phone';

    const about = document.createElement('div');

    about.className = 'admin-phone-about';

    const said = document.createElement('span');

    said.className = 'admin-about';
    said.textContent = phoneText(phone, now);
    about.append(said);

    const actions = document.createElement('div');

    actions.className = 'admin-actions';
    actions.append(sirenButtonFor(phone, line.memberId, now, about));

    if (!line.isYou) {
        const search = standingSearches(state.searches, now).get(phone.phoneId) ?? null;

        if (search) {
            const until = document.createElement('span');

            until.className = 'admin-about sought-until';
            until.textContent = WORDS.soughtUntil(untilText(search.endsAt));
            about.append(until);
        }

        actions.append(searchButton(phone, line.memberId, search));
    }

    iconsInRow(actions);
    row.append(about, actions);

    return row;
}

/**
 * A button of the panel that makes one write to the group, after [asked]
 * — a title, an optional note and the word on the button that goes ahead —
 * when the write cannot be taken back from here.
 */
function adminButton(text, write, { asked = null } = {}) {
    const button = document.createElement('button');

    button.type = 'button';
    button.textContent = text;
    button.addEventListener('click', () => {
        if (asked === null) {
            tryGroupWrite(write);
        } else {
            askConfirm(asked, write);
        }
    });

    return button;
}

/** The loudspeaker on the button that rings a phone, and the one struck through that stops it. */
const SPEAKER_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 9v6h4l5 4V5L8 9z"/><path d="M16.5 8.5a5 5 0 0 1 0 7"/><path d="M19.5 5.5a9 9 0 0 1 0 13"/></svg>';
const ALARM_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" aria-hidden="true"><path d="M6 5v9M12 5v9M18 5v9"/><circle cx="6" cy="19" r="1.3" fill="currentColor" stroke="none"/><circle cx="12" cy="19" r="1.3" fill="currentColor" stroke="none"/><circle cx="18" cy="19" r="1.3" fill="currentColor" stroke="none"/></svg>';
const BIN_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3"/></svg>';
const MUTE_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 9v6h4l5 4V5L8 9z"/><path d="M17 9l5 6M22 9l-5 6"/></svg>';

/**
 * What a row says of the call this page made: which phone, the seconds left
 * while it sounds, or that it never answered. Nothing once it is over.
 */
function sirenText(siren, now, phones) {
    const phone = phones.find((one) => one.phoneId === siren.phoneId)?.name
        ?? shortName(siren.phoneId.split('.')[1] ?? siren.phoneId);

    switch (now.kind) {
        case 'waiting':
            return WORDS.sirenWaiting(phone);
        case 'sounding':
            return WORDS.sirenSounding(
                phone,
                Math.max(0, Math.ceil((now.until - Date.now()) / 1000)),
            );
        case 'noAnswer':
            return WORDS.sirenNoAnswer(phone);
        case 'stopped':
            return WORDS.sirenStopped(phone);
        default:
            return null;
    }
}

/** Follows the phone just rung, and redraws its row every second while the call lasts. */
function followSiren(phoneId) {
    state.stopSirenWatch?.();
    state.sirenPhoneId = phoneId;
    state.siren = null;
    state.stopSirenWatch = watchSiren(phoneId, (siren) => {
        state.siren = siren;
        drawGroup();
    });

    clearInterval(state.sirenTick);
    state.sirenTick = setInterval(() => {
        const kind = state.siren ? sirenState(state.siren, Date.now()).kind : 'waiting';

        drawGroup();

        if (kind !== 'waiting' && kind !== 'sounding') {
            clearInterval(state.sirenTick);
            state.sirenTick = null;
        }
    }, 1000);
}

function askSiren(line) {
    const last = [...line.phones]
        .sort((one, other) => (other.onlineAt ?? 0) - (one.onlineAt ?? 0))[0];

    state.sirenAsk = {
        name: line.displayName ?? shortName(line.memberId),
        phones: line.phones,
        // The phone last in touch is the likeliest to hear it.
        selected: last.phoneId,
    };
    elements.sirenError.hidden = true;
    elements.sirenGo.disabled = false;
    drawSirenAsk();

    if (!elements.siren.open) {
        elements.siren.showModal();
    }
}

function drawSirenAsk() {
    const ask = state.sirenAsk;

    if (!ask) {
        return;
    }

    const now = Date.now();

    elements.sirenTitle.textContent = WORDS.sirenTitle(ask.name);
    elements.sirenPhones.hidden = ask.phones.length < 2;
    elements.sirenPhones.replaceChildren(
        ...ask.phones.map((phone) => {
            const button = document.createElement('button');

            button.type = 'button';
            button.textContent = phone.onlineAt === null
                ? phone.name
                : WORDS.sirenPhoneChip(phone.name, ageText(positionAge(phone.onlineAt, now)));
            button.setAttribute('aria-pressed', String(phone.phoneId === ask.selected));
            button.addEventListener('click', () => {
                ask.selected = phone.phoneId;
                drawSirenAsk();
            });

            return button;
        }),
    );
    elements.sirenNote.textContent = WORDS.sirenNote;
    elements.sirenGo.textContent = WORDS.sirenGo;
    elements.sirenCancel.textContent = WORDS.searchCancel;
}

/**
 * Rings the phone picked and waits for the database: a refusal stays on the
 * window rather than closing it on a call that never went out.
 */
elements.sirenGo.addEventListener('click', async () => {
    const ask = state.sirenAsk;

    if (!ask) {
        return;
    }

    elements.sirenGo.disabled = true;
    elements.sirenError.hidden = true;

    try {
        await callSiren(ask.selected, state.member.memberId);
        followSiren(ask.selected);

        if (state.sirenAsk === ask) {
            elements.siren.close();
        }
    } catch {
        elements.sirenError.textContent = WORDS.groupRefused;
        elements.sirenError.hidden = false;
        elements.sirenGo.disabled = false;
    }
});
elements.sirenCancel.addEventListener('click', () => elements.siren.close());
elements.siren.addEventListener('close', () => {
    state.sirenAsk = null;
});

/** The write waiting on the confirming window, or null while it is shut. */
let confirmedWrite = null;

function askConfirm({ title, note = '', go }, write) {
    confirmedWrite = write;
    elements.confirmTitle.textContent = title;
    elements.confirmNote.textContent = note;
    elements.confirmNote.hidden = note === '';
    elements.confirmGo.textContent = go;
    elements.confirmCancel.textContent = WORDS.searchCancel;

    if (!elements.confirm.open) {
        elements.confirm.showModal();
    }
}

elements.confirmGo.addEventListener('click', () => {
    const write = confirmedWrite;

    elements.confirm.close();

    if (write) {
        tryGroupWrite(write);
    }
});
elements.confirmCancel.addEventListener('click', () => elements.confirm.close());
elements.confirm.addEventListener('close', () => {
    confirmedWrite = null;
});

/**
 * A phone, when it was last in touch, and its charge: after its name where the
 * line stands for the member, and without it where the name is the line above.
 */
function phoneText(phone, now, named = true) {
    if (phone.onlineAt === null) {
        return named ? WORDS.adminPhoneSilent(phone.name) : WORDS.phoneSilent;
    }

    const age = ageText(positionAge(phone.onlineAt, now));
    const said = [named ? WORDS.adminPhoneOnline(phone.name, age) : WORDS.phoneOnline(age)];

    if (phone.batteryPercent !== null) {
        said.push(`${phone.batteryPercent}%`);
    }

    if (phone.charging) {
        said.push(WORDS.adminPhoneCharging);
    }

    return said.join(', ');
}

/**
 * The line under a name: who they are to this group, and what this viewer may
 * see of them.
 *
 * Said in one line rather than in marks beside the name, because a mark has to
 * be learnt and this is read once a month.
 */
function aboutThem(line) {
    const said = [];

    if (line.isYou) {
        said.push(WORDS.adminYou);
    }

    if (line.runsTheGroup) {
        said.push(WORDS.groupRunsIt);
    }

    said.push(
        line.phonesShared === 0 ? WORDS.adminNotShared : WORDS.adminPhonesShared(line.phonesShared),
    );

    return said.join(', ');
}

/**
 * Asks for a name for a phone or a person, for this viewer alone.
 *
 * `kind` is the map the name goes into — `devices` or `members` — and `key`
 * what it is filed under there: the phone's full address, or the account.
 */
function askName(kind, key) {
    const current = (kind === 'devices' ? state.labels.phones : state.labels.members).get(key);

    state.naming = { kind, key };
    elements.namingTitle.textContent = kind === 'devices' ? WORDS.namePhone : WORDS.namePerson;
    sayNaming(WORDS.nameForYou, false);
    elements.namingTyped.value = current ?? '';
    elements.namingReset.hidden = current === undefined;

    if (!elements.naming.open) {
        elements.naming.showModal();
    }
}

function closeNaming() {
    state.naming = null;

    if (elements.naming.open) {
        elements.naming.close();
    }
}

/**
 * Writes the name, or takes it away for null, and leaves the answer to the
 * subscription: the name shows up once the database has it, the same way it
 * does when it was given on a phone.
 *
 * The window stays until the database has answered. Closed at once, a refused
 * write left nothing on the screen: the page showed the new name for as long
 * as the write was pending, dropped it on the refusal, and the reason went
 * only to the console.
 */
async function saveName(name) {
    const naming = state.naming;

    if (!naming) {
        return;
    }

    sayNaming(WORDS.nameSaving, true);

    try {
        await writeLabel(state.member.memberId, naming.kind, naming.key, name);

        if (state.naming === naming) {
            closeNaming();
        }
    } catch (error) {
        console.error('labels:', error);

        if (state.naming === naming) {
            sayNaming(WORDS.nameFailed(error?.code ?? error?.message ?? String(error)), false);
        }
    }
}

/** What the naming window says under its title, and whether it is busy saving. */
function sayNaming(note, busy) {
    elements.namingNote.textContent = note;
    elements.namingNote.classList.toggle('naming-failed', !busy && note !== WORDS.nameForYou);
    elements.namingSave.disabled = busy;
    elements.namingReset.disabled = busy;
    elements.namingTyped.disabled = busy;
}

elements.namingSave.addEventListener('click', () => saveName(labelText(elements.namingTyped.value)));
elements.namingReset.addEventListener('click', () => saveName(null));
elements.namingTyped.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') {
        event.preventDefault();
        saveName(labelText(elements.namingTyped.value));
    }
});
elements.namingCancel.addEventListener('click', closeNaming);
elements.naming.addEventListener('close', () => {
    state.naming = null;
});
// The escape key that closes this window is not also meant for the card
// behind it, which would close on the same press.
elements.naming.addEventListener('keydown', (event) => event.stopPropagation());
// A click beside the window closes it, as it does for the panel.
elements.naming.addEventListener('click', (event) => {
    if (event.target === elements.naming
        && outsideTheBox(elements.naming.getBoundingClientRect(), event)) {
        closeNaming();
    }
});

function openAdmin() {
    if (!elements.admin.open) {
        elements.admin.showModal();
    }
}

function closeAdmin() {
    if (elements.admin.open) {
        elements.admin.close();
    }
}

/**
 * A write to the group, and what to say when the database refuses it.
 *
 * Every one of these is weighed by a rule that can say no for several reasons
 * at once — a code that has run out, a group already made, a member the last
 * administrator cannot be taken from — and a refusal names none of them. So
 * the page says what it can and leaves the roster on screen as it stands.
 */
async function tryGroupWrite(write) {
    state.groupTrouble = null;
    state.groupSaid = null;

    try {
        await write();
    } catch {
        state.groupTrouble = WORDS.groupRefused;
    }

    drawGroup();
}

/**
 * What a change to the searches changes: which phones this viewer may read,
 * so the positions are followed again; who is marked on the map and in the
 * strip; what the panel offers; and what this viewer is told about their own
 * phones.
 *
 * Also run when the next standing search runs out, since nothing arrives from
 * the database at that moment.
 */
function searchesChanged() {
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
function untilText(millis) {
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
function soughtLine(position) {
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
function drawSought() {
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
function adminSearch(search) {
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
function askSearch(phoneId, memberId, phoneName, renew) {
    state.searchAsk = { phoneId, memberId, phoneName, renew, hours: DEFAULT_SEARCH_HOURS };
    drawSearchAsk();

    if (!elements.search.open) {
        elements.search.showModal();
    }
}

function drawSearchAsk() {
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
}

function closeSearch() {
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

    elements.searchGo.disabled = true;
    elements.searchError.hidden = true;

    try {
        await (ask.renew
            ? renewSearch(ask.phoneId, adminId, ask.hours, ask.phoneName)
            : openSearch(ask.phoneId, adminId, ask.hours, ask.phoneName));

        if (state.searchAsk === ask) {
            closeSearch();
        }
    } catch {
        elements.searchError.textContent = WORDS.groupRefused;
        elements.searchError.hidden = false;
        elements.searchGo.disabled = false;
    }
});

elements.searchCancel.addEventListener('click', closeSearch);
elements.search.addEventListener('close', () => {
    state.searchAsk = null;
});

/**
 * Everything on this page is read out of one group, so joining another means
 * every subscription has to be made again against the new one.
 */
function regroup() {
    stop();
    start();
}

elements.groupFound.addEventListener('click', () =>
    tryGroupWrite(async () => {
        await foundGroup(state.member.memberId);
        regroup();
    }),
);

elements.groupAdmin.addEventListener('click', () => {
    // The menu stays open behind the panel: closing it would take the way back
    // out of the settings with it, and the panel is closed the way the help
    // windows over the map are.
    openAdmin();
    drawGroup();
});

elements.adminClose.addEventListener('click', closeAdmin);

// The escape key closes a dialog on its own, so only the dark behind the
// window is left to answer for: a click there closes it the way a tap beside a
// sheet does on a phone. Such a click arrives at the dialog itself, as does a
// click on its own padding, so the point is compared with the box to tell the
// two apart.
elements.admin.addEventListener('click', (event) => {
    // The panel is not "anywhere else" as far as the menu is concerned: it was
    // opened from the menu, and closing it should leave the settings standing
    // where they were rather than back on the bare map.
    event.stopPropagation();

    if (event.target !== elements.admin) {
        return;
    }

    if (outsideTheBox(elements.admin.getBoundingClientRect(), event)) {
        closeAdmin();
    }
});

elements.adminMakeInvitation.addEventListener('click', () =>
    tryGroupWrite(async () => {
        state.invitation = await makeInvitation(state.member.memberId);
    }),
);

elements.adminCopy.addEventListener('click', async () => {
    if (state.invitation === null) {
        return;
    }

    try {
        await navigator.clipboard.writeText(state.invitation);
        state.groupSaid = WORDS.groupCopied;
    } catch {
        // A browser that refuses the clipboard leaves the invitation on
        // screen, where it can be selected by hand.
        state.groupSaid = null;
    }

    drawGroup();
});

elements.groupJoinButton.addEventListener('click', () => {
    const invitation = invitationFrom(elements.groupTyped.value);

    if (invitation === null) {
        state.groupTrouble = WORDS.groupRefused;
        drawGroup();

        return;
    }

    elements.groupTyped.value = '';

    tryGroupWrite(async () => {
        await joinGroup(state.member.memberId, invitation);
        regroup();
    });
});

/**
 * The account picture, or the first letter of the name when there is none.
 *
 * The picture is hosted by whoever the account is with; a load that fails
 * leaves the letter showing rather than an empty circle.
 */
function showAccount(member) {
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
function notePageLooking(looking) {
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
function personName(position) {
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

// The ages have to move on their own: a member who has gone quiet — the case
// worth noticing — is exactly the one whose entry no write would ever refresh.
setInterval(() => {
    render();

    // The panel names how long ago each phone was in touch, and those ages
    // move on with the clock as the strip's do.
    if (elements.admin.open) {
        drawGroup();
    }
}, AGE_TICK_MS);

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
        state.stopWatching ??= keepWatching(state.member.memberId, () => {}, notePageLooking);
        render();
    }
});
