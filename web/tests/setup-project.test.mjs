import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    createDatabase,
    deployRules,
    enableFirestoreApi,
    ensureAndroidApp,
    ensureFingerprints,
    ensureWebApp,
    listProjects,
    makeApi,
    accountEmail,
    consoleLink,
    setupConfigOf,
    consoleBaseOf,
    failure,
    failureKind,
    failureWord,
    passing,
    retryPassing,
    waitOperation,
} from '../public/lib/setup-project.js';

import { WORDS_EN, WORDS_RU } from '../public/lib/words/setup.js';

const PROJECT = 'geoshare-test-project';
const PACKAGE = 'io.github.vitalyostanin.geoshare';
const APP_NAME = `projects/${PROJECT}/androidApps/1:123:android:abc`;
const SHA1 = 'aa'.repeat(20);
const SHA256 = 'bb'.repeat(32);

const noSleep = async () => {};

/**
 * A fetch that records every request and answers from `route(method, url, body)`,
 * which returns `[status, body]`; the body is sent back as JSON.
 */
function fakeFetch(route) {
    const requests = [];
    const fetchFn = async (url, init) => {
        const body = init.body ? JSON.parse(init.body) : undefined;

        requests.push({ method: init.method, url, body, headers: init.headers });

        const [status, answer] = route(init.method, url, body, requests.length);

        return {
            ok: status >= 200 && status < 300,
            status,
            text: async () => (answer === undefined ? '' : JSON.stringify(answer)),
        };
    };

    return { fetchFn, requests, api: makeApi(fetchFn, 'token-1') };
}

test('an answer that is not a success is thrown with its status and body', async () => {
    const { api, requests } = fakeFetch(() => [409, { error: { status: 'ALREADY_EXISTS', message: 'there' } }]);

    await assert.rejects(api('POST', 'https://example.test/x?a=1', { k: 1 }), (error) => {
        assert.equal(error.status, 409);
        assert.equal(error.data.error.status, 'ALREADY_EXISTS');
        assert.match(error.message, /POST https:\/\/example.test\/x -> 409 ALREADY_EXISTS there/);

        return true;
    });
    assert.equal(requests[0].headers.Authorization, 'Bearer token-1');
    assert.equal(requests[0].headers['Content-Type'], 'application/json');
});

test('an operation is polled until it is done and yields its response', async () => {
    const { api, requests } = fakeFetch((method, url, body, n) =>
        n < 3 ? [200, { name: 'operations/o', done: false }] : [200, { name: 'operations/o', done: true, response: { v: 1 } }],
    );
    const sleeps = [];

    const result = await waitOperation(api, 'https://base.test/v1', { name: 'operations/o' }, async (ms) => sleeps.push(ms));

    assert.deepEqual(result, { v: 1 });
    assert.equal(requests.length, 3);
    assert.equal(requests[0].url, 'https://base.test/v1/operations/o');
    assert.equal(sleeps.length, 3);
});

test('an operation that ended with an error is thrown', async () => {
    const { api } = fakeFetch(() => [200, {}]);

    await assert.rejects(
        waitOperation(api, 'https://base.test', { name: 'o', done: true, error: { code: 3 } }, noSleep),
        /"code":3/,
    );
});

test('the projects come from the results of the list', async () => {
    const { api } = fakeFetch(() => [200, { results: [{ projectId: PROJECT }] }]);
    const empty = fakeFetch(() => [200, {}]);

    assert.deepEqual(await listProjects(api), [{ projectId: PROJECT }]);
    assert.deepEqual(await listProjects(empty.api), []);
});

test('the projects of every page of the list are read', async () => {
    const { api, requests } = fakeFetch((method, url) =>
        url.includes('pageToken=next%2F1')
            ? [200, { results: [{ projectId: 'second' }] }]
            : [200, { results: [{ projectId: PROJECT }], nextPageToken: 'next/1' }]);

    assert.deepEqual(await listProjects(api), [{ projectId: PROJECT }, { projectId: 'second' }]);
    assert.equal(requests.length, 2);
});

test('the account address is read from userinfo, and is null when Google does not give it', async () => {
    const given = fakeFetch(() => [200, { sub: '1', email: 'owner@example.com' }]);
    const without = fakeFetch(() => [200, { sub: '1' }]);
    const refused = fakeFetch(() => [401, { error: 'invalid_token' }]);

    assert.equal(await accountEmail(given.api), 'owner@example.com');
    assert.equal(given.requests[0].url, 'https://www.googleapis.com/oauth2/v3/userinfo');
    assert.equal(await accountEmail(without.api), null);
    assert.equal(await accountEmail(refused.api), null);
});

