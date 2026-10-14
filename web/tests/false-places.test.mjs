import assert from 'node:assert/strict';
import { test } from 'node:test';

import { inFalsePlace, isGrey, placeOf, placesOf, withFalsePlaces } from '../public/lib/false-places.js';

const MINUTE = 60 * 1000;
const NOW = Date.parse('2026-09-27T12:00:00Z');

// A place two devices were put at, drawn six kilometres wide.
const LAKE = { latitude: 61.11758, longitude: 5.38558, radiusMetres: 6000 };

const at = (positionId, latitude, longitude, minutes = 1) => ({
    positionId,
    memberId: positionId.split('.')[0],
    latitude,
    longitude,
    positionAt: NOW - minutes * MINUTE,
    knownAt: NOW - minutes * MINUTE,
});

test('a document is read as a place only with numbers the devices accept', () => {
    assert.deepEqual(placeOf({ ...LAKE, named: 'lake' }), LAKE);
    assert.equal(placeOf({ ...LAKE, radiusMetres: 0 }), null);
    assert.equal(placeOf({ ...LAKE, radiusMetres: 50001 }), null);
    assert.equal(placeOf({ ...LAKE, latitude: 91 }), null);
    assert.equal(placeOf({ ...LAKE, longitude: '31' }), null);
    assert.equal(placeOf(null), null);
});

test('a document typed wrong is left out and the rest are kept', () => {
    assert.deepEqual(placesOf([{ latitude: 'x' }, LAKE]), [LAKE]);
});

// A device: written at 20:22, 3.3 km from the centre, inside once the
// place was widened to six.
test('the marker in the lake lies inside the widened place', () => {
    assert.equal(inFalsePlace(at('three.a', 61.14561, 5.4036), [LAKE]), true);
    assert.equal(inFalsePlace(at('three.a', 61.14561, 5.4036), [{ ...LAKE, radiusMetres: 3000 }]), false);
});

test('a position at home is left as it is', () => {
    const home = at('one.a', 61.03249, 4.4);

    assert.equal(withFalsePlaces([home], [LAKE])[0], home);
});

test('a position inside a place is marked, and marked grey', () => {
    const [marked] = withFalsePlaces([at('three.a', 61.14561, 5.4036)], [LAKE]);

    assert.equal(marked.inFalsePlace, true);
    assert.equal(isGrey(marked, NOW), true);
});

test('a fresh position outside every place is in colour, an old one grey', () => {
    assert.equal(isGrey(at('one.a', 61.03249, 4.4), NOW), false);
    assert.equal(isGrey(at('one.a', 61.03249, 4.4, 31), NOW), true);
});
