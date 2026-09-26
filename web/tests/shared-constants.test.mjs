// The numbers the phone and the page have to agree on, read out of both.
//
// Each client keeps its own copy of the rules the other one keeps — how long a
// position stays fresh, how wide a pin is, which colour a member is drawn in —
// and a change to one copy passes every other check there is. This reads the
// declarations out of the sources of both sides and holds them to each other,
// so a copy changed alone fails here rather than on somebody's screen.

import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';

const ROOT = new URL('../../', import.meta.url);
const KOTLIN = 'android/app/src/main/kotlin/io/github/vitalyostanin/geoshare/';

function source(path) {
    return readFileSync(new URL(path, ROOT), 'utf8');
}

/**
 * What a Kotlin declaration is set to, as written: up to the end of the line
 * on which every bracket it opened is closed again.
 */
function kotlin(file, name) {
    const text = source(KOTLIN + file);
    const found = new RegExp(String.raw`\bval\s+${name}\s*(?::\s*[\w.<>]+)?\s*=`).exec(text);

    assert.ok(found, `${name} is declared in ${file}`);

    const start = found.index + found[0].length;
    let depth = 0;
    let end = start;

    for (; end < text.length; end += 1) {
        const character = text[end];

        if ('([{'.includes(character)) {
            depth += 1;
        } else if (')]}'.includes(character)) {
            depth -= 1;
        } else if (character === '\n' && depth === 0 && text.slice(start, end).trim() !== '') {
            break;
        }
    }

    return text.slice(start, end).trim();
}

/** What a JavaScript declaration is set to, as written. */
function javascript(file, name) {
    const found = source(file).match(new RegExp(String.raw`\bconst\s+${name}\s*=\s*([^;]+);`));

    assert.ok(found, `${name} is declared in ${file}`);

    return found[1].trim();
}

/** A number as Kotlin writes one: 50_000.0, 44f, 8. */
function kotlinNumber(written) {
    return Number(written.replaceAll('_', '').replace(/f$/u, ''));
}

/** A number as the page writes one, products of literals included. */
function javascriptNumber(written) {
    assert.match(written, /^[\d\s*+]+$/u, `${written} is plain arithmetic`);

    return written.split('*').reduce((product, factor) => product * Number(factor), 1);
}

/** A Duration.ofMinutes(3) and the like, in milliseconds. */
function kotlinMilliseconds(written) {
    const found = written.match(/^Duration\.of(Seconds|Minutes|Hours)\((\d+)\)$/u);

    assert.ok(found, `${written} is a whole duration`);

    const unit = { Seconds: 1000, Minutes: 60 * 1000, Hours: 60 * 60 * 1000 }[found[1]];

    return Number(found[2]) * unit;
}

/** Every Color.rgb(0x1E, 0x88, 0xE5) in a declaration, as the page writes colours. */
function kotlinColours(written) {
    return [...written.matchAll(/Color\.rgb\(0x(\w\w),\s*0x(\w\w),\s*0x(\w\w)\)/gu)].map(
        (found) => `#${found.slice(1).join('')}`.toLowerCase(),
    );
}

/** Every '#1e88e5' in a declaration. */
function javascriptColours(written) {
    return [...written.matchAll(/'(#[0-9a-f]{6})'/giu)].map((found) => found[1].toLowerCase());
}

