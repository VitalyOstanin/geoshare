// The language the page speaks: the viewer's answer, or the one their browser
// asks for when they have not given one.
//
// The answer belongs to the browser rather than to the account, the same way
// the theme does: one member reads the map on a device and on a laptop, and
// those two are not always set up in the same language.

/** The answers, in the order their buttons stand in. */
export const LANGUAGES = ['ru', 'en', 'system'];

/** The answer to fall back on: whatever the browser asks for. */
export const DEFAULT_LANGUAGE = 'system';

/** The languages there are words for, and the one to speak to everybody else. */
export const SPOKEN = ['en', 'ru'];
export const FALLBACK_LANGUAGE = 'en';

/** Where the answer is kept, next to the theme. */
export const LANGUAGE_KEY = 'geoshare.language';

/**
 * The answer as it stands.
 *
 * A browser told to keep no site data throws on the attempt rather than
 * answering with nothing, and a page that cannot remember a language should
 * still be written in one.
 */
export function readLanguage(store = globalThis.localStorage) {
    try {
        const stored = store.getItem(LANGUAGE_KEY);

        return LANGUAGES.includes(stored) ? stored : DEFAULT_LANGUAGE;
    } catch {
        return DEFAULT_LANGUAGE;
    }
}

/** Remembers the answer, or carries on without remembering it. */
export function writeLanguage(language, store = globalThis.localStorage) {
    try {
        store.setItem(LANGUAGE_KEY, language);
    } catch {
        // Nothing to say to the viewer: the page they asked for is the one
        // they get, it is only the next visit that forgets.
    }
}

/**
 * The language to write in: the answer, or the first one the browser asks for
 * that there are words for.
 *
 * A browser asks in order — "ru-RU, ru, en" — and only the first part of a tag
 * is a language; Russian is Russian whether it comes from Moscow or from a
 * keyboard layout.
 */
export function spokenLanguage(language = readLanguage(), asked = askedFor()) {
    if (SPOKEN.includes(language)) {
        return language;
    }

    for (const tag of asked) {
        const base = String(tag).toLowerCase().split('-')[0];

        if (SPOKEN.includes(base)) {
            return base;
        }
    }

    return FALLBACK_LANGUAGE;
}

/** What the browser asks for, where there is a browser to ask. */
function askedFor() {
    try {
        return globalThis.navigator.languages ?? [globalThis.navigator.language];
    } catch {
        return [];
    }
}
