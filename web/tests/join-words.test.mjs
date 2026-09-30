import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';

import { JOIN_EN } from '../public/lib/words/join-en.js';
import { JOIN_RU } from '../public/lib/words/join-ru.js';

/** What a word is: a line to show, or a line built around something the link carries. */
const shapeOf = (dictionary) => Object.fromEntries(
    Object.entries(dictionary).map(([key, word]) => [
        key,
        typeof word === 'function' ? `function/${word.length}` : typeof word,
    ]),
);

test('both languages say the same things', () => {
    assert.deepEqual(Object.keys(JOIN_RU).sort(), Object.keys(JOIN_EN).sort());
});

test('a word built around something the link carries is one in both languages, of the same arity', () => {
    assert.deepEqual(shapeOf(JOIN_RU), shapeOf(JOIN_EN));
});

test('nothing is left untranslated by accident', () => {
    for (const [key, word] of Object.entries(JOIN_RU)) {
        if (typeof word !== 'string') {
            continue;
        }

        assert.notEqual(word, JOIN_EN[key], `${key} is still in English`);
    }
});

// The markup is painted before any module runs, so it carries its own words;
// the dictionary carries them too, and the page says them again in whichever
// language it settled on.
test('every word the markup is painted with has a place to be said again', async () => {
    const page = await readFile(new URL('../public/join.html', import.meta.url), 'utf8');

    for (const id of ['join-title', 'join-open', 'join-download']) {
        assert.ok(page.includes(`id="${id}"`), `the page has no ${id}`);
    }
});

test('the words of the markup are in the dictionaries', () => {
    for (const key of ['joinTitle', 'joinOpen', 'joinDownload']) {
        assert.ok(JOIN_EN[key], `English has no ${key}`);
        assert.ok(JOIN_RU[key], `Russian has no ${key}`);
    }
});
