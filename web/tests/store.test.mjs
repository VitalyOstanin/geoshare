// What `store.js` asks of Firebase and how it reads the answers: the group a
// browser is addressed to, the sign-in it reports, the link and watchers
// subscriptions, and the claim to be looking. The vendored SDK and the
// generated config are swapped for stubs that write down every call, so no app
// is started and nothing leaves the process; the stubs answer as
// `globalThis.fs` says.

import assert from 'node:assert/strict';
import { register } from 'node:module';
import { beforeEach, test } from 'node:test';

const firestore = `
const path = (ref, rest) => [ref.path, ...rest].filter(Boolean).join('/');
const log = (call) => { globalThis.fs.calls.push(call); return call; };
export const getFirestore = () => ({ path: '' });
export const connectFirestoreEmulator = (store, host, port) =>
    log({ emulator: 'firestore', host, port });
export const doc = (ref, ...rest) => ({ path: path(ref, rest) });
export const collection = doc;
export const serverTimestamp = () => 'NOW';
export const onSnapshot = (ref, ...rest) => {
    const [options, next, error] = typeof rest[0] === 'function' ? [undefined, ...rest] : rest;

    log({ watch: ref.path, options });
    globalThis.fs.answer(ref.path, next, error);

    return () => log({ stop: ref.path });
};
export const setDoc = async (ref, data) => log({ set: ref.path, data });
export const deleteDoc = async (ref) => log({ delete: ref.path });
export const disableNetwork = async () => {
    log({ network: 'off' });
    return globalThis.fs.network();
};
export const enableNetwork = async () => {
    log({ network: 'on' });
    return globalThis.fs.network();
};
`;
const auth = `
export class GoogleAuthProvider {}
export const getAuth = () => ({ auth: true });
export const connectAuthEmulator = (auth, url) =>
    globalThis.fs.calls.push({ emulator: 'auth', url });
export const onAuthStateChanged = (auth, next) => { globalThis.fs.user(next); return () => {}; };
export const getRedirectResult = async () => 'returned';
export const signInWithRedirect = async (auth, provider) => provider instanceof GoogleAuthProvider;
export const signOut = async () => 'left';
`;
const app = 'export const initializeApp = (config) => ({ config });\n';
const config = `
export const firebaseConfig = {
    projectId: 'demo-imaginary',
    appId: 'app',
    apiKey: 'key',
    authDomain: 'demo-imaginary.firebaseapp.com',
    emulators: { auth: 'http://127.0.0.1:9099', firestore: { host: '127.0.0.1', port: 8080 } },
};
`;
const hook = `
const data = (code) => 'data:text/javascript,' + encodeURIComponent(code);

export async function resolve(specifier, context, next) {
    if (specifier.endsWith('firebase-firestore.js')) {
        return { url: data(${JSON.stringify(firestore)}), shortCircuit: true };
    }
    if (specifier.endsWith('firebase-auth.js')) {
        return { url: data(${JSON.stringify(auth)}), shortCircuit: true };
    }
    if (specifier.endsWith('firebase-app.js')) {
        return { url: data(${JSON.stringify(app)}), shortCircuit: true };
    }
    if (specifier === '../firebase-config.js') {
        return { url: data(${JSON.stringify(config)}), shortCircuit: true };
    }
    return next(specifier, context);
}
`;

/** A `localStorage` of its own, so the group and the device name start empty. */
function storage(entries = {}) {
    const kept = new Map(Object.entries(entries));

    return {
        getItem: (key) => kept.get(key) ?? null,
        setItem: (key, value) => kept.set(key, String(value)),
        removeItem: (key) => kept.delete(key),
    };
}

/** A storage the browser refuses, as one set to keep no site data does. */
const refusing = {
    getItem: () => {
        throw new Error('refused');
    },
    setItem: () => {
        throw new Error('refused');
    },
};

globalThis.fs = {
    calls: [],
    answer() {},
    user() {},
    network: async () => {},
};
Object.defineProperty(globalThis, 'localStorage', {
    configurable: true,
    writable: true,
    value: storage({ 'geoshare.device': 'browser' }),
});
register(`data:text/javascript,${encodeURIComponent(hook)}`);

