// How a device was collecting its position, said in words a member can read.

import { WORDS } from './words.js';

/**
 * What each way of collecting is called on a card.
 *
 * The document carries the entry the device was in — `Standby`, `Viewing` — and
 * the card gives that mode its name rather than a sentence about it. The five
 * differ by what they ask the receiver for and how often they send, which is a
 * table to be read once against itself in the help rather than a phrase to be
 * read again on every card. The names live in the one dictionary the page
 * translates, and the entry the device wrote stays the key. The Android client
 * keeps the same names, so one card reads the same on both.
 */
const MODE_WORDS = {
    Idle: WORDS.modeIdle,
    Standby: WORDS.modeStandby,
    Moving: WORDS.modeMoving,
    Viewing: WORDS.modeViewing,
    Night: WORDS.modeNight,
};

/** The words for a mode, or the mode itself when it is one this build does not know. */
export function modeText(mode) {
    if (!mode) {
        return WORDS.unknown;
    }

    return MODE_WORDS[mode] ?? mode;
}
