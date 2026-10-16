// A Firestore subscription whose rules refuse it answers once through its
// error handler and then never again. The handlers keep their degraded
// answer, but each has to leave a warning, or the refusal is invisible.
//
// `store-group.js` and `store-search.js` import the vendored Firestore SDK and
// `store.js`; a resolve hook swaps both for stubs so no app is started.

import assert from 'node:assert/strict';
import { register } from 'node:module';
import { test } from 'node:test';

const stubs = {
    firestore: 'export const onSnapshot = (ref, next, error) => error({ code: \'permission-denied\' });\n'
        + 'export const getDoc = async () => { throw { code: \'permission-denied\' }; };\n'
        + 'export const [arrayRemove, arrayUnion, collection, deleteDoc, deleteField, doc,\n'
        + '    serverTimestamp, setDoc, updateDoc, writeBatch] = Array(10).fill(() => ({}));\n',
    store: 'export const store = {}, refusals = {}, RETRY_AFTER_MS = 0;\n'
        + 'export const currentGroup = () => \'group\', rememberGroup = () => {};\n',
};
const hook = `
export async function resolve(specifier, context, next) {
    if (specifier.endsWith('firebase-firestore.js')) {
        const code = ${JSON.stringify(stubs.firestore)};

        return { url: 'data:text/javascript,' + encodeURIComponent(code), shortCircuit: true };
    }
    if (specifier === './store.js') {
        const code = ${JSON.stringify(stubs.store)};

        return { url: 'data:text/javascript,' + encodeURIComponent(code), shortCircuit: true };
    }
    return next(specifier, context);
}
`;

register(`data:text/javascript,${encodeURIComponent(hook)}`);

const group = await import('../public/lib/store-group.js');
const search = await import('../public/lib/store-search.js');

/** Runs `action`, returning what it passed to `console.warn`. */
async function warned(action) {
    const calls = [];
    const original = console.warn;

    console.warn = (...args) => calls.push(args);
    try {
        await action();
    } finally {
        console.warn = original;
    }

    return calls;
}

function assertRefusalLogged(calls) {
    assert.equal(calls.length, 1);
    assert.equal(typeof calls[0][0], 'string');
    assert.equal(calls[0][1].code, 'permission-denied');
}

test('a refused roster subscription warns and answers null', async () => {
    let answer;

    assertRefusalLogged(await warned(() => group.watchRoster((roster) => {
        answer = roster;
    })));
    assert.equal(answer, null);
});

test('a refused project group read warns and answers null', async () => {
    let answer;

    assertRefusalLogged(await warned(async () => {
        answer = await group.projectGroupOf();
    }));
    assert.equal(answer, null);
});

test('a refused false places subscription warns and answers an empty list', async () => {
    let answer;

    assertRefusalLogged(await warned(() => search.watchFalsePlaces((places) => {
        answer = places;
    })));
    assert.deepEqual(answer, []);
});

test('a refused labels subscription warns', async () => {
    assertRefusalLogged(await warned(() => search.watchLabels('member', () => {})));
});
