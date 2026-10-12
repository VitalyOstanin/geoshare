// What the page is painted in: the viewer's answer, or the system's when they
// have not given one.
//
// The answer belongs to the browser rather than to the account: the same
// member reads the map on a device in the evening and on a laptop by day, and
// those two screens do not want the same thing.

import { keep, readKept } from './kept.js';

/** The answers, in the order their buttons stand in. */
export const THEMES = ['light', 'dark', 'system'];

/** The answer to fall back on: whatever the system is set to. */
export const DEFAULT_THEME = 'system';

/**
 * Where the answer is kept.
 *
 * Repeated in index.html on purpose: the theme has to be on the element
 * before the page is first painted, and a module runs after that. A test
 * holds the two spellings together.
 */
export const THEME_KEY = 'geoshare.theme';

/**
 * The answer as it stands.
 *
 * A browser told to keep no site data throws on the attempt rather than
 * answering with nothing, and a page that cannot remember a theme should
 * still draw one.
 */
export function readTheme(store = globalThis.localStorage) {
    const stored = readKept(THEME_KEY, store);

    return THEMES.includes(stored) ? stored : DEFAULT_THEME;
}

/** Remembers the answer, or carries on without remembering it. */
export function writeTheme(theme, store = globalThis.localStorage) {
    keep(THEME_KEY, theme, store);
}

/**
 * States the answer on the root element, which is what the stylesheet reads.
 *
 * "System" is the absence of the attribute rather than a value of its own:
 * the media query is then the only thing deciding, which is what following
 * the system means.
 */
export function applyTheme(theme, root = document.documentElement) {
    if (theme === 'light' || theme === 'dark') {
        root.dataset.theme = theme;
    } else {
        delete root.dataset.theme;
    }
}