const store = await import('../public/lib/store.js');

// What the module did as it loaded, before any test could clear the calls.
const atLoad = globalThis.fs.calls;

beforeEach(() => {
    globalThis.fs.calls = [];
    globalThis.fs.answer = () => {};
    globalThis.fs.user = () => {};
    globalThis.fs.network = async () => {};
});

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

test('a config naming the emulators connects both of them', () => {
    assert.deepEqual(atLoad, [
        { emulator: 'auth', url: 'http://127.0.0.1:9099' },
        { emulator: 'firestore', host: '127.0.0.1', port: 8080 },
    ]);
});

test('a browser that has joined nothing looks at the inherited group', () => {
    assert.equal(store.currentGroup(storage()), 'family');
});

test('a remembered group is the one looked at afterwards', () => {
    const kept = storage();

    store.rememberGroup('imaginary', kept);
    assert.equal(store.currentGroup(kept), 'imaginary');
});

test('a browser that refuses storage still looks at the inherited group', () => {
    assert.doesNotThrow(() => store.rememberGroup('imaginary', refusing));
    assert.equal(store.currentGroup(refusing), 'family');
});

test('a signed-in account is reported by its id, name, picture and address', () => {
    globalThis.fs.user = (next) => next({
        uid: 'member-a',
        displayName: 'A',
        photoURL: 'https://example.invalid/a.png',
        email: 'a@example.invalid',
    });

    let seen;

    store.watchSignIn((user) => { seen = user; });
    assert.deepEqual(seen, {
        memberId: 'member-a',
        name: 'A',
        picture: 'https://example.invalid/a.png',
        email: 'a@example.invalid',
    });
});

test('nobody signed in is reported as null', () => {
    globalThis.fs.user = (next) => next(null);

    let seen;

    store.watchSignIn((user) => { seen = user; });
    assert.equal(seen, null);
});

test('signing in, its return and leaving go to the auth library', async () => {
    assert.equal(await store.signIn(), true);
    assert.equal(await store.signInReturned(), 'returned');
    assert.equal(await store.leave(), 'left');
});

test('the link follows the group document with its metadata', () => {
    const answers = [];

    globalThis.fs.answer = (unusedPath, next, error) => {
        next({ metadata: { fromCache: true } });
        next({ metadata: { fromCache: false } });
        error({ code: 'permission-denied' });
    };
    store.watchLink((fromCache) => answers.push(fromCache));

    assert.deepEqual(globalThis.fs.calls, [
        { watch: 'groups/family', options: { includeMetadataChanges: true } },
    ]);
    // A refusal is the server answering, so it counts as a confirmation.
    assert.deepEqual(answers, [true, false, false]);
});

test('watchers drop this browser and markers without a server time', () => {
    const marker = (id, data) => ({ id, data: () => data });
    const since = (millis) => ({ toMillis: () => millis });

    globalThis.fs.answer = (unusedPath, next) => next({
        docs: [
            marker('member-a.browser', { memberId: 'member-a', since: since(1000) }),
            marker('member-a.device', { memberId: 'member-a', since: since(2000) }),
            marker('member-b.device', { memberId: 'member-b', since: 'NOW' }),
            marker('member-c.device', { since: since(3000) }),
        ],
    });

    let looking;

    store.watchWatchers('member-a', (watchers) => { looking = watchers; });
    assert.deepEqual(globalThis.fs.calls[0], { watch: 'groups/family/watch', options: undefined });
    // The same member on a device is somebody else to name; seven minutes on
    // from the server's stamp is how long the claim is believed.
    assert.deepEqual(looking, [{ memberId: 'member-a', until: 2000 + 7 * 60 * 1000 }]);
});

test('a refused watchers subscription warns and shows nobody', async () => {
    globalThis.fs.answer = (unusedPath, next, error) => {
        const data = () => ({ memberId: 'member-b', since: { toMillis: () => 1 } });

        next({ docs: [{ id: 'member-b.device', data }] });
        error({ code: 'permission-denied' });
    };

    const answers = [];
    const calls = await warned(() => store.watchWatchers('member-a', (one) => answers.push(one)));

    assert.equal(calls.length, 1);
    assert.equal(answers.length, 2);
    assert.deepEqual(answers.at(-1), []);
});

