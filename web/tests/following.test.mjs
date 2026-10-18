import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    BOUNDS_TOLERANCE_DEGREES,
    CATCH_UP_MS,
    PAUSE_AFTER_A_GESTURE_MS,
    boundsAreClose,
    boundsOf,
    catchUpTo,
    isPaused,
    targetPosition,
} from '../public/lib/following.js';

const at = (positionId, latitude, longitude) => ({ positionId, latitude, longitude });

test('a gesture pauses the map immediately and for exactly the held duration', () => {
    const pausedAt = 1000;

    assert.equal(isPaused(pausedAt, pausedAt), true);
    assert.equal(isPaused(pausedAt, pausedAt + PAUSE_AFTER_A_GESTURE_MS - 1), true);
    assert.equal(isPaused(pausedAt, pausedAt + PAUSE_AFTER_A_GESTURE_MS), false);
});

test('no gesture yet is never a pause', () => {
    assert.equal(isPaused(null, 0), false);
    assert.equal(isPaused(null, 1_000_000), false);
});

test('an empty set of positions has no bounds', () => {
    assert.equal(boundsOf([]), null);
});

test('a single position is its own zero-sized rectangle', () => {
    assert.deepEqual(boundsOf([at('a', 60.9, 4.3)]), {
        west: 4.3,
        south: 60.9,
        east: 4.3,
        north: 60.9,
    });
});

test('several positions give the rectangle that just contains them all', () => {
    const positions = [at('a', 60.9, 4.3), at('b', 61.0, 4.1), at('c', 60.8, 4.5)];

    assert.deepEqual(boundsOf(positions), {
        west: 4.1,
        south: 60.8,
        east: 4.5,
        north: 61.0,
    });
});

// Two members on either side of the date line, a few kilometres apart: the
// rectangle is the few kilometres across the line, not the rest of the globe.
test('positions on both sides of the date line are fitted across it', () => {
    const positions = [at('a', -16.8, 179.95), at('b', -16.9, -179.98)];

    const bounds = boundsOf(positions);

    assert.equal(bounds.west, 179.95);
    assert.ok(Math.abs(bounds.east - 180.02) < 1e-9);
    assert.equal(bounds.south, -16.9);
    assert.equal(bounds.north, -16.8);
});

test('positions on either side of Greenwich are not fitted across the date line', () => {
    const positions = [at('a', 10, -60), at('b', 10, 60)];

    assert.deepEqual(boundsOf(positions), { west: -60, south: 10, east: 60, north: 10 });
});

test('bounds within tolerance on every edge are close', () => {
    const one = { west: 4.1, south: 60.8, east: 4.5, north: 61.0 };
    const other = {
        west: one.west + BOUNDS_TOLERANCE_DEGREES / 2,
        south: one.south - BOUNDS_TOLERANCE_DEGREES / 2,
        east: one.east,
        north: one.north,
    };

    assert.equal(boundsAreClose(one, other), true);
});

test('a rectangle that shifted past the tolerance on one edge is not close', () => {
    const one = { west: 4.1, south: 60.8, east: 4.5, north: 61.0 };
    const other = { ...one, east: one.east + BOUNDS_TOLERANCE_DEGREES * 2 };

    assert.equal(boundsAreClose(one, other), false);
});

test('a custom tolerance is honoured instead of the default', () => {
    const one = { west: 0, south: 0, east: 0, north: 0 };
    const other = { west: 0.01, south: 0, east: 0, north: 0 };

    assert.equal(boundsAreClose(one, other, 0.02), true);
    assert.equal(boundsAreClose(one, other, 0.005), false);
});

test('the target position is found by its positionId', () => {
    const positions = [at('a', 1, 2), at('b', 3, 4)];

    assert.deepEqual(targetPosition(positions, 'b'), at('b', 3, 4));
});

// A member gone quiet, or whose claimed position was rejected, is simply not
// in the set the caller passes in; the map has nothing to centre on.
test('a target absent from the set has no position', () => {
    assert.equal(targetPosition([at('a', 1, 2)], 'missing'), null);
});

// A tap that wakes the page flies to the old place; the fresh one follows.
test('a member just picked who turns out to be elsewhere takes the map there', () => {
    const lookedAt = { positionId: 'olga', at: 1000, latitude: 59.43, longitude: 24.75 };
    const moved = at('olga', 59.44, 24.76);

    assert.equal(catchUpTo(lookedAt, [at('mark', 59.0, 24.0), moved], 2000), moved);
});

test('a member still where the map was sent leaves it alone', () => {
    const lookedAt = { positionId: 'olga', at: 1000, latitude: 59.43, longitude: 24.75 };

    assert.equal(catchUpTo(lookedAt, [at('olga', 59.43, 24.75)], 2000), null);
});

test('the catching up ends with its time, and never starts without a pick', () => {
    const lookedAt = { positionId: 'olga', at: 1000, latitude: 59.43, longitude: 24.75 };
    const moved = [at('olga', 59.44, 24.76)];

    assert.notEqual(catchUpTo(lookedAt, moved, 1000 + CATCH_UP_MS - 1), null);
    assert.equal(catchUpTo(lookedAt, moved, 1000 + CATCH_UP_MS), null);
    assert.equal(catchUpTo(null, moved, 1000), null);
});

test('a member picked who has dropped out of the set is not chased', () => {
    const lookedAt = { positionId: 'olga', at: 1000, latitude: 59.43, longitude: 24.75 };

    assert.equal(catchUpTo(lookedAt, [at('mark', 59.0, 24.0)], 2000), null);
});
