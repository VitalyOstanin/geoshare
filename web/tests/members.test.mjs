import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    colourIndex,
    freshestOf,
    memberInitial,
    memberName,
    orderedForNavigation,
    pictureAddress,
    positionDocuments,
    profileName,
    shortName,
} from '../public/lib/members.js';

// One position per device: the id is the account and the device joined by a
// dot.
const positionOf = (memberId, device = 'phone') => ({
    positionId: `${memberId}.${device}`,
    memberId,
    latitude: 60,
    longitude: 30,
});
const idsOf = (positions) => positions.map((position) => position.positionId);

test('the other members come before our own entry', () => {
    const order = orderedForNavigation([positionOf('me'), positionOf('her')], 'me');

    assert.deepEqual(idsOf(order), ['her.phone', 'me.phone']);
});

test('a grant naming more devices than a person carries is followed only so far', () => {
    const grants = new Map([
        ['her', { memberId: 'her', devices: Array.from({ length: 30 }, (unused, index) => `p${index}`) }],
    ]);

    assert.equal(positionDocuments(grants).length, 10);
});

test('the panel sorts ids the way the device does, by the characters themselves', () => {
    const members = [positionOf('Zeta'), positionOf('alpha'), positionOf('Beta')];

    assert.deepEqual(
        idsOf(orderedForNavigation(members, 'me')),
        ['Beta.phone', 'Zeta.phone', 'alpha.phone'],
    );
});

test('the others keep the same places whatever order they arrive in', () => {
    const members = [positionOf('c'), positionOf('a'), positionOf('b')];

    assert.deepEqual(idsOf(orderedForNavigation(members, 'me')), ['a.phone', 'b.phone', 'c.phone']);
    assert.deepEqual(
        idsOf(orderedForNavigation(members, 'me')),
        idsOf(orderedForNavigation([...members].reverse(), 'me')),
    );
});

// The indices below are what the Android client computes for the same ids, and
// the test exists so that a change to either side is caught here rather than
// by a member changing colour between two screens.
test('the colour index matches the one the Android client takes', () => {
    assert.equal(colourIndex(''), 0);
    assert.equal(colourIndex('a'), 2);
    assert.equal(colourIndex('member'), 4);
    assert.equal(colourIndex('Qm7d0Kh8oOOL8dKLzdocJ2isAjIh'), 1);
});

test('a name is the first characters of the id', () => {
    assert.equal(shortName('Qm7d0Kh8oOOL8dKLzdocJ2isAjIh'), 'Qm7d0K');
});

// The same four cases the Android client checks, so that a member is called
// the same thing on the device and on the laptop.
test('a member is called what their profile says', () => {
    assert.equal(memberName({ displayName: 'Ann', photoUrl: null }, 'her', false), 'Ann');
});

test('a member without a profile is called by the start of their id', () => {
    assert.equal(memberName(undefined, 'Qm7d0Kh8oOOL8dKLzdocJ2isAjIh', false), 'Qm7d0K');
});

test('our own entry says "you", profile or not', () => {
    assert.equal(memberName({ displayName: 'Ann', photoUrl: null }, 'me', true), 'You');
    assert.equal(memberName(undefined, 'me', true), 'You');
});

// The initial is what a member without a picture is drawn as, and the device
// takes it the same way; these are the cases where the two could drift apart.
test('the initial is the first letter of the name, in capitals', () => {
    assert.equal(memberInitial('Ann'), 'A');
    assert.equal(memberInitial('  anna  '), 'A');
});

test('a name with nothing in it leaves no initial', () => {
    assert.equal(memberInitial('   '), '');
});

// A profile is written by the member it describes, so an address in it can be
// anything at all; only a fetchable picture reaches the marker.
test('a picture is drawn when its address can be fetched', () => {
    assert.equal(
        pictureAddress('https://lh3.googleusercontent.com/a/example'),
        'https://lh3.googleusercontent.com/a/example',
    );
});

test('an address in any other scheme draws no picture', () => {
    assert.equal(pictureAddress('javascript:alert(1)'), null);
    assert.equal(pictureAddress('file:///etc/passwd'), null);
    assert.equal(pictureAddress('not an address'), null);
    assert.equal(pictureAddress(null), null);
});

// Every viewer fetches it, so an address on a server of the member's choosing
// would tell that server where each of them is online from.
test('a picture anywhere but the account host draws no picture', () => {
    assert.equal(pictureAddress('https://example.com/me.png'), null);
    assert.equal(pictureAddress('http://lh3.googleusercontent.com/a/x'), null);
    assert.equal(pictureAddress('https://example.com/lh3.googleusercontent.com/a/x'), null);
    assert.equal(pictureAddress('https://googleusercontent.com.example.com/a/x'), null);
    assert.equal(pictureAddress('https://x@example.com/.googleusercontent.com/a'), null);
    assert.equal(pictureAddress('https://colab.googleusercontent.com/a/x'), null);
});

// The same rule the Android panel follows: a member carrying two devices has
// them next to each other, in the same places at every glance.
test('the devices of one member keep their places', () => {
    const members = [positionOf('a', 'two'), positionOf('b'), positionOf('a', 'one')];

    assert.deepEqual(idsOf(orderedForNavigation(members, 'me')), [
        'a.one',
        'a.two',
        'b.phone',
    ]);
    assert.deepEqual(
        idsOf(orderedForNavigation(members, 'me')),
        idsOf(orderedForNavigation([...members].reverse(), 'me')),
    );
});

const grantWith = (memberId, devices) => [memberId, { memberId, devices }];

test('a document is named for every device of every member', () => {
    const grants = new Map([grantWith('alice', ['one', 'two']), grantWith('bob', ['phone'])]);

    assert.deepEqual(positionDocuments(grants), ['alice.one', 'alice.two', 'bob.phone']);
});

// A member who has signed in but never written a position has no device in
// their grant yet, and there is nothing to subscribe to for them.
test('a member without devices is left out', () => {
    assert.deepEqual(positionDocuments(new Map([grantWith('alice', [])])), []);
});

// The list decides whether the subscriptions are rebuilt, so the same group in
// a different order has to come out the same.
test('the documents do not depend on the order the devices arrived in', () => {
    const one = positionDocuments(new Map([grantWith('alice', ['two', 'one'])]));
    const other = positionDocuments(new Map([grantWith('alice', ['one', 'two'])]));

    assert.deepEqual(one, other);
});

test('a member goes by the name of their account', () => {
    assert.equal(profileName('  Anna Petrova ', 'anna.petrova@example.com'), 'Anna Petrova');
});

test('an account without a name goes by its shortened address', () => {
    assert.equal(profileName(null, 'tester@example.com'), 'test…@example.com');
    assert.equal(profileName('   ', 'tester@example.com'), 'test…@example.com');
});

test('an account with neither has nothing to say', () => {
    assert.equal(profileName(null, null), null);
    assert.equal(profileName('', ' '), null);
});

test('the freshest position of a member is the one last heard from', () => {
    const positions = [
        { memberId: 'anna', positionId: 'anna.old', knownAt: 1000 },
        { memberId: 'mark', positionId: 'mark.s24', knownAt: 9000 },
        { memberId: 'anna', positionId: 'anna.new', knownAt: 5000 },
    ];

    assert.equal(freshestOf(positions, 'anna').positionId, 'anna.new');
    assert.equal(freshestOf(positions, 'leo'), null);
});
