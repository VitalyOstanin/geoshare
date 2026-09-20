// The rules for how the map follows a group, kept apart from the map itself.
//
// Nothing here touches the DOM or the map library: the circle of modes, the
// pause after a hand moves the map, and the rectangle a set of positions sit
// in are all arithmetic over plain data, so they can be checked without a
// browser and reused if the map library is ever swapped out.

/** Nobody is tracked; the map only moves when a viewer moves it. */
export const FOLLOW_NONE = 'none';

/** The map keeps one member centred. */
export const FOLLOW_ONE = 'one';

/** The map keeps every visible member in view. */
export const FOLLOW_ALL = 'all';

/**
 * Advances the circle a tap on the follow button walks.
 *
 * The full circle is none -> one -> all -> none, but "one" only exists once a
 * member has actually been picked (`hasTarget`); a first tap with nobody
 * chosen yet has nothing to centre on, so it skips straight to "all". The same
 * skip applies leaving "one": if the previously followed member has since
 * gone quiet and dropped out of the position set, there is no target left to
 * hand the circle back to, and the tap moves on to "all" instead of getting
 * stuck.
 */
export function nextMode(mode, hasTarget) {
    if (mode === FOLLOW_NONE) {
        return hasTarget ? FOLLOW_ONE : FOLLOW_ALL;
    }

    if (mode === FOLLOW_ONE) {
        return FOLLOW_ALL;
    }

    return FOLLOW_NONE;
}

/**
 * How long a manual pan or zoom holds the map still afterwards.
 *
 * Without a pause, a viewer who nudges the map to look at a neighbourhood
 * would be fought back to the followed member on the very next position that
 * arrives. Twenty seconds is long enough to read what is on screen and short
 * enough that a map left alone is following again before the next position
 * after that one lands.
 */
export const PAUSE_AFTER_A_GESTURE_MS = 20000;

/**
 * Whether the last manual gesture is still holding the map still.
 *
 * `pausedAt` is `null` before any gesture has happened, which is never a
 * pause. The pause is inclusive at its start and exclusive at its end: a
 * gesture at `now` pauses immediately, and one exactly
 * `PAUSE_AFTER_A_GESTURE_MS` in the past has fully worn off.
 */
export function isPaused(pausedAt, now) {
    return pausedAt !== null && now - pausedAt < PAUSE_AFTER_A_GESTURE_MS;
}

/**
 * The rectangle that just contains every position, or `null` for none.
 *
 * Longitudes are treated as the plain -180..180 range without wrapping across
 * the antimeridian: a family sharing a map is in one city, not split across
 * the date line, and handling that crossing honestly is a separate piece of
 * work that this module does not attempt.
 */
export function boundsOf(positions) {
    if (positions.length === 0) {
        return null;
    }

    let west = positions[0].longitude;
    let east = positions[0].longitude;
    let south = positions[0].latitude;
    let north = positions[0].latitude;

    for (const position of positions) {
        west = Math.min(west, position.longitude);
        east = Math.max(east, position.longitude);
        south = Math.min(south, position.latitude);
        north = Math.max(north, position.latitude);
    }

    return { west, south, east, north };
}

/**
 * How close two bounding rectangles are allowed to be treated as the same.
 *
 * About twenty metres of latitude: a phone sitting on a table drifts by that
 * much between fixes on its own, and refitting the view for drift that small
 * would make the map visibly breathe while nobody has actually moved.
 */
export const BOUNDS_TOLERANCE_DEGREES = 0.0002;

/**
 * Whether two rectangles are close enough that refitting the view for the
 * difference would not be worth the jump.
 *
 * Each of the four edges is compared on its own rather than, say, comparing
 * centres or areas, because a rectangle that grew on one side while shrinking
 * on the opposite one could otherwise look unchanged while the view it needs
 * has actually shifted.
 */
export function boundsAreClose(one, other, tolerance = BOUNDS_TOLERANCE_DEGREES) {
    return (
        Math.abs(one.west - other.west) < tolerance &&
        Math.abs(one.south - other.south) < tolerance &&
        Math.abs(one.east - other.east) < tolerance &&
        Math.abs(one.north - other.north) < tolerance
    );
}

/**
 * The position a followed member is at, or `null` if they are not in the set.
 *
 * A member drops out of the set when their phone has gone quiet for too long
 * or their claimed position was rejected; the map has nothing to centre on
 * until they reappear, rather than freezing on a stale point.
 */
export function targetPosition(positions, target) {
    return positions.find((position) => position.positionId === target) ?? null;
}
