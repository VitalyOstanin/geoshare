// Who the page lists, and in what order.

import { shortenedAddress } from './join.js';
import { compareIds } from './order.js';
import { trimText } from './text.js';
import { WORDS } from './words.js';

/** How many devices of one member this page will follow. The rules say the same. */
const DEVICES_PER_MEMBER = 10;

/**
 * The other members come first: the page exists to answer where they are, and
 * one's own marker is the one already in the middle of the map. Within that,
 * the order is by id rather than by distance or freshness, so that the entries
 * stay where the eye left them.
 */
export function orderedForNavigation(positions, ownMemberId) {
    return positions.toSorted((left, right) => {
        const own = Number(left.memberId === ownMemberId) - Number(right.memberId === ownMemberId);

        if (own !== 0) {
            return own;
        }

        const member = compareIds(left.memberId, right.memberId);

        // The devices of one member follow each other, and keep their places
        // between two glances at the panel.
        return member !== 0 ? member : compareIds(left.positionId, right.positionId);
    });
}

/**
 * The position a member was last heard from at, of all their devices: the one
 * a viewer asking for them by name most likely means, and the one to open the
 * map on rather than an old device left at home. Null when there is none.
 */
export function freshestOf(positions, memberId) {
    let freshest = null;

    for (const position of positions) {
        if (position.memberId === memberId
            && (freshest === null || position.knownAt > freshest.knownAt)) {
            freshest = position;
        }
    }

    return freshest;
}

/** The colours members are told apart by, the same five the Android screen uses. */
const MEMBER_COLOURS = ['#1e88e5', '#43a047', '#e53935', '#8e24aa', '#f9a825'];

/** What an old position fades to. */
export const STALE_COLOUR = '#9e9e9e';

/**
 * The colour of one member, the same one every time and the same one both
 * clients draw, because it is taken from the id rather than from a list.
 */
export function memberColour(memberId) {
    return MEMBER_COLOURS[colourIndex(memberId)];
}

/**
 * The colour a member is drawn in on the pin, the card and the strip alike:
 * their own, or grey once the position is old. One place, so that the three
 * cannot come to disagree about which is which.
 */
export function pinColour(memberId, stale) {
    return stale ? STALE_COLOUR : memberColour(memberId);
}

/**
 * The index the colour is taken at, which is where the two clients have to
 * agree. The remainder is taken before the sign is dropped, matching the
 * Android side: the smallest negative integer has no positive counterpart.
 */
export function colourIndex(memberId) {
    let hash = 0;

    for (const character of memberId) {
        // The 32-bit wrap-around is deliberate: it is what makes this the same
        // arithmetic as String.hashCode on the other side.
        hash = (Math.imul(31, hash) + character.charCodeAt(0)) | 0;
    }

    return Math.abs(hash % MEMBER_COLOURS.length);
}

/** Enough characters of an id to tell two members apart at a glance. */
const SHORT_ID_LENGTH = 6;

export function shortName(memberId) {
    return memberId.slice(0, SHORT_ID_LENGTH);
}

/**
 * The name a member goes by in the group: the account's own, or, for an
 * account without one, its shortened address — the same the administrator
 * saw on the invitation, and more use to them than the start of an id.
 */
export function profileName(name, email) {
    const own = name && trimText(name);

    if (own) {
        return own;
    }

    const address = email && trimText(email);

    return address ? shortenedAddress(address) : null;
}

/**
 * What a member is called on screen.
 *
 * A member who has not signed in since profiles existed has no name to show,
 * and the first characters of the id are what told them apart before.
 */
export function memberName(profile, memberId, own) {
    if (own) {
        return WORDS.you;
    }

    return profile?.displayName ?? shortName(memberId);
}

/**
 * The letter that stands in for a member without a picture.
 *
 * The name is what the group calls them, so its first letter is what they are
 * recognised by; the same rule the device follows, so a member looks the same
 * on both clients.
 */
export function memberInitial(name) {
    return [...trimText(name)].slice(0, 1).join('').toUpperCase();
}

/**
 * The letter on the account button, which is the signed-in account's own
 * name rather than a member's.
 *
 * Taken as a member's initial is, so a name opening with an emoji or a space
 * does not leave half a pair or a blank circle; a name with nothing in it is
 * the same as no name, and shows a question mark.
 */
export function accountInitial(name) {
    return memberInitial(name ?? '') || '?';
}

/** The host account pictures are kept on, and the only one drawn from. */
const ACCOUNT_PICTURE = /^https:\/\/lh[0-9]+\.googleusercontent\.com\/.*$/;

/**
 * The address of a member's picture, or nothing.
 *
 * A profile is written by the member it describes, so the address in it is
 * whatever they put there. Only the host the sign-in provider keeps account
 * pictures on is drawn — the one the content policy lets images in from, and
 * the one the rules and the Android client hold the address to; anything else
 * would have every viewer call on a server of the member's choosing, and
 * leaves the marker with its initial.
 */
export function pictureAddress(pictureUrl) {
    return typeof pictureUrl === 'string' && ACCOUNT_PICTURE.test(pictureUrl) ? pictureUrl : null;
}

/**
 * Which documents hold the positions of the members who let this viewer watch.
 *
 * Named one by one rather than asked for as a collection: a member says who
 * may read their position and until when, and a permission with an expiry is
 * not something a query can prove, so the collection of positions is closed
 * and a reader walks the devices each grant names instead.
 */
export function positionDocuments(grants) {
    return [...grants.values()]
        .flatMap((grant) =>
            grant.devices
                // A subscription each, so a grant naming more devices than a
                // person can carry would put this page out of action — and the
                // grant is written by somebody else. The rules hold the list
                // to the same number; this is what happens to a document
                // written before they did.
                .slice(0, DEVICES_PER_MEMBER)
                .map((device) => `${grant.memberId}.${device}`),
        )
        .sort(compareIds);
}
