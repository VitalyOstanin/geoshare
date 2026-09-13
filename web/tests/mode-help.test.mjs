import assert from 'node:assert/strict';
import { test } from 'node:test';

import { modeHelpRows, outsideTheBox } from '../public/lib/mode-help.js';
import { WORDS } from '../public/lib/words.js';

test('the help describes every mode a phone writes', () => {
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

// A click on the dark behind the help closes it. That dark is the dialog's own
// backdrop rather than an element beside it, so the click arrives at the
// dialog -- and so does a click on the dialog's padding, which must not close
// anything. The coordinates are all that tell the two apart.
const HELP = { left: 100, right: 500, top: 80, bottom: 400 };

test('a click on the dark behind the help is outside it', () => {
    assert.ok(outsideTheBox(HELP, { clientX: 40, clientY: 240 }));
    assert.ok(outsideTheBox(HELP, { clientX: 600, clientY: 240 }));
    assert.ok(outsideTheBox(HELP, { clientX: 300, clientY: 20 }));
    assert.ok(outsideTheBox(HELP, { clientX: 300, clientY: 500 }));
});

test('a click on the padding of the help is not outside it', () => {
    assert.ok(!outsideTheBox(HELP, { clientX: 300, clientY: 240 }));
    assert.ok(!outsideTheBox(HELP, { clientX: 105, clientY: 85 }));
});

// The edge belongs to the window: a click on the border of something is a
// click on that thing, and closing on it would make the corner of the help a
// trap.
test('a click on the very edge of the help stays inside it', () => {
    assert.ok(!outsideTheBox(HELP, { clientX: 100, clientY: 80 }));
    assert.ok(!outsideTheBox(HELP, { clientX: 500, clientY: 400 }));
});
