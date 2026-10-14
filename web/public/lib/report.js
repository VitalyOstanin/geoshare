// A problem the viewer reports from the page: an issue on GitHub carrying what
// they wrote and the tail of the page's log, the way the application does it.
// Nothing is sent from here; the address opens GitHub's own form, filled in,
// and the viewer submits it there or does not.

import { NEW_ISSUE } from './unexpected.js';
import { PAGE_VERSION } from './version.js';

/** The longest address the issue page is handed; browsers and servers refuse much past this. */
const ISSUE_URL_LIMIT = 6000;

const DEFAULT_TITLE = 'Problem report';
const TITLE_LIMIT = 100;
const DESCRIPTION_LIMIT = 600;
export const LEFT_OUT = '… earlier lines left out; the whole log can be copied from the page';

/** The first line of what the viewer wrote, or a stock title when they wrote nothing. */
export function issueTitle(description) {
    const first = description
        .split('\n')
        .map((line) => line.trim())
        .find((line) => line !== '');

    return first ? first.slice(0, TITLE_LIMIT) : DEFAULT_TITLE;
}

function body(description, browser, tail) {
    return [
        ...(description ? [description, ''] : []),
        `Page version: ${PAGE_VERSION}`,
        `Browser: ${browser}`,
        '',
        '```',
        tail,
        '```',
        '',
    ].join('\n');
}

/**
 * The issue about to be opened: `url` to open, and `tail`, the part of [log]
 * it carries, so that the window can show exactly what goes; `leftOut` says
 * whether lines were dropped. [log] is already cleaned. Lines are dropped from
 * the start, whole ones, until the address is no longer than [limit]; the
 * shortest answer is found by bisection, as in the application.
 */
export function buildIssue(description, browser, log, limit = ISSUE_URL_LIMIT) {
    const written = description.trim().slice(0, DESCRIPTION_LIMIT);
    const all = log.filter((line) => line !== '');

    // Encoding never makes a character shorter: lines that alone pass the
    // limit cannot be part of the answer, and the bisection starts past them.
    let kept = 0;
    let fitting = 0;
    for (const line of [...all].reverse()) {
        kept += line.length + 1;
        if (kept > limit) {
            break;
        }
        fitting += 1;
    }

    const cutBefore = fitting < all.length;
    const lines = all.slice(all.length - fitting);
    const draft = (drop) => {
        const dropped = cutBefore || drop > 0;
        const tail = [...(dropped ? [LEFT_OUT] : []), ...lines.slice(drop)].join('\n');
        const query = new URLSearchParams({
            title: issueTitle(written),
            body: body(written, browser, tail),
        });

        // URLSearchParams writes a space as a plus, which some readers of an
        // address leave as a plus; %20 is read as a space by all of them.
        return { url: `${NEW_ISSUE}?${query.toString().replaceAll('+', '%20')}`, tail, leftOut: dropped };
    };

    let low = 0;
    let high = lines.length;
    while (low < high) {
        const middle = Math.floor((low + high) / 2);

        if (draft(middle).url.length <= limit) {
            high = middle;
        } else {
            low = middle + 1;
        }
    }

    return draft(low);
}
