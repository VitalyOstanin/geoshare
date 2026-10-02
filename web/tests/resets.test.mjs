import assert from 'node:assert/strict';
import { test } from 'node:test';

import { RESET_FRESH_MS, RESET_TOLD_MS, resetOf, resetState, resetTold } from '../public/lib/resets.js';

const NOW = Date.parse('2026-10-02T09:00:00Z');
const MINUTE = 60_000;

const asked = (askedAgo, extra = {}) =>
    resetOf('grandma.tablet', { askedBy: 'anna', askedAt: NOW - askedAgo, ...extra });

test('a document that is not a request is none', () => {
    assert.equal(resetOf('grandma.tablet', { askedAt: NOW }), null);
    assert.equal(resetOf('grandma.tablet', { askedBy: 'anna' }), null);
    assert.equal(resetOf('grandma', { askedBy: 'anna', askedAt: NOW }), null);
});

test('a request without its answer has none', () => {
    assert.equal(asked(MINUTE).doneAt, null);
});

test('the asking screen tells waiting, done and unanswered apart', () => {
    assert.deepEqual(resetState(asked(2 * 60 * MINUTE), NOW), { kind: 'waiting' });
    assert.deepEqual(resetState(asked(MINUTE, { doneAt: NOW - 30_000 }), NOW), {
        kind: 'done',
        at: NOW - 30_000,
    });
    assert.deepEqual(resetState(asked(2 * RESET_FRESH_MS), NOW), { kind: 'noAnswer' });
});

test('a row tells of a request for a while after it ends', () => {
    assert.equal(resetTold(asked(5 * 60 * MINUTE), NOW), true);
    assert.equal(resetTold(asked(10 * MINUTE, { doneAt: NOW - RESET_TOLD_MS + 1000 }), NOW), true);
    assert.equal(resetTold(asked(10 * MINUTE, { doneAt: NOW - RESET_TOLD_MS }), NOW), false);
    assert.equal(resetTold(asked(RESET_FRESH_MS + RESET_TOLD_MS), NOW), false);
});
