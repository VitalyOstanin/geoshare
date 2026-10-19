// The JavaScript half of tools/project.sh: what it reads out of the answers of
// the firebase command line and the Google APIs, the results its --json
// prints, and the quota-day arithmetic of `show --usage`. The script itself
// needs a signed-in account and a real project, so these are the parts of it
// that can be held to a test.
import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    appIds,
    appSettings,
    certificateHashes,
    chosenSite,
    consoleLink,
    googleSignInOn,
    listedShaHashes,
    storedToken,
} from '../../tools/project/firebase.mjs';
import {
    midnight,
    reportLines,
    share,
    size,
    stamp,
} from '../../tools/project/quota.mjs';
import {
    createResult,
    deleteResult,
    showResult,
    updateResult,
} from '../../tools/project/results.mjs';

test('a line the command line prints ahead of its JSON is skipped', () => {
    const raw = 'Update available\n{"result":[{"shaHash":"aa11"},{"shaHash":"bb22"}]}';

    assert.deepEqual(listedShaHashes(raw), ['aa11', 'bb22']);
});

test('a project without apps or fingerprints reads as empty, not as a failure', () => {
    assert.deepEqual(appIds('{}'), []);
    assert.deepEqual(certificateHashes('{}'), []);
    assert.deepEqual(appIds('{"apps":[{"appId":"1:10:android:ab"}]}'), ['1:10:android:ab']);
    assert.deepEqual(certificateHashes('{"certificates":[{"shaHash":"cc33"}]}'), ['cc33']);
});

test('a Google provider without the enabled field is off', () => {
    assert.equal(googleSignInOn('{"name":"google.com"}'), false);
    assert.equal(googleSignInOn('{"enabled":false}'), false);
    assert.equal(googleSignInOn('{"enabled":true}'), true);
});

test('the default site wins over one listed before it', () => {
    const body = JSON.stringify({ sites: [
        { name: 'projects/demo/sites/demo-extra', type: 'USER_SITE' },
        { name: 'projects/demo/sites/demo', type: 'DEFAULT_SITE' },
    ] });

    assert.equal(chosenSite(body), 'demo');
    assert.equal(chosenSite('{"sites":[{"name":"projects/demo/sites/other"}]}'), 'other');
    assert.equal(chosenSite('{}'), '');
});

test('the settings file is built out of the configuration inside the JSON', () => {
    const fileContents = JSON.stringify({
        project_info: { project_id: 'demo', project_number: '100' },
        client: [{
            client_info: { mobilesdk_app_id: '1:100:android:ab' },
            api_key: [{ current_key: 'key-for-tests' }],
        }],
    });
    const raw = `notice\n${JSON.stringify({ result: { fileContents } })}`;

    assert.deepEqual(appSettings(raw, 'demo-geoshare'), {
        version: 1,
        projectId: 'demo',
        applicationId: '1:100:android:ab',
        apiKey: 'key-for-tests',
        gcmSenderId: '100',
        site: 'demo-geoshare',
    });
});

test('the token is the named account\'s, whatever the case of its address', () => {
    const store = {
        user: { email: 'first@example.invalid' },
        tokens: { access_token: 'first-token', expires_at: 10_000_000 },
        additionalAccounts: [{
            user: { email: 'second@example.invalid' },
            tokens: { access_token: 'second-token', expires_at: 20_000_000 },
        }],
    };

    assert.deepEqual(storedToken(store, '', 4_000_000),
        { token: 'first-token', secondsLeft: 6000 });
    assert.deepEqual(storedToken(store, 'Second@Example.invalid', 4_000_500),
        { token: 'second-token', secondsLeft: 15_999 });
});

test('a console link opens in the account only when one is known', () => {
    const url = 'https://console.cloud.google.com/auth/clients?project=demo';

    assert.equal(consoleLink(url, ''), url);
    assert.equal(consoleLink(url, 'owner@example.invalid'),
        `${url}&authuser=owner%40example.invalid`);
});

test('create and show name the same addresses on the site', () => {
    const step = { what: 'Enable Google', url: 'https://console.invalid/providers' };
    const made = createResult({ projectId: 'demo', site: 'demo-geoshare', steps: [step, step] });
    const shown = showResult({
        projectId: 'demo', site: 'demo-geoshare', googleOn: false, googleStep: step,
    });

    assert.equal(made.connectLink, 'https://demo-geoshare.web.app/join');
    assert.equal(made.settingsUrl, 'https://demo-geoshare.web.app/geoshare.json');
    assert.equal(made.done, false);
    assert.deepEqual(Object.keys(made), Object.keys(shown));
    assert.deepEqual(shown.manualSteps, [step]);
    assert.equal('usage' in shown, false);
});

test('show is done once Google is on, and carries usage when asked for', () => {
    const shown = showResult({
        projectId: 'demo', site: 'demo', googleOn: true, googleStep: {}, usage: { days: 0 },
    });

    assert.deepEqual(shown.manualSteps, []);
    assert.equal(shown.done, true);
    assert.deepEqual(shown.usage, { days: 0 });
});

test('update and delete results', () => {
    assert.deepEqual(updateResult({ projectId: 'demo', site: 'demo' }),
        { projectId: 'demo', site: 'demo', deployed: true });
    assert.deepEqual(deleteResult({ projectId: 'demo', now: Date.UTC(2026, 0, 1, 12, 0, 0, 250) }),
        { projectId: 'demo', deleteRequested: true, restorableUntil: '2026-01-31T12:00:00Z' });
});

test('the quota day starts at midnight in Los Angeles, in either offset', () => {
    assert.equal(stamp(midnight(2026, 1, 15)), '2026-01-15T08:00:00Z');
    assert.equal(stamp(midnight(2026, 7, 15)), '2026-07-15T07:00:00Z');
    // The days daylight saving time begins and ends start in the offset of
    // the night before.
    assert.equal(stamp(midnight(2026, 3, 8)), '2026-03-08T08:00:00Z');
    assert.equal(stamp(midnight(2026, 11, 1)), '2026-11-01T07:00:00Z');
    // A day past the end of the month rolls over.
    assert.equal(stamp(midnight(2026, 2, 29)), '2026-03-01T08:00:00Z');
});

test('sizes and shares as the table shows them', () => {
    assert.equal(size(512), '512 B');
    assert.equal(size(1536), '1.5 KiB');
    assert.equal(size(3 * 1024 ** 4), '3072.0 GiB');
    assert.equal(share({ used: 25, limit: 200 }), '12.5%');
    assert.equal(share({ used: 25, limit: 0 }), '?');
});

test('earlier days get a table of their own, held against the daily limits', () => {
    const entry = { used: 10, limit: 100 };
    const report = {
        quotaDayFrom: '2026-07-15T07:00:00Z',
        firestore: { reads: entry, writes: entry, deletes: entry, storedBytes: entry },
        hosting: { sentBytesThisMonth: entry, storedBytes: entry },
        earlierDays: [],
    };
    const limits = { reads: 50_000, writes: 20_000, deletes: 20_000 };

    assert.equal(reportLines(report, limits).length, 6);

    const day = { from: '2026-07-14T07:00:00Z', reads: 25_000, writes: 2000, deletes: 0 };
    const lines = reportLines({ ...report, earlierDays: [day] }, limits);

    assert.equal(lines.length, 9);
    assert.equal(lines[6], '');
    assert.match(lines[8], /25000 50%\s+2000 10%\s+0 0%$/);
});
