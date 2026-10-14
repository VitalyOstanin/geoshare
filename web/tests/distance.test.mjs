import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    distanceFromViewer,
    distanceText,
    metresBetween,
    ownPlace,
} from '../public/lib/distance.js';

const MINUTE = 60 * 1000;
const NOW = Date.parse('2026-09-27T12:00:00Z');

/** A position as the store hands it over, minutes old and this wide. */
const at = (positionId, memberId, latitude, longitude, { minutes = 1, width = 20 } = {}) => ({
    positionId,
    memberId,
    latitude,
    longitude,
    positionAt: NOW - minutes * MINUTE,
    knownAt: NOW - minutes * MINUTE,
    accuracyMetres: width,
});

test('a degree of latitude is a hundred and eleven kilometres', () => {
    const metres = metresBetween({ latitude: 60, longitude: 30 }, { latitude: 61, longitude: 30 });

    assert.ok(Math.abs(metres - 111195) < 10, `${metres}`);
});

// Along a parallel a degree narrows with the cosine: half as wide at sixty.
test('a degree of longitude at sixty north is half that', () => {
    const metres = metresBetween({ latitude: 60, longitude: 30 }, { latitude: 60, longitude: 31 });

    assert.ok(Math.abs(metres - 55597) < 50, `${metres}`);
});

test('the viewer is where the freshest of their own devices is', () => {
    const positions = [
        at('me.old', 'me', 60, 30, { minutes: 10 }),
        at('me.new', 'me', 60.1, 30, { minutes: 2 }),
        at('anna.one', 'anna', 60.2, 30, { minutes: 1 }),
    ];

    assert.equal(ownPlace(positions, 'me', NOW).positionId, 'me.new');
});

// Half an hour is long enough to have walked off, and a distance from where
// the viewer was is read as a distance from where they are.
test('a place of the viewer gone grey is not measured from', () => {
    const positions = [
        at('me.one', 'me', 60, 30, { minutes: 31 }),
        at('anna.one', 'anna', 60.2, 30),
    ];

    assert.equal(distanceFromViewer(positions[1], positions, 'me', NOW), null);
});

// A device in touch every minute, the marker in the lake since 20:22.
test('a marker the viewer\'s device has left behind is not measured from', () => {
    const stuck = {
        ...at('me.one', 'me', 61.14561, 5.4036),
        positionAt: NOW - 90 * MINUTE,
        knownAt: NOW - MINUTE,
    };
    const anna = at('anna.one', 'anna', 61.03249, 4.4);

    assert.equal(distanceFromViewer(anna, [stuck, anna], 'me', NOW), null);
});

test('the card of the very place measured from says nothing', () => {
    const own = at('me.one', 'me', 60, 30);

    assert.equal(distanceFromViewer(own, [own], 'me', NOW), null);
});

test('another device of the viewer\'s own is measured like anybody else', () => {
    const phone = at('me.phone', 'me', 60, 30, { minutes: 1 });
    const other = at('me.other', 'me', 60.01, 30, { minutes: 5 });

    const away = distanceFromViewer(other, [phone, other], 'me', NOW);

    assert.ok(Math.abs(away - 1112) < 5, `${away}`);
});

test('a viewer with no place of their own is told nothing', () => {
    const anna = at('anna.one', 'anna', 60, 30);

    assert.equal(distanceFromViewer(anna, [anna], 'me', NOW), null);
});

// Forty metres between two answers sixty metres wide is still forty metres:
// the owner asked for the figure rather than a word.
test('two answers inside each other\'s width still get a figure', () => {
    const own = at('me.one', 'me', 60, 30, { width: 60 });
    const anna = at('anna.one', 'anna', 60.00036, 30, { width: 15 });

    assert.ok(Math.abs(distanceFromViewer(anna, [own, anna], 'me', NOW) - 40) < 1);
});

test('a distance is said in whole metres, nought included', () => {
    assert.equal(distanceText(0.06), '0 m');
    assert.equal(distanceText(39.6), '40 m');
    assert.equal(distanceText(999.4), '999 m');
});

test('a distance past a kilometre is said in kilometres to one place', () => {
    assert.equal(distanceText(999.6), '1.0 km');
    assert.equal(distanceText(2345), '2.3 km');
});

test('a place of the viewer inside a false place is not measured from', () => {
    const own = { ...at('me.one', 'me', 61.14561, 5.4036), inFalsePlace: true };
    const anna = at('anna.one', 'anna', 61.03249, 4.4);

    assert.equal(distanceFromViewer(anna, [own, anna], 'me', NOW), null);
});

test('a member inside a false place is not measured to', () => {
    const own = at('me.one', 'me', 61.03249, 4.4);
    const anna = { ...at('anna.one', 'anna', 61.14561, 5.4036), inFalsePlace: true };

    assert.equal(distanceFromViewer(anna, [own, anna], 'me', NOW), null);
});
