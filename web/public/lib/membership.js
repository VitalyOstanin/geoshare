// What a group is, and what the person looking at one may do about it.
//
// Nothing here touches the database or the page: given a roster and an account
// it says where that account stands, and given what somebody pasted it says
// whether it is an invitation. The same rules are written in the application,
// and the rules of the database weigh the same shapes again — this is the
// half that can be tested without either.

/**
 * Where an account stands in a group.
 *
 * Four of them because the screen shows a different thing in each, and two of
 * them look alike from outside: a group that has not been made and a group
 * this account is not on both read as no roster at all, since a group document
 * is closed to everybody who is not in it.
 */
export const UNGROUPED = 'ungrouped';
export const MEMBER = 'member';
export const ADMINISTRATOR = 'administrator';

/**
 * A group nobody runs: the household that started before invitations existed
 * has no administrators at all, and the rules let nobody change such a roster.
 * The screen says so rather than offering controls whose every write would be
 * refused.
 */
export const UNRUN = 'unrun';

export function standingIn(roster, memberId) {
    if (roster === null || !roster.members.includes(memberId)) {
        return UNGROUPED;
    }

    if (roster.admins.length === 0) {
        return UNRUN;
    }

    return roster.admins.includes(memberId) ? ADMINISTRATOR : MEMBER;
}

/**
 * Whether a member may be taken off the roster.
 *
 * The last administrator may not: a group whose roster nobody may change is a
 * group that goes back to the console. The rules refuse such a write, and
 * asking here is what keeps the screen from offering it.
 */
export function mayBeRemoved(roster, memberId) {
    if (!roster.members.includes(memberId)) {
        return false;
    }

    return !roster.admins.includes(memberId) || roster.admins.length > 1;
}

/** The roster as it stands once somebody has been taken out of it. */
export function rosterWithout(roster, memberId) {
    return {
        members: roster.members.filter((id) => id !== memberId),
        admins: roster.admins.filter((id) => id !== memberId),
    };
}

/** How long a code is, and what it is made of, as the rules hold it. */
const CODE_LENGTH = 8;
const CODE_CHARACTERS = /^[A-Z2-9]{8}$/;

/**
 * What a code handed out from here is made of.
 *
 * The letters and digits that are read for each other are left out: I and 1,
 * O and 0. The rules allow the whole of A to Z and 2 to 9, so a code from the
 * application is still accepted — this is what is handed out, not what is let
 * in.
 */
const ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

/**
 * How long an invitation lasts, in milliseconds.
 *
 * A day, the same as the application hands out, because the code cannot be
 * spent: adding oneself to the roster and marking the invitation used are two
 * writes, and nothing makes the second follow the first. What limits a code is
 * the time it stands rather than the number of people it lets in.
 */
export const INVITATION_LASTS_MS = 24 * 60 * 60 * 1000;

/**
 * A code nobody has had before.
 *
 * Drawn from the browser's own source of randomness rather than from
 * `Math.random`: what this string is worth is that nobody guesses it before it
 * runs out.
 */
export function newInvitationCode(random = crypto) {
    const drawn = new Uint32Array(CODE_LENGTH);

    random.getRandomValues(drawn);

    return [...drawn].map((value) => ALPHABET[value % ALPHABET.length]).join('');
}

/**
 * How long a name of a group may be: the limit an account id is held to by the
 * service that issues one. What the number is for is stopping a name of any
 * length from being typed into a path, not describing the ids.
 */
const GROUP_NAME_LIMIT = 128;

const GROUP_NAME_CHARACTERS = /^[A-Za-z0-9_-]+$/;

/**
 * The single string an invitation is handed over as.
 *
 * The name of the group travels with the code because the rule weighing
 * somebody's addition to a roster weighs a write to a path, and a path has to
 * be known before the write is made: a code alone would say which door to
 * knock at only if there were one door.
 */
export function invitationText(group, code) {
    return `${group}.${code}`;
}

/**
 * An invitation as somebody pasted it, read back into its two halves, or
 * nothing at all when it is not one.
 *
 * The dot is taken from the end rather than the start: the code has none, and
 * a group named before the rule that names a founder might. Only the code is
 * put into upper case — the group is an account id, where case is the
 * difference between two accounts.
 */
export function invitationFrom(typed) {
    const whole = typed.replace(/\s/gu, '');
    const dot = whole.lastIndexOf('.');

    if (dot <= 0) {
        return null;
    }

    const group = whole.slice(0, dot);
    const code = tidyCode(whole.slice(dot + 1));

    if (!looksLikeAGroupName(group) || !looksLikeACode(code)) {
        return null;
    }

    return { group, code };
}

/**
 * A typed code as the rules will see it.
 *
 * People type what they see: lower case, and the dashes a long string is
 * broken up with on the screen it was read from. None of that is part of the
 * code, and rejecting it would be rejecting somebody who typed it right.
 */
export function tidyCode(typed) {
    return typed.replaceAll('-', '').replaceAll('–', '').toUpperCase();
}

/** Whether a tidied code is one the rules would accept at all. */
export function looksLikeACode(tidied) {
    return tidied.length === CODE_LENGTH && CODE_CHARACTERS.test(tidied);
}

/**
 * Whether a name is one a group could have.
 *
 * Held to it because the name becomes part of a document path: what is refused
 * here is a name that would reach somewhere else in the database, and the
 * refusal is a sentence on the screen rather than an error from the server.
 */
export function looksLikeAGroupName(name) {
    return name.length > 0 &&
        name.length <= GROUP_NAME_LIMIT &&
        GROUP_NAME_CHARACTERS.test(name);
}
