// The numbers and the field names the device and the page have to agree on,
// read out of both.
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

/** A number as the page writes one, 15_000 and products of literals included. */
function javascriptNumber(written) {
    assert.match(written, /^[\d_\s*+]+$/u, `${written} is plain arithmetic`);

    return written
        .split('*')
        .reduce((product, factor) => product * Number(factor.replaceAll('_', '')), 1);
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
    // The device centres a face in the head; the page writes the margin out.
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
    assert.equal(
        kotlinNumber(kotlin('map/GroupLine.kt', 'PHONE_ID_SHOWN')),
        javascriptNumber(javascript('web/public/lib/group-lines.js', 'PHONE_ID_SHOWN')),
    );
});

test('a permission without an end ends on the same day everywhere', () => {
    const forever = quotedDate(kotlin('map/ShareSpan.kt', 'FOREVER'));

    assert.equal(quotedDate(javascript('firebase/emulator/rest.mjs', 'FOREVER')), forever);
    assert.equal(quotedDate(javascript('firebase/tests/rules/helpers.mjs', 'FOREVER')), forever);
});

/** The string values of a Kotlin `object Name { const val … = "…" }`. */
function kotlinFields(file, object) {
    const text = source(KOTLIN + file);
    const start = text.search(new RegExp(String.raw`\bobject\s+${object}\s*\{`));

    assert.ok(start >= 0, `${object} is declared in ${file}`);

    const body = text.slice(start, text.indexOf('\n}', start));

    return [...body.matchAll(/const\s+val\s+\w+\s*=\s*"(\w+)"/gu)].map((found) => found[1]);
}

/** The rules of one collection: from its `match` up to the next one. */
function rulesOf(collection) {
    const text = source('firebase/firestore.rules');
    const start = text.search(new RegExp(String.raw`match /${collection}/\{`));

    assert.ok(start >= 0, `the rules have a match for ${collection}`);

    const next = text.indexOf('match /', start + 1);

    return text.slice(start, next < 0 ? text.length : next);
}

/**
 * The fields a page module reads off a document, `data.name` or `data?.name`;
 * not the end of a module's name such as `./group-data.js`.
 */
function pageReads(file) {
    const reads = source(file).matchAll(/(?<![\w-])data\??\.(\w+)/gu);

    return new Set([...reads].map((found) => found[1]));
}

const FIELDS = 'service/StorePaths.kt';

/** Each object of field names, and the collection whose rules name them again. */
const COLLECTIONS = {
    GroupField: 'groups',
    InvitationField: 'invitations',
    JoinField: 'joins',
    PositionField: 'positions',
    ProfileField: 'members',
    GrantField: 'grants',
    PhoneField: 'devices',
    LabelField: 'labels',
    FalsePlaceField: 'falsePlaces',
    CirclingField: 'settings',
    WatchField: 'watch',
    SearchField: 'searches',
    ResetField: 'resets',
    SirenField: 'sirens',
};

test('the rules know every field the device writes, under the same name', () => {
    for (const [object, collection] of Object.entries(COLLECTIONS)) {
        const rules = rulesOf(collection);

        for (const field of kotlinFields(FIELDS, object)) {
            assert.match(
                rules,
                new RegExp(String.raw`'${field}'|\.${field}\b`, 'u'),
                `${object} names ${field}, and the rules for ${collection} do not`,
            );
        }
    }
});

test('the page reads only fields the device writes', () => {
    const readers = {
        'web/public/lib/position-data.js': ['PositionField'],
        'web/public/lib/store-positions.js': ['PositionField'],
        'web/public/lib/group-data.js': ['GroupField', 'ProfileField', 'PhoneField', 'GrantField'],
        'web/public/lib/store-group.js': ['GroupField', 'ProfileField', 'PhoneField', 'GrantField'],
        'web/public/lib/store-search.js': ['SearchField', 'ResetField', 'SirenField'],
        'web/public/lib/labels.js': ['LabelField'],
        'web/public/lib/resets.js': ['ResetField'],
        'web/public/lib/searches.js': ['SearchField'],
        'web/public/lib/sirens.js': ['SirenField'],
    };

    for (const [file, objects] of Object.entries(readers)) {
        const written = new Set(objects.flatMap((object) => kotlinFields(FIELDS, object)));

        for (const field of pageReads(file)) {
            assert.ok(written.has(field), `${file} reads ${field}, which ${objects.join(', ')} do not name`);
        }
    }
});

