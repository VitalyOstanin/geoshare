// Pasted texts made up by a seeded generator, and what the join page's
// invitationTyped makes of each. ConnectLinkTest holds the application's
// ConnectLink.parse to the same answers, so the two readers of a link are
// compared on several hundred texts nobody had to think of, not only on the
// cases in fixtures/connect-links.json. The pieces lean on what the two have
// to agree on by hand: white space that is blank to Kotlin but not to
// java.util.regex, a plus read as a space, percent escapes URLDecoder refuses
// or decodes to a broken character, and two fields with the same key.
//
// Run as a script to write the fixture again after join.js changes on purpose:
//     node tests/connect-link-cases.mjs
// join.test.mjs fails while the file and the generator disagree.

import { writeFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';

import { invitationTyped } from '../public/lib/join.js';

export const GENERATED_SEED = 700;

export const GENERATED_COUNT = 500;

export const GENERATED_FILE = new URL('./fixtures/connect-links-generated.json', import.meta.url);

/** A mulberry32 generator: the same seed gives the same numbers on every run. */
export function seeded(seed) {
    let state = seed;

    return () => {
        state = (state + 0x6d2b79f5) | 0;
        let t = Math.imul(state ^ (state >>> 15), 1 | state);

        t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;

        return ((t ^ (t >>> 14)) >>> 0) / 4_294_967_296;
    };
}

// Pieces that keep a link whole and readable, and pieces that test the edges
// one at a time: a value made only of the second kind would rarely leave an
// invitation to compare.
const PLAIN_PIECES = [
    'a', 'Z', '7', 'grp', 'ABCDEFGH', 'Я', 'é', '\u{1F600}', '+', '%2B', '%20', '%41', '.', '…',
    '%C2%A0', '%1C', '%1F', '%E2%80%A8', '%F0%9F%98%80', ' ', '\u001C', '\u001F',
];

const EDGE_PIECES = [
    ' ', '\t', ' ', ' ', '\u0085', '​', '﻿', '%E0%A4', '%E0%A4%A', '%zz',
    '%', '%%41', '%+1', '%-1', '&', '=', '#', '?', '/', ')',
];

const KEYS = ['by', 'by', 'n', 'n', 'to', 'to', 'g', 'c', 'G', 'x', '', 'g ', '%67'];

const HOSTS = [
    'https://imaginary-group.web.app/',
    'https://imaginary-group.web.app/',
    'https://imaginary-group.web.app/',
    'https://imaginary-group.firebaseapp.com/',
    'https://imaginary-2.web.app/',
    'http://imaginary-group.web.app/',
    'https://Imaginary-group.web.app/',
    'https://2imaginary.web.app/',
];

const PATHS = ['join', 'join', 'join', 'join', 'join', 'join/', 'join/', 'joins', ''];

const AROUND = ['', '', '', ' ', '\n', ' ', ' ', '(', 'Join us: ', '.', ').', '!', '\u{1F600}'];

function pick(next, list) {
    return list[Math.floor(next() * list.length)];
}

function value(next) {
    const length = Math.floor(next() * 5);
    let text = '';

    for (let i = 0; i < length; i += 1) {
        text += pick(next, next() < 0.2 ? EDGE_PIECES : PLAIN_PIECES);
    }

    return text;
}

/** A query or a fragment: the fields an invitation needs, often, and noise. */
function fields(next) {
    const pairs = [];

    if (next() < 0.85) {
        pairs.push(`g=${next() < 0.6 ? 'grp' : value(next)}`);
    }

    if (next() < 0.85) {
        pairs.push(`c=${next() < 0.6 ? 'ABCDEFGH' : value(next)}`);
    }

    const extra = Math.floor(next() * 4);

    for (let i = 0; i < extra; i += 1) {
        const key = pick(next, KEYS);

        pairs.push(next() < 0.1 ? key : `${key}=${value(next)}`);
    }

    // Shuffled, so that a repeated key lands either side of the first one.
    for (let i = pairs.length - 1; i > 0; i -= 1) {
        const j = Math.floor(next() * (i + 1));

        [pairs[i], pairs[j]] = [pairs[j], pairs[i]];
    }

    return pairs.join('&');
}

function text(next) {
    let link = pick(next, HOSTS) + pick(next, PATHS);

    if (next() < 0.3) {
        link += `?${fields(next)}`;
    }

    if (next() < 0.8) {
        link += `#${fields(next)}`;
    }

    return pick(next, AROUND) + link + pick(next, AROUND);
}

/** [count] texts made from [seed], each with what invitationTyped answers. */
export function generatedCases(seed = GENERATED_SEED, count = GENERATED_COUNT) {
    const next = seeded(seed);

    return Array.from({ length: count }, () => {
        const pasted = text(next);

        return { text: pasted, expected: invitationTyped(pasted) };
    });
}

/** The fixture's text: the header, then one case to a line for a readable diff. */
export function generatedFixture(seed = GENERATED_SEED, count = GENERATED_COUNT) {
    const header = {
        comment: 'Written by web/tests/connect-link-cases.mjs, not by hand. Each text is '
            + 'paired with what invitationTyped (join.js) answers; ConnectLinkTest holds '
            + 'ConnectLink.parse to the same answer. expected is null where the page finds no '
            + 'invitation, the owner\'s link among them; site is null for the older '
            + '"group.code" form, which only the page reads.',
        seed,
        count,
    };
    const head = JSON.stringify(header, null, 2).replace(/\n\}$/u, ',\n');
    const cases = generatedCases(seed, count).map((one) => `    ${JSON.stringify(one)}`);

    return `${head}  "cases": [\n${cases.join(',\n')}\n  ]\n}\n`;
}

if (process.argv[1] !== undefined && import.meta.url === pathToFileURL(process.argv[1]).href) {
    writeFileSync(GENERATED_FILE, generatedFixture());
}
