import assert from 'node:assert/strict';
import { test } from 'node:test';

import { devicesOf, idsOf, profileOf, rosterOf } from '../public/lib/group-data.js';

/** A document the way Firestore hands it over: an id, and its fields on asking. */
const documentOf = (id, data) => ({ id, data: () => data });

const at = (millis) => ({ toMillis: () => millis });

test('the roster names its members, administrators, founder and name', () => {
    const roster = rosterOf(
        documentOf('anna', { members: ['anna', 'mark'], admins: ['anna'], owner: 'mark', name: 'Family' }),
    );

    assert.deepEqual(roster, { members: ['anna', 'mark'], admins: ['anna'], founder: 'mark', name: 'Family' });
});

// A group from before the owner was written down is named after the account
// that made it, and one nobody has named has no name rather than a made-up one.
test('without an owner the founder is the account the group is named after', () => {
    const roster = rosterOf(documentOf('anna', { members: ['anna'], admins: ['anna'], owner: 7, name: 7 }));

    assert.equal(roster.founder, 'anna');
    assert.equal(roster.name, null);
});

test('a list of ids keeps only the ids, and anything else is no list', () => {
    assert.deepEqual(idsOf(['anna', 7, null, 'mark']), ['anna', 'mark']);
    assert.deepEqual(idsOf('anna'), []);
    assert.deepEqual(idsOf(undefined), []);
});

test('a broken roster reads as empty lists instead of failing', () => {
    const roster = rosterOf(documentOf('anna', {}));

    assert.deepEqual(roster.members, []);
    assert.deepEqual(roster.admins, []);
});

test('a profile carries the name, the picture and when it last changed', () => {
    const profile = profileOf(
        documentOf('anna', { displayName: 'Anna', photoUrl: 'https://example.com/a.png', updatedAt: at(1000) }),
    );

    assert.deepEqual(profile, [
        'anna',
        { memberId: 'anna', displayName: 'Anna', photoUrl: 'https://example.com/a.png', updatedAt: 1000 },
    ]);
});

test('a profile without a picture or a stamp says so with null', () => {
    const [, emptyPicture] = profileOf(documentOf('anna', { displayName: 'Anna', photoUrl: '', updatedAt: 1000 }));
    const [, noPicture] = profileOf(documentOf('anna', { displayName: 'Anna', photoUrl: 7 }));

    assert.equal(emptyPicture.photoUrl, null);
    assert.equal(emptyPicture.updatedAt, null);
    assert.equal(noPicture.photoUrl, null);
});

test('a profile without a name is no profile', () => {
    assert.equal(profileOf(documentOf('anna', {})), null);
    assert.equal(profileOf(documentOf('anna', { displayName: '   ' })), null);
});

test('the devices of a grant are its ids, and anything else is none', () => {
    assert.deepEqual(devicesOf(documentOf('anna', { devices: ['anna.phone', 7] })), ['anna.phone']);
    assert.deepEqual(devicesOf(documentOf('anna', { devices: 'anna.phone' })), []);
});
