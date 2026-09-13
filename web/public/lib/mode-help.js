// What the five ways of collecting mean, read against each other.

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

/**
 * Opens the help, and gives back the dialog it opened.
 *
 * Built each time rather than kept in the markup: it is read once in a while,
 * and a page that carries it from the start would have it to translate and to
 * keep hidden on every screen it is not wanted on.
 */
export function openModeHelp(document_ = document) {
    const dialog = document_.createElement('dialog');

    dialog.className = 'mode-help';

    const title = document_.createElement('h2');

    title.textContent = WORDS.modeHelpTitle;

    const intro = document_.createElement('p');

    intro.textContent = WORDS.modeHelpIntro;

    const table = document_.createElement('table');
    const head = document_.createElement('tr');

    for (const heading of ['', WORDS.modeHelpWhen, WORDS.modeHelpAsks]) {
        const cell = document_.createElement('th');

        cell.textContent = heading;
        head.append(cell);
    }

    table.append(head);

    for (const row of modeHelpRows()) {
        const line = document_.createElement('tr');
        const name = document_.createElement('th');

        name.className = 'mode-help-name';
        name.textContent = row.name;

        const when = document_.createElement('td');

        when.textContent = row.when;

        const asks = document_.createElement('td');

        asks.textContent = row.asks;

        line.append(name, when, asks);
        table.append(line);
    }

    // Nothing of the dialog outlives its closing: it is rebuilt on the next
    // opening, and a page that kept them would grow one for every reading.
    // Taken down here rather than on the dialog's own closing event, which is
    // delivered in the browser's own time and was seen not to arrive at all in
    // a window nothing was drawing.
    const dismiss = () => {
        dialog.close();
        dialog.remove();
    };

    // The same cross in the same corner as a member's card: two windows over
    // one map, closed by one gesture. The word stays as the label a screen
    // reader says, since a multiplication sign read aloud is not "close".
    const close = document_.createElement('button');

    close.className = 'close-cross';
    close.type = 'button';
    close.textContent = '\u00d7';
    close.setAttribute('aria-label', WORDS.close);
    close.addEventListener('click', dismiss);

    // The escape key asks to cancel before anything is closed, which is the
    // one chance to hear about it without the event above.
    dialog.addEventListener('cancel', dismiss);

    // A click on the dark behind the window closes it, the way a tap beside a
    // sheet does on a phone. The dark is not an element of its own -- it is
    // the dialog's own backdrop -- so such a click arrives at the dialog
    // itself, as does a click on the dialog's padding; the point is compared
    // with the box to tell the two apart.
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

/**
 * Whether a point falls outside a box.
 *
 * Kept apart from the dialog so that the arithmetic can be read and tested on
 * its own: a click on the backdrop and a click on the padding of the window
 * are the same event with the same target, and only the coordinates tell them
 * apart. The edge belongs to the box, because a click on the border of a
 * window is a click on that window.
 */
export function outsideTheBox(box, point) {
    return point.clientX < box.left ||
        point.clientX > box.right ||
        point.clientY < box.top ||
        point.clientY > box.bottom;
}
