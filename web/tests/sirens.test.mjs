import assert from 'node:assert/strict';
import { test } from 'node:test';

import { SIREN_SECONDS, shouldSound, sirenOf, sirenState, sirenTold } from '../public/lib/sirens.js';

const NOW = Date.parse('2026-09-27T19:40:00Z');
const SECOND = 1000;

const called = (calledSecondsAgo, extra = {}) =>
    sirenOf('grandma.tablet', {
        calledBy: 'anna',
        calledAt: NOW - calledSecondsAgo * SECOND,
        ...extra,
    });

test('a document that is not a siren is none', () => {
    assert.equal(sirenOf('grandma.tablet', { calledAt: NOW }), null);
    assert.equal(sirenOf('grandma.tablet', { calledBy: 'anna' }), null);
    assert.equal(sirenOf('grandma', { calledBy: 'anna', calledAt: NOW }), null);
});

test('the member is the phone id up to its dot', () => {
    assert.equal(called(0).memberId, 'grandma');
});

test('a stale call is not sounded', () => {
    assert.equal(shouldSound(called(61), null, NOW), false);
});

test('a call already answered is not sounded again', () => {
    const siren = called(10);

    assert.equal(shouldSound(siren, siren.calledAt, NOW), false);
    assert.equal(shouldSound(siren, NOW, NOW), false);
});

test('a stopped call is not sounded', () => {
    const siren = called(10, { stoppedBy: 'boris', stoppedAt: NOW });

    assert.equal(shouldSound(siren, null, NOW), false);
});

test('a fresh unanswered call is sounded', () => {
    const siren = called(10);

    assert.equal(shouldSound(siren, NOW - 20 * SECOND, NOW), true);
    assert.equal(shouldSound(siren, null, NOW), true);
});

test('called and still within reach is waiting', () => {
    assert.deepEqual(sirenState(called(59), NOW), { kind: 'waiting' });
});

test('heard and within its seconds is sounding', () => {
    const siren = called(30, { heardAt: NOW - 5 * SECOND });

    assert.deepEqual(sirenState(siren, NOW), {
        kind: 'sounding',
        until: siren.heardAt + SIREN_SECONDS * SECOND,
    });
});

test('heard and past its seconds is over', () => {
    const siren = called(30, { heardAt: NOW - (SIREN_SECONDS + 1) * SECOND });

    assert.deepEqual(sirenState(siren, NOW), { kind: 'over' });
});

test('unheard and past the fresh window is no answer', () => {
    assert.deepEqual(sirenState(called(61), NOW), { kind: 'noAnswer' });
});

test('stopped wins over every other state', () => {
    const siren = called(61, {
        heardAt: NOW - 30 * SECOND,
        stoppedBy: 'boris',
        stoppedAt: NOW,
    });

    assert.deepEqual(sirenState(siren, NOW), { kind: 'stopped', by: 'boris' });
});

test('a call over for more than five minutes is no longer told of', () => {
    const minute = 60 * SECOND;
    const stopped = called(10 * 60, { stoppedBy: 'anna', stoppedAt: NOW - 6 * minute });
    const justStopped = called(10 * 60, { stoppedBy: 'anna', stoppedAt: NOW - 4 * minute });

    assert.equal(sirenTold(stopped, NOW), false);
    assert.equal(sirenTold(justStopped, NOW), true);
    // Unanswered: over a minute after the call, told for five more.
    assert.equal(sirenTold(called(5 * 60), NOW), true);
    assert.equal(sirenTold(called(6 * 60 + 1), NOW), false);
    // Heard: over when its seconds ran out.
    assert.equal(sirenTold(called(10 * 60, { heardAt: NOW - 5 * minute }), NOW), true);
    assert.equal(sirenTold(called(10 * 60, { heardAt: NOW - 6 * minute }), NOW), false);
    // Still being called is always told.
    assert.equal(sirenTold(called(1), NOW), true);
});
