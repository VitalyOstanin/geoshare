// What `store-group.js` and `store-search.js` send to Firestore and how they
// read what comes back. The vendored SDK and `store.js` are swapped for stubs
// that write down every call, so no app is started and nothing leaves the
// process; the stubs answer as `globalThis.fs` says.

import assert from 'node:assert/strict';
import { register } from 'node:module';
import { beforeEach, test } from 'node:test';

const firestore = `
const path = (ref, rest) => [ref.path, ...rest].filter(Boolean).join('/');
const log = (call) => { globalThis.fs.calls.push(call); return call; };
export const doc = (ref, ...rest) => ({ path: path(ref, rest) });
export const collection = doc;
export const arrayUnion = (value) => ({ union: value });
export const arrayRemove = (value) => ({ remove: value });
export const deleteField = () => 'DELETED';
export const serverTimestamp = () => 'NOW';
export const onSnapshot = (ref, next, error) => {
    log({ watch: ref.path });
    globalThis.fs.answer(ref.path, next, error);

    return () => log({ stop: ref.path });
};
export const getDoc = async (ref) => {
    log({ read: ref.path });

    return globalThis.fs.document(ref.path);
};
export const setDoc = async (ref, data, options) => {
    log({ set: ref.path, data, options });
    if (globalThis.fs.refuse) { throw { code: 'permission-denied' }; }
};
export const updateDoc = async (ref, data) => log({ update: ref.path, data });
export const deleteDoc = async (ref) => log({ delete: ref.path });
export const writeBatch = () => {
    const batch = {
        set: (ref, data, options) => { log({ set: ref.path, data, options }); return batch; },
        update: (ref, data) => { log({ update: ref.path, data }); return batch; },
        delete: (ref) => { log({ delete: ref.path }); return batch; },
        commit: async () => { log({ commit: true }); },
    };

    return batch;
};
`;
const store = `
export const store = {};
export const RETRY_AFTER_MS = 0;
export const currentGroup = () => 'group';
export const rememberGroup = (id) => globalThis.fs.calls.push({ remember: id });
export const refusals = {
    retryLater: (key, again) => globalThis.fs.retries.push({ key, again }),
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

globalThis.fs = { calls: [], retries: [], answer() {}, document: () => missing() };
register(`data:text/javascript,${encodeURIComponent(hook)}`);

const group = await import('../public/lib/store-group.js');
const search = await import('../public/lib/store-search.js');

const missing = () => ({ exists: () => false });
const found = (data, id = 'x') => ({ exists: () => true, data: () => data, id });
const stamp = (millis) => ({ toMillis: () => millis });
const withData = (data, id) => ({ id, data: () => data });

beforeEach(() => {
    globalThis.fs.calls = [];
    globalThis.fs.retries = [];
    globalThis.fs.refuse = false;
    globalThis.fs.answer = () => {};
    globalThis.fs.document = missing;
});

/** Makes every subscription answer with what [answers] holds for its path. */
function answering(answers) {
    globalThis.fs.answer = (path, next, error) => {
        const answer = answers[path];

        if (answer instanceof Error || answer === 'refused') {
            error({ code: 'permission-denied' });
        } else if (answer !== undefined) {
            next(answer);
        }
    };
}

const writes = () => globalThis.fs.calls.filter((call) => !('watch' in call) && !('stop' in call));

test('a roster subscription reads the group document as a roster', () => {
    answering({ 'groups/group': found({ members: ['a', 'b'], admins: ['a'], name: 'Home' }, 'a') });

    let roster;

    group.watchRoster((one) => { roster = one; });
    assert.deepEqual(roster, { members: ['a', 'b'], admins: ['a'], founder: 'a', name: 'Home' });
});

test('a roster subscription answers null where the group document does not exist', () => {
    answering({ 'groups/group': missing() });

    let roster = 'unset';

    group.watchRoster((one) => { roster = one; });
    assert.equal(roster, null);
});

test('founding a group writes the group and the mark in one batch, then remembers it', async () => {
    await group.foundGroup('anna', '  Home  ');

    assert.deepEqual(writes(), [
        { set: 'groups/anna', data: { members: ['anna'], admins: ['anna'], name: 'Home' }, options: undefined },
        { set: 'project/group', data: { group: 'anna' }, options: undefined },
        { commit: true },
        { remember: 'anna' },
    ]);
});

test('a group founded without a name leaves the name out of the write', async () => {
    await group.foundGroup('anna', '   ');

    assert.deepEqual(writes()[0].data, { members: ['anna'], admins: ['anna'] });
});

test('the project group is the one the mark names, or null where there is none', async () => {
    globalThis.fs.document = () => found({ group: 'anna' });
    assert.equal(await group.projectGroupOf(), 'anna');
    globalThis.fs.document = () => found({ group: '' });
    assert.equal(await group.projectGroupOf(), null);
    globalThis.fs.document = () => found({});
    assert.equal(await group.projectGroupOf(), null);
    globalThis.fs.document = missing;
    assert.equal(await group.projectGroupOf(), null);
});

test('an invitation is written under its code and handed back whole', async () => {
    const before = Date.now();
    const invitation = await group.makeInvitation('anna', ' bob@example.org ');
    const [write] = writes();

    assert.equal(invitation.groupId, 'group');
    assert.equal(invitation.email, 'bob@example.org');
    assert.match(invitation.code, /^[A-Za-z0-9]+$/);
    assert.ok(invitation.expiresAt.getTime() >= before + 24 * 60 * 60 * 1000);
    assert.equal(write.set, `groups/group/invitations/${invitation.code}`);
    assert.deepEqual(write.data, {
        createdBy: 'anna',
        expiresAt: invitation.expiresAt,
        email: 'bob@example.org',
    });
});

test('joining claims the code, appends the member and spends the code in one batch', async () => {
    await group.joinGroup('bob', { group: 'anna', code: 'ABCD2345' });

    assert.deepEqual(writes(), [
        { set: 'groups/anna/joins/bob', data: { code: 'ABCD2345' }, options: undefined },
        { update: 'groups/anna', data: { members: { union: 'bob' } } },
        { delete: 'groups/anna/invitations/ABCD2345' },
        { commit: true },
        { remember: 'anna' },
    ]);
});

test('taking a member off writes the two lists alone', async () => {
    await group.removeMember({ members: ['a', 'b'], admins: ['a', 'b'], founder: 'a' }, 'b');

    assert.deepEqual(writes(), [
        { update: 'groups/group', data: { members: ['a'], admins: ['a'] } },
    ]);
});

test('running the group is handed over and taken back one element at a time', async () => {
    await group.setRunning('b', true);
    await group.setRunning('b', false);

    assert.deepEqual(writes().map((call) => call.data), [
        { admins: { union: 'b' } },
        { admins: { remove: 'b' } },
    ]);
});

test('a group is renamed with the name alone, trimmed', async () => {
    await group.renameGroup('  Family ');

    assert.deepEqual(writes(), [{ update: 'groups/group', data: { name: 'Family' } }]);
});

test('profiles are read into a map by member, those without a name left out', () => {
    answering({
        'groups/group/members': {
            docs: [
                withData({ displayName: 'Anna', photoUrl: '' }, 'a'),
                withData({ displayName: ' ' }, 'b'),
            ],
        },
    });

    let profiles;

    group.watchProfiles((one) => { profiles = one; }, () => {});
    assert.deepEqual([...profiles.keys()], ['a']);
    assert.equal(profiles.get('a').displayName, 'Anna');
    assert.equal(profiles.get('a').photoUrl, null);
});

test('a profile subscription hands its refusal to the caller', () => {
    answering({ 'groups/group/members': 'refused' });

    let heard;

    group.watchProfiles(() => {}, (error) => { heard = error; });
    assert.equal(heard.code, 'permission-denied');
});

test('a stored profile that matches the account is not written again', async () => {
    const member = { memberId: 'a', name: 'Anna', email: 'a@example.org', picture: null };

    await group.publishProfile(member, { displayName: 'Anna', photoUrl: null }, () => {});
    assert.deepEqual(writes(), []);
});

test('an account with neither name nor address publishes nothing', async () => {
    await group.publishProfile({ memberId: 'a', name: '', email: '' }, null, () => {});
    assert.deepEqual(writes(), []);
});

test('a changed profile is merged into the member document', async () => {
    const member = { memberId: 'a', name: 'Anna', email: '', picture: 'not an address' };

    await group.publishProfile(member, { displayName: 'Old', photoUrl: null }, () => {});
    assert.deepEqual(writes(), [{
        set: 'groups/group/members/a',
        data: { displayName: 'Anna', photoUrl: null, updatedAt: 'NOW' },
        options: { merge: true },
    }]);
});

test('a refused profile write goes to the error handler instead of being thrown', async () => {
    globalThis.fs.refuse = true;

    let heard;

    await group.publishProfile({ memberId: 'a', name: 'Anna' }, null, (error) => { heard = error; });
    assert.equal(heard.code, 'permission-denied');
});

test('grants are followed per member and published with the devices they name', () => {
    answering({
        'groups/group/grants/a': found({ devices: ['p1', 7] }),
        'groups/group/devices/a': found({ p2: { model: 'M' } }),
        'groups/group/grants/b': missing(),
        'groups/group/devices/b': missing(),
    });

    const seen = [];
    const stop = group.watchGrants(['a', 'b'], (grants) => seen.push(grants));
    const last = seen.at(-1);

    assert.deepEqual(last.get('a').devices.sort(), ['p1', 'p2']);
    assert.deepEqual(last.get('b').devices, []);
    stop();
    assert.ok(globalThis.fs.calls.some((call) => call.forget === 'grants/a'));
    assert.ok(globalThis.fs.calls.some((call) => call.stop === 'groups/group/devices/b'));
});

test('a refused grant leaves the member out and asks again later', () => {
    answering({
        'groups/group/grants/a': 'refused',
        'groups/group/devices/a': 'refused',
    });

    const seen = [];
    const stop = group.watchGrants(['a'], (grants) => seen.push(grants));

    assert.equal(seen.at(-1).size, 0);
    assert.deepEqual(globalThis.fs.retries.map((retry) => retry.key), ['grants/a', 'devices/a']);

    // The timer fires while still wanted: the document is followed again.
    globalThis.fs.calls = [];
    globalThis.fs.retries[0].again();
    assert.deepEqual(globalThis.fs.calls[0], { watch: 'groups/group/grants/a' });

    // Once the watcher is stopped a late timer follows nothing.
    stop();
    globalThis.fs.calls = [];
    globalThis.fs.retries[1].again();
    assert.deepEqual(globalThis.fs.calls, []);
});

test('a refusal that arrives after the watcher stopped is not asked again', () => {
    let refuse;

    globalThis.fs.answer = (path, next, error) => {
        if (path.includes('/grants/')) {
            refuse = error;
        }
    };

    const stop = group.watchGrants(['a'], () => {});

    stop();
    refuse({ code: 'permission-denied' });
    assert.deepEqual(globalThis.fs.retries, []);
});

test('false places are read through the page rules', () => {
    answering({ 'groups/group/falsePlaces': { docs: [withData({})] } });

    let places;

    search.watchFalsePlaces((one) => { places = one; });
    assert.deepEqual(places, []);
});

test('searches are read with the server times as milliseconds, broken ones left out', () => {
    answering({
        'groups/group/searches': {
            docs: [
                withData({ openedBy: 'a', hours: 2, renewedAt: stamp(5000), startedAt: stamp(1000) }, 'a.p1'),
                withData({ hours: 0 }, 'a.p2'),
            ],
        },
    });

    let searches;

    search.watchSearches((one) => { searches = one; });
    assert.deepEqual([...searches.keys()], ['a.p1']);
    assert.equal(searches.get('a.p1').startedAt, 1000);
});

test('a refused searches subscription warns and answers an empty map', () => {
    answering({ 'groups/group/searches': 'refused' });

    let searches;
    const warn = console.warn;

    console.warn = () => {};
    try {
        search.watchSearches((one) => { searches = one; });
    } finally {
        console.warn = warn;
    }
    assert.equal(searches.size, 0);
});

test('a search is opened whole, renewed in part and ended by deleting its document', async () => {
    await search.openSearch('a.p1', 'admin', 3, 'Phone');
    await search.openSearch('a.p1', 'admin', 3, '');
    await search.renewSearch('a.p1', 'admin', 6, 'Phone');
    await search.renewSearch('a.p1', 'admin', 6);
    await search.endSearch('a.p1');

    const [opened, openedBare, renewed, renewedBare, ended] = writes();

    assert.deepEqual(opened.data, {
        openedBy: 'admin',
        startedAt: 'NOW',
        renewedBy: 'admin',
        renewedAt: 'NOW',
        hours: 3,
        phoneName: 'Phone',
    });
    assert.equal(opened.set, 'groups/group/searches/a.p1');
    assert.ok(!('phoneName' in openedBare.data));
    assert.deepEqual(renewed.data, { renewedBy: 'admin', renewedAt: 'NOW', hours: 6, phoneName: 'Phone' });
    assert.ok(!('phoneName' in renewedBare.data));
    assert.deepEqual(ended, { delete: 'groups/group/searches/a.p1' });
});

test('a siren is read as no siren where its document is missing or refused', () => {
    const warn = console.warn;

    console.warn = () => {};
    try {
        for (const answer of [missing(), 'refused']) {
            answering({ 'groups/group/sirens/a.p1': answer });

            let siren = 'unset';

            search.watchSiren('a.p1', (one) => { siren = one; });
            assert.equal(siren, null);
        }
    } finally {
        console.warn = warn;
    }
});

test('a siren is read with its server times as milliseconds', () => {
    answering({
        'groups/group/sirens/a.p1': found({ calledBy: 'b', calledAt: stamp(100), heardAt: stamp(200) }),
    });

    let siren;

    search.watchSiren('a.p1', (one) => { siren = one; });
    assert.equal(siren.calledAt, 100);
    assert.equal(siren.heardAt, 200);
    assert.equal(siren.stoppedAt, null);
});

test('a siren is called with two fields, written whole, and stopped by updating two', async () => {
    await search.callSiren('a.p1', 'b');
    await search.stopSiren('a.p1', 'b');

    assert.deepEqual(writes(), [
        { set: 'groups/group/sirens/a.p1', data: { calledBy: 'b', calledAt: 'NOW' }, options: undefined },
        { update: 'groups/group/sirens/a.p1', data: { stoppedBy: 'b', stoppedAt: 'NOW' } },
    ]);
});

test('a reset request is read with its times, and as none where missing or refused', () => {
    const warn = console.warn;

    console.warn = () => {};
    try {
        for (const answer of [missing(), 'refused']) {
            answering({ 'groups/group/resets/a.p1': answer });

            let reset = 'unset';

            search.watchReset('a.p1', (one) => { reset = one; });
            assert.equal(reset, null);
        }
    } finally {
        console.warn = warn;
    }

    answering({
        'groups/group/resets/a.p1': found({ askedBy: 'b', askedAt: stamp(10), doneAt: stamp(20) }),
    });

    let reset;

    search.watchReset('a.p1', (one) => { reset = one; });
    assert.deepEqual(reset, { phoneId: 'a.p1', askedBy: 'b', askedAt: 10, doneAt: 20 });
});

test('a reset is asked for with two fields, written whole', async () => {
    await search.askReset('a.p1', 'b');

    assert.deepEqual(writes(), [
        { set: 'groups/group/resets/a.p1', data: { askedBy: 'b', askedAt: 'NOW' }, options: undefined },
    ]);
});

test('forgetting a device removes its four traces in one batch', async () => {
    await search.forgetPhone('anna.p1');

    assert.deepEqual(writes(), [
        { delete: 'groups/group/positions/anna.p1' },
        { delete: 'groups/group/watch/anna.p1' },
        {
            set: 'groups/group/grants/anna',
            data: { devices: { remove: 'p1' } },
            options: { merge: true },
        },
        {
            set: 'groups/group/devices/anna',
            data: { p1: 'DELETED' },
            options: { merge: true },
        },
        { commit: true },
    ]);
});

test('labels are read as the names a member gave, none where the document is missing', () => {
    answering({ 'groups/group/labels/me': found({ devices: { 'a.p1': 'Dad' }, members: { b: 'Bob' } }) });

    let labels;

    search.watchLabels('me', (one) => { labels = one; });
    assert.equal(labels.phones.get('a.p1'), 'Dad');
    assert.equal(labels.members.get('b'), 'Bob');

    answering({ 'groups/group/labels/me': missing() });
    search.watchLabels('me', (one) => { labels = one; });
    assert.equal(labels.phones.size, 0);
});

test('a name is merged into its map, and taken away by deleting the field', async () => {
    await search.writeLabel('me', 'devices', 'a.p1', 'Dad');
    await search.writeLabel('me', 'members', 'b', null);

    assert.deepEqual(writes(), [
        {
            set: 'groups/group/labels/me',
            data: { devices: { 'a.p1': 'Dad' } },
            options: { merge: true },
        },
        {
            set: 'groups/group/labels/me',
            data: { members: { b: 'DELETED' } },
            options: { merge: true },
        },
    ]);
});
