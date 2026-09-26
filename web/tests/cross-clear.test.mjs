import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';

const stylesheet = () => readFile(new URL('../public/styles.css', import.meta.url), 'utf8');

/** How much space past the button the eye needs to read the gap as one. */
const VISIBLE_GAP = 0.75;

const rem = (styles, pattern) => {
    const found = styles.match(pattern);

    assert.ok(found, `the stylesheet does not say ${pattern}`);

    return Number(found[1]);
};

// The cross that closes a window over the map is laid over the corner of that
// window rather than beside its text, so a title with nothing to stop it runs
// underneath the cross. What stops it is a padding, and the padding has to be
// at least as wide as the part of the button that reaches into the text.
test('the text of a card stops before the cross that closes it', async () => {
    const styles = await stylesheet();
    const clear = rem(styles, /--cross-clear:\s*([\d.]+)rem/);
    const cross = /\.card-close,\s*\n\.close-cross \{[^}]*/.source;
    const inset = rem(styles, new RegExp(`${cross}right:\\s*([\\d.]+)rem`));
    const width = rem(styles, new RegExp(`${cross}width:\\s*([\\d.]+)rem`));
    const padding = rem(styles, /#card \{[^}]*padding:\s*[\d.]+rem\s+([\d.]+)rem/);
    const reach = inset + width - padding;

    assert.ok(clear >= reach, `${clear}rem of room leaves the text under ${reach}rem of button`);

    // Not touching is not the same as being clear of it. At a quarter of a rem
    // -- four pixels -- a name ending against the circle read as one run of
    // marks with the cross, which is the whole complaint this padding exists
    // to answer.
    assert.ok(
        clear >= reach + VISIBLE_GAP,
        `${clear}rem leaves only ${(clear - reach).toFixed(2)}rem between the text and the button`,
    );
});

// Every line that can run the width of such a window keeps the same distance:
// the button beside a member's name (the name and the model under it stop at
// the button), the heading of a group, the way back to it, and the title of
// the help.
test('each line that can reach the corner keeps that distance', async () => {
    const styles = await stylesheet();

    for (const selector of ['.detail-more', '.group h2', '.group-back',
        '.mode-help h2', '.admin h2']) {
        const rule = new RegExp(`\\${selector.replace(' ', ' ')} \\{[^}]*(?:padding|margin)-right:\\s*var\\(--cross-clear\\)`);

        assert.match(styles, rule, `${selector} does not keep clear of the cross`);
    }
});
