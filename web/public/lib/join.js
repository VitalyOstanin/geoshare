// What the /join page needs to work out before anybody has signed in: what a
// link carries, what it says about it, and where "open in the app" goes.
//
// Every function here takes what it needs as an argument rather than reading
// window.location or navigator itself, so the page's own script is the only
// place that touches the browser and this module can be run under node:test
// without one.

/** The package the application is published under, for the intent link. */
export const APP_PACKAGE = 'io.github.vitalyostanin.geoshare';

/** Where "download the app" leads, and the fallback Chrome opens with no app to catch it. */
export const RELEASES_URL = 'https://github.com/VitalyOstanin/geoshare/releases';

/** A caption from the link is shown as typed, but only up to this many characters. */
const CAPTION_LIMIT = 60;

function capCaption(value) {
    return value === null ? '' : value.trim().slice(0, CAPTION_LIMIT);
}

/**
 * The project a link is for, read off the host it was opened from.
 *
 * `tools/new-project.sh` calls a site the label before ".web.app" in the
 * address it deploys to, and that is the only name of the project this page
 * is allowed to show — nothing here ever names the project's owner. A host
 * that is not such an address, the emulator or a custom domain among them,
 * is shown whole rather than guessed at.
 */
export function siteFromHost(host) {
    const suffix = '.web.app';

    return host.endsWith(suffix) ? host.slice(0, -suffix.length) : host;
}

/**
 * The invitation half of a join link, or null for a link with none.
 *
 * The fragment never reaches a server — a static page has no other way to
 * carry it — and nothing here is trusted: it is what a member typed when
 * making the invitation, carried by whoever holds the link now. Whether the
 * code still lets somebody in is for the rules alone to say once the
 * application signs in; what is read here is shown as a caption and capped
 * at CAPTION_LIMIT so a stray link cannot turn the page into somebody
 * else's billboard.
 */
export function parseInvitation(hash) {
    const raw = hash.startsWith('#') ? hash.slice(1) : hash;

    if (raw === '') {
        return null;
    }

    const params = new URLSearchParams(raw);

    // Neither half of an invitation, only whatever a messenger appended: the
    // project owner's link, the same way the application reads it.
    if (!params.has('g') && !params.has('c')) {
        return null;
    }

    return {
        groupId: params.get('g') ?? '',
        code: params.get('c') ?? '',
        by: capCaption(params.get('by')),
        name: capCaption(params.get('n')),
        to: capCaption(params.get('to')),
    };
}

/** How much of an address's name shortenedAddress keeps. */
const SHORTENED_NAME_LENGTH = 4;

/**
 * An address as the page and the screens show it (ADR-0018): enough of the
 * name to tell one's own accounts apart, and the domain whole.
 * `ivan.petrov@example.com` becomes `ivan…@example.com`; a name of four
 * characters or fewer keeps only its first, so the rest is never shown in
 * full. The same as the application's ConnectLink.shortenedAddress.
 */
export function shortenedAddress(email) {
    const at = email.lastIndexOf('@');

    if (at <= 0) {
        return `${email.slice(0, 1)}…`;
    }

    const name = email.slice(0, at);
    const kept = name.length > SHORTENED_NAME_LENGTH ? SHORTENED_NAME_LENGTH : 1;

    return `${name.slice(0, kept)}…${email.slice(at)}`;
}

/**
 * The link an administrator sends (ADR-0017, ADR-0019): the site's `/join`
 * with the group, the code and the captions after the `#`, which never
 * reaches the server. The address goes in only shortened; the full one is
 * known to the rules alone (ADR-0018). A missing caption is left out rather
 * than written empty. Written the way the application writes it, so a link
 * reads the same whichever of the two made it.
 */
export function invitationLink({ site, groupId, code, by, name, email }) {
    const fields = [
        ['g', groupId],
        ['c', code],
        ['by', by === null || by === undefined ? '' : capCaption(by)],
        ['n', name === null || name === undefined ? '' : capCaption(name)],
        ['to', shortenedAddress(email)],
    ].filter(([, value]) => value !== '');

    return `https://${site}.web.app/join#` +
        fields.map(([key, value]) => `${key}=${encodeURIComponent(value)}`).join('&');
}

/**
 * What the page says about the link: a title, and a second line naming the
 * invited address where the link carries one.
 *
 * Both names on an invitation are optional — a member may have made the link
 * before the application learned to send them, or left the group unnamed —
 * so the title falls back a step at a time rather than showing a blank.
 */
export function joinCaption(words, invitation) {
    if (invitation === null) {
        return { title: words.joinTitle, invitedFor: null };
    }

    const { by, name, to } = invitation;
    let title;

    if (by && name) {
        title = words.joinInvited(by, name);
    } else if (name) {
        title = words.joinInvitedNoBy(name);
    } else if (by) {
        title = words.joinInvitedNoGroup(by);
    } else {
        title = words.joinInvitedBare;
    }

    return { title, invitedFor: to ? words.joinInvitedFor(to) : null };
}

/**
 * The intent:// link Chrome opens the application from.
 *
 * The invitation moves from the fragment into the query string, because the
 * `#Intent` marker takes the fragment's place in an intent URL; the
 * application accepts both forms. Nothing here is re-checked — it is passed
 * on exactly as the link carried it, the same way the rest of the page shows
 * it.
 */
export function buildIntentUrl(host, invitation, fallbackUrl = RELEASES_URL) {
    const params = new URLSearchParams();

    if (invitation !== null) {
        for (const [key, field] of [
            ['g', 'groupId'], ['c', 'code'], ['by', 'by'], ['n', 'name'], ['to', 'to'],
        ]) {
            if (invitation[field]) {
                params.set(key, invitation[field]);
            }
        }
    }

    const query = params.toString();
    const path = `/join${query ? `?${query}` : ''}`;

    return `intent://${host}${path}#Intent;scheme=https;package=${APP_PACKAGE};`
        + `S.browser_fallback_url=${encodeURIComponent(fallbackUrl)};end`;
}

/**
 * Whether the browser is Chrome on Android, the one place an intent link
 * does anything: everywhere else there is no application to hand the link
 * to, and offering the button anyway would open nothing and explain nothing.
 */
export function isAndroid(userAgent) {
    return /Android/u.test(userAgent ?? '');
}
