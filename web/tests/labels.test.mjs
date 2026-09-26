import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    LABEL_LIMIT,
    NO_LABELS,
    keepLabels,
    labelText,
    labelledPositions,
    labelledProfiles,
    labelsFrom,
    readKeptLabels,
} from '../public/lib/labels.js';
import { combinedGrants, phonesOf } from '../public/lib/phones.js';

const HER = 'uid-her';

const position = {
    positionId: `${HER}.phone`,
    memberId: HER,
    deviceName: 'MAR-LX1',
};

const grantsNaming = (name) => combinedGrants(
    new Map(),
    new Map([[HER, phonesOf({ phone: name ? { name } : {} })]]),
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

test('the model is left where nobody named the phone', () => {
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
