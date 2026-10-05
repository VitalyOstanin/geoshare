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
    waitOperation,
} from '../public/lib/setup-project.js';

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
    const { api, requests } = fakeFetch(() => [500, {}]);

    await assert.rejects(createDatabase(api, PROJECT, 'europe-north1', noSleep), (error) => error.status === 500);
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
