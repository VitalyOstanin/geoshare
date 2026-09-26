import assert from 'node:assert/strict';
import { test } from 'node:test';

import { watchingBesides, watchingMembers } from '../public/lib/watchers.js';

const NOW = Date.parse('2026-09-13T12:00:00Z');

test('a member watching from two places is named once', () => {
    const watchers = [
        { memberId: 'anna', until: NOW + 60_000 },
        { memberId: 'anna', until: NOW + 120_000 },
    ];

    assert.deepEqual(watchingMembers(watchers, NOW), ['anna']);
});

test('a claim that has run out names nobody', () => {
    assert.deepEqual(watchingMembers([{ memberId: 'anna', until: NOW - 1 }], NOW), []);
});

test('the ones still looking are kept and the stale ones dropped', () => {
    const watchers = [
        { memberId: 'boris', until: NOW + 30_000 },
        { memberId: 'anna', until: NOW - 30_000 },
        { memberId: 'clara', until: NOW + 30_000 },
    ];

    assert.deepEqual(watchingMembers(watchers, NOW), ['boris', 'clara']);
});

test('the order does not follow the order the claims arrived in', () => {
    const watchers = [
        { memberId: 'clara', until: NOW + 30_000 },
        { memberId: 'anna', until: NOW + 30_000 },
    ];

    assert.deepEqual(watchingMembers(watchers, NOW), ['anna', 'clara']);
});

test('a page that is looking itself folds its own account into itself', () => {
    assert.deepEqual(watchingBesides(['anna', 'boris'], 'anna', true), ['boris']);
});

test('a page that is not looking names its own account watching elsewhere', () => {
    assert.deepEqual(watchingBesides(['anna', 'boris'], 'anna', false), ['anna', 'boris']);
});
