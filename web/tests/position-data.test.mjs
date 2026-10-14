import assert from 'node:assert/strict';
import { test } from 'node:test';

import { positionOf, WIDEST_CIRCLE_METRES } from '../public/lib/position-data.js';

const at = (millis) => ({ toMillis: () => millis });

/** A position document as a current build writes it, with [changes] over it. */
const documentOf = (changes = {}, id = 'anna.phone') => ({
    id,
    data: () => ({
        memberId: 'anna',
        latitude: 59.437,
        longitude: 24.745,
        takenAt: at(1000),
        writtenAt: at(3000),
        mode: 'sharing',
        batteryPercent: 81,
        charging: true,
        accuracyMetres: 12,
        foundBy: 'gps',
        leftOutCount: 3,
        leftOutReason: 'speed',
        settling: true,
        ...changes,
    }),
});

test('a position carries everything the device wrote', () => {
    assert.deepEqual(positionOf(documentOf()), {
        positionId: 'anna.phone',
        memberId: 'anna',
        latitude: 59.437,
        longitude: 24.745,
        takenAt: 1000,
        knownAt: 3000,
        positionAt: 1000,
        mode: 'sharing',
        batteryPercent: 81,
        charging: true,
        accuracyMetres: 12,
        foundBy: 'gps',
        leftOutCount: 3,
        leftOutReason: 'speed',
        settling: true,
    });
});

// What the older builds wrote: no owner field, no server stamp, and none of
// the fields added since.
test('a position from an older build reads with what it does not say left out', () => {
    const position = positionOf({
        id: 'anna',
        data: () => ({ latitude: 59.437, longitude: 24.745, takenAt: at(1000) }),
    });

    assert.equal(position.memberId, 'anna');
    assert.equal(position.knownAt, 1000);
    assert.equal(position.mode, null);
    assert.equal(position.batteryPercent, null);
    assert.equal(position.charging, false);
    assert.equal(position.accuracyMetres, null);
    assert.equal(position.foundBy, null);
    assert.equal(position.leftOutCount, null);
    assert.equal(position.leftOutReason, null);
    assert.equal(position.settling, false);
});

test('the owner comes from the id when the document does not name one', () => {
    assert.equal(positionOf(documentOf({ memberId: 7 }, 'mark.tablet')).memberId, 'mark');
});

test('fields of the wrong type are read as not said', () => {
    const position = positionOf(
        documentOf({ batteryPercent: '81', charging: 'yes', foundBy: 7, leftOutCount: '3', leftOutReason: 7, settling: 1 }),
    );

    assert.equal(position.batteryPercent, null);
    assert.equal(position.charging, false);
    assert.equal(position.foundBy, null);
    assert.equal(position.leftOutCount, null);
    assert.equal(position.leftOutReason, null);
    assert.equal(position.settling, false);
});

test('a document without a place on the globe is no position', () => {
    const unplaceable = [
        { latitude: '59.437' },
        { latitude: Number.NaN },
        { latitude: Number.POSITIVE_INFINITY },
        { latitude: 90.5 },
        { latitude: -91 },
        { longitude: 180.5 },
        { longitude: -181 },
        { longitude: null },
        { takenAt: undefined },
    ];

    for (const changes of unplaceable) {
        assert.equal(positionOf(documentOf(changes)), null, JSON.stringify(changes));
    }
});

test('the edges of the globe are places on it', () => {
    assert.notEqual(positionOf(documentOf({ latitude: 90, longitude: -180 })), null);
    assert.notEqual(positionOf(documentOf({ latitude: -90, longitude: 180 })), null);
});

test('only a width worth drawing is kept', () => {
    const widthOf = (accuracyMetres) => positionOf(documentOf({ accuracyMetres })).accuracyMetres;

    assert.equal(widthOf(WIDEST_CIRCLE_METRES), WIDEST_CIRCLE_METRES);
    assert.equal(widthOf(WIDEST_CIRCLE_METRES + 1), null);
    assert.equal(widthOf(0), null);
    assert.equal(widthOf(-5), null);
    assert.equal(widthOf(Number.POSITIVE_INFINITY), null);
    assert.equal(widthOf('12'), null);
});
