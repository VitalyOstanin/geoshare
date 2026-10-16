// What a group is called, on both clients' screens.

import { memberName } from './members.js';
import { trimText } from './text.js';
import { WORDS } from './words.js';

/**
 * The caption a group is shown under: its own name, or — until an
 * administrator gives it one — the founder's, in the words each language
 * turns that into (ADR-0016, ADR-0019).
 *
 * The name is trimmed again here: `founding()` and `renaming()` only check the
 * trimmed length, so the document could in principle carry one with spaces at
 * an edge, and a caption with them would look like a mistake on screen.
 */
export function groupCaption(roster, profiles, ownMemberId) {
    const trimmed = typeof roster.name === 'string' ? trimText(roster.name) : '';

    if (trimmed !== '') {
        return trimmed;
    }

    return WORDS.groupUnnamed(
        memberName(profiles?.get(roster.founder), roster.founder, roster.founder === ownMemberId),
    );
}
