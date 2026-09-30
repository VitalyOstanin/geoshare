import assert from 'node:assert/strict';
import { test } from 'node:test';

import { groupCaption } from '../public/lib/group-name.js';

const rosterWith = (name, founder = 'founder') => ({
    members: [founder],
    admins: [founder],
    founder,
    name,
});

test('a group with a name of its own is called by it', () => {
    assert.equal(groupCaption(rosterWith('Hiking crew'), null, 'me'), 'Hiking crew');
});

test('a name of nothing but spaces is read as no name at all', () => {
    assert.equal(groupCaption(rosterWith('   '), null, 'me'), "founde's group");
});

test('a nameless group is called after its founder', () => {
    const profiles = new Map([['founder', { displayName: 'Ann', photoUrl: null }]]);

    assert.equal(groupCaption(rosterWith(null), profiles, 'me'), "Ann's group");
});

test('a founder with no profile is called by the start of their id', () => {
    assert.equal(
        groupCaption(rosterWith(null, 'Qm7d0Kh8oOOL8dKLzdocJ2isAjIh'), null, 'me'),
        "Qm7d0K's group",
    );
});

test('a nameless group calls its own founder "you"', () => {
    assert.equal(groupCaption(rosterWith(null, 'me'), null, 'me'), "You's group");
});
