// The page: sign in, then everyone on a map with a panel to jump between them.

import { positionDocuments } from './lib/members.js';
import { createMap, remeasure } from './lib/map.js';
import { compareIds } from './lib/order.js';
import {
    NO_LABELS,
    keepLabels,
    labelledPositions,
    labelledProfiles,
    readKeptLabels,
} from './lib/labels.js';
import { withFalsePlaces } from './lib/false-places.js';
import { withPhones } from './lib/phones.js';
import { withSearches, standingSearches } from './lib/searches.js';
import { followProjectGroup } from './lib/membership.js';
import {
    currentGroup,
    keepWatching,
    refusals,
    rememberGroup,
    watchSignIn,
    watchWatchers,
} from './lib/store.js';
import { watchPositions } from './lib/store-positions.js';
import { lastProfileChange, profilesChanged } from './lib/refusals.js';
import {
    projectGroupOf,
    publishProfile,
    watchGrants,
    watchProfiles,
    watchRoster,
} from './lib/store-group.js';
import { watchFalsePlaces, watchLabels, watchSearches } from './lib/store-search.js';
import { AGE_TICK_MS, SIGNED_IN_KEY, elements, state } from './page/state.js';
import {
    FULL_SCREEN,
    onSignOutRefused,
    showFollowMenu,
    showFullScreen,
    showMenu,
} from './page/menus.js';
import { drawGroup } from './page/group-panel.js';
import { closeAdmin } from './page/admin-panel.js';
import { askName, closeNaming } from './page/dialogs.js';
import './page/report-ui.js';
import { closeSearch, drawSearchAsk, drawSought, searchesChanged, soughtLine } from './page/search-ui.js';
import { deviceActions } from './page/card-actions.js';
import { drawSirenAsk } from './page/siren-ui.js';
import { followLink, knownNote, unfollowLink } from './page/link-ui.js';
import { followAttention, unfollowAttention } from './page/pause.js';
import { notePageLooking, personName, render, showAccount } from './page/strip.js';
import { keep } from './lib/kept.js';
import { keepPrivate } from './lib/fault.js';

watchSignIn((member) => {
    state.member = member;
    // Kept out of what the page reports when something fails unexpectedly.
    keepPrivate(member?.name, member?.email);
    rememberSignIn(member !== null);

    if (member) {
        start();
    } else {
        stop();
    }

    render();
});

/**
 * What a write nobody waits on does when it is refused: nothing on the page
 * depends on it, so the console is the only place that hears.
 */
function warnOf(what) {
    return (error) => console.warn(`${what}:`, error);
}

/**
 * Keeps whether this browser is signed in for boot-sign-in.js to read on the
 * next load, and ends the waiting it started: the answer is in either way.
 */
function rememberSignIn(signedIn) {
    // Only the next load is worse off where the browser keeps nothing: it
    // shows the button while waiting.
    keep(SIGNED_IN_KEY, signedIn ? 'yes' : null);

    delete document.documentElement.dataset.signIn;
}

export function start() {
    showTheMap();
    followProfiles();
    followLabels();
    followTheGroup();
}

/** Puts the map, the panel and the account up in place of the sign-in screen. */
function showTheMap() {
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
        state.view.deviceActions = deviceActions;
        state.view.knownNote = knownNote;
    }

    // The map measures its element when it is created, and until the sign-in
    // screen goes away that measurement is of a hidden box.
    remeasure(state.view);
}

