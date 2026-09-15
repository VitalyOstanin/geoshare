// How a phone arrived at its position, said in words a member can read.

import { WORDS } from './words.js';

/**
 * What each way of arriving at a position is called on a card.
 *
 * The document carries the word the phone wrote — `Satellites`, `Assistance` —
 * and the card names it in the language the page is speaking. The name of the
 * provider is not what is written there and not what is shown: an answer
 * arrives under the name of the satellites having been sent over the network
 * by an assistance service, and a member shown `gps` for that would be told
 * the opposite of what happened. The Android client keeps the same names, so
 * one card reads the same on both.
 */
const FOUND_WORDS = {
    Satellites: WORDS.foundSatellites,
    Assistance: WORDS.foundAssistance,
    Unheard: WORDS.foundUnheard,
    OtherRadios: WORDS.foundOtherRadios,
    Pretended: WORDS.foundPretended,
    Unsaid: WORDS.foundUnsaid,
};

/**
 * The words for a source, or the unknown for anything this build cannot name.
 *
 * Unknown rather than the word as it came, which is what a mode does: the
 * entries are English identifiers, and a Russian card showing one would be
 * showing a word of another language to say something it has a word for.
 */
export function foundText(found) {
    if (!found) {
        return WORDS.unknown;
    }

    return FOUND_WORDS[found] ?? WORDS.unknown;
}

/**
 * What each reason for leaving an answer out is called on a card.
 *
 * The same arrangement as the sources above: the document carries the word the
 * phone wrote — an entry of `JumpReason`, or `Source` for an answer whose
 * source the member switched off — and each client says it in its own
 * language. The Android client keeps the same names.
 */
const LEFT_OUT_WORDS = {
    Drifted: WORDS.leftOutDrifted,
    Contradicted: WORDS.leftOutContradicted,
    Width: WORDS.leftOutWidth,
    Speed: WORDS.leftOutSpeed,
    Contradiction: WORDS.leftOutContradiction,
    FalsePlace: WORDS.leftOutFalsePlace,
    Unmoved: WORDS.leftOutUnmoved,
    Circling: WORDS.leftOutCircling,
    OneBand: WORDS.leftOutOneBand,
    Repeating: WORDS.leftOutRepeating,
    Source: WORDS.leftOutBySource,
};

/** The words for a reason, or the unknown for anything this build cannot name. */
export function leftOutText(reason) {
    if (!reason) {
        return WORDS.unknown;
    }

    return LEFT_OUT_WORDS[reason] ?? WORDS.unknown;
}
