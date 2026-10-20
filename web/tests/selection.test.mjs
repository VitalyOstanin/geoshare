import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    listShown,
    markerTapped,
    memberTapCloses,
    openMember,
    selectedPositionId,
    shownPositionIds,
} from '../public/lib/selection.js';

test('tapping the marker whose card is already open closes it', () => {
    const selection = { key: 'a', positionId: 'a' };

    assert.equal(markerTapped(selection, 'a', ['a']), null);
});

test('tapping a marker with one member opens that member\'s card directly', () => {
    assert.deepEqual(markerTapped(null, 'a', ['a']), { key: 'a', positionId: 'a' });
});

test('tapping a marker with several members opens the list of them', () => {
    assert.deepEqual(markerTapped(null, 'a,b', ['a', 'b']), { key: 'a,b', positionId: null });
});

test('tapping a marker with nobody under it opens nothing', () => {
    assert.equal(markerTapped(null, 'a', []), null);
});

test('tapping a different marker replaces the selection instead of closing it', () => {
    const selection = { key: 'a', positionId: 'a' };

    assert.deepEqual(markerTapped(selection, 'b', ['b']), { key: 'b', positionId: 'b' });
});

test('tapping the strip entry of the member whose card is open closes it', () => {
    assert.equal(memberTapCloses({ key: 'a', positionId: 'a' }, 'a'), true);
});

test('tapping another member in the strip opens their card instead', () => {
    assert.equal(memberTapCloses({ key: 'a', positionId: 'a' }, 'b'), false);
});

test('tapping a member in the strip over the list of a marker opens their card', () => {
    assert.equal(memberTapCloses({ key: 'a,b', positionId: null }, 'a'), false);
});

test('tapping a member in the strip with nothing open opens their card', () => {
    assert.equal(memberTapCloses(null, 'a'), false);
});

test('opening a member by name sets both the marker key and the member', () => {
    assert.deepEqual(openMember('a,b', 'b'), { key: 'a,b', positionId: 'b' });
});

test('going back to the list keeps the marker key and clears which member', () => {
    const selection = { key: 'a,b', positionId: 'b' };

    assert.deepEqual(listShown(selection), { key: 'a,b', positionId: null });
});

test('going back to the list with nothing open stays closed', () => {
    assert.equal(listShown(null), null);
});

// The member picked from a list stands on a pin of their own while the card is
// open, so the way back is the list they were picked from, not their own pin.
test('a member picked from a list goes back to that list', () => {
    const picked = openMember('b', 'b', 'a,b,c');

    assert.deepEqual(picked, { key: 'b', positionId: 'b', list: 'a,b,c' });
    assert.deepEqual(listShown(picked), { key: 'a,b,c', positionId: null });
});

test('the strip highlights nobody while nothing is selected', () => {
    assert.deepEqual(shownPositionIds(null, ['a', 'b']), new Set());
});

test('the strip highlights every member under an open list', () => {
    const selection = { key: 'a,b', positionId: null };

    assert.deepEqual(shownPositionIds(selection, ['a', 'b']), new Set(['a', 'b']));
});

test('the strip highlights only the one member whose card is open', () => {
    const selection = { key: 'a,b', positionId: 'b' };

    assert.deepEqual(shownPositionIds(selection, ['a', 'b']), new Set(['b']));
});

test('the accuracy circle has nobody to draw around while nothing is selected', () => {
    assert.equal(selectedPositionId(null), null);
});

test('the accuracy circle is drawn around the member whose card is open', () => {
    const selection = { key: 'a,b', positionId: 'b' };

    assert.equal(selectedPositionId(selection), 'b');
});

test('the accuracy circle is not drawn while the list is open', () => {
    const selection = { key: 'a,b', positionId: null };

    assert.equal(selectedPositionId(selection), null);
});

test('none of the functions mutate the selection or the id arrays passed in', () => {
    const selection = { key: 'a,b', positionId: 'b' };
    const frozenSelection = Object.freeze({ ...selection });
    const positionIds = Object.freeze(['a', 'b']);

    markerTapped(frozenSelection, 'a,b', positionIds);
    listShown(frozenSelection);
    shownPositionIds(frozenSelection, positionIds);
    selectedPositionId(frozenSelection);

    assert.deepEqual(frozenSelection, selection);
    assert.deepEqual(positionIds, ['a', 'b']);
});
