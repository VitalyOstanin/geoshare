import assert from 'node:assert/strict';
import { test } from 'node:test';

import { STALE_AFTER_MS, ageText, isStale, positionAge } from '../public/lib/age.js';

const NOW = Date.parse('2026-09-11T12:00:00Z');
const MINUTE = 60 * 1000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

const ageOf = (age) => positionAge(NOW - age, NOW);

test('a fresh position is just now', () => {
    assert.deepEqual(ageOf(59 * 1000), { unit: 'justNow', count: 0 });
});

test('a clock running ahead of ours is just now rather than a negative age', () => {
    assert.deepEqual(ageOf(-5 * MINUTE), { unit: 'justNow', count: 0 });
});

test('a minute old is counted in minutes', () => {
    assert.deepEqual(ageOf(MINUTE), { unit: 'minutes', count: 1 });
});

test('the last minute before an hour is still minutes', () => {
    assert.deepEqual(ageOf(59 * MINUTE), { unit: 'minutes', count: 59 });
});

test('an hour old is counted in hours', () => {
    assert.deepEqual(ageOf(HOUR), { unit: 'hours', count: 1 });
});

test('the count is whole units, not rounded up', () => {
    assert.deepEqual(ageOf(179 * MINUTE), { unit: 'hours', count: 2 });
});

test('a day old is counted in days', () => {
    assert.deepEqual(ageOf(DAY), { unit: 'days', count: 1 });
});

test('a position younger than the stale mark is not stale', () => {
    assert.equal(isStale(NOW - STALE_AFTER_MS + 1, NOW), false);
});

test('exactly at the stale mark counts as stale', () => {
    assert.equal(isStale(NOW - STALE_AFTER_MS, NOW), true);
});

test('one of a unit is spelled without the plural', () => {
    assert.equal(ageText({ unit: 'minutes', count: 1 }), '1 minute ago');
    assert.equal(ageText({ unit: 'hours', count: 2 }), '2 hours ago');
    assert.equal(ageText({ unit: 'justNow', count: 0 }), 'just now');
});

// A phone whose satellites are being spoofed says its last true position
// again rather than a new false one, so the writing is fresh while the
// position is not. The card is for the phone being in touch; the age is for
// the position, and the two must not be taken from the same stamp.
const positionOf = (takenAt, writtenAt) => Math.min(takenAt, writtenAt);

test('a position said again carries the age of the position, not of the writing', () => {
    const takenAt = NOW - 40 * MINUTE;

    assert.deepEqual(positionAge(positionOf(takenAt, NOW), NOW), { unit: 'minutes', count: 40 });
    assert.equal(isStale(NOW, NOW), false);
});

test('a phone whose clock runs ahead is aged by the server stamp instead', () => {
    const takenAt = NOW + 10 * MINUTE;
    const writtenAt = NOW - MINUTE;

    assert.deepEqual(positionAge(positionOf(takenAt, writtenAt), NOW), { unit: 'minutes', count: 1 });
});

test('a position said again goes stale by the writing, however old the fix', () => {
    assert.equal(isStale(NOW - STALE_AFTER_MS, NOW), true);
    assert.equal(isStale(NOW - MINUTE, NOW), false);
});
