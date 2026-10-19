import assert from 'node:assert/strict';
import { test } from 'node:test';

import { combinedGrants, deviceIdOf, memberIdOf, phonesOf, withPhones } from '../public/lib/phones.js';

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

test('a device entry keeps what a device writes and drops the rest', () => {
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

// Ten is the most the page follows of one member; an entry that is not a
// device must not take the place of one that is.
test('entries that are not devices do not count towards the ten', () => {
    const data = { broken: null, junk: 'text' };
    for (let n = 0; n < 10; n += 1) {
        data[`device${n}`] = { model: 'BVL-N49' };
    }

    const phones = phonesOf(data);

    assert.equal(phones.size, 10);
    assert.equal(phones.has('device9'), true);
});

// The case this exists for: the answers are being thrown away, the position
// is an hour old, and the device has since been put on a cable.
test('what the device said after the position was written is laid over it', () => {
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

test('what the device said before the position was written is not', () => {
    const grants = combinedGrants(
        new Map([[HER, ['phone']]]),
        new Map([[HER, phonesOf({ phone: { onlineAt: at(500), charging: true } })]]),
    );

    assert.deepEqual(withPhones([position], grants), [position]);
});

// An administrator reads the devices of everybody and the grant of nobody: the
// devices are shown to them, and no position is followed.
test('devices read without the grant lead to no positions', () => {
    const grants = combinedGrants(
        new Map(),
        new Map([[HER, phonesOf({ phone: { model: 'MAR-LX1' } })]]),
    );

    assert.deepEqual(grants.get(HER).devices, []);
    assert.equal(grants.get(HER).phones.size, 1);
});

// A device of an older build names itself in the grant and nowhere else, and a
// newer one names itself in both.
test('the devices to follow are those of the grant and of the devices together', () => {
    const grants = combinedGrants(
        new Map([[HER, ['old', 'new']]]),
        new Map([[HER, phonesOf({ new: {}, newer: {} })]]),
    );

    assert.deepEqual(grants.get(HER).devices, ['old', 'new', 'newer']);
});

// The account's id has no dot in it, and everything after the first one is
// the device's, whatever it holds.
test('a device id is split at the first dot only', () => {
    assert.equal(memberIdOf('u1.dev.2'), 'u1');
    assert.equal(deviceIdOf('u1.dev.2'), 'dev.2');
});

test('an id from before positions were kept per device is the member\'s own', () => {
    assert.equal(memberIdOf('u1'), 'u1');
});

// An emoji across the limit goes whole rather than leaving its first half.
test('a device name is not cut between the halves of an emoji', () => {
    const phones = phonesOf({ phone: { name: `${'a'.repeat(39)}😀` } });

    assert.equal(phones.get('phone').name, 'a'.repeat(39));
});
