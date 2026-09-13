// How a phone was collecting its position, said in words a member can read.

import { WORDS } from './words.js';

/**
 * What each way of collecting is called on a card.
 *
 * The document carries the name of the enumeration entry the phone was in —
 * `Standby`, `Viewing` — which says nothing to whoever opens the card: "Idle"
 * does not explain that the phone asked for nothing because it had not left
 * the place it was. The phrases are repeated in the Android client so that one
 * card reads the same on both.
 */
const MODE_WORDS = {
    Idle: 'staying put',
    Standby: 'checking now and then',
    Moving: 'on the move',
    Viewing: 'somebody is looking',
    Night: 'night, checking rarely',
};

/** The words for a mode, or the mode itself when it is one this build does not know. */
export function modeText(mode) {
    if (!mode) {
        return WORDS.unknown;
    }

    return MODE_WORDS[mode] ?? mode;
}
