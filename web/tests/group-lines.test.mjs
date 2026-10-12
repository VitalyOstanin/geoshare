import assert from 'node:assert/strict';
import { test } from 'node:test';

import { groupOverview, phoneLines } from '../public/lib/group-lines.js';
import { labelsFrom } from '../public/lib/labels.js';
import { combinedGrants, phonesOf } from '../public/lib/phones.js';

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
// owns: a grant nobody may read says nothing about how many devices there are.
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

test('the panel offers the role to a member and takes it from an administrator', () => {
    const two = { members: [PARENT, SPOUSE, CHILD], admins: [PARENT, SPOUSE] };
    const lines = groupOverview(two, profiles, nothingShared, PARENT);

    assert.equal(lineFor(lines, CHILD).mayBeMadeAdmin, true);
    assert.equal(lineFor(lines, CHILD).mayStopRunning, false);
    assert.equal(lineFor(lines, SPOUSE).mayBeMadeAdmin, false);
    assert.equal(lineFor(lines, SPOUSE).mayStopRunning, true);
    assert.equal(lineFor(lines, PARENT).mayStopRunning, true);
});

// The panel is drawn before the first answer about the profiles arrives, and a
// list with no names in it is still a list of the household.
test('a panel drawn before the profiles arrive still lists everybody', () => {
    const lines = groupOverview(roster, null, nothingShared, PARENT);

    assert.equal(lines.length, 3);
    assert.deepEqual(lines.map((line) => line.displayName), [null, null, null]);
});

// An administrator reads the devices without the grant, which is the case the
// panel lists them for: whether a device is in touch is not a secret of whom
// its owner shares with.
test('the phones are listed by the names the map gives them', () => {
    const phones = phonesOf({
        old: { model: 'MAR-LX1', onlineAt: { toMillis: () => 5 } },
        new: { model: 'MAR-LX1', name: 'school phone' },
    });
    const grants = combinedGrants(new Map(), new Map([[CHILD, phones]]));
    const labels = labelsFrom({ devices: { [`${CHILD}.old`]: 'the old one' } });

    const lines = groupOverview(roster, profiles, grants, PARENT, labels);
    const listed = lines.find((line) => line.memberId === CHILD).phones;

    assert.deepEqual(listed.map((phone) => phone.name), ['school phone', 'the old one']);
    assert.equal(listed[1].onlineAt, 5);
});

// The map shows the later of the device's state and its position; the panel
// has to say the same, or the two disagree about one device.
test('a position newer than the phone state sets the moment in touch', () => {
    const phones = new Map([['dev-one', { onlineAt: 1000, batteryPercent: 50, charging: false }]]);
    const [line] = phoneLines(CHILD, phones, undefined, new Map([[`${CHILD}.dev-one`, 5000]]));

    assert.equal(line.onlineAt, 5000);
});

test('a position older than the phone state leaves the state in place', () => {
    const phones = new Map([['dev-one', { onlineAt: 9000, batteryPercent: 50, charging: false }]]);
    const [line] = phoneLines(CHILD, phones, undefined, new Map([[`${CHILD}.dev-one`, 5000]]));

    assert.equal(line.onlineAt, 9000);
});

test('a phone with no state yet is in touch as of its position', () => {
    const phones = new Map([['dev-one', { onlineAt: null, batteryPercent: null, charging: null }]]);
    const [line] = phoneLines(CHILD, phones, undefined, new Map([[`${CHILD}.dev-one`, 5000]]));

    assert.equal(line.onlineAt, 5000);
    assert.equal(phoneLines(CHILD, phones)[0].onlineAt, null);
});