test('a console link names the account, and stays as it was without one', () => {
    const providers = `https://console.firebase.google.com/project/${PROJECT}/authentication/providers`;
    const clients = `https://console.cloud.google.com/auth/clients?project=${PROJECT}`;

    assert.equal(consoleLink(providers, 'owner+1@example.com'), `${providers}?authuser=owner%2B1%40example.com`);
    assert.equal(consoleLink(clients, 'owner@example.com'), `${clients}&authuser=owner%40example.com`);
    assert.equal(consoleLink(clients, null), clients);
});

test('enabling Firestore waits only for an operation that is not done', async () => {
    const done = fakeFetch(() => [200, { done: true }]);
    const pending = fakeFetch((method, url, body, n) =>
        n === 1 ? [200, { name: 'operations/e', done: false }] : [200, { name: 'operations/e', done: true }],
    );

    await enableFirestoreApi(done.api, '123', noSleep);
    await enableFirestoreApi(pending.api, '123', noSleep);

    assert.equal(done.requests.length, 1);
    assert.match(done.requests[0].url, /projects\/123\/services\/firestore.googleapis.com:enable$/);
    assert.equal(pending.requests.length, 2);
});

test('a database is created, and one that exists already is left alone', async () => {
    const fresh = fakeFetch(() => [200, { name: 'operations/d', done: true, response: {} }]);
    const there = fakeFetch(() => [409, { error: { status: 'ALREADY_EXISTS' } }]);

    assert.equal(await createDatabase(fresh.api, PROJECT, 'europe-north1', noSleep), 'created');
    assert.deepEqual(fresh.requests[0].body, { locationId: 'europe-north1', type: 'FIRESTORE_NATIVE' });
    assert.equal(await createDatabase(there.api, PROJECT, 'europe-north1', noSleep), 'existing');
});

test('a 403 is retried while the API spreads and then the database is created', async () => {
    const { api, requests } = fakeFetch((method, url, body, n) =>
        n <= 2 ? [403, { error: { status: 'PERMISSION_DENIED' } }] : [200, { done: true, response: {} }],
    );
    const sleeps = [];

    assert.equal(await createDatabase(api, PROJECT, 'europe-north1', async (ms) => sleeps.push(ms)), 'created');
    assert.equal(requests.length, 3);
    assert.deepEqual(sleeps, [15_000, 15_000]);
});

test('a 403 that never goes away is thrown after twenty attempts', async () => {
    const { api, requests } = fakeFetch(() => [403, { error: { status: 'PERMISSION_DENIED' } }]);

    await assert.rejects(createDatabase(api, PROJECT, 'europe-north1', noSleep), (error) => error.status === 403);
    assert.equal(requests.length, 20);
});

test('another failure of the database is thrown at once', async () => {
    const { api, requests } = fakeFetch(() => [400, {}]);

    await assert.rejects(createDatabase(api, PROJECT, 'europe-north1', noSleep), (error) => error.status === 400);
    assert.equal(requests.length, 1);
});

test('the web app is created once and found on a re-run', async () => {
    const app = { name: `projects/${PROJECT}/webApps/w`, appId: '1:1:web:w' };
    const empty = fakeFetch((method) =>
        method === 'GET' ? [200, {}] : [200, { done: true, response: app }],
    );
    const there = fakeFetch(() => [200, { apps: [app] }]);

    assert.deepEqual(await ensureWebApp(empty.api, PROJECT), {
        name: `projects/${PROJECT}/webApps/w`,
        appId: '1:1:web:w',
        created: true,
    });
    assert.equal((await ensureWebApp(there.api, PROJECT)).created, false);
    assert.deepEqual(
        there.requests.map((r) => r.method),
        ['GET'],
    );
});

test('the Android app is told by its package, and created when no one has it', async () => {
    const other = { name: 'projects/p/androidApps/o', appId: 'o', packageName: 'some.other' };
    const mine = { name: APP_NAME, appId: '1:123:android:abc', packageName: PACKAGE };
    const there = fakeFetch(() => [200, { apps: [other, mine] }]);
    const empty = fakeFetch((method) =>
        method === 'GET' ? [200, { apps: [other] }] : [200, { done: true, response: mine }],
    );

    assert.deepEqual(await ensureAndroidApp(there.api, PROJECT, PACKAGE, noSleep), {
        name: APP_NAME,
        appId: '1:123:android:abc',
        created: false,
    });
    assert.equal((await ensureAndroidApp(empty.api, PROJECT, PACKAGE, noSleep)).created, true);
    assert.deepEqual(empty.requests[1].body, { packageName: PACKAGE, displayName: 'geoshare' });
});

