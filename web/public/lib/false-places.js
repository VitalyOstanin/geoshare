// Positions that lie where a false signal is known to put devices.
//
// The devices refuse an answer that lands in such a place, but a position
// written before the place was named — or before it was drawn wide enough —
// stays in the database until the device has something true to write over it.
// Under a spoofer that can be hours, and a marker standing in the lake all
// that time is read as where the member is. The page cannot take the position
// back and does not try: it greys the marker the way it greys one gone quiet,
// says why on the card, and measures nothing from it.
//
// The places are the ones the devices read, from the group's own settings, and
// they are weighed here by the same numbers the devices hold them to.

import { isStale } from './age.js';
import { metresBetween } from './distance.js';

/** How many places the page will weigh. The devices keep the same number. */
const PLACES_KEPT = 50;

/** The widest a place may be. The rules and the devices say the same. */
const WIDEST_METRES = 50000;

/**
 * A document read as a place, or null where it is not one: a document typed
 * wrong in the console must not grey every marker on the map, nor stop the
 * rest of the list from being read.
 */
export function placeOf(data) {
    const { latitude, longitude, radiusMetres } = data ?? {};

    if (
        typeof latitude !== 'number' || latitude < -90 || latitude > 90
        || typeof longitude !== 'number' || longitude < -180 || longitude > 180
        || typeof radiusMetres !== 'number' || radiusMetres <= 0 || radiusMetres > WIDEST_METRES
    ) {
        return null;
    }

    return { latitude, longitude, radiusMetres };
}

/** The places in the order the database lists them, as many as are weighed. */
export function placesOf(documents) {
    return documents.map(placeOf).filter(Boolean).slice(0, PLACES_KEPT);
}

/** Whether a position lies inside any of the places. */
export function inFalsePlace(position, places) {
    return places.some((place) => metresBetween(place, position) <= place.radiusMetres);
}

/** The positions, each inside a place marked so. */
export function withFalsePlaces(positions, places) {
    if (places.length === 0) {
        return positions;
    }

    return positions.map((position) =>
        inFalsePlace(position, places) ? { ...position, inFalsePlace: true } : position,
    );
}

/**
 * Whether a marker is drawn grey: gone quiet, or standing where the lie puts
 * devices. Both say the same to the viewer — the member is not to be looked for
 * here — and one colour for both is one fewer to learn.
 */
export function isGrey(position, now) {
    return isStale(position.knownAt, now) || position.inFalsePlace === true;
}