test('pausing and resuming switch the network and only warn on failure', async () => {
    assert.deepEqual(await warned(async () => {
        await store.pauseDatabase();
        await store.resumeDatabase();
    }), []);
    assert.deepEqual(globalThis.fs.calls, [{ network: 'off' }, { network: 'on' }]);

    globalThis.fs.network = async () => {
        throw new Error('offline');
    };
    assert.equal((await warned(async () => {
        await store.pauseDatabase();
        await store.resumeDatabase();
    })).length, 2);
});

/** The claim of `member-a` from this browser, whose name the storage keeps as `browser`. */
const MARKER = 'groups/family/watch/member-a.browser';

/** Lets a claim's write and the chain behind it finish before the next check. */
const settled = () => new Promise((done) => globalThis.setImmediate(done));

/** A visible, focused page whose attention events can be fired by hand. */
function page() {
    const documentEvents = new globalThis.EventTarget();
    const windowEvents = new globalThis.EventTarget();
    const shown = { visibilityState: 'visible', focused: true };

    // Defined rather than assigned: `Object.assign` would copy the getter's
    // value once, and the page could then never be hidden.
    globalThis.document = Object.defineProperties(documentEvents, {
        visibilityState: { get: () => shown.visibilityState },
        hasFocus: { value: () => shown.focused },
    });
    globalThis.window = windowEvents;

    return shown;
}

test('a looked-at page claims under this browser and withdraws when hidden', async () => {
    const shown = page();
    const claims = [];
    const stop = store.keepWatching('member-a', assert.fail, (claimed) => claims.push(claimed));

    await settled();
    assert.deepEqual(globalThis.fs.calls, [
        { set: MARKER, data: { memberId: 'member-a', since: 'NOW' } },
    ]);
    assert.deepEqual(claims, [true]);

    shown.visibilityState = 'hidden';
    globalThis.document.dispatchEvent(new globalThis.Event('visibilitychange'));
    await settled();
    assert.deepEqual(globalThis.fs.calls.at(-1), { delete: MARKER });
    assert.deepEqual(claims, [true, false]);

    // Shown again and touched: the first sign of somebody claims anew.
    shown.visibilityState = 'visible';
    globalThis.window.dispatchEvent(new globalThis.Event('pointermove'));
    await settled();
    assert.deepEqual(claims, [true, false, true]);
    assert.equal(globalThis.fs.calls.filter((call) => 'set' in call).length, 2);

    await stop();
    assert.deepEqual(claims, [true, false, true, false]);
    assert.deepEqual(globalThis.fs.calls.at(-1), { delete: MARKER });
});

test('a page out of focus does not claim, and stopping it still withdraws', async () => {
    const shown = page();

    shown.focused = false;

    const claims = [];
    const stop = store.keepWatching('member-a', assert.fail, (claimed) => claims.push(claimed));

    assert.deepEqual(claims, []);
    await stop();
    // Withdrawn regardless: a claim made by an earlier page of this browser
    // may still stand.
    assert.deepEqual(globalThis.fs.calls, [{ delete: MARKER }]);
    assert.deepEqual(claims, [false]);
});

test('a browser that cannot keep its name still claims under a new one', async () => {
    globalThis.localStorage = refusing;
    try {
        page();

        const stop = store.keepWatching('member-a', assert.fail);

        await settled();
        const [{ set }] = globalThis.fs.calls;

        assert.match(set, /^groups\/family\/watch\/member-a\.[\da-f-]{36}$/u);
        await stop();
    } finally {
        globalThis.localStorage = storage({ 'geoshare.device': 'browser' });
    }
});

test('a browser with no name yet makes one up and keeps it', () => {
    const kept = storage();

    globalThis.localStorage = kept;
    try {
        store.watchWatchers('member-a', () => {});
        assert.match(kept.getItem('geoshare.device'), /^[0-9a-f-]{36}$/u);
    } finally {
        globalThis.localStorage = storage({ 'geoshare.device': 'browser' });
    }
});
