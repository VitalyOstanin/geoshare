// Which marker is drawn over which, when two of them overlap.

/**
 * The highest a marker may stand.
 *
 * Screens are not this tall, so nothing is ever pressed against the ceiling in
 * practice; the bound exists so that a projection gone wrong — a position off
 * the edge of the world, a map mid-animation — cannot hand the page a number
 * that outranks everything else on it for good.
 */
const HIGHEST = 9_999;

/**
 * How high a marker stands in the pile, from where its point is on screen.
 *
 * The one further down the screen wins. That is how a drawn scene reads: what
 * is lower is nearer the viewer, and a pin whose point is in front should not
 * be covered by the head of a pin standing behind it. Latitude would give the
 * same answer on a north-up map and the wrong one on a turned map, so the
 * screen is asked rather than the globe.
 *
 * Never below zero: a marker given a negative order would slide under the map
 * itself, which is the one place a member must never end up.
 */
export function markerOrder(screenY) {
    // Not a number at all, which a projection answers with while the map has
    // no size yet: the pile it would take part in does not exist either.
    if (Number.isNaN(screenY)) {
        return 0;
    }

    return Math.min(HIGHEST, Math.max(0, Math.round(screenY)));
}