test('only the fingerprints the app does not have are posted, each with its type', async () => {
    const { api, requests } = fakeFetch((method) =>
        method === 'GET' ? [200, { certificates: [{ shaHash: SHA1.toUpperCase() }] }] : [200, {}],
    );

    const posted = await ensureFingerprints(api, APP_NAME, [SHA1, SHA256]);
    const posts = requests.filter((r) => r.method === 'POST');

    assert.deepEqual(posted, [SHA256]);
    assert.deepEqual(posts.map((r) => r.body), [{ shaHash: SHA256, certType: 'SHA_256' }]);
});

test('a re-run with every fingerprint present posts nothing', async () => {
    const { api, requests } = fakeFetch(() => [
        200,
        { certificates: [{ shaHash: SHA1 }, { shaHash: SHA256 }] },
    ]);

    assert.deepEqual(await ensureFingerprints(api, APP_NAME, [SHA1, SHA256]), []);
    assert.equal(requests.length, 1);
});

test('a SHA-1 is posted as SHA_1', async () => {
    const { api, requests } = fakeFetch((method) => (method === 'GET' ? [200, {}] : [200, {}]));

    await ensureFingerprints(api, APP_NAME, [SHA1]);

    assert.equal(requests[1].body.certType, 'SHA_1');
});

test('fingerprints written as keytool prints them are posted bare, each with its type', async () => {
    const { api, requests } = fakeFetch((method) =>
        method === 'GET' ? [200, { certificates: [{ shaHash: SHA256 }] }] : [200, {}],
    );
    const colons = (hash) => hash.toUpperCase().match(/../gu).join(':');

    const posted = await ensureFingerprints(api, APP_NAME, [colons(SHA1), colons(SHA256)]);

    assert.deepEqual(posted, [SHA1]);
    assert.deepEqual(
        requests.filter((r) => r.method === 'POST').map((r) => r.body),
        [{ shaHash: SHA1, certType: 'SHA_1' }],
    );
});

test('the rules are released as new when there is no release yet', async () => {
    const { api, requests } = fakeFetch((method, url) =>
        url.endsWith('/rulesets') ? [200, { name: `projects/${PROJECT}/rulesets/r1` }] : [200, {}],
    );

    assert.equal(await deployRules(api, PROJECT, 'rules text'), `projects/${PROJECT}/rulesets/r1`);
    assert.deepEqual(requests[0].body.source.files, [{ name: 'firestore.rules', content: 'rules text' }]);
    assert.deepEqual(requests.map((r) => r.method), ['POST', 'POST']);
    assert.equal(requests[1].body.rulesetName, `projects/${PROJECT}/rulesets/r1`);
});

test('a release that exists already is moved to the new ruleset by a PATCH', async () => {
    const { api, requests } = fakeFetch((method, url) => {
        if (url.endsWith('/rulesets')) {
            return [200, { name: `projects/${PROJECT}/rulesets/r2` }];
        }

        return method === 'POST' ? [409, { error: { status: 'ALREADY_EXISTS' } }] : [200, {}];
    });

    await deployRules(api, PROJECT, 'rules text');

    const patch = requests[requests.length - 1];

    assert.equal(patch.method, 'PATCH');
    assert.match(patch.url, /releases\/cloud.firestore$/);
    assert.equal(patch.body.release.rulesetName, `projects/${PROJECT}/rulesets/r2`);
});

test('a release that fails otherwise is thrown', async () => {
    const { api } = fakeFetch((method, url) =>
        url.endsWith('/rulesets') ? [200, { name: 'r' }] : [400, { error: { status: 'INVALID_ARGUMENT' } }],
    );

    await assert.rejects(deployRules(api, PROJECT, 'x'), (error) => error.status === 400);
});