/** The date inside Instant.parse("…"), new Date('…') or a bare '…'. */
function quotedDate(written) {
    return written.match(/["'](\d{4}-\d\d-\d\dT[\d:]+Z)["']/u)[1];
}

test('a position goes stale after the same time', () => {
    assert.equal(
        kotlinMilliseconds(kotlin('collection/Freshness.kt', 'STALE_AFTER')),
        javascriptNumber(javascript('web/public/lib/age.js', 'STALE_AFTER_MS')),
    );
});

test('a watch lasts as long, and is renewed as often', () => {
    assert.equal(
        kotlinMilliseconds(kotlin('collection/Watching.kt', 'WATCH_AHEAD')),
        javascriptNumber(javascript('web/public/lib/store.js', 'WATCH_AHEAD_MS')),
    );
    assert.equal(
        kotlinMilliseconds(kotlin('collection/Watching.kt', 'WATCH_REFRESH')),
        javascriptNumber(javascript('web/public/lib/store.js', 'WATCH_REFRESH_MS')),
    );
});

test('an invitation lasts as long, and its code is as long', () => {
    assert.equal(
        kotlinMilliseconds(kotlin('membership/Codes.kt', 'INVITATION_LASTS')),
        javascriptNumber(javascript('web/public/lib/membership.js', 'INVITATION_LASTS_MS')),
    );
    assert.equal(
        kotlinNumber(kotlin('membership/Codes.kt', 'CODE_LENGTH')),
        javascriptNumber(javascript('web/public/lib/membership.js', 'CODE_LENGTH')),
    );
    assert.equal(
        kotlinNumber(kotlin('membership/Codes.kt', 'GROUP_NAME_LIMIT')),
        javascriptNumber(javascript('web/public/lib/membership.js', 'GROUP_NAME_LIMIT')),
    );
});

test('the false places are weighed by the same limits', () => {
    assert.equal(
        kotlinNumber(kotlin('collection/FalsePlaces.kt', 'FALSE_PLACES_KEPT')),
        javascriptNumber(javascript('web/public/lib/false-places.js', 'PLACES_KEPT')),
    );
    assert.equal(
        kotlinNumber(kotlin('collection/FalsePlaces.kt', 'FALSE_PLACE_WIDEST_METRES')),
        javascriptNumber(javascript('web/public/lib/false-places.js', 'WIDEST_METRES')),
    );
});

test('pins gather members within the same radius, and hold as many circles', () => {
    assert.equal(
        kotlinNumber(kotlin('map/MemberMap.kt', 'CLUSTER_RADIUS_DP')),
        javascriptNumber(javascript('web/public/lib/map.js', 'CLUSTER_RADIUS')),
    );
    assert.equal(
        kotlinNumber(kotlin('map/MarkerIcon.kt', 'PIN_MAX_CIRCLES')),
        javascriptNumber(javascript('web/public/lib/clusters.js', 'PIN_MAX_CIRCLES')),
    );
});

test('a pin is drawn to the same measurements', () => {
    const head = kotlinNumber(kotlin('map/MarkerIcon.kt', 'PIN_HEAD_DP'));
    const face = kotlinNumber(kotlin('map/MarkerIcon.kt', 'FACE_DP'));

    assert.equal(head, javascriptNumber(javascript('web/public/lib/map.js', 'PIN_HEAD')));
    assert.equal(
        kotlinNumber(kotlin('map/MarkerIcon.kt', 'FACE_STEP_DP')),
        javascriptNumber(javascript('web/public/lib/map.js', 'FACE_STEP')),
    );
    // The phone centres a face in the head; the page writes the margin out.
    assert.equal(
        (head - face) / 2,
        javascriptNumber(javascript('web/public/lib/map.js', 'FACE_INSET')),
    );
});

test('members are drawn in the same colours, and fade to the same grey', () => {
    assert.deepEqual(
        kotlinColours(kotlin('map/MarkerIcon.kt', 'MEMBER_COLOURS')),
        javascriptColours(javascript('web/public/lib/members.js', 'MEMBER_COLOURS')),
    );
    assert.deepEqual(
        kotlinColours(kotlin('map/MarkerIcon.kt', 'STALE_COLOUR')),
        javascriptColours(javascript('web/public/lib/members.js', 'STALE_COLOUR')),
    );
    assert.deepEqual(
        kotlinColours(kotlin('map/MarkerIcon.kt', 'GROUP_COLOUR')),
        javascriptColours(javascript('web/public/lib/map.js', 'GROUP_COLOUR')),
    );
});

test('the map opens at the same zoom', () => {
    assert.equal(
        kotlinNumber(kotlin('map/MemberMap.kt', 'START_ZOOM')),
        javascriptNumber(javascript('web/public/lib/map.js', 'START_ZOOM')),
    );
});

test('a member without a name is shown by as much of the id', () => {
    const length = kotlinNumber(kotlin('map/MemberOrder.kt', 'SHORT_ID_LENGTH'));

    assert.equal(
        length,
        javascriptNumber(javascript('web/public/lib/members.js', 'SHORT_ID_LENGTH')),
    );
    assert.equal(length, kotlinNumber(kotlin('map/AdminPanel.kt', 'NAME_SHOWN_INSTEAD')));
    assert.equal(
        kotlinNumber(kotlin('map/GroupLine.kt', 'PHONE_ID_SHOWN')),
        javascriptNumber(javascript('web/public/lib/group-lines.js', 'PHONE_ID_SHOWN')),
    );
});

test('a permission without an end ends on the same day everywhere', () => {
    const forever = quotedDate(kotlin('map/ShareSpan.kt', 'FOREVER'));

    assert.equal(quotedDate(javascript('firebase/emulator/seed.mjs', 'FOREVER')), forever);
    assert.equal(quotedDate(javascript('firebase/tests/rules.test.mjs', 'FOREVER')), forever);
});
