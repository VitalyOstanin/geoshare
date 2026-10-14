import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';

import { browserOf, faultOf, keepPrivate, scrubbed, stackPlaceOf } from '../public/lib/fault.js';
import { NEW_ISSUE, detailsText, issueAddress } from '../public/lib/unexpected.js';
import { PAGE_VERSION } from '../public/lib/version.js';

const ROOT = new URL('../../', import.meta.url);
const ORIGIN = 'https://family-ab12.web.app';
const UID = 'Xq3v9LmZ0aB4cD5eF6gH7iJ8kL9m';

// What leaves the page in a report is read by whoever it is passed to and, in
// an issue, by anybody: none of this may survive the cleaning.
test('a report keeps no address, id, site, project or private name', () => {
    keepPrivate('Anna Petrova', null, ' ');
    const text = scrubbed(
        `FirebaseError: denied at groups/${UID}/grants/${UID} for anna@gmail.com ` +
            'in projects/family-ab12/databases on https://other-site.firebaseapp.com, ' +
            `told by Anna Petrova, from ${ORIGIN}/lib/store-group.js:212:7`,
        ORIGIN,
    );

    for (const secret of [UID, 'anna@gmail.com', 'family-ab12', 'other-site', 'Anna Petrova']) {
        assert.ok(!text.includes(secret), `${secret} is left in: ${text}`);
    }
    assert.match(text, /groups\/‹id›\/grants\/‹id›/);
    assert.match(text, /lib\/store-group\.js:212/);
});

test('the place is the first of the page’s own files in a stack', () => {
    const chrome =
        `TypeError: x\n    at draw (${ORIGIN}/lib/map-card.js:120:9)\n` +
        `    at ${ORIGIN}/app.js:40:1`;
    const firefox = `draw@${ORIGIN}/page/strip.js:33:5\n@${ORIGIN}/app.js:40:1`;

    assert.equal(stackPlaceOf(chrome), 'lib/map-card.js:120');
    assert.equal(stackPlaceOf(firefox), 'page/strip.js:33');
    assert.equal(stackPlaceOf(undefined), null);
});

test('a rejection with no error in it still says something', () => {
    const fault = faultOf({ kind: 'rejection', reason: undefined }, ORIGIN);

    assert.equal(fault.message, 'no reason given');
    assert.equal(fault.place, null);
});

test('the browser and system are named from the user agent', () => {
    assert.deepEqual(
        browserOf('Mozilla/5.0 (X11; Linux x86_64; rv:140.0) Gecko/20100101 Firefox/140.0'),
        ['Firefox 140', 'Linux'],
    );
    assert.deepEqual(
        browserOf('Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/141.0.0.0 Mobile Safari/537.36'),
        ['Chrome 141', 'Android'],
    );
    assert.deepEqual(browserOf(''), ['unknown browser', 'unknown system']);
});

test('the details name the kind, the message, the place and the page', () => {
    const words = { rejection: 'Unhandled rejection', exception: 'Exception', page: 'page' };
    const text = detailsText(
        { kind: 'rejection', message: 'FirebaseError: denied', place: 'lib/store.js:9' },
        words,
        { now: new Date(0), agent: 'Firefox/140.0 Linux', language: 'en' },
    );
    const lines = text.split('\n');

    assert.equal(lines[0], 'Unhandled rejection');
    assert.equal(lines[1], 'FirebaseError: denied');
    assert.equal(lines[2], 'lib/store.js:9');
    assert.match(lines[3], new RegExp(`page ${PAGE_VERSION.replace(/\./g, '\\.')}$`));
    assert.equal(lines[4], 'Firefox 140 · Linux · en');
});

test('the issue opens on the project with the warning first and the details in a block', () => {
    const address = new URL(issueAddress('Exception\nTypeError: x'));
    const body = address.searchParams.get('body');

    assert.equal(`${address.origin}${address.pathname}`, NEW_ISSUE);
    assert.match(body.split('\n')[0], /^> Issues on GitHub are public\./);
    assert.ok(body.includes('```\nException\nTypeError: x\n```'));
});

// The page names a version in its reports; it is the application's.
test('the page version is the application version', () => {
    const properties = readFileSync(new URL('android/gradle.properties', ROOT), 'utf8');
    const [, version] = /^appVersionName=(.+)$/m.exec(properties);

    assert.equal(PAGE_VERSION, version.trim());
});

test('every page loads the handler ahead of its own module', () => {
    for (const [page, own] of [
        ['index.html', 'app.js'],
        ['join.html', 'join-app.js'],
        ['setup.html', 'setup-app.js'],
    ]) {
        const html = readFileSync(new URL(`web/public/${page}`, ROOT), 'utf8');
        const handler = html.indexOf('src="lib/unexpected.js"');

        assert.ok(handler > 0, `${page} does not load the handler`);
        assert.ok(handler < html.indexOf(`src="${own}"`), `${page} loads it after ${own}`);
        assert.ok(html.includes('href="styles/unexpected.css"'), `${page} lacks its style`);
    }
});