test('a failed step is told to the page by a kind, not by the text of the exception', async () => {
    const denied = makeApi(fakeFetch(() => [403, { error: { status: 'PERMISSION_DENIED' } }]).fetchFn, 't');
    const unauthorized = makeApi(fakeFetch(() => [401, {}]).fetchFn, 't');
    const gone = makeApi(fakeFetch(() => [404, {}]).fetchFn, 't');
    const broken = makeApi(fakeFetch(() => [500, 'oops']).fetchFn, 't');
    const busy = makeApi(fakeFetch(() => [429, {}]).fetchFn, 't');
    const refused = makeApi(fakeFetch(() => [400, {}]).fetchFn, 't');
    const offline = makeApi(async () => {
        throw new TypeError('Failed to fetch');
    }, 't');
    const kindOf = async (api) => failureKind(await api('GET', 'https://x.test/a').catch((e) => e));

    assert.equal(await kindOf(denied), 'denied');
    assert.equal(await kindOf(unauthorized), 'denied');
    assert.equal(await kindOf(gone), 'missing');
    assert.equal(await kindOf(broken), 'transient');
    assert.equal(await kindOf(busy), 'transient');
    assert.equal(await kindOf(refused), 'other');
    assert.equal(await kindOf(offline), 'network');
});

test('a fetch that failed keeps what it failed with', async () => {
    const reason = new TypeError('Failed to fetch');
    const offline = makeApi(async () => {
        throw reason;
    }, 't');
    const error = await offline('GET', 'https://x.test/a').catch((e) => e);

    assert.equal(error.cause, reason);
});

// The operation goes on at Google while one poll of it is lost to the network
// or to a busy server: the step waits on rather than failing with it.
test('a poll lost to the network or a busy server is polled again', async () => {
    const answers = [
        () => {
            throw failure('offline for a moment', 'network');
        },
        () => {
            throw Object.assign(new Error('503'), { status: 503, kind: 'transient' });
        },
        () => ({ name: 'op', done: true, response: { v: 2 } }),
    ];
    const api = async () => answers.shift()();

    assert.deepEqual(await waitOperation(api, 'https://x.test', { name: 'op' }, noSleep), { v: 2 });
});

test('polls lost one after another give up after a few', async () => {
    let polls = 0;
    const api = async () => {
        polls++;
        throw failure('offline', 'network');
    };
    const error = await waitOperation(api, 'https://x.test', { name: 'op' }, noSleep)
        .catch((e) => e);

    assert.equal(failureKind(error), 'network');
    assert.ok(polls < 10, `${polls} polls`);
});

test('a poll the service refused is not polled again', async () => {
    let polls = 0;
    const api = async () => {
        polls++;
        throw Object.assign(new Error('403'), { status: 403, kind: 'denied' });
    };

    await assert.rejects(waitOperation(api, 'https://x.test', { name: 'op' }, noSleep), /403/);
    assert.equal(polls, 1);
});

test('an operation that does not finish is a timeout', async () => {
    const api = async () => ({ name: 'op', done: false });
    const error = await waitOperation(api, 'https://x.test', { name: 'op' }, noSleep)
        .catch((e) => e);

    assert.equal(failureKind(error), 'timeout');
});

test('every kind of failure has a phrase in both languages', () => {
    for (const kind of ['network', 'denied', 'timeout', 'missing', 'transient', 'other']) {
        const key = failureWord(kind);

        assert.equal(typeof WORDS_EN[key], 'string', key);
        assert.equal(typeof WORDS_RU[key], 'string', key);
    }

    assert.equal(failureWord(failureKind(new Error('anything'))), failureWord('other'));
});

test('a setup config as the script writes it is taken, a broken one is not', () => {
    const written = {
        clientId: '000000000000-example.apps.googleusercontent.com',
        packageName: 'io.github.vitalyostanin.geoshare',
        location: 'europe-north1',
        fingerprints: [],
        consoleUrl: 'https://console.firebase.google.com/',
    };

    assert.equal(setupConfigOf(written), written);
    assert.equal(setupConfigOf({ ...written, consoleUrl: undefined }).location, 'europe-north1');
    assert.equal(setupConfigOf(null), null);
    assert.equal(setupConfigOf([]), null);
    assert.equal(setupConfigOf({ ...written, clientId: '' }), null);
    assert.equal(setupConfigOf({ ...written, location: undefined }), null);
    assert.equal(setupConfigOf({ ...written, fingerprints: 'AB:CD' }), null);
    assert.equal(setupConfigOf({ ...written, fingerprints: [42] }), null);
});

test('the console address defaults and always ends with a slash', () => {
    const written = {
        clientId: 'x.apps.googleusercontent.com',
        packageName: 'io.github.vitalyostanin.geoshare',
        location: 'europe-north1',
        fingerprints: [],
    };
    const base = 'https://console.firebase.google.com/';
    const custom = 'https://c.example/fb';

    assert.equal(consoleBaseOf(setupConfigOf({ ...written, consoleUrl: undefined })), base);
    assert.equal(consoleBaseOf(null), base);
    assert.equal(consoleBaseOf({ ...written, consoleUrl: custom }), `${custom}/`);
    assert.equal(consoleBaseOf({ ...written, consoleUrl: `${custom}/` }), `${custom}/`);
});

