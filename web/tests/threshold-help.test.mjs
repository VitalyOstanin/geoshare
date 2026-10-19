import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';

import { EN } from '../public/lib/words/en.js';
import { RU } from '../public/lib/words/ru.js';

const ANDROID = new URL('../../android/app/src/main/', import.meta.url);
const COLLECTION = 'kotlin/io/github/vitalyostanin/geoshare/collection/';

/** Reads a numeric `const val` from the Android collection package. */
const constant = async (file, name) => {
    const source = await readFile(new URL(COLLECTION + file, ANDROID), 'utf8');
    const found = source.match(new RegExp(`const val ${name}: Double = ([0-9.]+)`));
    assert.ok(found, `${name} is not found in ${file}`);
    return Number(found[1]);
};

const androidString = async (folder, name) => {
    const xml = await readFile(new URL(`res/${folder}/strings.xml`, ANDROID), 'utf8');
    const found = xml.match(new RegExp(`<string name="${name}">([^<]*)</string>`));
    assert.ok(found, `${name} is not found in ${folder}`);
    return found[1];
};

/** The help texts spell the speed limit in whole tens of km/h, so the constant is rounded. */
const speedKmh = (mps) => Math.round((mps * 3.6) / 10) * 10;

test('help texts name the geofence radius the collector uses, on both clients', async () => {
    const radius = String(await constant('Geofence.kt', 'GEOFENCE_RADIUS_METRES') | 0);
    const texts = [
        await androidString('values', 'mode_help_when_idle'),
        await androidString('values-ru', 'mode_help_when_idle'),
        EN.modeWhenIdle,
        RU.modeWhenIdle,
    ];
    for (const text of texts) {
        assert.match(text, new RegExp(`\\b${radius}\\b`), text);
    }
});

test('help texts name the plausible speed the jump check uses, on both clients', async () => {
    const limit = String(speedKmh(await constant('Jumps.kt', 'PLAUSIBLE_SPEED_MPS')));
    const texts = [
        await androidString('values', 'reason_help_sign_speed'),
        await androidString('values-ru', 'reason_help_sign_speed'),
        EN.reasonSignSpeed,
        RU.reasonSignSpeed,
    ];
    for (const text of texts) {
        assert.match(text, new RegExp(`\\b${limit}\\b`), text);
    }
});
