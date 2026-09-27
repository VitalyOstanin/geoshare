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
    const search = searchOf('grandma', written(2, 24));

    assert.equal(search.endsAt, NOW + 22 * HOUR);
    assert.equal(search.openedBy, 'anna');
    assert.equal(search.renewedBy, 'boris');
    assert.equal(search.startedAt, NOW - 30 * HOUR);
});

test('a document that is not a search is none', () => {
    assert.equal(searchOf('grandma', {}), null);
    assert.equal(searchOf('grandma', written(1, 1.5)), null);
    assert.equal(searchOf('grandma', written(1, 0)), null);
    assert.equal(searchOf('grandma', { ...written(1, 6), renewedAt: undefined }), null);
    assert.equal(searchOf('grandma', { ...written(1, 6), openedBy: 7 }), null);
});

test('a search written before its start came back starts at its renewal', () => {
    const search = searchOf('grandma', { ...written(0, 6), startedAt: undefined });

    assert.equal(search.startedAt, NOW);
});

test('only the searches still standing are kept', () => {
    const searches = new Map([
        ['grandma', searchOf('grandma', written(2, 24))],
        ['uncle', searchOf('uncle', written(7, 6))],
    ]);

    assert.deepEqual([...standingSearches(searches, NOW).keys()], ['grandma']);
});

test('every phone of the member looked for is marked, and nobody else', () => {
    const search = searchOf('grandma', written(1, 6));
    const positions = [
        { positionId: 'grandma.a', memberId: 'grandma' },
        { positionId: 'grandma.b', memberId: 'grandma' },
        { positionId: 'anna.a', memberId: 'anna' },
    ];

    const marked = withSearches(positions, new Map([['grandma', search]]), NOW);

    assert.deepEqual(
        marked.map((position) => position.sought?.memberId ?? null),
        ['grandma', 'grandma', null],
    );
});

test('a search that has run out marks nobody', () => {
    const search = searchOf('grandma', written(7, 6));
    const positions = [{ positionId: 'grandma.a', memberId: 'grandma', sought: search }];

    const marked = withSearches(positions, new Map([['grandma', search]]), NOW);

    assert.equal(marked[0].sought, null);
});

test('the next moment to look again is the nearest end', () => {
    const searches = new Map([
        ['grandma', searchOf('grandma', written(2, 24))],
        ['uncle', searchOf('uncle', written(1, 6))],
        ['gone', searchOf('gone', written(9, 6))],
    ]);

    assert.equal(nextSearchEnd(searches, NOW), NOW + 5 * HOUR);
    assert.equal(nextSearchEnd(new Map(), NOW), null);
});

test('a choice of hours is an end counted from now', () => {
    assert.equal(endIfChosen(72, NOW), NOW + 72 * HOUR);
});