test('only a network or transient failure is worth asking again', () => {
    for (const kind of ['network', 'transient']) {
        assert.equal(passing({ kind }), true);
    }

    for (const kind of ['denied', 'missing', 'timeout', 'other', undefined]) {
        assert.equal(passing({ kind }), false);
    }

    assert.equal(passing(new Error('untagged')), false);
});

test('a call that fails in a way that may pass is made again with a growing pause', async () => {
    const sleeps = [];
    let runs = 0;
    const result = await retryPassing(
        async () => {
            runs++;

            if (runs <= 3) {
                throw failure('busy', 'transient');
            }

            return 'done';
        },
        async (ms) => sleeps.push(ms),
        () => 1,
    );

    assert.equal(result, 'done');
    assert.deepEqual(sleeps, [1000, 2000, 4000]);
});

test('the pause is cut by the random share, never below half', async () => {
    const sleeps = [];

    await assert.rejects(
        retryPassing(
            async () => {
                throw failure('offline', 'network');
            },
            async (ms) => sleeps.push(ms),
            () => 0,
            3,
        ),
        (error) => error.kind === 'network',
    );
    assert.deepEqual(sleeps, [500, 1000]);
});

test('a call that is refused for good is not made again', async () => {
    let runs = 0;

    await assert.rejects(
        retryPassing(
            async () => {
                runs++;
                throw failure('no', 'denied');
            },
            noSleep,
        ),
        (error) => error.kind === 'denied',
    );
    assert.equal(runs, 1);
});

test('a single call of a step is made again after a 429 and given up after four runs', async () => {
    const busy = fakeFetch((method, url, body, n) =>
        n === 1 ? [429, {}] : [200, { apps: [{ name: 'n', appId: 'a' }] }],
    );
    const dead = fakeFetch(() => [503, {}]);

    assert.equal((await ensureWebApp(busy.api, PROJECT, noSleep)).created, false);
    assert.equal(busy.requests.length, 2);
    assert.equal((await listProjects(busy.api, noSleep)).length, 0);
    await assert.rejects(listProjects(dead.api, noSleep), (error) => error.status === 503);
    assert.equal(dead.requests.length, 4);
});

test('a database POST that a busy server refused is made again', async () => {
    const { api, requests } = fakeFetch((method, url, body, n) =>
        n === 1 ? [503, {}] : [200, { done: true, response: {} }],
    );

    assert.equal(await createDatabase(api, PROJECT, 'europe-north1', noSleep), 'created');
    assert.equal(requests.length, 2);
});

test('a rules release refused by a busy server is made again', async () => {
    let releases = 0;
    const { api } = fakeFetch((method, url) => {
        if (url.endsWith('/rulesets')) {
            return [200, { name: 'r' }];
        }

        releases++;

        return releases === 1 ? [500, {}] : [200, {}];
    });

    assert.equal(await deployRules(api, PROJECT, 'x', noSleep), 'r');
    assert.equal(releases, 2);
});

test('an operation that ended with an error is told by the code of its status', async () => {
    const { api } = fakeFetch(() => [200, {}]);
    const kinds = {
        7: 'denied',
        16: 'denied',
        8: 'transient',
        14: 'transient',
        4: 'timeout',
        5: 'missing',
        3: 'other',
    };

    for (const [code, kind] of Object.entries(kinds)) {
        const op = { name: 'o', done: true, error: { code: Number(code) } };

        await assert.rejects(
            waitOperation(api, 'https://base.test', op, noSleep),
            (error) => error.kind === kind,
        );
    }
});

test('a web app made by a creation that failed on the way back is not made twice', async () => {
    const app = { name: `projects/${PROJECT}/webApps/w`, appId: '1:1:web:w' };
    let made = false;
    const lost = fakeFetch((method) => {
        if (method === 'POST') {
            made = true;

            return [503, {}];
        }

        return [200, made ? { apps: [app] } : {}];
    });

    assert.equal((await ensureWebApp(lost.api, PROJECT, noSleep)).created, false);
    assert.deepEqual(
        lost.requests.map((r) => r.method),
        ['GET', 'POST', 'GET'],
    );
});