/** Follows the members' profiles, which say who is in the group and what each is called. */
function followProfiles() {
    state.stopProfiles ??= watchProfiles(
        (profiles) => {
            state.accountProfiles = profiles;
            keepPrivate(...[...profiles.values()].map((profile) => profile?.displayName));

            // A member handing a permission back touches their profile, and
            // that is the one thing a refused subscription cannot see for
            // itself, so every document turned away is asked for again now.
            const touched = lastProfileChange(profiles);

            if (profilesChanged(state.profileChange, touched)) {
                refusals.retryNow();
            }
            if (state.profileChange === null || profilesChanged(state.profileChange, touched)) {
                state.profileChange = touched;
            }
            state.profiles = labelledProfiles(profiles, state.labels);
            state.reachable = true;
            state.outsideGroup = false;
            publishProfile(state.member, profiles.get(state.member.memberId), warnOf('profile'));
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
}

/** Follows the names this member gave the others, kept for the next load as well. */
function followLabels() {
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
}

/** Follows the rest: the link, the roster, searches, false places and who is watching. */
function followTheGroup() {
    // A silent database changes the banner, the markers, the rows and any
    // window offering a call, so all of them are drawn again.
    followLink(() => {
        render();
        drawGroup();
        drawSirenAsk();
        drawSearchAsk();
    });
    followAttention();

    state.stopRoster ??= watchRoster((roster) => {
        state.roster = roster;
        readProjectGroup(roster);
        followWatchers();
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
}

/** Whether the roster last read names this account. */
function onRoster() {
    return state.roster?.members.includes(state.member.memberId) === true;
}

/**
 * Claims to be looking, and follows who else is, only while the roster names
 * this account: the rules let nobody else read the claims or write one, and
 * somebody outside the group would only be refused twice over.
 */
function followWatchers() {
    if (onRoster()) {
        state.stopWatching ??= keepWatching(state.member.memberId, warnOf('watching'), notePageLooking);
        state.stopWatchers ??= watchWatchers(state.member.memberId, (watchers) => {
            state.watchers = watchers;
            render();
        });

        return;
    }

    state.stopWatching?.();
    state.stopWatching = null;
    state.stopWatchers?.();
    state.stopWatchers = null;
    state.watchers = [];
}

// A sign-out the account refused leaves the session alive, and the claim
// withdrawn ahead of it is made again.
onSignOutRefused(followWatchers);

/** Stops following the roster and forgets it, and the mark read beside it. */
function forgetRoster() {
    state.stopRoster?.();
    state.stopRoster = null;
    state.roster = null;
    state.projectGroup = undefined;
}

/**
 * Reads whether the project already has a group, once per sign-in and only
 * for somebody with no group: a member needs no answer, and the mark does not
 * change under anybody's feet often enough to be worth following.
 */
function readProjectGroup(roster) {
    // Forgotten while there is a roster, so that somebody who leaves the
    // group is told about the project as it is by then.
    if (roster !== null) {
        state.projectGroup = undefined;
        return;
    }
    if (state.projectGroup !== undefined) {
        return;
    }

    const member = state.member;

    state.projectGroup = null;
    followProjectGroup(projectGroupOf, {
        isCurrent: () => state.member === member,
        memberId: member.memberId,
        current: currentGroup,
        onOwn: (own) => {
            rememberGroup(own);
            stop();
            start();
        },
        onOther: (group) => {
            state.projectGroup = group;
            drawGroup();
        },
        onFailure: warnOf('project group'),
    });
}

/**
 * Subscribes to what each member gives out, and again whenever the household
 * itself changes.
 *
 * The grants cannot be listed, so who to ask about comes from the profiles,
 * which every member may read. A member who shares with this viewer answers
 * with their devices; one who does not is simply absent, and the positions
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
        // What the panel counts beside each member is how many of their devices
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
 * what a subscription is made of: a member who picks up a second device adds it
 * to their grant, a search starting or ending adds or drops its own device,
 * and this is what notices both.
 */
export function followPositions(grants) {
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
 * The positions as the strip and the map show them: what the devices said about
 * themselves since laid over what they wrote, and each device called what this
 * viewer or its owner called it.
 */
export function showPositions() {
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

export function stop() {
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
    state.profileChange = null;

    state.stopLabels?.();
    state.stopLabels = null;
    state.labels = NO_LABELS;
    closeNaming();

    state.stopWatching?.();
    state.stopWatching = null;

    state.stopWatchers?.();
    state.stopWatchers = null;
    state.watchers = [];

    forgetRoster();
    unfollowAttention();
    unfollowLink();

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

// The ages have to move on their own: a member who has gone quiet — the case
// worth noticing — is exactly the one whose entry no write would ever refresh.
setInterval(() => {
    render();

    // The panel names how long ago each device was in touch, and those ages
    // move on with the clock as the strip's do.
    if (elements.admin.open) {
        drawGroup();
    }
}, AGE_TICK_MS);

// A hidden tab is not redrawn, so the first thing to do on the way back is to
// catch the ages up with the clock.
document.addEventListener('visibilitychange', () => {
    if (!document.hidden) {
        render();
    }
});

// Leaving the claim behind would hold every device in its fifteen-second mode
// for a tab nobody has open any more. The field is cleared with it: a page put
// aside and taken back out again is restored rather than loaded, and the claim
// has to be made anew — a field still holding the stopped one would keep the
// page a viewer in name only.
window.addEventListener('pagehide', () => {
    state.stopWatching?.();
    state.stopWatching = null;
});

// Restored from the back-forward cache, which is what going back to the page
// does on a device. Nothing else runs then: the account has not changed, so the
// sign-in listener stays quiet.
window.addEventListener('pageshow', () => {
    if (state.member && onRoster()) {
        state.stopWatching ??= keepWatching(state.member.memberId, warnOf('watching'), notePageLooking);
        render();
    }
});
