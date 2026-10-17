import assert from 'node:assert/strict';
import { test } from 'node:test';

import { haversineMetres, metresOrKilometres } from '../public/lib/units.js';

test('a degree of latitude is a hundred and eleven kilometres', () => {
    const metres = haversineMetres(60, 30, 61, 30);

    assert.ok(Math.abs(metres - 111_195) < 10, `${metres}`);
});

// The one place the two callers differ: what a figure that rounds to nought
// metres may say.
test('the least a figure in metres may say is the caller\'s to choose', () => {
    assert.equal(metresOrKilometres(0.2, 0), '0 m');
    assert.equal(metresOrKilometres(0.2, 1), '1 m');
});

test('a length past a kilometre is written in kilometres to one place', () => {
    assert.equal(metresOrKilometres(999.4, 0), '999 m');
    assert.equal(metresOrKilometres(1049, 0), '1.0 km');
    assert.equal(metresOrKilometres(2345, 1), '2.3 km');
});