/** The first `--name: #rrggbb` a stylesheet declares, or the one after `from`. */
function cssToken(file, name, from = '') {
    const text = source(file);
    const start = from === '' ? 0 : text.indexOf(from);

    assert.ok(start >= 0, `${file} has ${from}`);

    const pattern = new RegExp(String.raw`--${name}:\s*(#[0-9a-f]{6})\b`, 'iu');
    const found = pattern.exec(text.slice(start));

    assert.ok(found, `${file} declares --${name}`);

    return found[1].toLowerCase();
}

/** A property of the Material scheme, `primary = Color(0xFF1E88E5)`, in the given branch. */
function schemeColour(branch, name) {
    const text = source(KOTLIN + 'AppColours.kt');
    const split = text.indexOf('} else {', text.indexOf('fun appColours'));
    const body = branch === 'dark' ? text.slice(0, split) : text.slice(split);
    const found = new RegExp(String.raw`\b${name} = Color\(0xFF(\w{6})\)`, 'u').exec(body);

    assert.ok(found, `${name} is set in the ${branch} scheme`);

    return `#${found[1]}`.toLowerCase();
}

test('the accent is the same blue, by day and by night', () => {
    assert.equal(schemeColour('light', 'primary'), cssToken('web/public/styles/base.css', 'link'));
    assert.equal(
        schemeColour('dark', 'primary'),
        cssToken('web/public/styles/base.css', 'night-link'),
    );
});

test('an error is the same red, by day and by night', () => {
    assert.equal(schemeColour('light', 'error'), cssToken('web/public/styles/base.css', 'error'));
    assert.equal(
        schemeColour('dark', 'error'),
        cssToken('web/public/styles/base.css', 'night-error'),
    );
});

test('a search is the same red on the pin and in words, by day and by night', () => {
    const [pin] = kotlinColours(kotlin('map/MarkerIcon.kt', 'SOUGHT_COLOUR'));
    const css = 'web/public/styles/search.css';

    assert.equal(pin, cssToken(css, 'alarm'));
    assert.equal(pin, cssToken(css, 'alarm-text'));

    const words = source(KOTLIN + 'AppColours.kt').match(
        /fun soughtWords\(dark: Boolean\): Color = if \(dark\) Color\(0xFF(\w{6})\)/u,
    );

    assert.ok(words, 'soughtWords names its night colour');
    assert.equal(
        `#${words[1]}`.toLowerCase(),
        cssToken(css, 'alarm-text', 'prefers-color-scheme: dark'),
    );
});

test('the map is drawn from the same tiles on the device and on the page', () => {
    const properties = source('android/gradle.properties');
    const [, device] = /^tileUrl=(.+)$/mu.exec(properties);

    const page = javascript('web/public/lib/map.js', 'TILE_URL').replaceAll("'", '');

    assert.equal(device.trim(), page);
});

test('the page reports problems to the issue page the device is built with', () => {
    const properties = source('android/gradle.properties');
    const [, device] = /^issuePageUrl=(.+)$/mu.exec(properties);

    const page = javascript('web/public/lib/unexpected.js', 'NEW_ISSUE').replaceAll("'", '');

    assert.equal(device.trim(), page);
});

test('the page links the releases of the repository the device checks for updates', () => {
    const properties = source('android/gradle.properties');
    const [, latest] = /^latestReleaseUrl=(.+)$/mu.exec(properties);
    const [, repository] = /^https:\/\/api\.github\.com\/repos\/([^/]+\/[^/]+)\/releases\/latest$/u
        .exec(latest.trim());

    const page = javascript('web/public/lib/join.js', 'RELEASES_URL').replaceAll("'", '');

    assert.equal(page, `https://github.com/${repository}/releases`);
});
