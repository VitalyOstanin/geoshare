import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';

import {
    appSettings,
    deploySite,
    ensureSite,
    googleSignInEnabled,
    releasedPaths,
    versionConfig,
    webConfigFile,
} from '../public/lib/setup-hosting.js';

const PROJECT = 'example-try-61d5c3';

function failure(status) {
    const error = new Error(`status ${status}`);

    error.status = status;

    return error;
}

/** A fake api that records every call and answers by the first matching route. */
function fakeApi(routes) {
    const calls = [];
    const api = async (method, url, body, raw) => {
        calls.push({ method, url, body, raw });

        for (const [pattern, answer] of routes) {
            if (pattern.test(`${method} ${url}`)) {
                const result = typeof answer === 'function' ? answer(body) : answer;

                if (result instanceof Error) {
                    throw result;
                }

                return result;
            }
        }

        throw new Error(`unexpected ${method} ${url}`);
    };

    api.calls = calls;

    return api;
}

test('the default site named like the project is used as it is', async () => {
    const site = { name: `projects/${PROJECT}/sites/${PROJECT}`, type: 'DEFAULT_SITE' };
    const api = fakeApi([[/GET .*\/sites$/, { sites: [site] }]]);

    assert.deepEqual(await ensureSite(api, PROJECT, 'web-app'), site);
    assert.equal(api.calls.length, 1);
});

test('a site is created under the project name when there is none', async () => {
    const created = { name: `projects/${PROJECT}/sites/${PROJECT}` };
    const api = fakeApi([
        [/GET .*\/sites$/, {}],
        [/POST .*\/sites\?siteId=/, created],
    ]);

    assert.deepEqual(await ensureSite(api, PROJECT, 'web-app'), created);
    assert.match(api.calls[1].url, new RegExp(`siteId=${PROJECT}$`));
    assert.deepEqual(api.calls[1].body, { appId: 'web-app' });
});

test('a taken site name is told of, and no other name is tried', async () => {
    for (const status of [400, 409]) {
        const api = fakeApi([
            [/GET .*\/sites$/, { sites: [{ name: 'projects/x/sites/other' }] }],
            [/POST .*\/sites\?siteId=/, failure(status)],
        ]);

        await assert.rejects(ensureSite(api, PROJECT, 'web-app'), { code: 'site-name-taken' });
        assert.equal(api.calls.filter((call) => call.method === 'POST').length, 1);
    }
});

test('other failures of creating the site pass through', async () => {
    const api = fakeApi([
        [/GET .*\/sites$/, {}],
        [/POST .*\/sites\?siteId=/, failure(500)],
    ]);

    await assert.rejects(ensureSite(api, PROJECT, 'web-app'), { status: 500 });
});

test('the settings come out of the android app config', async () => {
    const services = {
        project_info: { project_id: PROJECT, project_number: '123456' },
        client: [
            {
                client_info: { mobilesdk_app_id: '1:123456:android:abc' },
                api_key: [{ current_key: 'key-1' }],
            },
        ],
    };
    const api = fakeApi([
        [/GET .*\/config$/, { configFileContents: btoa(JSON.stringify(services)) }],
    ]);

    const app = `projects/${PROJECT}/androidApps/1:1:android:x`;

    assert.deepEqual(await appSettings(api, app, 's1'), {
        version: 1,
        projectId: PROJECT,
        applicationId: '1:123456:android:abc',
        apiKey: 'key-1',
        gcmSenderId: '123456',
        site: 's1',
    });
});

function deployApi(required) {
    return fakeApi([
        [/POST .*\/versions$/, { name: `sites/${PROJECT}/versions/v1` }],
        [
            /populateFiles$/,
            (body) => ({
                uploadUrl: 'https://upload.example/up',
                uploadRequiredHashes: required(body.files),
            }),
        ],
        [/POST https:\/\/upload\.example/, {}],
        [/PATCH /, {}],
        [/POST .*\/releases/, {}],
    ]);
}

