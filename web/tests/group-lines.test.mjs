import assert from 'node:assert/strict';
import { test } from 'node:test';

import { groupOverview } from '../public/lib/group-lines.js';

const PARENT = 'uid-parent';
const SPOUSE = 'uid-spouse';
const CHILD = 'uid-child';

const roster = { members: [SPOUSE, CHILD, PARENT], admins: [PARENT] };

const profiles = new Map([
    [PARENT, { memberId: PARENT, displayName: 'Parent', pictureUrl: null }],
    [CHILD, { memberId: CHILD, displayName: 'Child', pictureUrl: null }],
]);

const nothingShared = new Map();

const lineFor = (lines, memberId) => lines.find((line) => line.memberId === memberId);

// The panel is read by somebody checking on the others, and the first question
// a list of names raises is which of them is this browser.
test('the person looking is first', () => {
    const lines = groupOverview(roster, profiles, nothingShared, CHILD);

    assert.equal(lines[0].memberId, CHILD);
    assert.equal(lines[0].isYou, true);
});

// An order following the roster would follow the order people happened to join
// in, which is the order nobody can predict.
test('the rest follow the name they are shown under', () => {
    const lines = groupOverview(roster, profiles, nothingShared, CHILD);

    assert.deepEqual(lines.map((line) => line.memberId), [CHILD, PARENT, SPOUSE]);
});

test('a member who published no name is ordered by the id shown in its place', () => {
    const lines = groupOverview(roster, profiles, nothingShared, PARENT);

    assert.equal(lines.at(-1).displayName, null);
    assert.equal(lines.at(-1).memberId, SPOUSE);
});

test('the one running the group is marked', () => {
    const lines = groupOverview(roster, profiles, nothingShared, CHILD);

    assert.equal(lineFor(lines, PARENT).runsTheGroup, true);
    assert.equal(lineFor(lines, SPOUSE).runsTheGroup, false);
});

// What the panel counts is what this viewer may watch, not what the member
// owns: a grant nobody may read says nothing about how many phones there are.
test('the phones counted are the ones this viewer was given', () => {
    const grants = new Map([[CHILD, { memberId: CHILD, devices: ['one', 'two'] }]]);
    const lines = groupOverview(roster, profiles, grants, PARENT);

    assert.equal(lineFor(lines, CHILD).phonesShared, 2);
    assert.equal(lineFor(lines, SPOUSE).phonesShared, 0);
});

// Leaving a group is not the same act as taking somebody out of it, and a row
// offering both in one place is how a click in the wrong place empties a group.
test('nobody is offered their own removal', () => {
    const lines = groupOverview(roster, profiles, nothingShared, PARENT);

    assert.equal(lines.find((line) => line.isYou).removable, false);
    assert.equal(lineFor(lines, CHILD).removable, true);
});

// The rules refuse a roster left with nobody to run it, so the panel must not
// offer the write that would make one.
test('the last administrator is not offered for removal', () => {
    const alone = { members: [PARENT, CHILD], admins: [PARENT] };
    const lines = groupOverview(alone, profiles, nothingShared, CHILD);

    assert.equal(lineFor(lines, PARENT).removable, false);
});

// The panel is drawn before the first answer about the profiles arrives, and a
// list with no names in it is still a list of the household.
test('a panel drawn before the profiles arrive still lists everybody', () => {
    const lines = groupOverview(roster, null, nothingShared, PARENT);

    assert.equal(lines.length, 3);
    assert.deepEqual(lines.map((line) => line.displayName), [null, null, null]);
});
