// How far a member is from the viewer, as the card says it.
//
// The same rules the application holds, written again here for the reason the
// width is: the two clients share a database and nothing else, and a distance
// that read one way on a phone and another in a browser would be read as two
// different answers.

import { isStale, isStuck } from './age.js';

/** The radius the distance is measured on: the mean sphere the width uses. */
const EARTH_RADIUS_METRES = 6371008.8;

const RADIANS_IN_A_DEGREE = Math.PI / 180;

/**
 * Where the viewer is: the freshest place any of their own phones has written.
 *
 * Their own phones rather than the browser, because asking the browser is a
 * question the page would have to put to the viewer on every visit, and a
 * member reading the map carries a phone that has already answered it.
 *
 * A place that has gone grey is no place to measure from: the viewer has had
 * half an hour to walk off, and a distance from where they were is read as a
 * distance from where they are. A marker left behind by a phone that is still
 * online is the same place gone grey, only without the colour.
 */
export function ownPlace(positions, ownMemberId, now) {
    const standing = positions.filter(
        (position) =>
            position.memberId === ownMemberId
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

/** Haversine: good to a few metres at any distance a family is spread over. */
export function metresBetween(from, to) {
    const fromLatitude = from.latitude * RADIANS_IN_A_DEGREE;
    const toLatitude = to.latitude * RADIANS_IN_A_DEGREE;
    const northwards = toLatitude - fromLatitude;
    const eastwards = (to.longitude - from.longitude) * RADIANS_IN_A_DEGREE;

    const a =
        Math.sin(northwards / 2) ** 2
        + Math.cos(fromLatitude) * Math.cos(toLatitude) * Math.sin(eastwards / 2) ** 2;

    return 2 * EARTH_RADIUS_METRES * Math.asin(Math.sqrt(Math.min(a, 1)));
}

/**
 * How far the member on a card is from the viewer, or null when the card has
 * nothing to say about it.
 *
 * Nothing on the card of the very place measured from, and nothing when the
 * viewer has no place of their own to measure from.
 *
 * Near rather than a number while the two are inside each other's width: forty
 * metres between two answers each sixty metres wide is two answers about one
 * flat, and a figure would say the member is across the street.
 */
export function distanceFromViewer(position, positions, ownMemberId, now) {
    const own = ownPlace(positions, ownMemberId, now);

    if (own === null || own.positionId === position.positionId) {
        return null;
    }

    const metres = metresBetween(own, position);
    const widest = Math.max(own.accuracyMetres ?? 0, position.accuracyMetres ?? 0);

    return metres <= widest ? { near: true } : { near: false, metres };
}
