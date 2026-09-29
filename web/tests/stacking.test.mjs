import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';

import { markerOrder } from '../public/lib/stacking.js';
import { readStyles } from './stylesheets.mjs';

test('the marker further down the screen is drawn over the one above it', () => {
    assert.ok(markerOrder(400) > markerOrder(120));
});

test('a marker above the top of the screen never slides under the map', () => {
    assert.equal(markerOrder(-80), 0);
});

test('an order is a whole number, because that is what the page takes', () => {
    assert.equal(markerOrder(120.6), 121);
    assert.equal(markerOrder(120.4), 120);
});

test('a projection gone wrong cannot outrank the page for good', () => {
    assert.equal(markerOrder(1e9), markerOrder(2e9));
    assert.equal(markerOrder(Number.NaN), 0);
    assert.equal(markerOrder(Number.POSITIVE_INFINITY), markerOrder(1e9));
});

// A marker can be given any order up to stacking.js's own ceiling, and the
// card is not in that pile at all any more: it is drawn in the panel below
// the map rather than inside #map itself, so no marker's z-index — however
// far that ceiling is ever raised — has a stacking context in common with the
// card to outrank it in. #map's own isolation is what makes that true:
// without it, a marker's number would be compared against the rest of the
// page directly, card included.
test('a marker can never outrank the card, because #map isolates its own stack', async () => {
    const styles = await readStyles();
    const html = await readFile(new URL('../public/index.html', import.meta.url), 'utf8');

    assert.match(
        styles,
        /#map \{[^}]*isolation:\s*isolate/,
        'the map no longer walls its markers off into their own stacking context',
    );

    const map = html.match(/<div id="map"[^>]*>[\s\S]*?<\/div>/);

    assert.ok(map, 'the map element is missing from the markup');
    assert.doesNotMatch(
        map[0],
        /id="card"/,
        'the card is drawn inside the map rather than beside it',
    );

    const panel = html.match(/<section id="panel"[^>]*>[\s\S]*<\/section>/);

    assert.ok(panel, 'the panel is missing from the markup');
    assert.match(panel[0], /id="card"/, 'the card is not where the panel expects it');
});
