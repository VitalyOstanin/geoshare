import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    BOUNDS_TOLERANCE_DEGREES,
    FOLLOW_ALL,
    FOLLOW_NONE,
    FOLLOW_ONE,
    PAUSE_AFTER_A_GESTURE_MS,
    boundsAreClose,
    boundsOf,
    isPaused,
    nextMode,
    targetPosition,
} from '../public/lib/following.js';

const at = (positionId, latitude, longitude) => ({ positionId, latitude, longitude });

// With a member already chosen, the button walks the full three-state circle.
test('the full circle with a target: none -> one -> all -> none', () => {
    assert.equal(nextMode(FOLLOW_NONE, true), FOLLOW_ONE);
    assert.equal(nextMode(FOLLOW_ONE, true), FOLLOW_ALL);
    assert.equal(nextMode(FOLLOW_ALL, true), FOLLOW_NONE);
});

// Nobody has been picked yet: "one" has nothing to centre on, so the first
// tap skips it and goes straight to "all".
test('the circle without a target skips "one": none -> all -> none', () => {
    assert.equal(nextMode(FOLLOW_NONE, false), FOLLOW_ALL);
    assert.equal(nextMode(FOLLOW_ALL, false), FOLLOW_NONE);
});

// The previously followed member went quiet and dropped out of the set
// while "one" was active: the tap still has to move the circle forward.
test('leaving "one" without a target still advances to "all"', () => {
    assert.equal(nextMode(FOLLOW_ONE, false), FOLLOW_ALL);
});

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
