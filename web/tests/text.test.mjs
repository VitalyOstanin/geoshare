import assert from 'node:assert/strict';
import { test } from 'node:test';

import { cutToLimit } from '../public/lib/text.js';

test('a cut that ends between the halves of a pair drops the first half too', () => {
    assert.equal(cutToLimit(`${'a'.repeat(39)}\u{1F600}`, 40), 'a'.repeat(39));
});

test('a pair that fits whole is kept', () => {
    const fits = `${'a'.repeat(38)}\u{1F600}`;

    assert.equal(cutToLimit(fits, 40), fits);
    assert.equal(cutToLimit('abc', 40), 'abc');
});
