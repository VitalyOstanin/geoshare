import assert from 'node:assert/strict';
import { test } from 'node:test';

import { markerOrder } from '../public/lib/stacking.js';

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
