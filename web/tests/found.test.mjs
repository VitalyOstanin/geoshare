import assert from 'node:assert/strict';
import { test } from 'node:test';

import { foundText } from '../public/lib/found.js';
import { WORDS } from '../public/lib/words.js';

// The English name of a source is close to the entry the phone writes, so
// comparing the two would pass whether the dictionary holds it or not: what is
// checked is that the name came from the dictionary, which is where a
// translation of it would be.
test('every source a phone writes is named by the dictionary', () => {
    const sources = {
        Satellites: 'foundSatellites',
        Assistance: 'foundAssistance',
        Unheard: 'foundUnheard',
        OtherRadios: 'foundOtherRadios',
        Pretended: 'foundPretended',
    };

    for (const [source, key] of Object.entries(sources)) {
        const named = WORDS[key];

        assert.ok(named, `${source} has no name in the dictionary`);
        assert.equal(foundText(source), named);
    }
});

test('a position from before the source was written says so', () => {
    assert.equal(foundText(null), WORDS.unknown);
    assert.equal(foundText(undefined), WORDS.unknown);
});

// Unlike a mode, which is shown as it came: the entries are English
// identifiers, and a Russian card showing one would be saying in another
// language something it has a word for.
test('a source this build has never heard of is shown as unknown', () => {
    assert.equal(foundText('Lighthouse'), WORDS.unknown);
});
