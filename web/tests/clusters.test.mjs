import assert from 'node:assert/strict';
import { test } from 'node:test';

import { clusterMembers, facesShown } from '../public/lib/clusters.js';

const at = (id, x, y) => ({ id, x, y });

// The same expectations the Android client checks: the two clients group the
// same members the same way, or a family standing together would be one marker
// on the phone and two on the page.
test('members far apart each keep their own marker', () => {
    const clusters = clusterMembers([at('a', 0, 0), at('b', 300, 0)], 40);

    assert.deepEqual(clusters, [['a'], ['b']]);
});

test('members whose pins overlap are gathered into one', () => {
    const clusters = clusterMembers([at('a', 100, 100), at('b', 112, 108), at('c', 95, 90)], 40);

    assert.deepEqual(clusters, [['a', 'b', 'c']]);
});

test('the grouping does not depend on the order the members arrive in', () => {
    const placed = [at('c', 95, 90), at('a', 100, 100), at('b', 112, 108)];

    assert.deepEqual(clusterMembers(placed, 40), [['a', 'b', 'c']]);
    assert.deepEqual(clusterMembers([...placed].reverse(), 40), [['a', 'b', 'c']]);
});

// A row of members each standing a pin apart from the next one: without an
// anchor they would all end up in one marker somewhere in the middle, and that
// marker would sit where nobody is.
test('a chain of neighbours does not collapse into one marker', () => {
    const clusters = clusterMembers([at('a', 0, 0), at('b', 30, 0), at('c', 60, 0)], 40);

    assert.deepEqual(clusters, [['a', 'b'], ['c']]);
});

test('nobody on the map is no markers at all', () => {
    assert.deepEqual(clusterMembers([], 40), []);
});

// The same rule the Android client follows, so that a crowd in one place is
// the same marker wherever it is looked at.
test('a small group shows every face it has', () => {
    assert.equal(facesShown(1), 1);
    assert.equal(facesShown(2), 2);
    assert.equal(facesShown(3), 3);
});

test('a group too large to draw gives its last circle to the count', () => {
    assert.equal(facesShown(4), 2);
    assert.equal(facesShown(9), 2);
});

// The circle around the member whose card is open is drawn where they are; a
// pin in the middle of the crowd would point somewhere else.
test('the member looked at keeps a pin of their own', () => {
    const placed = [at('a', 100, 100), at('b', 112, 108), at('c', 95, 90)];

    assert.deepEqual(clusterMembers(placed, 40, 'b'), [['a', 'c'], ['b']]);
    assert.deepEqual(clusterMembers(placed, 40, 'a'), [['a'], ['b', 'c']]);
});
