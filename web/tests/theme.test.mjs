import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';

import { THEME_KEY, applyTheme, readTheme, writeTheme } from '../public/lib/theme.js';

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

const rootOf = () => ({ dataset: {} });

test('an answer given earlier is the one read back', () => {
    assert.equal(readTheme(storeOf({ [THEME_KEY]: 'dark' })), 'dark');
});

test('a viewer who has said nothing follows the system', () => {
    assert.equal(readTheme(storeOf()), 'system');
});

test('a stored answer that means nothing is not one', () => {
    assert.equal(readTheme(storeOf({ [THEME_KEY]: 'sepia' })), 'system');
});

test('a browser that keeps no site data still gets a theme', () => {
    assert.equal(readTheme(refusingStore), 'system');
});

test('remembering an answer that cannot be remembered is not an error', () => {
    assert.doesNotThrow(() => writeTheme('dark', refusingStore));
});

test('a chosen theme is stated on the element', () => {
    const root = rootOf();

    applyTheme('dark', root);

    assert.equal(root.dataset.theme, 'dark');
});

test('following the system is the absence of a statement', () => {
    const root = rootOf();

    applyTheme('dark', root);
    applyTheme('system', root);

    assert.equal('theme' in root.dataset, false);
});

// The page applies the theme before the first paint, from a script of its own
// that cannot import this module. The key it reads has to be this one.
test('the page reads the theme from the key this module writes', async () => {
    const page = await readFile(new URL('../public/index.html', import.meta.url), 'utf8');

    assert.ok(page.includes(`'${THEME_KEY}'`));
});
