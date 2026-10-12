// How a device arrived at its position, said in words a member can read.

import { WORDS } from './words.js';

/**
 * What each way of arriving at a position is called on a card.
 *
 * The document carries the word the device wrote — `Satellites`, `Assistance` —
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
 * device wrote — an entry of `JumpReason`, or `Source` for an answer whose
 * source the member switched off — and each client says it in its own
 * language. The Android client keeps the same names.
 */
const LEFT_OUT_WORDS = {
    Drifted: WORDS.leftOutDrifted,
    Hurried: WORDS.leftOutHurried,
    Contradicted: WORDS.leftOutContradicted,
    Width: WORDS.leftOutWidth,
    Speed: WORDS.leftOutSpeed,
    Contradiction: WORDS.leftOutContradiction,
    FalsePlace: WORDS.leftOutFalsePlace,
    Unmoved: WORDS.leftOutUnmoved,
    Circling: WORDS.leftOutCircling,
    OneBand: WORDS.leftOutOneBand,
    OneTransmitter: WORDS.leftOutOneTransmitter,
    TimeApart: WORDS.leftOutTimeApart,
    OffOrbit: WORDS.leftOutOffOrbit,
    Repeating: WORDS.leftOutRepeating,
    Settling: WORDS.leftOutSettling,
    Source: WORDS.leftOutBySource,
};

/**
 * What each switch is called, for a reason that names the one switched off.
 *
 * The switches are not the sources one for one: the two ways of arriving at a
 * place from the towers and the Wi-Fi are one switch, and the card is where
 * the difference between them is told.
 */
const SWITCH_WORDS = {
    Satellites: WORDS.foundSatellites,
    OtherRadios: WORDS.foundOtherRadios,
    Unheard: WORDS.foundUnheard,
    Pretended: WORDS.foundPretended,
    Unsaid: WORDS.foundUnsaid,
};

/**
 * The words for a reason, or the unknown for anything this build cannot name.
 *
 * A source the member switched off arrives as `Source:Satellites`, and the
 * switch is named: which of the five it was is the whole of what the member
 * opens the card for. A build that wrote the fact without the switch, and a
 * switch this build has never heard of, both come out as the plain sentence.
 */
export function leftOutText(reason) {
    if (!reason) {
        return WORDS.unknown;
    }

    if (reason.startsWith('Source:')) {
        const named = SWITCH_WORDS[reason.slice('Source:'.length)];

        return named ? WORDS.leftOutBySourceNamed(named) : WORDS.leftOutBySource;
    }

    return LEFT_OUT_WORDS[reason] ?? WORDS.unknown;
}
