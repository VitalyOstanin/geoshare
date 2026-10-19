import assert from 'node:assert/strict';
import { test } from 'node:test';

import { keepConsole, noteInLog, pageLog, PAGE_LOG_LINES } from '../public/lib/page-log.js';
import { LEFT_OUT, buildIssue, issueTitle } from '../public/lib/report.js';
import { NEW_ISSUE } from '../public/lib/unexpected.js';
import { PAGE_VERSION } from '../public/lib/version.js';

const UID = 'Xq3v9LmZ0aB4cD5eF6gH7iJ8kL9m';
const ORIGIN = 'https://family-ab12.web.app';

test('an issue title is the first written line, or a stock one', () => {
    assert.equal(issueTitle('\n  The map is blank  \nsince noon'), 'The map is blank');
    assert.equal(issueTitle('   '), 'Problem report');
    assert.equal(issueTitle('x'.repeat(300)).length, 100);
});

test('a short log goes whole, under what was written and the version', () => {
    const draft = buildIssue('Blank map', 'Firefox 140 · Linux', ['12:00:01 warn one', '12:00:02 error two']);
    const address = new URL(draft.url);
    const body = address.searchParams.get('body');

    assert.equal(`${address.origin}${address.pathname}`, NEW_ISSUE);
    assert.equal(address.searchParams.get('title'), 'Blank map');
    assert.ok(body.startsWith('Blank map\n'));
    assert.ok(body.includes(`Page version: ${PAGE_VERSION}`));
    assert.ok(body.includes('Firefox 140 · Linux'));
    assert.equal(draft.tail, '12:00:01 warn one\n12:00:02 error two');
    assert.equal(draft.leftOut, false);
    assert.ok(!draft.url.includes('+'), 'spaces are written as %20');
});

// The address is what has a limit, and the oldest lines are what give way.
test('a long log is cut from its start to fit the address, and says so', () => {
    const log = Array.from({ length: 400 }, (_, at) => `12:00:00 warn line number ${at} — сеть`);
    const draft = buildIssue('Blank map', 'Chrome 141 · Android', log, 6000);

    assert.ok(draft.url.length <= 6000, `${draft.url.length}`);
    assert.equal(draft.leftOut, true);
    assert.ok(draft.tail.startsWith(LEFT_OUT));
    assert.ok(draft.tail.endsWith('line number 399 — сеть'));

    // One line more would not have fitted: nothing was dropped needlessly.
    const kept = draft.tail.split('\n').length - 1;
    const first = Number(/number (\d+)/.exec(draft.tail.split('\n')[1])[1]);
    assert.equal(first, 400 - kept);
    const next = encodeURIComponent(`${log[first - 1]}\n`).length;
    assert.ok(draft.url.length + next > 6000, `${draft.url.length} + ${next}`);
});

test('a line too long for any address is left out rather than cut', () => {
    const draft = buildIssue('', 'Firefox 140 · Linux', ['x'.repeat(7000), 'last'], 6000);

    assert.equal(draft.tail, `${LEFT_OUT}\nlast`);
});

// What the console was told goes on to the console, and a copy of it is
// kept, cleaned, for a report.
test('the page log keeps warnings and errors, cleaned and bounded', () => {
    const written = [];
    const write = (...values) => written.push(values);
    const target = { warn: write, error: write };

    keepConsole(target);
    target.warn('denied at', `groups/${UID}`, 'for anna@gmail.com');
    target.error(new TypeError(`no ${ORIGIN}/lib/map.js`));

    assert.equal(written.length, 2);
    const log = pageLog(ORIGIN);
    assert.match(log.at(-2), /^\d\d:\d\d:\d\d warn denied at groups\/‹id› for ‹email›$/);
    assert.match(log.at(-1), /error TypeError: no lib\/map\.js$/);

    for (let index = 0; index < PAGE_LOG_LINES + 10; index += 1) {
        noteInLog('warn', [`n${index}`]);
    }
    assert.equal(pageLog().length, PAGE_LOG_LINES);
    assert.match(pageLog().at(-1), /warn n209$/);
});

// The cut falling inside an emoji used to leave its first half, which the
// address carries as a replacement character.
test('the title and the description are not cut between the halves of an emoji', () => {
    const emoji = '😀';

    assert.equal(issueTitle('a'.repeat(99) + emoji), 'a'.repeat(99));

    const draft = buildIssue('a'.repeat(599) + emoji, 'Firefox 140 · Linux', ['first']);

    assert.ok(!draft.url.includes('%EF%BF%BD'));
    assert.ok(new URL(draft.url).searchParams.get('body').startsWith(`${'a'.repeat(599)}\n\n`));
});
