// How `store-positions.js` follows the positions of the group: one subscription
// per document, the list held back until each has answered once, and a refused
// document dropped and asked for again later. The vendored SDK and `store.js`
// are swapped for stubs that write down every call and keep each
// subscription's handlers, so a test answers for the database by hand.

import assert from 'node:assert/strict';
import { register } from 'node:module';
import { beforeEach, test } from 'node:test';

const firestore = `
export const doc = (ref, ...rest) => ({ path: rest.join('/') });
export const onSnapshot = (ref, next, error) => {
    globalThis.fs.calls.push({ watch: ref.path });
    globalThis.fs.handlers.set(ref.path, { next, error });

    return () => globalThis.fs.calls.push({ stop: ref.path });
};
`;
const store = `
export const store = {};
export const currentGroup = () => 'group';
export const refusals = {
    retryLater: (key, again) => globalThis.fs.retries.set(key, again),
    forget: (key) => globalThis.fs.calls.push({ forget: key }),
};
`;
const hook = `
const data = (code) => 'data:text/javascript,' + encodeURIComponent(code);

export async function resolve(specifier, context, next) {
    if (specifier.endsWith('firebase-firestore.js')) {
        return { url: data(${JSON.stringify(firestore)}), shortCircuit: true };
    }
    if (specifier === './store.js') {
        return { url: data(${JSON.stringify(store)}), shortCircuit: true };
    }
    return next(specifier, context);
}
`;

globalThis.fs = { calls: [], handlers: new Map(), retries: new Map() };
register(`data:text/javascript,${encodeURIComponent(hook)}`);

const { watchPositions } = await import('../public/lib/store-positions.js');

beforeEach(() => {
    globalThis.fs = { calls: [], handlers: new Map(), retries: new Map() };
});

const stamp = (millis) => ({ toMillis: () => millis });

/** A position document of `id`, in the made-up area the other tests use. */
const position = (id, takenAt = 1000) => ({
    id,
    exists: () => true,
    data: () => ({ latitude: 59.437, longitude: 24.745, takenAt: stamp(takenAt) }),
});
const missing = (id) => ({ id, exists: () => false });

/** Answers the subscription to `id` with `snapshot`, or refuses it. */
function answer(id, snapshot) {
    const handlers = globalThis.fs.handlers.get(`groups/group/positions/${id}`);

    if (snapshot === 'refused') {
        handlers.error({ code: 'permission-denied' });
    } else {
        handlers.next(snapshot);
    }
}

/** Starts watching `documents`, collecting the ids of every list said and every refused count. */
function watching(documents) {
    const lists = [];
    const refused = [];
    const stop = watchPositions(
        documents,
        (positions) => lists.push(positions.map((one) => one.positionId)),
        (count) => refused.push(count),
    );

    return { lists, refused, stop };
}

test('each document gets a subscription of its own in the current group', () => {
    watching(['a.one', 'b.one']);

    assert.deepEqual(globalThis.fs.calls, [
        { watch: 'groups/group/positions/a.one' },
        { watch: 'groups/group/positions/b.one' },
    ]);
});

test('an empty list is said at once, the rest only once every document answered', () => {
    const { lists } = watching(['a.one', 'b.one']);

    assert.deepEqual(lists, [[]]);

    answer('a.one', position('a.one'));
    assert.deepEqual(lists, [[]]);

    answer('b.one', missing('b.one'));
    assert.deepEqual(lists, [[], ['a.one']]);

    answer('b.one', position('b.one'));
    assert.deepEqual(lists, [[], ['a.one'], ['a.one', 'b.one']]);
});

test('a document that is still missing says nothing new', () => {
    const { lists } = watching(['a.one']);

    answer('a.one', missing('a.one'));
    answer('a.one', missing('a.one'));
    assert.deepEqual(lists, [[], []]);
});

test('a document without usable coordinates drops the marker it had', () => {
    const { lists } = watching(['a.one']);

    answer('a.one', position('a.one'));
    answer('a.one', { id: 'a.one', exists: () => true, data: () => ({ latitude: 'x' }) });
    assert.deepEqual(lists, [[], ['a.one'], []]);
});

test('a refused document drops its marker, is counted and asked for again later', () => {
    const { lists, refused } = watching(['a.one', 'b.one']);

    answer('a.one', position('a.one'));
    answer('b.one', position('b.one'));
    answer('a.one', 'refused');

    assert.deepEqual(lists.at(-1), ['b.one']);
    assert.deepEqual(refused, [1]);
    assert.deepEqual(globalThis.fs.calls.at(-1), { stop: 'groups/group/positions/a.one' });
    assert.deepEqual([...globalThis.fs.retries.keys()], ['positions/a.one']);

    // Asked again, the permission is back: the marker returns and the count
    // goes down.
    globalThis.fs.retries.get('positions/a.one')();
    answer('a.one', position('a.one', 2000));
    assert.deepEqual(lists.at(-1), ['b.one', 'a.one']);
    assert.deepEqual(refused, [1, 0]);
});

test('a refusal counts as the first answer of its document', () => {
    const { lists, refused } = watching(['a.one', 'b.one']);

    answer('a.one', 'refused');
    answer('b.one', position('b.one'));
    assert.deepEqual(lists, [[], ['b.one']]);
    assert.deepEqual(refused, [1]);
});

test('stopping forgets the retries, ends the subscriptions and asks nothing again', () => {
    const { stop } = watching(['a.one', 'b.one']);

    answer('a.one', 'refused');
    globalThis.fs.calls = [];
    stop();

    assert.deepEqual(globalThis.fs.calls, [
        { forget: 'positions/a.one' },
        { forget: 'positions/b.one' },
        { stop: 'groups/group/positions/b.one' },
    ]);

    // A retry that was already due when the page stopped finds nobody wanting it.
    globalThis.fs.retries.get('positions/a.one')();
    assert.equal(globalThis.fs.calls.length, 3);
});

test('a refusal after stopping is not asked for again', () => {
    const { stop } = watching(['a.one']);
    const { error } = globalThis.fs.handlers.get('groups/group/positions/a.one');

    stop();
    error({ code: 'permission-denied' });
    assert.equal(globalThis.fs.retries.size, 0);
});
