// Which of the page's scripts the tests never load. Node's coverage table
// leaves such a file out altogether rather than showing it at nought, so its
// percentages describe only what the tests already reach. The files that stay
// out are listed in web/tests/unreached.txt: a new file missing from both the
// report and the list fails the run, and so does a listed file the tests have
// started to load, so the list only ever gets shorter.
//
//   node tests/coverage-reach.mjs build/lcov.info    (from web/)
import { readFile, readdir } from 'node:fs/promises';
import { join, relative } from 'node:path';

const PUBLIC = 'public';
const VENDOR = 'public/lib/vendor';
const LIST = 'tests/unreached.txt';
// Written by tools/web-config.sh and kept out of git: a fresh clone has none.
const GENERATED = 'public/firebase-config.js';

async function scripts(dir) {
    const found = [];

    for (const entry of await readdir(dir, { withFileTypes: true })) {
        const path = join(dir, entry.name);

        if (entry.isDirectory() && path !== VENDOR) {
            found.push(...(await scripts(path)));
        } else if (entry.isFile() && entry.name.endsWith('.js') && path !== GENERATED) {
            found.push(relative(PUBLIC, path));
        }
    }

    return found;
}

const report = await readFile(process.argv[2], 'utf8');
const reached = new Set(
    report.split('\n')
        .filter((line) => line.startsWith('SF:'))
        .map((line) => relative(join(process.cwd(), PUBLIC), line.slice(3))),
);
const listed = new Set(
    (await readFile(LIST, 'utf8')).split('\n')
        .map((line) => line.trim())
        .filter((line) => line && !line.startsWith('#')),
);

const all = await scripts(PUBLIC);
const unreached = all.filter((path) => !reached.has(path)).sort();
const added = unreached.filter((path) => !listed.has(path));
const gone = [...listed].filter((path) => !all.includes(path)).sort();
const nowReached = [...listed].filter((path) => all.includes(path) && reached.has(path)).sort();

console.log(`${unreached.length} of the page's scripts are not loaded by any test`);

for (const path of added) {
    console.error(`not loaded by any test and not in ${LIST}: ${path}`);
}

for (const path of gone) {
    console.error(`no such script, take it out of ${LIST}: ${path}`);
}

for (const path of nowReached) {
    console.error(`loaded by the tests now, take it out of ${LIST}: ${path}`);
}

process.exitCode = added.length + gone.length + nowReached.length > 0 ? 1 : 0;
