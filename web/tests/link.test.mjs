import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    LINK_GRACE_MS,
    linkFrom,
    linkLost,
    linkLostAt,
    nextLink,
    withinAnswerWait,
} from '../public/lib/link.js';

const NOW = Date.parse('2026-09-29T22:24:00Z');

test('a database that never answers is told of after the grace', () => {
    const link = linkFrom(NOW);

    assert.equal(linkLost(link, NOW + LINK_GRACE_MS - 1), false);
    assert.equal(linkLost(link, NOW + LINK_GRACE_MS), true);
    assert.equal(link.confirmedAt, null);
});

test('the cache counts from the first cached snapshot and keeps the last confirmation', () => {
    const confirmed = nextLink(linkFrom(NOW), false, NOW + 1000);
    const cached = nextLink(confirmed, true, NOW + 5000);
    const again = nextLink(cached, true, NOW + 9000);

    assert.deepEqual(again, { confirmedAt: NOW + 1000, cachedSince: NOW + 5000 });
    assert.equal(linkLostAt(again), NOW + 5000 + LINK_GRACE_MS);
});

test('a confirmation ends the silence at once', () => {
    const cached = nextLink(linkFrom(NOW), true, NOW);
    const back = nextLink(cached, false, NOW + 60_000);

    assert.equal(linkLost(back, NOW + 120_000), false);
    assert.equal(linkLostAt(back), null);
});

test('a write the server answers in time is its answer', async () => {
    assert.equal(await withinAnswerWait(Promise.resolve('done'), 50), 'done');
});

test('a write left unanswered is refused as unanswered', async () => {
    await assert.rejects(withinAnswerWait(new Promise(() => {}), 10), { code: 'unanswered' });
});

test('a write refused by the database keeps its own refusal', async () => {
    const refusal = Object.assign(new Error('no'), { code: 'permission-denied' });

    await assert.rejects(withinAnswerWait(Promise.reject(refusal), 50), { code: 'permission-denied' });
});
