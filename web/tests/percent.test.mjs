import assert from 'node:assert/strict';
import { test } from 'node:test';

import { percentText } from '../public/lib/percent.js';

test('English writes the sign against the number', () => {
    assert.equal(percentText(64, 'en'), '64%');
});

test('Russian sets the sign apart from the number', () => {
    assert.match(percentText(64, 'ru'), /^64\s%$/u);
});
