import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';

// The labels on the map and the error banner take their size from the
// --text-* scale of base.css, so one setting moves them with the rest of the
// page's text.
for (const sheet of ['markers.css', 'unexpected.css']) {
    test(`${sheet} takes every font size from the text scale`, async () => {
        const css = await readFile(new URL(`../public/styles/${sheet}`, import.meta.url), 'utf8');
        const sizes = [...css.matchAll(/font-size:\s*([^;]+);/g)].map((found) => found[1].trim());

        assert.ok(sizes.length > 0);
        for (const size of sizes) {
            assert.match(size, /^var\(--text-(small|body|large|title)\)$/);
        }
    });
}
