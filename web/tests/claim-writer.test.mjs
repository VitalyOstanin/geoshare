import assert from 'node:assert/strict';
import { test } from 'node:test';
import { setImmediate as nextTurn } from 'node:timers/promises';

import { claimWriter } from '../public/lib/claim-writer.js';

const REFRESH = 5 * 60 * 1000;
const MINUTE = 60 * 1000;

/** A clock the test moves by hand. */
function clock() {
    let at = 0;

    return { now: () => at, pass: (ms) => (at += ms) };
}

// Without a network the write never answers. Ten minutes of checks, one a
// minute, must not queue ten writes to be sent at once when it is back.
test('a write the server has not answered is not made again', () => {
    const time = clock();
    let writes = 0;
    const writer = claimWriter(
        () => {
            writes++;

            return new Promise(() => {});
        },
        async () => {},
        () => {},
        REFRESH,
        time.now,
    );

    writer.push();

    for (let minute = 0; minute < 10; minute++) {
        time.pass(MINUTE);
        writer.pushWhenDue();
    }

    assert.equal(writes, 1);
});

test('an answered write is made again once the refresh is due', async () => {
    const time = clock();
    let writes = 0;
    const writer = claimWriter(
        async () => {
            writes++;
        },
        async () => {},
        () => {},
        REFRESH,
        time.now,
    );

    await writer.push();
    time.pass(REFRESH - MINUTE);
    writer.pushWhenDue();
    assert.equal(writes, 1);

    time.pass(MINUTE);
    await writer.pushWhenDue();
    assert.equal(writes, 2);
});

test('a failed write is tried again at the next check', async () => {
    const time = clock();
    const failures = [];
    let writes = 0;
    const writer = claimWriter(
        async () => {
            writes++;

            if (writes === 1) {
                throw new Error('offline');
            }
        },
        async () => {},
        (error) => failures.push(error.message),
        REFRESH,
        time.now,
    );

    await writer.push();
    time.pass(MINUTE);
    writer.pushWhenDue();

    assert.deepEqual(failures, ['offline']);
    assert.equal(writes, 2);
});

test('a withdrawal that fails is told, not thrown', async () => {
    const failures = [];
    const writer = claimWriter(
        async () => {},
        async () => {
            throw new Error('denied');
        },
        (error) => failures.push(error.message),
        REFRESH,
    );

    await writer.withdraw();

    assert.deepEqual(failures, ['denied']);
});

test('a push after a withdrawal makes a new write though the old one is unanswered', async () => {
    const time = clock();
    const answers = [];
    let writes = 0;
    let removals = 0;
    const writer = claimWriter(
        () => {
            writes++;

            return new Promise((resolve) => answers.push(resolve));
        },
        async () => {
            removals++;
        },
        () => {},
        REFRESH,
        time.now,
    );

    writer.push();
    await writer.withdraw();
    const second = writer.push();

    answers[0]();
    await nextTurn();
    answers[1]();
    await second;

    assert.equal(writes, 2);
    assert.equal(removals, 1);

    // The label is written, so nothing is due before the refresh.
    time.pass(MINUTE);
    writer.pushWhenDue();
    assert.equal(writes, 2);
});

test('a push waiting on an old write is dropped when another withdrawal comes', async () => {
    const answers = [];
    let writes = 0;
    const writer = claimWriter(
        () => {
            writes++;

            return new Promise((resolve) => answers.push(resolve));
        },
        async () => {},
        () => {},
        REFRESH,
        clock().now,
    );

    writer.push();
    await writer.withdraw();
    writer.push();
    await writer.withdraw();

    answers[0]();
    await nextTurn();

    assert.equal(writes, 1);
});
