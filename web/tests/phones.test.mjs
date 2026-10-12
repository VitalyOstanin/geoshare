import assert from 'node:assert/strict';
import { test } from 'node:test';

import { combinedGrants, phonesOf, withPhones } from '../public/lib/phones.js';

const HER = 'uid-her';

/** A server timestamp as the library hands it over. */
const at = (millis) => ({ toMillis: () => millis });

const position = {
    positionId: `${HER}.phone`,
    memberId: HER,
    knownAt: 1000,
    positionAt: 1000,
    mode: 'Standby',
    batteryPercent: 40,
    charging: false,
};

test('a phone entry keeps what a phone writes and drops the rest', () => {
    const phones = phonesOf({
        phone: { model: 'MAR-LX1', onlineAt: at(5), batteryPercent: 250, charging: 'yes' },
        broken: 'not an entry',
    });

    assert.deepEqual([...phones.keys()], ['phone']);
    assert.deepEqual(phones.get('phone'), {
        model: 'MAR-LX1',
        name: null,
        onlineAt: 5,
        mode: null,
        batteryPercent: null,
        charging: false,
    });
});

// The case this exists for: the answers are being thrown away, the position
// is an hour old, and the device has since been put on a cable.
test('what the phone said after the position was written is laid over it', () => {
    const grants = combinedGrants(
        new Map([[HER, ['phone']]]),
        new Map([[HER, phonesOf({
            phone: { onlineAt: at(9000), mode: 'Viewing', batteryPercent: 100, charging: true },
        })]]),
    );

    const [shown] = withPhones([position], grants);

    assert.equal(shown.knownAt, 9000);
    assert.equal(shown.positionAt, 1000);
    assert.equal(shown.batteryPercent, 100);
    assert.equal(shown.charging, true);
    assert.equal(shown.mode, 'Viewing');
});

test('what the phone said before the position was written is not', () => {
    const grants = combinedGrants(
        new Map([[HER, ['phone']]]),
        new Map([[HER, phonesOf({ phone: { onlineAt: at(500), charging: true } })]]),
    );

    assert.deepEqual(withPhones([position], grants), [position]);
});

// An administrator reads the devices of everybody and the grant of nobody: the
// devices are shown to them, and no position is followed.
test('phones read without the grant lead to no positions', () => {
    const grants = combinedGrants(
        new Map(),
        new Map([[HER, phonesOf({ phone: { model: 'MAR-LX1' } })]]),
    );

    assert.deepEqual(grants.get(HER).devices, []);
    assert.equal(grants.get(HER).phones.size, 1);
});

// A device of an older build names itself in the grant and nowhere else, and a
// newer one names itself in both.
test('the phones to follow are those of the grant and of the phones together', () => {
    const grants = combinedGrants(
        new Map([[HER, ['old', 'new']]]),
        new Map([[HER, phonesOf({ new: {}, newer: {} })]]),
    );

    assert.deepEqual(grants.get(HER).devices, ['old', 'new', 'newer']);
});
