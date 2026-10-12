import assert from 'node:assert/strict';
import test from 'node:test';

import { createRefusals, lastProfileChange, profilesChanged } from '../public/lib/refusals.js';

/** A clock that runs only when told to, standing in for setTimeout. */
function fakeTimers() {
    const pending = new Map();
    let next = 0;

    return {
        schedule: (run, after) => {
            next += 1;
            pending.set(next, { run, after });

            return next;
        },
        cancel: (id) => pending.delete(id),
        pending,
        fire: () => [...pending.entries()].forEach(([id, { run }]) => {
            pending.delete(id);
            run();
        }),
    };
}

test('a refused document is asked for again when the backstop runs out', () => {
    const timers = fakeTimers();
    const refusals = createRefusals(900_000, timers);
    let asked = 0;

    refusals.retryLater('a', () => { asked += 1; });
    assert.equal([...timers.pending.values()][0].after, 900_000);
    timers.fire();
    assert.equal(asked, 1);
});

test('a change of profile asks again at once and leaves no timer behind', () => {
    const timers = fakeTimers();
    const refusals = createRefusals(900_000, timers);
    const asked = [];

    refusals.retryLater('a', () => asked.push('a'));
    refusals.retryLater('b', () => asked.push('b'));
    refusals.retryNow();
    assert.deepEqual(asked, ['a', 'b']);
    assert.equal(timers.pending.size, 0);
    refusals.retryNow();
    assert.deepEqual(asked, ['a', 'b']);
});

test('a stopped subscription is not asked for again', () => {
    const timers = fakeTimers();
    const refusals = createRefusals(900_000, timers);
    let asked = 0;

    refusals.retryLater('a', () => { asked += 1; });
    refusals.forget('a');
    refusals.retryNow();
    timers.fire();
    assert.equal(asked, 0);
});

test('asking for the same key twice keeps one wait', () => {
    const timers = fakeTimers();
    const refusals = createRefusals(900_000, timers);

    refusals.retryLater('a', () => {});
    refusals.retryLater('a', () => {});
    assert.equal(timers.pending.size, 1);
});

test('the latest touch of any profile is the profile change', () => {
    const profiles = new Map([
        ['x', { updatedAt: 5 }],
        ['y', { updatedAt: 9 }],
        ['z', { updatedAt: null }],
    ]);

    assert.equal(lastProfileChange(profiles), 9);
    assert.equal(lastProfileChange(new Map([['z', { updatedAt: null }]])), null);
    assert.equal(lastProfileChange(null), null);
});

test('only a later touch than the one seen counts as a change', () => {
    assert.equal(profilesChanged(5, 9), true);
    assert.equal(profilesChanged(9, 9), false);
    assert.equal(profilesChanged(null, 9), false, 'the first snapshot is not a change');
    assert.equal(profilesChanged(5, null), false);
});
