import assert from 'node:assert/strict';
import { test } from 'node:test';

import { mayActOnDevice } from '../public/lib/card-actions.js';

test('a member is offered the buttons for their own device', () => {
    assert.equal(mayActOnDevice('anna', 'anna', false), true);
    assert.equal(mayActOnDevice('anna', 'anna', true), true);
});

test('somebody else\'s device is offered to an administrator only', () => {
    assert.equal(mayActOnDevice('anna', 'boris', true), true);
    assert.equal(mayActOnDevice('anna', 'boris', false), false);
});

test('nobody is offered the buttons before there is a viewer or an owner', () => {
    assert.equal(mayActOnDevice(null, 'boris', true), false);
    assert.equal(mayActOnDevice('anna', '', true), false);
});
