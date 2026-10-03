import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    LABEL_LIMIT,
    NO_LABELS,
    keepLabels,
    labelText,
    labelledPositions,
    labelledProfiles,
    phoneNames,
    labelsFrom,
    readKeptLabels,
} from '../public/lib/labels.js';
import { combinedGrants, phonesOf } from '../public/lib/phones.js';

const HER = 'uid-her';

const position = {
    positionId: `${HER}.phone`,
    memberId: HER,
};

const grantsNaming = (name, model = null) => combinedGrants(
    new Map(),
    new Map([[HER, phonesOf({ phone: { ...name && { name }, ...model && { model } } })]]),
);

/** A store that keeps what it is given, the way localStorage does. */
function memoryStore() {
    const kept = new Map();

    return {
        getItem: (key) => kept.get(key) ?? null,
        setItem: (key, value) => kept.set(key, String(value)),
    };
}

test("the viewer's own name for a phone comes first", () => {
    const labels = labelsFrom({ devices: { [`${HER}.phone`]: 'new phone' } });

    const [shown] = labelledPositions([position], labels, grantsNaming("mum's phone"));

    assert.equal(shown.deviceName, 'new phone');
});

test("the owner's name for a phone comes next", () => {
    const [shown] = labelledPositions([position], NO_LABELS, grantsNaming("mum's phone"));

    assert.equal(shown.deviceName, "mum's phone");
});

test("the phone's own model is shown where nobody named it", () => {
    const [shown] = labelledPositions([position], NO_LABELS, grantsNaming(null, 'MAR-LX1'));

    assert.equal(shown.deviceName, 'MAR-LX1');
});

test('a phone that has said nothing about itself is left unnamed', () => {
    const [shown] = labelledPositions([position], NO_LABELS, grantsNaming(null));

    assert.equal(shown, position);
});

test("the viewer's name for a person replaces the one on the account", () => {
    const profiles = new Map([
        [HER, { displayName: 'Maria' }],
        ['uid-him', { displayName: 'Ivan' }],
    ]);

    const shown = labelledProfiles(profiles, labelsFrom({ members: { [HER]: 'grandma' } }));

    assert.equal(shown.get(HER).displayName, 'grandma');
    assert.equal(shown.get('uid-him').displayName, 'Ivan');
});

test('a document keeps what is a name and drops the rest', () => {
    const labels = labelsFrom({
        devices: { [`${HER}.phone`]: '  new phone  ', 'his.phone': '', odd: 5 },
        members: 'not a map',
    });

    assert.deepEqual([...labels.phones], [[`${HER}.phone`, 'new phone']]);
    assert.equal(labels.members.size, 0);
});

test('an empty name resets and a long one is cut', () => {
    assert.equal(labelText('   '), null);
    assert.equal(labelText('x'.repeat(LABEL_LIMIT + 10)).length, LABEL_LIMIT);
});

test('the names kept in the browser come back as they were', () => {
    const store = memoryStore();
    const labels = labelsFrom({ devices: { [`${HER}.phone`]: 'new phone' }, members: { [HER]: 'grandma' } });

    keepLabels(HER, labels, store);

    assert.deepEqual(readKeptLabels(HER, store), labels);
    assert.equal(readKeptLabels('somebody-else', store), NO_LABELS);
});

test('a browser that refuses storage starts without names', () => {
    const refusing = { getItem: () => { throw new Error('refused'); } };

    assert.equal(readKeptLabels(HER, refusing), NO_LABELS);
});

// Two phones of one model that nobody named read the same everywhere; a number
// after the name tells them apart, in the order of their ids so that every
// screen numbers them alike.
test('phones sharing a name are numbered in the order of their ids', () => {
    const phones = phonesOf({
        zulu: { model: 'BVL-N49' },
        alpha: { model: 'BVL-N49' },
        other: { model: 'MAR-LX1' },
    });

    const names = phoneNames(HER, phones, NO_LABELS);

    assert.equal(names.get('alpha'), 'BVL-N49 (1)');
    assert.equal(names.get('zulu'), 'BVL-N49 (2)');
    assert.equal(names.get('other'), 'MAR-LX1');
});

test('twins skip a number another device is already called by', () => {
    const phones = phonesOf({
        alpha: { model: 'BVL-N49' },
        zulu: { model: 'BVL-N49' },
        third: { model: 'MAR-LX1' },
    });
    const labels = labelsFrom({ devices: { [`${HER}.third`]: 'BVL-N49 (1)' } });

    const names = phoneNames(HER, phones, labels);

    assert.equal(names.get('third'), 'BVL-N49 (1)');
    assert.equal(names.get('alpha'), 'BVL-N49 (2)');
    assert.equal(names.get('zulu'), 'BVL-N49 (3)');
});

test('a phone named apart from its twin leaves the other unnumbered', () => {
    const phones = phonesOf({ alpha: { model: 'BVL-N49' }, zulu: { model: 'BVL-N49' } });
    const labels = labelsFrom({ devices: { [`${HER}.zulu`]: 'old phone' } });

    const names = phoneNames(HER, phones, labels);

    assert.equal(names.get('alpha'), 'BVL-N49');
    assert.equal(names.get('zulu'), 'old phone');
});

test('the map numbers twins the way the panel does', () => {
    const grants = combinedGrants(
        new Map(),
        new Map([[HER, phonesOf({ alpha: { model: 'BVL-N49' }, zulu: { model: 'BVL-N49' } })]]),
    );
    const twins = ['zulu', 'alpha'].map((deviceId) => ({
        positionId: `${HER}.${deviceId}`,
        memberId: HER,
    }));

    const shown = labelledPositions(twins, NO_LABELS, grants);

    assert.deepEqual(shown.map((one) => one.deviceName), ['BVL-N49 (2)', 'BVL-N49 (1)']);
});

test('phones of two members are numbered apart', () => {
    const grants = combinedGrants(new Map(), new Map([
        [HER, phonesOf({ alpha: { model: 'BVL-N49' } })],
        ['uid-him', phonesOf({ beta: { model: 'BVL-N49' } })],
    ]));
    const shown = labelledPositions([
        { positionId: `${HER}.alpha`, memberId: HER },
        { positionId: 'uid-him.beta', memberId: 'uid-him' },
    ], NO_LABELS, grants);

    assert.deepEqual(shown.map((one) => one.deviceName), ['BVL-N49', 'BVL-N49']);
});
