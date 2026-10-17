import { test } from 'node:test';
import assert from 'node:assert/strict';
import { countLines, reachSummary } from './reach-summary.mjs';

test('countLines ignores the final newline and counts an empty text as none', () => {
    assert.equal(countLines(''), 0);
    assert.equal(countLines('a'), 1);
    assert.equal(countLines('a\n'), 1);
    assert.equal(countLines('a\nb\n'), 2);
    assert.equal(countLines('a\n\nb'), 3);
});

test('reachSummary gives scripts, lines and the share of the whole page', () => {
    assert.equal(
        reachSummary({ total: 10, unreachedLines: [100, 50], allLines: 600 }),
        '2 of 10 scripts (150 lines, 25 % of the page\'s code) are not loaded by any test',
    );
});

test('reachSummary copes with nothing unreached and with an empty page', () => {
    assert.equal(
        reachSummary({ total: 3, unreachedLines: [], allLines: 90 }),
        '0 of 3 scripts (0 lines, 0 % of the page\'s code) are not loaded by any test',
    );
    assert.equal(
        reachSummary({ total: 0, unreachedLines: [], allLines: 0 }),
        '0 of 0 scripts (0 lines, 0 % of the page\'s code) are not loaded by any test',
    );
});
