import assert from 'node:assert/strict';
import { test } from 'node:test';

import { nameFailureText } from '../public/lib/name-failure.js';
import { EN } from '../public/lib/words/en.js';
import { RU } from '../public/lib/words/ru.js';

const KNOWN = [
    'permission-denied',
    'unavailable',
    'deadline-exceeded',
    'unauthenticated',
    'resource-exhausted',
];

test('a refused write of a name is told in Russian, not as a raw code', () => {
    const text = nameFailureText(RU, { code: 'permission-denied' });

    assert.equal(text, RU.nameFailed(RU.nameFailedDenied));
    assert.ok(!text.includes('permission-denied'));
});

test('every known code has its own reason in both languages', () => {
    for (const words of [RU, EN]) {
        const texts = KNOWN.map((code) => nameFailureText(words, { code }));

        assert.equal(new Set(texts).size, KNOWN.length);

        for (const [i, text] of texts.entries()) {
            assert.ok(!text.includes(KNOWN[i]), text);
        }
    }
});

test('an unknown code gets the general text and the code stays out of it', () => {
    const warn = console.warn;

    console.warn = () => {};

    try {
        for (const words of [RU, EN]) {
            const text = nameFailureText(words, { code: 'data-loss', message: 'raw sdk text' });

            assert.equal(text, words.nameFailed(words.nameFailedOther));
            assert.ok(!text.includes('data-loss') && !text.includes('raw sdk text'));
        }
    } finally {
        console.warn = warn;
    }
});
