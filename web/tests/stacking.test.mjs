import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';

import { HIGHEST, markerOrder } from '../public/lib/stacking.js';

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

// The card a marker opens is drawn by the map library, in the same box as the
// markers: it stands above them because the stylesheet says so, and the number
// it says has to be above anything a marker can be given.
test('a bubble outranks the highest a marker can reach', async () => {
    const styles = await readFile(new URL('../public/styles.css', import.meta.url), 'utf8');
    const rule = styles.match(/\.maplibregl-popup \{[^}]*z-index:\s*(\d+)/);

    assert.ok(rule, 'the stylesheet gives the bubble no place of its own');
    assert.ok(Number(rule[1]) > HIGHEST, `${rule[1]} is not above ${HIGHEST}`);
});
