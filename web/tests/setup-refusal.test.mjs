import assert from 'node:assert/strict';
import { test } from 'node:test';

import { refusalText } from '../public/lib/setup-project.js';
import { WORDS_EN, WORDS_RU } from '../public/lib/words/setup.js';

const KNOWN = [
    'access_denied',
    'popup_closed',
    'popup_failed_to_open',
    'immediate_failed',
    'interaction_required',
    'invalid_scope',
];

test('a known Google sign-in refusal is told in words, not as a raw code', () => {
    for (const words of [WORDS_RU, WORDS_EN]) {
        for (const code of KNOWN) {
            const text = refusalText(words, code);

            assert.ok(!text.includes(code), `${code}: ${text}`);
        }
    }
});

test('the refusal reasons differ per code', () => {
    const texts = KNOWN.map((code) => refusalText(WORDS_EN, code));

    // immediate_failed and interaction_required share one phrase.
    assert.equal(new Set(texts).size, KNOWN.length - 1);
});

test('the closed sign-in window is said in Russian', () => {
    assert.match(refusalText(WORDS_RU, 'popup_closed'), /окно входа закрыто/);
});

test('an unknown refusal gets the general phrase with the code in brackets', () => {
    assert.equal(
        refusalText(WORDS_EN, 'brand_new_code'),
        'Access not granted (brand_new_code).',
    );
    assert.match(refusalText(WORDS_RU, 'brand_new_code'), /\(brand_new_code\)/);
});

test('a refusal without a code still gets the general phrase', () => {
    assert.equal(refusalText(WORDS_EN, undefined), 'Access not granted.');
    assert.equal(refusalText(WORDS_RU, ''), 'Доступ не получен.');
});
