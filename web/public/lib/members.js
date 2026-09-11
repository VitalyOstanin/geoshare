// Who the page lists, and in what order.

/**
 * The other members come first: the page exists to answer where they are, and
 * one's own marker is the one already in the middle of the map. Within that,
 * the order is by id rather than by distance or freshness, so that the entries
 * stay where the eye left them.
 */
export function orderedForNavigation(positions, ownMemberId) {
    return [...positions].sort((left, right) => {
        const own = Number(left.memberId === ownMemberId) - Number(right.memberId === ownMemberId);

        return own !== 0 ? own : left.memberId.localeCompare(right.memberId);
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
