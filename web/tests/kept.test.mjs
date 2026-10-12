// What the browser keeps, through the one pair of functions every module uses.

import assert from 'node:assert/strict';
import { test } from 'node:test';

import { keep, readKept } from '../public/lib/kept.js';

function memoryStore() {
    const items = new Map();

    return {
        getItem: (key) => items.get(key) ?? null,
        setItem: (key, value) => items.set(key, String(value)),
        removeItem: (key) => items.delete(key),
    };
}

// A browser told to keep no site data throws on every attempt.
const refusingStore = {
    getItem() { throw new Error('SecurityError'); },
    setItem() { throw new Error('SecurityError'); },
    removeItem() { throw new Error('SecurityError'); },
};

test('what is kept is read back, and null forgets it', () => {
    const store = memoryStore();

    assert.equal(keep('k', 'v', store), true);
    assert.equal(readKept('k', store), 'v');
    assert.equal(keep('k', null, store), true);
    assert.equal(readKept('k', store), null);
});

test('a browser that refuses storage reads as nothing kept', () => {
    assert.equal(readKept('k', refusingStore), null);
    assert.equal(keep('k', 'v', refusingStore), false);
});
