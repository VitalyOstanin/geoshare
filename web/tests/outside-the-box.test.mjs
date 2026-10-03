import assert from 'node:assert/strict';
import { test } from 'node:test';

import { outsideTheBox } from '../public/lib/outside-the-box.js';

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
