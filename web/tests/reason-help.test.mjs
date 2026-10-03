import assert from 'node:assert/strict';
import { test } from 'node:test';

import { leftOutText } from '../public/lib/found.js';
import { reasonHelpRows } from '../public/lib/reason-help.js';
import { WORDS } from '../public/lib/words.js';

// The entries a phone writes into the document. Kept here in full rather than
// read off the help itself, so that a reason dropped from the help fails the
// test instead of quietly agreeing with it.
const WRITTEN = [
    'Circling',
    'Contradicted',
    'Contradiction',
    'Drifted',
    'FalsePlace',
    'Hurried',
    'OffOrbit',
    'OneBand',
    'OneTransmitter',
    'Repeating',
    'Settling',
    'Source',
    'Speed',
    'TimeApart',
    'Unmoved',
    'Width',
];

test('the help explains every reason a phone writes', () => {
    const described = reasonHelpRows().map((row) => row.reason).sort();

    assert.deepEqual(described, WRITTEN);
});

test('every row carries the name a card shows and both halves of the meaning', () => {
    for (const row of reasonHelpRows()) {
        assert.equal(row.name, leftOutText(row.reason), `${row.reason} is named differently`);
        assert.ok(row.sign, `${row.reason} does not say what was seen`);
        assert.ok(row.means, `${row.reason} does not say what it means`);
    }
});

// The order is the axis the help is read along: what the rule had to look at.
// The width is read off one answer; the switched-off source is not a fault.
test('the reason read off one answer comes first and the member choice last', () => {
    const order = reasonHelpRows().map((row) => row.reason);

    assert.equal(order[0], 'Width');
    assert.equal(order.at(-1), 'Source');
});

test('no two reasons are explained with the same words', () => {
    const rows = reasonHelpRows();
    const signs = new Set(rows.map((row) => row.sign));
    const means = new Set(rows.map((row) => row.means));

    assert.equal(signs.size, rows.length);
    assert.equal(means.size, rows.length);
});

test('the help has a title and an opening of its own', () => {
    assert.ok(WORDS.reasonHelpTitle);
    assert.ok(WORDS.reasonHelpIntro);
    assert.notEqual(WORDS.reasonHelpTitle, WORDS.reasonHelpIntro);
});
