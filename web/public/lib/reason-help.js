// What each reason for rejecting a position means.

import { leftOutText } from './found.js';
import { outsideTheBox } from './mode-help.js';
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

/**
 * Opens the help, and gives back the dialog it opened.
 *
 * Built each time rather than kept in the markup, as the mode help is: it is
 * read once in a while, and a page carrying it from the start would have it
 * to translate and to keep hidden on every screen it is not wanted on.
 */
export function openReasonHelp(document_ = document) {
    const dialog = document_.createElement('dialog');

    dialog.className = 'mode-help';

    const title = document_.createElement('h2');

    title.textContent = WORDS.reasonHelpTitle;

    const intro = document_.createElement('p');

    intro.textContent = WORDS.reasonHelpIntro;

    const table = document_.createElement('table');
    const head = document_.createElement('tr');

    for (const heading of ['', WORDS.reasonHelpSign, WORDS.reasonHelpMeans]) {
        const cell = document_.createElement('th');

        cell.textContent = heading;
        head.append(cell);
    }

    table.append(head);

    for (const row of reasonHelpRows()) {
        const line = document_.createElement('tr');
        const name = document_.createElement('th');

        name.className = 'mode-help-name';
        name.textContent = row.name;

        const sign = document_.createElement('td');

        sign.textContent = row.sign;

        const means = document_.createElement('td');

        means.textContent = row.means;

        line.append(name, sign, means);
        table.append(line);
    }

    // Nothing of the dialog outlives its closing, for the reason the mode
    // help gives: it is rebuilt on the next opening, and a page that kept
    // them would grow one for every reading.
    const dismiss = () => {
        dialog.close();
        dialog.remove();
    };

    const close = document_.createElement('button');

    close.className = 'close-cross';
    close.type = 'button';
    close.textContent = '×';
    close.setAttribute('aria-label', WORDS.close);
    close.addEventListener('click', dismiss);

    dialog.addEventListener('cancel', dismiss);

    dialog.addEventListener('click', (event) => {
        if (event.target !== dialog) {
            return;
        }

        if (outsideTheBox(dialog.getBoundingClientRect(), event)) {
            dismiss();
        }
    });

    dialog.append(close, title, intro, table);
    document_.body.append(dialog);
    dialog.showModal();

    return dialog;
}
