import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    colourIndex,
    memberInitial,
    memberName,
    orderedForNavigation,
    pictureAddress,
    shortName,
} from '../public/lib/members.js';

// One position per phone: the id is the account and the phone joined by a dot.
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
    assert.equal(colourIndex('u7ExampleOwnerUid00000000000'), 1);
});

test('a name is the first characters of the id', () => {
    assert.equal(shortName('u7ExampleOwnerUid00000000000'), 'E9WyZT');
});

// The same four cases the Android client checks, so that a member is called
// the same thing on the phone and on the laptop.
test('a member is called what their profile says', () => {
    assert.equal(memberName({ displayName: 'Ann', photoUrl: null }, 'her', false), 'Ann');
});

test('a member without a profile is called by the start of their id', () => {
    assert.equal(memberName(undefined, 'u7ExampleOwnerUid00000000000', false), 'E9WyZT');
});

test('our own entry says "you", profile or not', () => {
    assert.equal(memberName({ displayName: 'Ann', photoUrl: null }, 'me', true), 'You');
    assert.equal(memberName(undefined, 'me', true), 'You');
});

// The initial is what a member without a picture is drawn as, and the phone
// takes it the same way; these are the cases where the two could drift apart.
test('the initial is the first letter of the name, in capitals', () => {
    assert.equal(memberInitial('Ann'), 'A');
    assert.equal(memberInitial('  oona  '), 'O');
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

// The same rule the Android panel follows: a member carrying two phones has
// them next to each other, in the same places at every glance.
test('the phones of one member keep their places', () => {
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
