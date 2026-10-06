// The panel that runs the group: its roster, its invitation, and the windows
// that rename the group and make an invitation.

import { groupOverview } from '../lib/group-lines.js';
import { groupCaption } from '../lib/group-name.js';
import {
    invitationLink,
    invitationUntilText,
    shortenedAddress,
    siteFromHost,
} from '../lib/join.js';
import { outsideTheBox } from '../lib/outside-the-box.js';
import { looksLikeAnEmail } from '../lib/membership.js';
import { shortName } from '../lib/members.js';
import { standingSearches } from '../lib/searches.js';
import { SPEAKING, WORDS } from '../lib/words.js';
import { makeInvitation, removeMember, renameGroup, setRunning } from '../lib/store-group.js';
import { elements, state } from './state.js';
import { aboutThem, askConfirm, phoneText } from './dialogs.js';
import { adminSearch, untilText } from './search-ui.js';
import {
    closeOnClickOutside,
    drawGroup,
    iconsInRow,
    searchButton,
    sirenButtonFor,
    tryGroupWrite,
} from './group-panel.js';
import { resetButtonFor } from './reset-ui.js';

/**
 * The invitation just made, built into the link it is sent as, or null
 * before one exists.
 *
 * Built here rather than kept in `state` alongside the fields it is made
 * from: the group's name and this viewer's own published name can both
 * change while the invitation stands on screen, and a link built once at
 * creation would go stale under it.
 */
function currentInvitationLink() {
    if (state.invitation === null) {
        return null;
    }

    return invitationLink({
        site: siteFromHost(window.location.host),
        groupId: state.invitation.groupId,
        code: state.invitation.code,
        by: state.accountProfiles?.get(state.member.memberId)?.displayName ?? null,
        name: state.roster?.name ?? null,
        email: state.invitation.email,
    });
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
export function drawAdmin(running) {
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
            new Map(state.positions.map((position) => [position.positionId, position.knownAt])),
        ).map(adminRow),
    );

    elements.adminSearches.replaceChildren(
        ...[...standingSearches(state.searches, Date.now()).values()].map(adminSearch),
    );

    elements.adminMakeInvitation.textContent =
        state.invitation === null ? WORDS.groupMakeInvitation : WORDS.groupMakeAnotherInvitation;

    const link = currentInvitationLink();

    elements.adminInvitation.hidden = link === null;

    if (link !== null) {
        elements.adminInvitationTitle.textContent =
            WORDS.groupInvitationTitle(shortenedAddress(state.invitation.email));
        elements.adminInvitationLink.textContent = link;
        elements.adminInvitationNote.textContent = WORDS.groupInvitationNote(
            invitationUntilText(state.invitation.expiresAt.getTime(), SPEAKING),
        );
    }

    // Shown only where the browser itself offers a share sheet: the link is
    // always there to copy, whether or not this one does.
    elements.adminShare.hidden = typeof navigator.share !== 'function';
    elements.adminShare.textContent = WORDS.groupShare;
    elements.adminCopy.textContent = WORDS.groupCopy;

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
    // Plain and before the siren, for every phone the siren is offered for.
    actions.append(resetButtonFor(phone, now, about));
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

export function openAdmin() {
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
 * Opens the window that asks who an invitation is made out to, on top of the
 * panel that runs the group, with an empty address rather than one left over
 * from the last invitation: an address is not typed twice by mistake the way
 * a group's own name might be left standing.
 */
function openInvite() {
    elements.inviteEmail.value = '';
    drawInvite();

    if (!elements.invite.open) {
        elements.invite.showModal();
    }
}

/**
 * What the invitation window says, and whether making one is worth trying:
 * the rules refuse an address that does not look like one at all
 * (`looksLikeAnEmail`), and the button says so before the write is asked for
 * rather than after it is refused.
 */
function drawInvite() {
    elements.inviteTitle.textContent = WORDS.groupInviteTitle(
        groupCaption(state.roster, state.profiles, state.member.memberId),
    );
    elements.inviteEmail.placeholder = WORDS.groupInviteEmailLabel;
    elements.inviteNote.textContent = WORDS.groupInviteNote;
    elements.inviteCreate.textContent = WORDS.groupInviteCreate;
    elements.inviteCancel.textContent = WORDS.nameCancel;
    elements.inviteCreate.disabled = !looksLikeAnEmail(elements.inviteEmail.value.trim());
}

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

elements.inviteEmail.addEventListener('input', drawInvite);
elements.inviteCancel.addEventListener('click', () => elements.invite.close());
elements.inviteCreate.addEventListener('click', () => {
    const email = elements.inviteEmail.value.trim();

    elements.invite.close();
    tryGroupWrite(async () => {
        state.invitation = await makeInvitation(state.member.memberId, email);
    });
});
closeOnClickOutside(elements.invite, () => elements.invite.close());

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

elements.adminMakeInvitation.addEventListener('click', openInvite);

elements.adminShare.addEventListener('click', async () => {
    const link = currentInvitationLink();

    if (link === null) {
        return;
    }

    try {
        await navigator.share({ url: link });
    } catch {
        // Closing the share sheet without choosing anybody throws
        // AbortError, which is not a failure the panel has anything to say
        // about — the link stays on screen, to copy instead.
    }
});

elements.adminCopy.addEventListener('click', async () => {
    const link = currentInvitationLink();

    if (link === null) {
        return;
    }

    try {
        await navigator.clipboard.writeText(link);
        state.groupSaid = WORDS.groupCopied;
    } catch {
        // A browser that refuses the clipboard leaves the invitation on
        // screen, where it can be selected by hand.
        state.groupSaid = null;
    }

    drawGroup();
});
