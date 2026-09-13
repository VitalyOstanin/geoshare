import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';

import {
    LANGUAGES,
    LANGUAGE_KEY,
    readLanguage,
    spokenLanguage,
    writeLanguage,
} from '../public/lib/language.js';

/** A browser's store, as much of one as these functions touch. */
const storeOf = (initial = {}) => ({
    items: { ...initial },
    getItem(key) {
        return key in this.items ? this.items[key] : null;
    },
    setItem(key, value) {
        this.items[key] = value;
    },
});

/** A store that keeps nothing, the way a browser told to block site data does. */
const refusingStore = {
    getItem() {
        throw new Error('site data is off');
    },
    setItem() {
        throw new Error('site data is off');
    },
};

test('an answer given earlier is the one read back', () => {
    assert.equal(readLanguage(storeOf({ [LANGUAGE_KEY]: 'ru' })), 'ru');
});

test('a viewer who has said nothing follows the browser', () => {
    assert.equal(readLanguage(storeOf()), 'system');
});

test('a stored answer that means nothing is not one', () => {
    assert.equal(readLanguage(storeOf({ [LANGUAGE_KEY]: 'la' })), 'system');
});

test('a browser that keeps no site data still gets a language', () => {
    assert.equal(readLanguage(refusingStore), 'system');
});

test('remembering an answer that cannot be remembered is not an error', () => {
    assert.doesNotThrow(() => writeLanguage('ru', refusingStore));
});

test('a named language is the one spoken, whatever the browser asks for', () => {
    assert.equal(spokenLanguage('en', ['ru-RU', 'ru']), 'en');
    assert.equal(spokenLanguage('ru', ['en-GB']), 'ru');
});

test('following the browser reads the region off the tag', () => {
    assert.equal(spokenLanguage('system', ['ru-RU', 'en-US']), 'ru');
});

test('following the browser takes the first language there are words for', () => {
    assert.equal(spokenLanguage('system', ['fr-FR', 'de', 'ru']), 'ru');
});

test('a browser asking for nothing anybody here speaks is answered in English', () => {
    assert.equal(spokenLanguage('system', ['fr-FR', 'de']), 'en');
    assert.equal(spokenLanguage('system', []), 'en');
});

// The row in the menu is what makes the answer reachable at all: without a
// button for each, a viewer can only change the language of their browser.
test('the menu carries a button for every answer', async () => {
    const page = await readFile(new URL('../public/index.html', import.meta.url), 'utf8');

    for (const language of LANGUAGES) {
        assert.ok(page.includes(`data-language="${language}"`), `no button for ${language}`);
    }
});
