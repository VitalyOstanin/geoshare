// Members standing in the same place, as the one marker that gets drawn for
// them.
//
// The arithmetic is repeated in the Android client: a crowd in one spot should
// be the same marker whichever client is looking at it.

import { compareIds } from './order.js';

/**
 * How many circles a pin ever has in it.
 *
 * Three at 28 pixels apart is a head of 104 pixels, about a third of the width
 * of a narrow phone; a group of any size stops there and counts the rest
 * instead.
 */
export const PIN_MAX_CIRCLES = 3;

/**
 * Gathers the positions whose pins overlap.
 *
 * Anchored rather than chained: a member joins the group they are within reach
 * of, and a third one further along starts a group of their own instead of
 * dragging the first two across the map. A chain would let a row of members
 * standing a pin apart each collapse into a single marker somewhere in the
 * middle of them all.
 *
 * The order is taken from the ids rather than from the list, because the list
 * comes from the database and can arrive in any order; the same members in the
 * same places have to produce the same groups every tick, or the map would
 * redraw itself for nothing.
 *
 * `alone` is the member whose card is open, kept out of every group: the
 * circle around them is drawn where they are, and a pin standing in the middle
 * of a crowd would point somewhere else.
 *
 * @param placed positions with the screen point their pin came out at
 * @param radius how close two pins have to come to be drawn as one
 * @param alone the position kept on a pin of its own, if any
 */
export function clusterMembers(placed, radius, alone = null) {
    const ordered = [...placed].sort((one, other) => compareIds(one.id, other.id));
    const taken = new Set();
    const clusters = [];

    for (const anchor of ordered) {
        if (taken.has(anchor.id)) {
            continue;
        }

        taken.add(anchor.id);

        const group = [anchor.id];

        for (const other of ordered) {
            if (anchor.id === alone || other.id === alone) {
                continue;
            }

            if (taken.has(other.id) || !within(anchor, other, radius)) {
                continue;
            }

            taken.add(other.id);
            group.push(other.id);
        }

        clusters.push(group);
    }

    return clusters;
}

/**
 * How many of a group's faces are drawn, the rest being left to the counter.
 *
 * The Android client repeats this: three or fewer are all drawn, and a larger
 * group gives its last circle up to the count of who is not shown.
 */
export function facesShown(count) {
    return count > PIN_MAX_CIRCLES ? PIN_MAX_CIRCLES - 1 : count;
}

/** Squared on both sides, to keep a root out of a comparison that does not need one. */
function within(anchor, other, radius) {
    const dx = anchor.x - other.x;
    const dy = anchor.y - other.y;

    return dx * dx + dy * dy <= radius * radius;
}
