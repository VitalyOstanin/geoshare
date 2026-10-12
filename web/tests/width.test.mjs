import assert from 'node:assert/strict';
import { test } from 'node:test';

import { accuracyRing, widthIsWide, widthText } from '../public/lib/width.js';

const EARTH = 6371008.8;

const northwardsMetres = (degrees) => (degrees / 180) * Math.PI * EARTH;

const eastwardsMetres = (degrees, latitude) =>
    northwardsMetres(degrees) * Math.cos((latitude / 180) * Math.PI);

// The four kinds of answer seen in the logs of the devices: the satellites
// under open sky, the Wi-Fi in a flat, the assistance service, and a tower.
test('an answer under a kilometre is said in whole metres', () => {
    assert.equal(widthText(11.7), '12 m');
    assert.equal(widthText(28.1), '28 m');
    assert.equal(widthText(122.4), '122 m');
});

test('an answer from a tower is said in kilometres to one place', () => {
    assert.equal(widthText(1700), '1.7 km');
});

// Nine hundred and ninety-nine and a half metres rounds to a thousand, and
// "1000 m" is the one thing the rule exists to avoid writing.
test('an answer that rounds up to a kilometre is written as one', () => {
    assert.equal(widthText(999.6), '1.0 km');
    assert.equal(widthText(999.4), '999 m');
});

// A receiver that calls its answer a fraction of a metre wide is not claiming
// the device is at a point, and "0 m" would say it is.
test('an answer narrower than a metre is still a metre', () => {
    assert.equal(widthText(0.2), '1 m');
});

test('a hundred metres is where the colour changes', () => {
    assert.equal(widthIsWide(99.9), false);
    assert.equal(widthIsWide(100), true);
    assert.equal(widthIsWide(1700), true);
});

test('the ring is closed', () => {
    const ring = accuracyRing(61.03268, 4.403, 100);

    assert.deepEqual(ring[0], ring[ring.length - 1]);
});

// A hundred metres north of sixty degrees is the same hundred metres east, and
// the point of dividing by the cosine is that both come out that way.
test('a hundred metres is a hundred metres in both directions', () => {
    const ring = accuracyRing(60, 30, 100, 4);

    assert.ok(Math.abs(northwardsMetres(ring[0][1] - 60) - 100) < 0.5);
    assert.ok(Math.abs(eastwardsMetres(ring[1][0] - 30, 60) - 100) < 0.5);
});

// A ring round a position on the date line runs on past it rather than
// jumping to the other end of the map, which would draw it round the globe.
test('a ring on the date line stays in one piece', () => {
    const ring = accuracyRing(-16.8, 179.999, 1000);
    const longitudes = ring.map(([east]) => east);

    assert.ok(Math.max(...longitudes) - Math.min(...longitudes) < 1);
});

test('a ring at the pole stays on the globe', () => {
    const ring = accuracyRing(89.999, 30, 10000);

    assert.ok(ring.every(([east, north]) => Math.abs(north) <= 90 && Math.abs(east) <= 180));
});
