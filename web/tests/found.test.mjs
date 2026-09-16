import assert from 'node:assert/strict';
import { test } from 'node:test';

import { foundText, leftOutText } from '../public/lib/found.js';
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

// The same check for the other list of words a phone writes: why an answer was
// left out. A reason missing from the dictionary would be shown as unknown on
// every card, and nothing else would go wrong to say so.
test('every reason a phone writes is named by the dictionary', () => {
    const reasons = {
        Drifted: 'leftOutDrifted',
        Hurried: 'leftOutHurried',
        Contradicted: 'leftOutContradicted',
        Width: 'leftOutWidth',
        Speed: 'leftOutSpeed',
        Contradiction: 'leftOutContradiction',
        FalsePlace: 'leftOutFalsePlace',
        Unmoved: 'leftOutUnmoved',
        Circling: 'leftOutCircling',
        OneBand: 'leftOutOneBand',
        OneTransmitter: 'leftOutOneTransmitter',
        TimeApart: 'leftOutTimeApart',
        Repeating: 'leftOutRepeating',
        Settling: 'leftOutSettling',
        Source: 'leftOutBySource',
    };

    for (const [reason, key] of Object.entries(reasons)) {
        const named = WORDS[key];

        assert.ok(named, `${reason} has no name in the dictionary`);
        assert.equal(leftOutText(reason), named);
    }
});

test('a position with nothing left out names no reason', () => {
    assert.equal(leftOutText(null), WORDS.unknown);
    assert.equal(leftOutText(undefined), WORDS.unknown);
});

test('a reason this build has never heard of is shown as unknown', () => {
    assert.equal(leftOutText('Sunspots'), WORDS.unknown);
});

// Which of the five was switched off is the whole of what the row is opened
// for, so the phone writes the switch beside the fact.
test('a reason names the switch that was turned off', () => {
    assert.equal(leftOutText('Source:Satellites'), WORDS.leftOutBySourceNamed(WORDS.foundSatellites));
    assert.equal(leftOutText('Source:OtherRadios'), WORDS.leftOutBySourceNamed(WORDS.foundOtherRadios));
});

// The older form, and a switch from a build newer than this one: both say a
// source was switched off without naming it.
test('a switch this build cannot name still says a source was switched off', () => {
    assert.equal(leftOutText('Source'), WORDS.leftOutBySource);
    assert.equal(leftOutText('Source:Lighthouse'), WORDS.leftOutBySource);
});
