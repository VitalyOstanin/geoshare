// The window both helps open: a title, a line of introduction and a table.

import { outsideTheBox } from './outside-the-box.js';
import { WORDS } from './words.js';

/**
 * Opens a help window and gives back the dialog it opened.
 *
 * [headings] are the two column titles; each of [rows] is a name, read as the
 * row's own heading, and the two cells beside it.
 *
 * Built each time rather than kept in the markup: it is read once in a while,
 * and a page that carries it from the start would have it to translate and to
 * keep hidden on every screen it is not wanted on.
 */
export function openHelpTable({ title, intro, headings, rows }, document_ = document) {
    const dialog = document_.createElement('dialog');

    dialog.className = 'mode-help';

    const heading = document_.createElement('h2');

    heading.textContent = title;

    const introduction = document_.createElement('p');

    introduction.textContent = intro;

    const table = document_.createElement('table');
    const head = document_.createElement('tr');

    for (const text of ['', ...headings]) {
        const cell = document_.createElement('th');

        cell.textContent = text;
        head.append(cell);
    }

    table.append(head);

    for (const row of rows) {
        const line = document_.createElement('tr');
        const name = document_.createElement('th');

        name.className = 'mode-help-name';
        name.textContent = row.name;
        line.append(name);

        for (const text of row.cells) {
            const cell = document_.createElement('td');

            cell.textContent = text;
            line.append(cell);
        }

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
    close.textContent = '×';
    close.setAttribute('aria-label', WORDS.close);
    close.addEventListener('click', dismiss);

    // The escape key asks to cancel before anything is closed, which is the
    // one chance to hear about it without the event above.
    dialog.addEventListener('cancel', dismiss);

    // A click on the dark behind the window closes it, the way a tap beside a
    // sheet does on a device. The dark is not an element of its own -- it is
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

    dialog.append(close, heading, introduction, table);
    document_.body.append(dialog);
    dialog.showModal();

    return dialog;
}
