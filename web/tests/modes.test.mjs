import assert from 'node:assert/strict';
import { test } from 'node:test';

import { modeText } from '../public/lib/modes.js';
import { WORDS } from '../public/lib/words.js';

// The English name of a mode is the entry the phone writes, so comparing the
// two would pass whether the dictionary holds the mode or not: what is checked
// is that the name came from the dictionary, which is where a translation of
// it would be.
test('every mode a phone writes is named by the dictionary', () => {
    const modes = ['Idle', 'Standby', 'Moving', 'Viewing', 'Night'];

    for (const mode of modes) {
        const named = WORDS[`mode${mode}`];

        assert.ok(named, `${mode} has no name in the dictionary`);
        assert.equal(modeText(mode), named);
    }
});

test('a position from before modes were written says so', () => {
    assert.equal(modeText(null), 'unknown');
    assert.equal(modeText(undefined), 'unknown');
});

test('a mode this build has never heard of is shown as it came', () => {
    assert.equal(modeText('Hiking'), 'Hiking');
});
