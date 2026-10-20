// Colours read off what they are drawn on, by the WCAG formula: the words of
// a search on the panel at night, and the sign in a circle of the setup
// steps.
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';

const read = (name) => readFile(new URL(`../public/styles/${name}`, import.meta.url), 'utf8');

/** The value a stylesheet gives a custom property inside the block that starts with [opening]. */
function tokenIn(css, opening, name) {
    const start = css.indexOf(opening);
    assert.ok(start >= 0, `no block ${opening}`);
    const block = css.slice(start, css.indexOf('}', start));
    const found = block.match(new RegExp(`${name}:\\s*([^;]+);`));
    assert.ok(found, `${name} is not set in ${opening}`);

    return found[1].trim();
}

/** Red, green, blue and alpha out of #rrggbb or rgb()/rgba(). */
function channels(colour) {
    const hex = colour.match(/^#([0-9a-f]{6})$/i);

    if (hex) {
        const value = Number.parseInt(hex[1], 16);

        return [value >> 16, (value >> 8) & 0xff, value & 0xff, 1];
    }

    const parts = colour.match(/^rgba?\(([^)]+)\)$/);
    assert.ok(parts, `cannot read ${colour}`);
    const [r, g, b, a = 1] = parts[1].split(',').map(Number);

    return [r, g, b, a];
}

/** [top] laid over an opaque [under]. */
function over(top, under) {
    const [r, g, b, a] = channels(top);

    return [r, g, b].map((value, i) => value * a + under[i] * (1 - a));
}

function luminance(rgb) {
    const [r, g, b] = rgb.map((value) => {
        const share = value / 255;

        return share <= 0.04045 ? share / 12.92 : ((share + 0.055) / 1.055) ** 2.4;
    });

    return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

function contrast(one, other) {
    const [light, dark] = [luminance(one), luminance(other)].sort((x, y) => y - x);

    return (light + 0.05) / (dark + 0.05);
}

const NIGHT = ':root[data-theme="dark"]';
const DAY = ':root {';

test('the words of a search keep their contrast on the panel by day and by night', async () => {
    const [base, search] = await Promise.all([read('base.css'), read('search.css')]);

    for (const [when, words, panel] of [
        ['day', tokenIn(search, DAY, '--alarm-text'), tokenIn(base, DAY, '--surface')],
        ['night', tokenIn(search, NIGHT, '--alarm-text'), tokenIn(base, DAY, '--night-surface')],
    ]) {
        // The panel is all but opaque; laid over the darkest map it can sit on.
        const ground = over(panel, when === 'day' ? [255, 255, 255] : [0, 0, 0]);
        const ratio = contrast(over(words, ground), ground);

        assert.ok(ratio >= 4.5, `${when}: ${words} on the panel is ${ratio.toFixed(2)}:1`);
    }
});

test('the words of a search keep their contrast on the red tint of a sought member at night', async () => {
    const [base, search] = await Promise.all([read('base.css'), read('search.css')]);
    const panel = over(tokenIn(base, DAY, '--night-surface'), [0, 0, 0]);
    const tint = over(tokenIn(search, NIGHT, '--alarm-soft'), panel);
    const words = channels(tokenIn(search, NIGHT, '--alarm-text')).slice(0, 3);
    const ratio = contrast(words, tint);

    assert.ok(ratio >= 4.5, `the tint gives ${ratio.toFixed(2)}:1`);
});

test('the night red of the words is the same whether the system or the viewer chose the dark', async () => {
    const search = await read('search.css');

    assert.equal(
        tokenIn(search, ':root:not([data-theme="light"])', '--alarm-text'),
        tokenIn(search, NIGHT, '--alarm-text'),
    );
});

test('the sign in a finished or failed step keeps its contrast on its circle by day and by night', async () => {
    const [base, setup] = await Promise.all([read('base.css'), read('setup.css')]);

    for (const [status, fill, sign] of [
        ['done', 'charging', 'on-charging'],
        ['failed', 'error', 'on-error'],
    ]) {
        const rule = `.line.${status} .mark { background: var(--${fill}); color: var(--${sign}); }`;

        assert.ok(setup.includes(rule), `setup.css has no ${rule}`);

        for (const [when, circle, mark] of [
            ['day', tokenIn(base, DAY, `--${fill}`), tokenIn(base, DAY, `--${sign}`)],
            ['night', tokenIn(base, DAY, `--night-${fill}`), tokenIn(base, DAY, `--night-${sign}`)],
        ]) {
            const ratio = contrast(channels(mark).slice(0, 3), channels(circle).slice(0, 3));

            assert.ok(
                ratio >= 4.5,
                `${status} by ${when}: ${mark} on ${circle} is ${ratio.toFixed(2)}:1`,
            );
        }

        for (const opening of [':root:not([data-theme="light"])', NIGHT]) {
            assert.equal(tokenIn(base, opening, `--${sign}`), `var(--night-${sign})`);
        }
    }
});
