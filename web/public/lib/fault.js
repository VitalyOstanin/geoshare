// A failure nothing on the page caught, written down so that it can be shown
// and passed on: what kind of event it was, what it said, where it came from,
// and the page and browser it happened in. Nothing private goes into it.
//
// The text is read by whoever the viewer passes it to and, through an issue,
// by anybody at all, so it is cleaned before anything else sees it: e-mail
// addresses, the ids in document paths, the group's site and project, and any
// value the page has been told is private (keepPrivate) become placeholders.

/** Collections whose document ids are account, group or device ids. */
const ID_COLLECTIONS =
    'groups|members|grants|positions|watch|invitations|joins|falsePlaces|settings|devices|labels|searches|sirens|resets';

const EMAIL = /[\w.+-]+@[\w-]+(?:\.[\w-]+)+/g;
const DOCUMENT_ID = new RegExp(`\\b(${ID_COLLECTIONS})/([^/\\s'"\`,)]+)`, 'g');
const PROJECT = /\bprojects\/[^/\s'"`,)]+/g;
const SITE = /\bhttps?:\/\/[\w-]+\.(?:web\.app|firebaseapp\.com)\b/g;
// An account id is 28 letters and digits; nothing else on the page is that long
// without a break.
const LONG_ID = /\b[A-Za-z0-9]{20,}\b/g;

const privates = new Set();

/** Values that must not leave the page in a report: names, addresses. */
export function keepPrivate(...values) {
    for (const value of values) {
        if (typeof value === 'string' && value.trim().length >= 2) {
            privates.add(value.trim());
        }
    }
}

/** [text] with everything private in it replaced by a placeholder. */
export function scrubbed(text, origin = globalThis.location?.origin) {
    let clean = String(text);

    // The page's own files, named from the root of the site rather than by an
    // address that is the group's site.
    if (origin) {
        clean = clean.split(`${origin}/`).join('');
    }
    for (const value of [...privates].sort((a, b) => b.length - a.length)) {
        clean = clean.split(value).join('‹private›');
    }

    return clean
        .replace(EMAIL, '‹email›')
        .replace(SITE, '‹site›')
        .replace(PROJECT, 'projects/‹project›')
        .replace(DOCUMENT_ID, '$1/‹id›')
        .replace(LONG_ID, '‹id›');
}

/** The first of the page's own files a stack names, as `file:line`. */
export function stackPlaceOf(stack) {
    const match = /((?:lib|page)\/[\w./-]+\.js|\b[\w-]+\.js):(\d+)(?::\d+)?/.exec(String(stack ?? ''));

    return match ? `${match[1]}:${match[2]}` : null;
}

/** The browser and the system it runs on, from the user agent string. */
export function browserOf(agent) {
    const browsers = [
        [/Edg\/(\d+)/, 'Edge'],
        [/Firefox\/(\d+)/, 'Firefox'],
        [/Chrome\/(\d+)/, 'Chrome'],
        [/Version\/(\d+)[\d.]* .*Safari/, 'Safari'],
    ];
    const systems = [
        [/Android/, 'Android'],
        [/iPhone|iPad/, 'iOS'],
        [/Windows/, 'Windows'],
        [/Mac OS X/, 'macOS'],
        [/Linux/, 'Linux'],
    ];
    const browser = browsers
        .map(([pattern, name]) => [pattern.exec(agent), name])
        .find(([match]) => match);
    const system = systems.find(([pattern]) => pattern.test(agent));

    return [browser ? `${browser[1]} ${browser[0][1]}` : 'unknown browser', system ? system[1] : 'unknown system'];
}

/**
 * What a failure caught by the page's own listeners says, cleaned: `kind` is
 * `rejection` or `exception`; `reason` is what was thrown or rejected with;
 * `place` is the file and line an `error` event carries, when it does.
 */
export function faultOf({ kind, reason, place = null }, origin) {
    const message =
        reason instanceof Error ? `${reason.name}: ${reason.message}` : String(reason ?? 'no reason given');

    return {
        kind,
        message: scrubbed(message, origin),
        place: scrubbed(place ?? stackPlaceOf(reason?.stack) ?? '', origin) || null,
    };
}
