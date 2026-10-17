// How far a member is from the viewer, as the card says it.
//
// The same rules the application holds, written again here for the reason the
// width is: the two clients share a database and nothing else, and a distance
// that read one way on a device and another in a browser would be read as two
// different answers.

import { isStale, isStuck } from './age.js';
import { haversineMetres, metresOrKilometres } from './units.js';

/**
 * Where the viewer is: the freshest place any of their own devices has written.
 *
 * Their own devices rather than the browser, because asking the browser is a
 * question the page would have to put to the viewer on every visit, and a
 * member reading the map carries a device that has already answered it.
 *
 * A place that has gone grey is no place to measure from: the viewer has had
 * half an hour to walk off, and a distance from where they were is read as a
 * distance from where they are. A marker left behind by a device that is still
 * in touch is the same place gone grey, only without the colour, and so is one
 * inside a place a false signal is known to put devices at.
 */
export function ownPlace(positions, ownMemberId, now) {
    const standing = positions.filter(
        (position) =>
            position.memberId === ownMemberId
            && position.inFalsePlace !== true
            && !isStale(position.knownAt, now)
            && !isStuck(position.positionAt, position.knownAt, now),
    );

    if (standing.length === 0) {
        return null;
    }

    return standing.reduce((freshest, position) =>
        position.positionAt > freshest.positionAt ? position : freshest,
    );
}

/** The metres between two positions, over the sphere the width is drawn on. */
export function metresBetween(from, to) {
    return haversineMetres(from.latitude, from.longitude, to.latitude, to.longitude);
}

/**
 * How far the member on a card is from the viewer, in metres, or null when the
 * card has nothing to say about it: on the card of the very place measured
 * from, on the card of a position a false signal is known to have put there,
 * and when the viewer has no place of their own to measure from.
 */
export function distanceFromViewer(position, positions, ownMemberId, now) {
    const own = ownPlace(positions, ownMemberId, now);

    if (own === null || own.positionId === position.positionId || position.inFalsePlace === true) {
        return null;
    }

    return metresBetween(own, position);
}

/**
 * The distance as the card writes it: whole metres up to a kilometre and
 * kilometres to one place above it, the units the width is written in.
 *
 * Unlike a width, nought metres is an answer: two devices on one table.
 */
export function distanceText(metres) {
    return metresOrKilometres(metres, 0);
}
