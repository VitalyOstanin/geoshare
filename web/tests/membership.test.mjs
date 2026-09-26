import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    ADMINISTRATOR,
    MEMBER,
    UNGROUPED,
    UNRUN,
    invitationFrom,
    invitationText,
    looksLikeACode,
    looksLikeAGroupName,
    mayBeMadeAdmin,
    mayBeRemoved,
    mayStopRunning,
    newInvitationCode,
    rosterWithout,
    runsTheGroup,
    standingIn,
    tidyCode,
} from '../public/lib/membership.js';

const ALICE = 'alice-uid';
const BOB = 'bob-uid';
const STRANGER = 'stranger-uid';

test('an account with no group at all is offered a way into one', () => {
    assert.equal(standingIn(null, ALICE), UNGROUPED);
});

// A group this account is not on reads the same as no group at all: the
// document is closed to everybody outside it, so the refusal is all there is.
test('an account off the roster stands where an account with no group stands', () => {
    const roster = { members: [ALICE], admins: [ALICE] };

    assert.equal(standingIn(roster, STRANGER), UNGROUPED);
});

test('the roster tells a member from the one running the group', () => {
    const roster = { members: [ALICE, BOB], admins: [ALICE] };

    assert.equal(standingIn(roster, ALICE), ADMINISTRATOR);
    assert.equal(standingIn(roster, BOB), MEMBER);
});

// The household that predates invitations: the rules let nobody change such a
// roster, so the menu says so rather than offering controls whose every write
// would be refused.
test('a group nobody runs is its own standing', () => {
    assert.equal(standingIn({ members: [ALICE, BOB], admins: [] }, ALICE), UNRUN);
});

test('removing a member takes the running of the group with them', () => {
    const roster = { members: [ALICE, BOB], admins: [ALICE, BOB] };

    assert.deepEqual(rosterWithout(roster, BOB), { members: [ALICE], admins: [ALICE] });
});

test('the last administrator cannot be removed', () => {
    const roster = { members: [ALICE, BOB], admins: [ALICE] };

    assert.equal(mayBeRemoved(roster, ALICE), false);
    assert.equal(mayBeRemoved(roster, BOB), true);
    assert.equal(mayBeRemoved(roster, STRANGER), false);
});

// The household made in the console names no administrators, and its owner
// runs it all the same once the document says who that is.
test('the founder runs the group without being named among its administrators', () => {
    const roster = { members: [ALICE, BOB], admins: [], founder: ALICE };

    assert.equal(standingIn(roster, ALICE), ADMINISTRATOR);
    assert.equal(standingIn(roster, BOB), MEMBER);
    assert.equal(runsTheGroup(roster, ALICE), true);
});

test('a founder who has left the roster runs nothing', () => {
    const roster = { members: [BOB], admins: [], founder: ALICE };

    assert.equal(runsTheGroup(roster, ALICE), false);
    assert.equal(standingIn(roster, BOB), UNRUN);
});

test('the founder is taken off by nobody but themselves', () => {
    const roster = { members: [ALICE, BOB], admins: [ALICE, BOB], founder: ALICE };

    assert.equal(mayBeRemoved(roster, ALICE, BOB), false);
    assert.equal(mayBeRemoved(roster, BOB, ALICE), true);
});

test('an administrator is taken off only by the founder', () => {
    const CAROL = 'carol-uid';
    const roster = { members: [ALICE, BOB, CAROL], admins: [ALICE, BOB, CAROL], founder: ALICE };

    assert.equal(mayBeRemoved(roster, CAROL, BOB), false);
    assert.equal(mayBeRemoved(roster, CAROL, ALICE), true);
});

test('an invitation says which group it lets somebody into', () => {
    assert.equal(invitationText(ALICE, 'ABCD2345'), `${ALICE}.ABCD2345`);
    assert.deepEqual(invitationFrom(`${ALICE}.ABCD2345`), { group: ALICE, code: 'ABCD2345' });
});

// A message carrying one is broken across lines by whatever it travelled in,
// and the code half is read aloud as often as it is pasted.
test('an invitation arrives as it was sent however it travelled', () => {
    assert.deepEqual(invitationFrom(` ${ALICE}.abcd-2345 `), {
        group: ALICE,
        code: 'ABCD2345',
    });
    assert.deepEqual(invitationFrom(`${ALICE}.\nABCD 2345`), {
        group: ALICE,
        code: 'ABCD2345',
    });
});

