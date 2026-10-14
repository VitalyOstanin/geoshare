import assert from 'node:assert/strict';
import { test } from 'node:test';

import { modeHelpRows } from '../public/lib/mode-help.js';
import { WORDS } from '../public/lib/words.js';

test('the help describes every mode a device writes', () => {
    const described = modeHelpRows().map((row) => row.mode).sort();

    assert.deepEqual(described, ['Idle', 'Moving', 'Night', 'Standby', 'Viewing']);
});

test('every row carries the name a card shows and both halves of the meaning', () => {
    for (const row of modeHelpRows()) {
        assert.equal(row.name, WORDS[`mode${row.mode}`], `${row.mode} is named differently`);
        assert.ok(row.when, `${row.mode} does not say when it happens`);
        assert.ok(row.asks, `${row.mode} does not say what the phone does`);
    }
});

test('the hardest working mode is read first and the quietest last', () => {
    const order = modeHelpRows().map((row) => row.mode);

    assert.equal(order[0], 'Viewing');
    assert.equal(order.at(-1), 'Idle');
});
