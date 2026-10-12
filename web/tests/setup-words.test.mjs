import assert from 'node:assert/strict';
import { test } from 'node:test';

import { WORDS_EN, WORDS_RU } from '../public/lib/words/setup.js';

/** What a word is: a line, a list, or a line built around something. */
const shapeOf = (dictionary) => Object.fromEntries(
    Object.entries(dictionary).map(([key, word]) => [
        key,
        typeof word === 'function'
            ? `function/${word.length}`
            : Array.isArray(word) ? `array/${word.length}` : typeof word,
    ]),
);

test('the setup page says the same things in both languages', () => {
    assert.deepEqual(Object.keys(WORDS_RU).sort(), Object.keys(WORDS_EN).sort());
});

test('a setup word has the same shape in both languages', () => {
    assert.deepEqual(shapeOf(WORDS_RU), shapeOf(WORDS_EN));
});
