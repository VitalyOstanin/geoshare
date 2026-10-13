import assert from 'node:assert/strict';
import { readdirSync, readFileSync } from 'node:fs';
import { test } from 'node:test';

const ROOT = new URL('../../', import.meta.url);
const PUBLIC = new URL('web/public/', ROOT);
const VENDOR = new URL('lib/vendor/', PUBLIC);
const pins = JSON.parse(readFileSync(new URL('web/package.json', ROOT), 'utf8')).devDependencies;
const pinned = Object.entries(pins).map(([name, version]) => `${name}-${version}`);
const RUN = 'run tools/vendor-web.sh and commit lib/vendor with the new pins';

// Dependabot raises a pin in package.json and nothing else: the copy the page
// serves follows only when tools/vendor-web.sh is run. Until it is, these fail,
// and a bump cannot land with the old library still in place.
test('lib/vendor holds the pinned versions and nothing else', () => {
    const held = readdirSync(VENDOR, { withFileTypes: true })
        .filter((entry) => entry.isDirectory())
        .map((entry) => entry.name);

    assert.deepEqual(held.sort(), [...pinned].sort(), RUN);
});

// The version is the address: the hosting keeps lib/vendor a year as immutable,
// so a pin that drifted from a range would serve a new library under an old name.
test('every pin is an exact version', () => {
    for (const [name, version] of Object.entries(pins)) {
        assert.match(version, /^\d+\.\d+\.\d+$/, `${name} is pinned to ${version}`);
    }
});

test('the page loads every library from its pinned copy', () => {
    const sources = readdirSync(PUBLIC, { recursive: true })
        .map(String)
        .filter((path) => /\.(js|html)$/.test(path) && !path.startsWith('lib/vendor/'));
    const wrong = [];

    for (const path of sources) {
        const text = readFileSync(new URL(path, PUBLIC), 'utf8');
        for (const [, directory] of text.matchAll(/vendor\/([^/'"]+)\//g)) {
            if (!pinned.includes(directory)) {
                wrong.push(`${path}: ${directory}`);
            }
        }
        if (/from\s+['"]https:/.test(text)) {
            wrong.push(`${path}: a module from another server`);
        }
    }

    assert.deepEqual(wrong, [], RUN);
});

// The rules tests talk to the emulator through the same SDK; on another version
// they would pass for a client the page no longer is.
test('the rules tests run on the Firebase the page serves', () => {
    const tests = JSON.parse(readFileSync(new URL('firebase/tests/package.json', ROOT), 'utf8'));

    assert.equal(tests.devDependencies.firebase, pins.firebase);
});
