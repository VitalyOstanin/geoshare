import assert from 'node:assert/strict';
import { test } from 'node:test';

import { readStyles } from './stylesheets.mjs';

// The pin colour is set per marker from the script, so the colour written after
// the comma is only what a marker shows when that setting did not happen. The
// head, its point and the count of the faces that did not fit are one shape and
// have to fall back to one colour, or a marker without the setting is two.
test('every use of --pin falls back to the same colour', async () => {
    const styles = await readStyles();
    const fallbacks = [...styles.matchAll(/var\(--pin\s*(?:,\s*([^)]*))?\)/g)].map((found) =>
        found[1]?.trim(),
    );

    assert.ok(fallbacks.length >= 3, 'the pin parts no longer read --pin');
    assert.equal(new Set(fallbacks).size, 1, `fallbacks differ: ${fallbacks.join(', ')}`);
    assert.ok(fallbacks[0], '--pin is read without a fallback');
});
