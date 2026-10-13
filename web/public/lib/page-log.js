// What the page has had to say while open: its warnings and errors, and the
// failures nothing caught, kept in memory so that a problem can be reported
// with them. The page writes no file, so this is all the log it has; it is
// gone when the tab is closed, and it leaves the page only in a report the
// viewer opens, cleaned.

import { scrubbed } from './fault.js';

/** The lines kept: enough for the minutes before a report, small in memory. */
export const PAGE_LOG_LINES = 200;

const lines = [];

/** Text for one value written to the console, an error by its name and message. */
function said(value) {
    if (value instanceof Error) {
        return `${value.name}: ${value.message}`;
    }
    if (typeof value === 'object' && value !== null) {
        try {
            return JSON.stringify(value);
        } catch {
            return String(value);
        }
    }

    return String(value);
}

/** Adds one line under [level], stamped with the time of day. */
export function noteInLog(level, values, now = new Date()) {
    const time = now.toTimeString().slice(0, 8);

    lines.push(`${time} ${level} ${values.map(said).join(' ')}`);
    if (lines.length > PAGE_LOG_LINES) {
        lines.splice(0, lines.length - PAGE_LOG_LINES);
    }
}

/** The log as it stands, cleaned of anything private, one line per entry. */
export function pageLog(origin) {
    return lines.map((line) => scrubbed(line, origin));
}

/**
 * Keeps a copy of what is written to the console as a warning or an error,
 * the page's own and its libraries'. The console still gets every line.
 */
export function keepConsole(target = globalThis.console) {
    for (const level of ['warn', 'error']) {
        const write = target[level].bind(target);

        target[level] = (...values) => {
            noteInLog(level, values);
            write(...values);
        };
    }
}
