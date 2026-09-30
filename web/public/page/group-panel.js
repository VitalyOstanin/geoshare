// The group in the menu, "Мои устройства" and the panel that runs the group.

import { phoneLines, groupOverview } from '../lib/group-lines.js';
import { groupCaption } from '../lib/group-name.js';
import { outsideTheBox } from '../lib/mode-help.js';
import { lookAt } from '../lib/map.js';
import { memberName, shortName } from '../lib/members.js';
import { standingSearches } from '../lib/searches.js';
import { sirenState, sirenTold } from '../lib/sirens.js';
import { WORDS } from '../lib/words.js';
import {
    ADMINISTRATOR,
    UNGROUPED,
    UNRUN,
    invitationFrom,
    runsTheGroup,
    standingIn,
} from '../lib/membership.js';
import {
    foundGroup,
    joinGroup,
    makeInvitation,
    removeMember,
    endSearch,
    forgetPhone,
    renameGroup,
    setRunning,
    stopSiren,
} from '../lib/store.js';
import { elements, state } from './state.js';
import { linkIsLost, troubleOf } from './link-ui.js';
import { showMenu } from './menus.js';
import { start, stop } from '../app.js';
import { aboutThem, askConfirm, phoneText } from './dialogs.js';
import {
    ALARM_ICON,
    BIN_ICON,
    MUTE_ICON,
    SPEAKER_ICON,
    askSiren,
    followSirens,
    sirenText,
} from './siren-ui.js';
import { adminSearch, askSearch, closeSearch, untilText } from './search-ui.js';

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
export function drawGroup() {
    if (!state.member) {
        return;
    }

    state.sirensShown = new Set();

    const standing = standingIn(state.roster, state.member.memberId);
    const ungrouped = standing === UNGROUPED;
    const running = standing === ADMINISTRATOR;

    elements.groupLabel.textContent = WORDS.group;

    elements.groupNote.hidden = !ungrouped && standing !== UNRUN;
    elements.groupNote.textContent = ungrouped ? WORDS.groupNone : WORDS.groupUnrun;

    elements.groupMembers.replaceChildren(
        ...(state.roster?.members ?? []).map((memberId) => memberRow(memberId)),
    );

    elements.groupFoundName.hidden = !ungrouped;
    elements.groupFoundName.placeholder = WORDS.groupNameLabel;

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
    followSirens();
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
            state.sirensShown.add(phone.phoneId);

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
    const siren = state.sirens.get(phone.phoneId) ?? null;
    const sirenNow = siren === null ? null : sirenState(siren, now);

    if (siren !== null && sirenTold(siren, now)) {
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
        askSiren({
            displayName: memberName(
                state.profiles?.get(memberId),
                memberId,
                memberId === state.member.memberId,
            ),
            memberId,
            phones: [phone],
        });
    });

    return ring;
}

elements.groupPhones.addEventListener('click', () => {
    if (!elements.phones.open) {
        elements.phones.showModal();
    }

    drawGroup();
});

// A closed panel lets go of the sirens it was following.
elements.phones.addEventListener('close', () => drawGroup());
elements.admin.addEventListener('close', () => drawGroup());
elements.phonesClose.addEventListener('click', () => elements.phones.close());

/**
 * A click on the dark behind a window closes it, as a tap beside a sheet does
 * on a phone; the same rule as the panel's, for every other window of the
 * page. The click is kept from the menu underneath, which stays as it was.
 */
export function closeOnClickOutside(dialog, close) {
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

    elements.adminTitle.textContent =
        groupCaption(state.roster, state.profiles, state.member.memberId);
    elements.adminClose.setAttribute('aria-label', WORDS.close);
    elements.adminHelp.title = WORDS.helpOpen;
    elements.adminHelp.setAttribute('aria-label', WORDS.helpOpen);

    // Only whoever runs the group may change its name; a member who does not
    // sees the name alone, the way the roster below shows them no button
    // either.
    elements.adminRename.hidden = !running;
    elements.adminRename.textContent = WORDS.groupRename;

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

    // Drawn whether the panel is open or not; only an open one is watched.
    if (elements.admin.open) {
        state.sirensShown.add(phone.phoneId);
    }

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

function openAdmin() {
    if (!elements.admin.open) {
        elements.admin.showModal();
    }
}

export function closeAdmin() {
    if (elements.admin.open) {
        elements.admin.close();
    }
}

/**
 * Opens the window that renames the group, on top of the panel that runs it,
 * with its own name already typed in.
 */
function openRename() {
    elements.renameTyped.value = state.roster?.name ?? '';
    drawRename();

    if (!elements.rename.open) {
        elements.rename.showModal();
    }
}

/**
 * What the renaming window says, and whether saving is worth trying: the
 * rules refuse a name that is empty once trimmed or longer than 60
 * characters (`nameFits()`), and the button says so before the write is
 * asked for rather than after it is refused.
 */
function drawRename() {
    const trimmed = elements.renameTyped.value.trim();

    elements.renameTitle.textContent = WORDS.groupNameLabel;
    elements.renameSave.textContent = WORDS.nameSave;
    elements.renameCancel.textContent = WORDS.nameCancel;
    elements.renameSave.disabled = trimmed === '' || trimmed.length > 60;
}

/**
 * A write to the group, and what to say when the database refuses it.
 *
 * Every one of these is weighed by a rule that can say no for several reasons
 * at once — a code that has run out, a group already made, a member the last
 * administrator cannot be taken from — and a refusal names none of them. So
 * the page says what it can and leaves the roster on screen as it stands.
 */
export async function tryGroupWrite(write) {
    state.groupTrouble = null;
    state.groupSaid = null;

    if (linkIsLost()) {
        // Not left in Firestore's queue: a siren or a search going out an
        // hour later, when nobody is waiting for it, is worse than none.
        state.groupTrouble = WORDS.linkLostAction;
        drawGroup();

        return;
    }

    try {
        await write();
    } catch (error) {
        state.groupTrouble = troubleOf(error);
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
        await foundGroup(state.member.memberId, elements.groupFoundName.value);
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

elements.adminRename.addEventListener('click', openRename);
elements.renameTyped.addEventListener('input', drawRename);
elements.renameCancel.addEventListener('click', () => elements.rename.close());
elements.renameSave.addEventListener('click', () => {
    const name = elements.renameTyped.value.trim();

    elements.rename.close();
    tryGroupWrite(() => renameGroup(name));
});
closeOnClickOutside(elements.rename, () => elements.rename.close());

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
