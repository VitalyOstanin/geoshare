// The page: sign in, then everyone on a map with a panel to jump between them.

import { ageText, isStale, positionAge } from './lib/age.js';
import { showFace } from './lib/faces.js';
import { FOLLOW_ALL, FOLLOW_ONE } from './lib/following.js';
import {
    advanceFollow,
    batteryText,
    centreOnce,
    createMap,
    followMode,
    followPaused,
    followTarget,
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
import { SPEAKING, WORDS } from './lib/words.js';
import {
    ADMINISTRATOR,
    UNGROUPED,
    UNRUN,
    invitationFrom,
    mayBeRemoved,
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
    signIn,
    watchPositions,
    watchGrants,
    watchProfiles,
    watchRoster,
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
    card: document.querySelector('#card'),
    followButton: document.querySelector('#follow-button'),
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
    groupLabel: document.querySelector('#group-label'),
    groupNote: document.querySelector('#group-note'),
    groupMembers: document.querySelector('#group-members'),
    groupInvitation: document.querySelector('#group-invitation'),
    groupCode: document.querySelector('#group-code'),
    groupCodeLasts: document.querySelector('#group-code-lasts'),
    groupFound: document.querySelector('#group-found'),
    groupMakeInvitation: document.querySelector('#group-make-invitation'),
    groupCopy: document.querySelector('#group-copy'),
    groupJoin: document.querySelector('#group-join'),
    groupTyped: document.querySelector('#group-typed'),
    groupJoinButton: document.querySelector('#group-join-button'),
    groupSaid: document.querySelector('#group-said'),
    groupError: document.querySelector('#group-error'),
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
    // The name of whoever the follow button last had a target for. The map
    // only remembers a position id, which stops meaning anything the moment
    // that member goes quiet; the name is kept here so the button still has
    // something to say about them once their position drops out of the set.
    followTargetName: null,
    // Whose cards the strip highlighted on the last drawing, so that render
    // can tell a highlight that has actually changed from the same one drawn
    // again — the strip redraws on every clock tick regardless, and scrolling
    // it on every one of those would drag the view out from under a reader's
    // thumb for no reason at all.
    shownPositionIds: new Set(),
    // Who is in the group, or nothing at all — which is both a group that has
    // not been made and a group this account is not on, since neither can be
    // read.
    roster: null,
    stopRoster: null,
    // The invitation just made, held until the menu is closed. Nothing reads
    // it back from the database: nobody may read that collection at all.
    invitation: null,
    // What a refused write said, in a sentence. The rules weigh several things
    // at once and a refusal names none of them, so the page says what it can.
    groupTrouble: null,
    // What went right and is worth one line: the invitation reaching the
    // clipboard, which leaves nothing on screen to show for itself.
    groupSaid: null,
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

// advanceFollow moves the map itself and calls the map's own onCardChange,
// which is render — nothing further is needed here.
elements.followButton.addEventListener('click', () => advanceFollow(state.view));

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
    // The group section says its own words, since what stands there depends on
    // the roster rather than on the markup alone.
    drawGroup();

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
    elements.followButton.hidden = false;
    elements.panel.hidden = false;
    elements.account.hidden = false;

    showAccount(state.member);
    drawGroup();

    if (!state.view) {
        state.view = createMap(elements.map, render, elements.card);
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

    state.stopRoster ??= watchRoster((roster) => {
        state.roster = roster;
        drawGroup();
    });

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

    state.stopRoster?.();
    state.stopRoster = null;
    state.roster = null;
    state.invitation = null;
    state.groupTrouble = null;
    state.groupSaid = null;

    state.positions = [];
    state.refused = 0;
    state.reachable = true;
    // A new sign-in is a different group, and its members are not the one
    // this name belonged to.
    state.followTargetName = null;
    state.shownPositionIds = new Set();

    elements.signIn.hidden = false;
    elements.map.hidden = true;
    elements.followButton.hidden = true;
    elements.panel.hidden = true;
    elements.account.hidden = true;
    showMenu(false);
}

/**
 * The group, as the menu shows it.
 *
 * Four positions to stand in and a different thing in each: somebody with no
 * group is offered the two ways into one, a member sees the roster, the one
 * running the group hands out invitations and takes members off, and a group
 * nobody runs says so rather than offering controls whose every write would be
 * refused.
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
        ...(state.roster?.members ?? []).map((memberId) => memberRow(memberId, running)),
    );

    elements.groupFound.hidden = !ungrouped;
    elements.groupFound.textContent = WORDS.groupFound;

    elements.groupJoin.hidden = !ungrouped;
    elements.groupTyped.placeholder = WORDS.groupInvitation;
    elements.groupJoinButton.textContent = WORDS.groupJoin;

    elements.groupMakeInvitation.hidden = !running;
    elements.groupMakeInvitation.textContent =
        state.invitation === null ? WORDS.groupMakeInvitation : WORDS.groupMakeAnotherInvitation;

    elements.groupInvitation.hidden = state.invitation === null;
    elements.groupCopy.hidden = state.invitation === null;
    elements.groupCopy.textContent = WORDS.groupCopy;
    elements.groupCode.textContent = state.invitation ?? '';
    elements.groupCodeLasts.textContent = WORDS.groupCodeLasts;

    elements.groupSaid.hidden = state.groupSaid === null;
    elements.groupSaid.textContent = state.groupSaid ?? '';

    elements.groupError.hidden = state.groupTrouble === null;
    elements.groupError.textContent = state.groupTrouble ?? '';
}

/**
 * One person on the roster: their name, whether they run the group, and the
 * way to take them off it.
 *
 * Nobody removes themselves here — leaving is not the same act as taking
 * somebody out, and one row holding both is how a click in the wrong place
 * empties a group — and the last administrator cannot be removed at all,
 * because the rules refuse a group nobody runs.
 */
function memberRow(memberId, running) {
    const row = document.createElement('div');

    row.className = 'group-member';

    const name = document.createElement('span');

    name.textContent = memberName(
        state.profiles?.get(memberId),
        memberId,
        memberId === state.member.memberId,
    );
    row.append(name);

    if (state.roster?.admins.includes(memberId)) {
        const runs = document.createElement('span');

        runs.className = 'group-runs';
        runs.textContent = WORDS.groupRunsIt;
        row.append(runs);
    }

    const removable =
        running && memberId !== state.member.memberId && mayBeRemoved(state.roster, memberId);

    if (removable) {
        const remove = document.createElement('button');

        remove.type = 'button';
        remove.textContent = WORDS.groupRemove;
        remove.addEventListener('click', () =>
            tryGroupWrite(() => removeMember(state.roster, memberId)),
        );
        row.append(remove);
    }

    return row;
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

elements.groupMakeInvitation.addEventListener('click', () =>
    tryGroupWrite(async () => {
        state.invitation = await makeInvitation(state.member.memberId);
    }),
);

elements.groupCopy.addEventListener('click', async () => {
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
    const ordered = orderedForNavigation(state.positions, state.member.memberId);
    const entries = ordered.map((position) => memberEntry(position, labels, pictures, shown, now));

    elements.members.replaceChildren(...entries);
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

/** Whether two sets of ids name exactly the same members, in any order. */
function sameIds(a, b) {
    return a.size === b.size && [...a].every((id) => b.has(id));
}

/**
 * The follow button's caption, and whether it needs to say its member has
 * gone quiet.
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

    elements.followButton.textContent = followCaption(mode);
    elements.followButton.classList.toggle('paused', followPaused(state.view));
    elements.followButton.classList.toggle('missing', missing);
    elements.followButton.title = missing ? WORDS.followTargetMissing : '';
}

/**
 * The words the follow button says.
 *
 * The member's name is said inside a phrase rather than on its own: a button
 * reading only "Oona" among captions like "Following everyone" says who
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

    const battery = document.createElement('span');
    battery.className = 'battery';

    if (position.batteryPercent !== null) {
        batteryText(position, battery);
    }

    entry.append(face, name, age, battery);

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
