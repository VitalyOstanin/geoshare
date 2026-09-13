import assert from 'node:assert/strict';
import { test } from 'node:test';

import { modeText } from '../public/lib/modes.js';

test('every mode a phone writes has words of its own', () => {
    const modes = ['Idle', 'Standby', 'Moving', 'Viewing', 'Night'];

    for (const mode of modes) {
        assert.notEqual(modeText(mode), mode, `${mode} is still shown as the machine name`);
    }
});

test('a position from before modes were written says so', () => {
    assert.equal(modeText(null), 'unknown');
    assert.equal(modeText(undefined), 'unknown');
});

test('a mode this build has never heard of is shown as it came', () => {
    assert.equal(modeText('Hiking'), 'Hiking');
});
