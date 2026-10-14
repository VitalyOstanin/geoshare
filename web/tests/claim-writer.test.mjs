import assert from 'node:assert/strict';
import { test } from 'node:test';

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
