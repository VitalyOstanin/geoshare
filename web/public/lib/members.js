// Who the page lists, and in what order.

import { compareIds } from './order.js';
import { WORDS } from './words.js';

/** How many phones of one member this page will follow. The rules say the same. */
const DEVICES_PER_MEMBER = 10;

/**
 * The other members come first: the page exists to answer where they are, and
 * one's own marker is the one already in the middle of the map. Within that,
 * the order is by id rather than by distance or freshness, so that the entries
 * stay where the eye left them.
 */
export function orderedForNavigation(positions, ownMemberId) {
    return [...positions].sort((left, right) => {
        const own = Number(left.memberId === ownMemberId) - Number(right.memberId === ownMemberId);

        if (own !== 0) {
            return own;
        }

        const member = compareIds(left.memberId, right.memberId);

        // The phones of one member follow each other, and keep their places
        // between two glances at the panel.
        return member !== 0 ? member : compareIds(left.positionId, right.positionId);
    });
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
export function shortName(memberId) {
    return memberId.slice(0, 6);
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
 * recognised by; the same rule the phone follows, so a member looks the same
 * on both clients.
 */
export function memberInitial(name) {
    return [...name.trim()].slice(0, 1).join('').toUpperCase();
}

/**
 * The address of a member's picture, or nothing.
 *
 * A profile is written by the member it describes, so the address in it is
 * whatever they put there; only the two schemes a picture can be fetched over
 * are drawn, and anything else leaves the marker with its initial.
 */
export function pictureAddress(pictureUrl) {
    if (!pictureUrl) {
        return null;
    }

    try {
        const address = new URL(pictureUrl);

        return address.protocol === 'https:' || address.protocol === 'http:' ? address.href : null;
    } catch {
        return null;
    }
}

/**
 * Which documents hold the positions of the members whose profiles are known.
 *
 * Named one by one rather than asked for as a collection: a member says who
 * may read their position and until when, and a permission with an expiry is
 * not something a query can prove, so the collection of positions is closed
 * and a reader walks the devices each profile names instead.
 */
export function positionDocuments(profiles) {
    return [...profiles.values()]
        .flatMap((profile) =>
            profile.devices
                // A subscription each, so a profile naming more phones than a
                // person can carry would put this page out of action — and the
                // profile is written by somebody else. The rules hold the list
                // to the same number; this is what happens to a document
                // written before they did.
                .slice(0, DEVICES_PER_MEMBER)
                .map((device) => `${profile.memberId}.${device}`),
        )
        .sort(compareIds);
}
