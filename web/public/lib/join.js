// What the /join page needs to work out before anybody has signed in: what a
// link carries, what it says about it, and where "open in the app" goes.
//
// Every function here takes what it needs as an argument rather than reading
// window.location or navigator itself, so the page's own script is the only
// place that touches the browser and this module can be run under node:test
// without one.

import { invitationFrom } from './membership.js';

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
 * `tools/project.sh` calls a site the label before ".web.app" in the
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

/**
 * Where a link found in pasted text ends, and what it carries after `/join`:
 * a query, a fragment, or both, either of which may hold the invitation.
 *
 * The query is read too, not only the fragment invitationLink writes: the
 * application's own deep link moves the fields there (see buildIntentUrl),
 * and a person forwarding that link — from a browser's address bar rather
 * than the page's own button — pastes the query form. Either Hosting host
 * is accepted, and `/join/` as well as `/join`, as the application does
 * (ConnectLink.parse).
 */
const PASTED_LINK = new RegExp(
    String.raw`https://([a-z0-9-]+)\.(?:web\.app|firebaseapp\.com)/join/?`
        + String.raw`(?:\?([^\s#]*))?(?:#([^\s]*))?`,
    'iu',
);

/** Punctuation a messenger tacks onto a link it autolinks, trimmed off the end. */
const TRAILING_PUNCTUATION = /[).,;:!?\]}'"<>]+$/u;

/**
 * An invitation read out of whatever was pasted into the "join" field
 * (ADR-0017, ADR-0019): a link found anywhere in the text, the older
 * "group.code" form kept for a link made before invitations were links at
 * all, or null when neither is there.
 *
 * A link rarely arrives alone — a forwarded message carries a messenger's own
 * signature around it — so PASTED_LINK looks for the address inside the text
 * rather than asking the whole field to be one. The invited address is not
 * capped a second time: the sender wrote nothing that parseInvitation did not
 * already trim and cut short.
 */
export function invitationTyped(text) {
    const found = PASTED_LINK.exec(text);

    if (found !== null) {
        const [, site, query, fragment] = found;
        // `||` rather than `??`: a link cut off right after its "#" has an
        // empty fragment, and the invitation is then in the query.
        const raw = (fragment || query || '').replace(TRAILING_PUNCTUATION, '');
        const invitation = parseInvitation(raw);

        return invitation === null ? null : { site, ...invitation };
    }

    const typed = invitationFrom(text);

    return typed === null
        ? null
        : { site: null, groupId: typed.group, code: typed.code, by: '', name: '', to: '' };
}

/**
 * Whether an invitation's link names a site other than the one the page is
 * running on, and is worth refusing for it.
 *
 * A host that does not end in ".web.app" — the emulator, a developer's own
 * domain — names no site the way siteFromHost reads one, so nothing here is
 * compared against it: this exists to keep one project's invitation out of
 * another project's page, not to turn away every host that is not a Firebase
 * Hosting address.
 */
export function isForeignSite(invitation, host) {
    return invitation.site !== null &&
        host.endsWith('.web.app') &&
        siteFromHost(host) !== invitation.site;
}

/**
 * Whether an invitation is plainly not for the account signed in: its `to`
 * differs from the signed-in address, shortened the same way (ADR-0018) and
 * compared without regard to case, the way ConnectLink and
 * PendingInvitation.meantForAnotherAccount compare it in the application.
 * Nothing to compare either way — a link with no `to`, or nobody signed in
 * yet — reads as no mismatch; the rules alone answer for such a write once it
 * is tried.
 */
export function isWrongAccount(invitation, signedInEmail) {
    if (!invitation.to || !signedInEmail) {
        return false;
    }

    // Cut as the link's own `to` was when it was read, so that an account
    // with a long domain still matches its own invitation.
    const signedIn = capCaption(shortenedAddress(signedInEmail));

    return signedIn.toLowerCase() !== invitation.to.toLowerCase();
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
 * When an invitation stops working, the way a person reads it: the day and
 * the month spelled out rather than run together as digits, since this is
 * read once off a freshly made link rather than watched running down like a
 * search's clock (see `untilText` in `page/search-ui.js`). `locale` is taken
 * as an argument for the reason every function here is — so a test can ask
 * for either language without a browser to ask.
 */
export function invitationUntilText(millis, locale) {
    const when = new Date(millis);
    const day = when.toLocaleDateString(locale, { day: 'numeric', month: 'long' });
    const time = when.toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' });

    return `${day}, ${time}`;
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
