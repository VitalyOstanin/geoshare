// How far a member is from the viewer, as the card says it.
//
// The same rules the application holds, written again here for the reason the
// width is: the two clients share a database and nothing else, and a distance
// that read one way on a phone and another in a browser would be read as two
// different answers.

import { isStale, isStuck } from './age.js';
import { WORDS, SPEAKING } from './words.js';

/** The radius the distance is measured on: the mean sphere the width uses. */
const EARTH_RADIUS_METRES = 6371008.8;

const RADIANS_IN_A_DEGREE = Math.PI / 180;

/** Where metres stop being the unit worth writing and kilometres start. */
const METRES_IN_A_KILOMETRE = 1000;

/** How a tenth of a kilometre is arrived at: hundreds of metres, then back. */
const METRES_IN_A_TENTH = 100;
const TENTHS_IN_A_KILOMETRE = 10;

const TENTHS = new Intl.NumberFormat(SPEAKING, {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
});

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
 * online is the same place gone grey, only without the colour, and so is one
 * inside a place a false signal is known to put phones at.
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
 * Unlike a width, nought metres is an answer: two phones on one table.
 */
export function distanceText(metres) {
    const whole = Math.round(metres);

    if (whole < METRES_IN_A_KILOMETRE) {
        return WORDS.widthMetres(whole);
    }

    return WORDS.widthKilometres(
        TENTHS.format(Math.round(metres / METRES_IN_A_TENTH) / TENTHS_IN_A_KILOMETRE),
    );
}
