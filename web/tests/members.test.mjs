import assert from 'node:assert/strict';
import { test } from 'node:test';

import { colourIndex, orderedForNavigation, shortName } from '../public/lib/members.js';

const positionOf = (memberId) => ({ memberId, latitude: 60, longitude: 30 });
const idsOf = (positions) => positions.map((position) => position.memberId);

test('the other members come before our own entry', () => {
    const order = orderedForNavigation([positionOf('me'), positionOf('her')], 'me');

    assert.deepEqual(idsOf(order), ['her', 'me']);
});

test('the others keep the same places whatever order they arrive in', () => {
    const members = [positionOf('c'), positionOf('a'), positionOf('b')];

    assert.deepEqual(idsOf(orderedForNavigation(members, 'me')), ['a', 'b', 'c']);
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