// The case of an account id is the difference between two accounts, so the
// half that names the group is passed on exactly as it came.
test('the group half of an invitation keeps its case', () => {
    assert.deepEqual(invitationFrom('Ab3xYz.abcd2345'), { group: 'Ab3xYz', code: 'ABCD2345' });
});

// The group becomes part of a document path, and the code is written into a
// document whose shape the rules hold to exactly eight characters.
test('an invitation that would not reach a document is refused', () => {
    assert.equal(invitationFrom('ABCD2345'), null);
    assert.equal(invitationFrom('.ABCD2345'), null);
    assert.equal(invitationFrom(`${ALICE}.`), null);
    assert.equal(invitationFrom(`${ALICE}.ABCD234`), null);
    assert.equal(invitationFrom('../other.ABCD2345'), null);
    assert.equal(invitationFrom('groups/other.ABCD2345'), null);
});

test('a group is named the way an account is', () => {
    assert.equal(looksLikeAGroupName(ALICE), true);
    assert.equal(looksLikeAGroupName('family'), true);
    assert.equal(looksLikeAGroupName(''), false);
    assert.equal(looksLikeAGroupName('a'.repeat(129)), false);
});

test('a typed code is tidied into the one that was sent', () => {
    assert.equal(tidyCode('abcd-2345'), 'ABCD2345');
    assert.equal(tidyCode('abcd–2345'), 'ABCD2345');
});

test('what the rules would refuse is refused before it is written', () => {
    assert.equal(looksLikeACode('SHORT'), false);
    assert.equal(looksLikeACode('ABCD2340'), false);
    assert.equal(looksLikeACode('ABCD2345'), true);
});

// The letters and digits read for each other are left out of what is handed
// out, and the string is drawn from the browser's own randomness.
test('a code carries neither the letters nor the digits read for each other', () => {
    const codes = Array.from({ length: 200 }, () => newInvitationCode());

    assert.equal(codes.every((code) => looksLikeACode(code)), true);
    assert.equal(codes.some((code) => /[IO01]/u.test(code)), false);
    assert.equal(new Set(codes).size > 190, true);
});

const CAROL = 'carol-uid';

test('whoever runs the group hands it to a member who does not run it yet', () => {
    const roster = { members: [ALICE, BOB, CAROL], admins: [ALICE, BOB] };

    assert.equal(mayBeMadeAdmin(roster, CAROL, ALICE), true);
    assert.equal(mayBeMadeAdmin(roster, BOB, ALICE), false);
    assert.equal(mayBeMadeAdmin(roster, STRANGER, ALICE), false);
    assert.equal(mayBeMadeAdmin(roster, ALICE, CAROL), false);
});

test('the founder is handed nothing: they run the group unnamed', () => {
    const roster = { members: [ALICE, BOB], admins: [], founder: ALICE };

    assert.equal(mayBeMadeAdmin(roster, ALICE, ALICE), false);
    assert.equal(mayBeMadeAdmin(roster, BOB, ALICE), true);
});

// The rules let an administrator who is not the founder relieve another, and
// themselves: only the founder's place is out of their reach.
test('an administrator relieves another, and may give the role up', () => {
    const roster = { members: [ALICE, BOB, CAROL], admins: [ALICE, BOB] };

    assert.equal(mayStopRunning(roster, BOB, ALICE), true);
    assert.equal(mayStopRunning(roster, ALICE, ALICE), true);
    assert.equal(mayStopRunning(roster, CAROL, ALICE), false);
    assert.equal(mayStopRunning(roster, ALICE, CAROL), false);
});

test('the last administrator keeps the role while no founder runs the group', () => {
    const roster = { members: [ALICE, BOB], admins: [ALICE] };

    assert.equal(mayStopRunning(roster, ALICE, ALICE), false);
});

test('with the founder there, the last named administrator may go', () => {
    const roster = { members: [ALICE, BOB], admins: [BOB], founder: ALICE };

    assert.equal(mayStopRunning(roster, BOB, ALICE), true);
    assert.equal(mayStopRunning(roster, BOB, BOB), true);
});

// The household of the project owner names them among the administrators as
// well; taking the name off would change nothing, and only they may.
test('the founder is not offered to be relieved even when named', () => {
    const roster = { members: [ALICE, BOB], admins: [ALICE, BOB], founder: ALICE };

    assert.equal(mayStopRunning(roster, ALICE, BOB), false);
    assert.equal(mayStopRunning(roster, ALICE, ALICE), false);
});
