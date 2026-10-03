// What the five ways of collecting mean, read against each other.

import { openHelpTable } from './help-table.js';
import { modeText } from './modes.js';
import { WORDS } from './words.js';

/**
 * The modes in the order the help reads them: hardest working first.
 *
 * Not the order of the enumeration, which is the order they happen to be
 * written in. A member opening this has just seen one name on a card and wants
 * to know where it stands among the others, and "how much is this phone doing"
 * is the axis that answers it.
 */
const HELP_ORDER = ['Viewing', 'Moving', 'Standby', 'Night', 'Idle'];

/** One row of the help: the name a card shows, and the two things it means. */
export function modeHelpRows() {
    return HELP_ORDER.map((mode) => ({
        mode,
        name: modeText(mode),
        when: WORDS[`modeWhen${mode}`],
        asks: WORDS[`modeAsks${mode}`],
    }));
}

/** Opens the help, and gives back the dialog it opened. */
export function openModeHelp(document_ = document) {
    return openHelpTable({
        title: WORDS.modeHelpTitle,
        intro: WORDS.modeHelpIntro,
        headings: [WORDS.modeHelpWhen, WORDS.modeHelpAsks],
        rows: modeHelpRows().map((row) => ({ name: row.name, cells: [row.when, row.asks] })),
    }, document_);
}
