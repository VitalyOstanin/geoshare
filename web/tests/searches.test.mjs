import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    endIfChosen,
    nextSearchEnd,
    searchOf,
    standingSearches,
    withSearches,
} from '../public/lib/searches.js';

const NOW = Date.parse('2026-09-27T19:40:00Z');
const HOUR = 60 * 60 * 1000;

const written = (renewedHoursAgo, hours, extra = {}) => ({
    openedBy: 'anna',
    startedAt: NOW - 30 * HOUR,
    renewedBy: 'boris',
    renewedAt: NOW - renewedHoursAgo * HOUR,
    hours,
    ...extra,
});

test('a search ends its hours after the last renewal', () => {
    const search = searchOf('grandma.a', written(2, 24));

    assert.equal(search.endsAt, NOW + 22 * HOUR);
    assert.equal(search.memberId, 'grandma');
    assert.equal(search.phoneName, null);
    assert.equal(search.openedBy, 'anna');
    assert.equal(search.renewedBy, 'boris');
    assert.equal(search.startedAt, NOW - 30 * HOUR);
});

test('the device\'s name comes along when the administrator gave one', () => {
    const search = searchOf('grandma.a', written(2, 24, { phoneName: 'Old phone' }));

    assert.equal(search.phoneName, 'Old phone');
});

test('a document that is not a search is none', () => {
    assert.equal(searchOf('grandma.a', {}), null);
    assert.equal(searchOf('grandma.a', written(1, 1.5)), null);
    assert.equal(searchOf('grandma.a', written(1, 0)), null);
    assert.equal(searchOf('grandma.a', { ...written(1, 6), renewedAt: undefined }), null);
    assert.equal(searchOf('grandma.a', { ...written(1, 6), openedBy: 7 }), null);
});

// An id without a dot is a search of the old, per-member shape, which the
// rules no longer accept from a client: it is ignored rather than read as a
// device's, the way a document of the wrong shape always is.
test('a search document without a dot in its id is ignored', () => {
    assert.equal(searchOf('grandma', written(1, 6)), null);
});

test('a search written before its start came back starts at its renewal', () => {
    const search = searchOf('grandma.a', { ...written(0, 6), startedAt: undefined });

    assert.equal(search.startedAt, NOW);
});

test('only the searches still standing are kept', () => {
    const searches = new Map([
        ['grandma.a', searchOf('grandma.a', written(2, 24))],
        ['uncle.a', searchOf('uncle.a', written(7, 6))],
    ]);

    assert.deepEqual([...standingSearches(searches, NOW).keys()], ['grandma.a']);
});

// Several devices may be looked for at once, of one member or of several: the
// family may have lost two of grandma's devices together, and be looking for
// uncle's at the same time.
test('two devices of one member stand as two searches', () => {
    const searches = new Map([
        ['grandma.a', searchOf('grandma.a', written(1, 6))],
        ['grandma.b', searchOf('grandma.b', written(2, 24))],
    ]);

    assert.deepEqual(
        [...standingSearches(searches, NOW).keys()].sort(),
        ['grandma.a', 'grandma.b'],
    );
});

test('devices of two members stand at once', () => {
    const searches = new Map([
        ['grandma.a', searchOf('grandma.a', written(1, 6))],
        ['uncle.a', searchOf('uncle.a', written(1, 6))],
    ]);

    assert.deepEqual(
        [...standingSearches(searches, NOW).keys()].sort(),
        ['grandma.a', 'uncle.a'],
    );
});

test('every position is keyed by the same id its search stands under', () => {
    const search = searchOf('grandma.a', written(1, 6));
    const searches = new Map([['grandma.a', search]]);

    // The search's own id is the position it opens, so this is what a
    // subscription built from the standing searches has to ask for.
    assert.deepEqual([...standingSearches(searches, NOW).keys()], ['grandma.a']);
});

test('only the device looked for is marked, and neither its sibling nor a stranger', () => {
    const search = searchOf('grandma.a', written(1, 6));
    const positions = [
        { positionId: 'grandma.a', memberId: 'grandma' },
        { positionId: 'grandma.b', memberId: 'grandma' },
        { positionId: 'anna.a', memberId: 'anna' },
    ];

    const marked = withSearches(positions, new Map([['grandma.a', search]]), NOW);

    assert.deepEqual(
        marked.map((position) => position.sought?.phoneId ?? null),
        ['grandma.a', null, null],
    );
});

test('a device with no name of its own is called what the search saw', () => {
    const search = searchOf('grandma.a', written(1, 6, { phoneName: 'Old phone' }));
    const positions = [{ positionId: 'grandma.a', memberId: 'grandma', deviceName: null }];

    const marked = withSearches(positions, new Map([['grandma.a', search]]), NOW);

    assert.equal(marked[0].deviceName, 'Old phone');
});

test('a device already named keeps its own name over the search\'s', () => {
    const search = searchOf('grandma.a', written(1, 6, { phoneName: 'Old phone' }));
    const positions = [{ positionId: 'grandma.a', memberId: 'grandma', deviceName: 'Grandma\'s' }];

    const marked = withSearches(positions, new Map([['grandma.a', search]]), NOW);

    assert.equal(marked[0].deviceName, 'Grandma\'s');
});

test('a search that has run out marks nobody', () => {
    const search = searchOf('grandma.a', written(7, 6));
    const positions = [{ positionId: 'grandma.a', memberId: 'grandma', sought: search }];

    const marked = withSearches(positions, new Map([['grandma.a', search]]), NOW);

    assert.equal(marked[0].sought, null);
});

test('the next moment to look again is the nearest end', () => {
    const searches = new Map([
        ['grandma.a', searchOf('grandma.a', written(2, 24))],
        ['uncle.a', searchOf('uncle.a', written(1, 6))],
        ['gone.a', searchOf('gone.a', written(9, 6))],
    ]);

    assert.equal(nextSearchEnd(searches, NOW), NOW + 5 * HOUR);
    assert.equal(nextSearchEnd(new Map(), NOW), null);
});

test('a choice of hours is an end counted from now', () => {
    assert.equal(endIfChosen(72, NOW), NOW + 72 * HOUR);
});
