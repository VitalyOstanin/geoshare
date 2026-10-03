// What each reason for rejecting a position means.

import { leftOutText } from './found.js';
import { openHelpTable } from './help-table.js';
import { WORDS } from './words.js';

/**
 * Every reason a position can be rejected for, in the order the help reads
 * them: by how much the rule had to look at before it could speak.
 *
 * Not the order the rules happen to be written in. A member opening this has
 * just read one reason on a card, and what places it among the others is what
 * it rests on — the answer alone, then the answer against the one before it,
 * then the phone's own sensors, then the other radios, then the shape of a
 * whole track, then the sky itself. The switched-off source comes last
 * because it is not a fault at all. The Android client keeps the same order.
 */
const HELP_ORDER = [
    'Width',
    'Speed',
    'Contradiction',
    'FalsePlace',
    'Unmoved',
    'Drifted',
    'Hurried',
    'Contradicted',
    'Circling',
    'Repeating',
    'OneBand',
    'OneTransmitter',
    'TimeApart',
    'OffOrbit',
    'Settling',
    'Source',
];

/** One row of the help: the name a card shows, and the two things it means. */
export function reasonHelpRows() {
    return HELP_ORDER.map((reason) => ({
        reason,
        name: leftOutText(reason),
        sign: WORDS[`reasonSign${reason}`],
        means: WORDS[`reasonMeans${reason}`],
    }));
}

/** Opens the help, and gives back the dialog it opened, as the mode help does. */
export function openReasonHelp(document_ = document) {
    return openHelpTable({
        title: WORDS.reasonHelpTitle,
        intro: WORDS.reasonHelpIntro,
        headings: [WORDS.reasonHelpSign, WORDS.reasonHelpMeans],
        rows: reasonHelpRows().map((row) => ({ name: row.name, cells: [row.sign, row.means] })),
    }, document_);
}
