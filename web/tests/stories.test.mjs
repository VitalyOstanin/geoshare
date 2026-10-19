// The user stories in docs/stories, held to what they point at.
//
// A story names the records it rests on and the tests that hold it, and both
// move: a record is superseded, a test class renamed or a test file split. A
// link that points at nothing passes every other check there is, so this
// reads the stories and looks each link up.

import assert from 'node:assert/strict';
import { readdirSync, readFileSync } from 'node:fs';
import { test } from 'node:test';

const ROOT = new URL('../../', import.meta.url);
const STORIES = new URL('docs/stories/', ROOT);
const ROLES = ['project-owner.md', 'administrator.md', 'member.md', 'device.md', 'guest.md'];
const STATUSES = ['done', 'planned', 'partly'];

function read(url) {
    return readFileSync(url, 'utf8');
}

/** Every file under [dir], as names, however deep. */
function filesUnder(dir) {
    return readdirSync(new URL(dir, ROOT), { recursive: true }).map(String);
}

/** Each story of [file]: its id and the fields of the list under its heading. */
function storiesOf(file) {
    const text = read(new URL(file, STORIES));
    const sections = text.split(/^#+ (?=[A-Z]{3}-\d+ )/m).slice(1);

    return sections.map((section) => {
        const id = section.split(' ')[0];
        const field = (name) => {
            const found = new RegExp(String.raw`^- ${name}: (.*(?:\n  .*)*)`, 'm').exec(section);

            return found ? found[1].replace(/\n\s+/g, ' ') : null;
        };

        return {
            id,
            file,
            status: field('Status'),
            adr: field('ADR'),
            tests: field('Tests'),
            notCovered: field('Not covered'),
        };
    });
}

const stories = ROLES.flatMap(storiesOf);
const records = readdirSync(new URL('docs/ADR/', ROOT)).filter((name) => /^\d{4}-/.test(name));
const androidTests = [
    ...filesUnder('android/app/src/test/'),
    ...filesUnder('android/app/src/androidTest/'),
];
const webTests = filesUnder('web/tests/');
const ruleTests = filesUnder('firebase/tests/rules/');

test('every role file has stories, and every id is used once', () => {
    for (const role of ROLES) {
        assert.ok(stories.some((story) => story.file === role), `${role} has stories`);
    }

    const ids = stories.map((story) => story.id);

    assert.deepEqual(ids, [...new Set(ids)]);
});

test('an id carries the prefix of the role it is filed under', () => {
    const prefixes = {
        'project-owner.md': 'OWN',
        'administrator.md': 'ADM',
        'member.md': 'MEM',
        'device.md': 'DEV',
        'guest.md': 'GST',
    };

    for (const story of stories) {
        assert.ok(story.id.startsWith(`${prefixes[story.file]}-`), `${story.id} in ${story.file}`);
    }
});

test('every story says how far it is', () => {
    for (const story of stories) {
        const word = story.status?.split(' ')[0];

        assert.ok(STATUSES.includes(word), `${story.id}: status "${story.status}"`);
    }
});

test('every record a story names is there', () => {
    for (const story of stories) {
        assert.ok(story.adr !== null, `${story.id} says which records it rests on, or "—"`);

        const numbers = story.adr === '—' ? [] : story.adr.split(/,\s*/);

        for (const number of numbers) {
            assert.ok(
                records.some((name) => name.startsWith(`${number}-`)),
                `${story.id}: ADR-${number}`,
            );
        }
    }
});

test('every test a story names is there', () => {
    for (const story of stories) {
        assert.ok(story.tests !== null, `${story.id} names its tests, or "—"`);

        for (const [, name] of story.tests.matchAll(/`([A-Za-z]+Test)`/g)) {
            assert.ok(
                androidTests.some((path) => path.endsWith(`/${name}.kt`)),
                `${story.id}: Android ${name}`,
            );
        }

        for (const [, name] of story.tests.matchAll(/`([a-z-]+\.test\.mjs)`/g)) {
            assert.ok(
                webTests.includes(name) || ruleTests.includes(name),
                `${story.id}: ${name}`,
            );
        }
    }
});

test('a story without tests says what is left to check by hand', () => {
    for (const story of stories.filter((it) => it.tests === '—')) {
        assert.ok(story.notCovered, `${story.id} has no tests and does not say why`);
    }
});

test('a story planned is not claimed to be held by tests alone', () => {
    for (const story of stories.filter((it) => it.status === 'planned')) {
        assert.ok(story.notCovered, `${story.id} is planned and says nothing is covered yet`);
    }
});

test('the index lists the records the other way round, as the stories name them', () => {
    const index = read(new URL('README.md', STORIES));

    for (const name of records) {
        const number = name.slice(0, 4);
        const row = index.split('\n').find((line) => line.includes(`](../ADR/${name})`));

        assert.ok(row, `the index has a row for ADR-${number}`);

        const named = stories
            .filter((story) => story.adr !== '—' && story.adr.split(/,\s*/).includes(number))
            .map((story) => story.id);
        const listed = row.split('|').at(-2).trim();

        if (named.length === 0) {
            assert.ok(listed.startsWith('—'), `ADR-${number} is listed with stories no story names`);
        } else {
            assert.deepEqual(listed.split(/,\s*/), named, `ADR-${number}`);
        }
    }
});
