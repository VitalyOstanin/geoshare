import assert from 'node:assert/strict';
import test from 'node:test';

import { signOutOf } from '../public/lib/sign-out.js';

test('a refused sign-out is handed back and the claim is made again', async () => {
    const calls = [];
    const refusal = Object.assign(new Error('storage'), { code: 'auth/internal-error' });

    const result = await signOutOf({
        stopWatching: async () => calls.push('withdraw'),
        leave: () => Promise.reject(refusal),
        resume: () => calls.push('resume'),
    });

    assert.equal(result, refusal);
    assert.deepEqual(calls, ['withdraw', 'resume']);
});

test('a sign-out that goes through withdraws the claim first and resumes nothing', async () => {
    const calls = [];

    const result = await signOutOf({
        stopWatching: async () => calls.push('withdraw'),
        leave: async () => calls.push('leave'),
        resume: () => calls.push('resume'),
    });

    assert.equal(result, null);
    assert.deepEqual(calls, ['withdraw', 'leave']);
});

test('signing out without a claim held still signs out', async () => {
    let left = false;

    const result = await signOutOf({
        stopWatching: null,
        leave: async () => { left = true; },
        resume: () => assert.fail('nothing to resume'),
    });

    assert.equal(result, null);
    assert.ok(left);
});
