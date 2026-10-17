import assert from 'node:assert/strict';
import { readdirSync, readFileSync } from 'node:fs';
import { test } from 'node:test';

const ROOT = new URL('../../', import.meta.url);
const hosting = JSON.parse(readFileSync(new URL('firebase.json', ROOT), 'utf8')).hosting;

/** A hosting glob as a regular expression, for the few forms firebase.json uses. */
function globToRegExp(glob) {
    const body = glob
        .replace(/[.+^${}[\]\\]/g, '\\$&')
        .replace(/@\(([^)]*)\)/g, '($1)')
        .replace(/\*\*/g, '\uE000')
        .replace(/\*/g, '[^/]*')
        .replace(/\uE000/g, '.*');

    return new RegExp(`^${body}$`);
}

const unstored = hosting.headers
    .filter(({ headers }) => headers.some(({ key, value }) => key === 'Cache-Control' && value === 'no-cache'))
    .map(({ source }) => globToRegExp(source));

// A module served from the cache beside a fresh one it imports from is a page
// of two versions at once: after a release a browser would run the old panel
// over the new lines for as long as the cache lasted. Every script and style
// is asked anew, except the vendored libraries, whose names change with them.
test('every script and style of the page is served uncached', () => {
    const left = readdirSync(new URL('web/public/', ROOT), { recursive: true })
        .map((name) => `/${String(name)}`)
        .filter((path) => /\.(js|css)$/.test(path) && !path.startsWith('/lib/vendor/'))
        .filter((path) => !unstored.some((pattern) => pattern.test(path)));

    assert.deepEqual(left, []);
});
