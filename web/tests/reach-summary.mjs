// The line printed at the end of the coverage run about the scripts no test
// loads. Node's table leaves them out, so the 94 % describes only the rest;
// this says how much of the page that rest leaves aside. Pure, so a test can
// pin the wording and the counting without reading the page's files.

/** Number of lines in a script's text; a trailing newline does not add one. */
export function countLines(text) {
    if (text === '') {
        return 0;
    }

    const lines = text.split('\n').length;

    return text.endsWith('\n') ? lines - 1 : lines;
}

/**
 * One line: how many of the page's scripts, and how many of their lines, no
 * test loads. `total` is the count of all scripts; `unreachedLines` is the
 * line count of each script that is not loaded; `allLines` is the line count
 * of every script, for the share of the page's code.
 */
export function reachSummary({ total, unreachedLines, allLines }) {
    const lines = unreachedLines.reduce((sum, n) => sum + n, 0);
    const share = allLines > 0 ? Math.round((lines * 100) / allLines) : 0;

    return `${unreachedLines.length} of ${total} scripts (${lines} lines, ${share} % of`
        + ' the page\'s code) are not loaded by any test';
}