test('only the hashes hosting asked for are uploaded', async () => {
    const api = deployApi((hashes) => [hashes['/geoshare.json']]);
    const url = await deploySite(api, PROJECT, {
        '/index.html': '<p>hi</p>',
        '/geoshare.json': '{}',
        '/bytes.bin': new Uint8Array([1, 2, 3]),
    });
    const populate = api.calls.find((call) => call.url.endsWith(':populateFiles'));
    const uploads = api.calls.filter((call) => call.url.startsWith('https://upload.example'));

    assert.equal(url, `https://${PROJECT}.web.app`);
    assert.equal(uploads.length, 1);
    const hash = populate.body.files['/geoshare.json'];

    assert.equal(uploads[0].url, `https://upload.example/up/${hash}`);
    assert.equal(uploads[0].raw, true);
    assert.ok(uploads[0].body instanceof Uint8Array);
    assert.match(populate.body.files['/index.html'], /^[0-9a-f]{64}$/);
});

test('nothing is uploaded when hosting holds every file already', async () => {
    const api = deployApi(() => []);

    await deploySite(api, PROJECT, { '/index.html': 'x' });

    const uploads = api.calls.filter((call) => call.url.startsWith('https://upload.example'));

    assert.equal(uploads.length, 0);
});

test('the version is finalized before it is released', async () => {
    const api = deployApi((hashes) => Object.values(hashes));

    await deploySite(api, PROJECT, { '/index.html': 'x' });

    const steps = api.calls.map((call) => call.method + ' ' + call.url.split('?')[0]);
    const finalized = api.calls.findIndex((call) => call.body?.status === 'FINALIZED');
    const released = steps.findIndex((step) => step.endsWith('/releases'));

    assert.ok(finalized > 0);
    assert.ok(released > finalized);
    assert.equal(api.calls[released].url.split('versionName=')[1], `sites/${PROJECT}/versions/v1`);
});

test("the group's site keeps the /join rewrite and the headers of firebase.json", async () => {
    const hosting = JSON.parse(readFileSync(new URL('../../firebase.json', import.meta.url))).hosting;
    const config = versionConfig(hosting);
    const api = deployApi(() => []);

    await deploySite(api, PROJECT, { '/index.html': 'x' }, config);

    const created = api.calls.find((call) => call.url.endsWith('/versions'));

    assert.deepEqual(created.body.config.rewrites, [{ glob: '/join', path: '/join.html' }]);
    assert.match(created.body.config.headers[0].headers['Content-Security-Policy'], /default-src/);
    assert.equal(created.body.config.headers.length, hosting.headers.length);
});

test('google sign-in is on only when the config says so, and off when there is none', async () => {
    const on = fakeApi([[/GET /, { enabled: true }]]);
    const off = fakeApi([[/GET /, { enabled: false }]]);
    const missing = fakeApi([[/GET /, failure(404)]]);
    const broken = fakeApi([[/GET /, failure(403)]]);

    assert.equal(await googleSignInEnabled(on, PROJECT), true);
    assert.equal(await googleSignInEnabled(off, PROJECT), false);
    assert.equal(await googleSignInEnabled(missing, PROJECT), false);
    await assert.rejects(googleSignInEnabled(broken, PROJECT), { status: 403 });
});

test("the group's page signs in through its own site", async () => {
    const api = fakeApi([
        [
            /GET .*webApps\/w\/config$/,
            { projectId: PROJECT, appId: '1:2:web:3', apiKey: 'k', authDomain: `${PROJECT}.firebaseapp.com` },
        ],
    ]);
    const file = await webConfigFile(api, `projects/${PROJECT}/webApps/w`, PROJECT);
    const config = JSON.parse(file.slice(file.indexOf('{'), file.lastIndexOf('}') + 1));

    assert.match(file, /^\/\/ .*\nexport const firebaseConfig = /);
    assert.equal(config.authDomain, `${PROJECT}.web.app`);
    assert.equal(config.appId, '1:2:web:3');
});

test('the files the site serves are read from the live release, page by page', async () => {
    const version = `sites/${PROJECT}/versions/v1`;
    const api = fakeApi([
        [/GET .*channels\/live$/, { release: { version: { name: version } } }],
        [/pageToken=next$/, { files: [{ path: '/geoshare.json' }] }],
        [/files\?status=ACTIVE$/, { files: [{ path: '/index.html' }], nextPageToken: 'next' }],
    ]);

    assert.deepEqual([...(await releasedPaths(api, PROJECT))], ['/index.html', '/geoshare.json']);
});

test('a site with nothing released serves no files', async () => {
    const api = fakeApi([[/GET .*channels\/live$/, {}]]);

    assert.equal((await releasedPaths(api, PROJECT)).size, 0);
    assert.equal(api.calls.length, 1);
});
