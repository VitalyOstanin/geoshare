import assert from 'node:assert/strict';
import { test } from 'node:test';

import { cutToLimit, trimText } from '../public/lib/text.js';

test('a cut that ends between the halves of a pair drops the first half too', () => {
    assert.equal(cutToLimit(`${'a'.repeat(39)}\u{1F600}`, 40), 'a'.repeat(39));
});

test('a pair that fits whole is kept', () => {
    const fits = `${'a'.repeat(38)}\u{1F600}`;

    assert.equal(cutToLimit(fits, 40), fits);
    assert.equal(cutToLimit('abc', 40), 'abc');
});

// Shared with TextTest.kt: both clients must strip the same characters.
const STRIPPED = [
    0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x20, 0xa0, 0x1680, 0x2000, 0x2001, 0x2002, 0x2003, 0x2004,
    0x2005, 0x2006, 0x2007, 0x2008, 0x2009, 0x200a, 0x2028, 0x2029, 0x202f, 0x205f, 0x3000, 0xfeff,
];
// The ones the two platforms' own trim() disagree on or neither strips.
const KEPT = [0x1c, 0x1d, 0x1e, 0x1f, 0x85, 0x180e, 0x200b, 0x200c, 0x200d, 0x2060];

test('trimText strips every character of the shared table from both edges', () => {
    for (const code of STRIPPED) {
        const mark = String.fromCharCode(code);
        assert.equal(trimText(`${mark}${mark}ab c${mark}`), 'ab c', code.toString(16));
    }
});

test('trimText keeps the characters the shared table leaves out', () => {
    for (const code of KEPT) {
        const mark = String.fromCharCode(code);
        assert.equal(trimText(`${mark}ab${mark}`), `${mark}ab${mark}`, code.toString(16));
    }
});

test('trimText of only white space is empty and inner white space is kept', () => {
    assert.equal(trimText('\u00a0\ufeff \u3000'), '');
    assert.equal(trimText(' a\u00a0b '), 'a\u00a0b');
});
