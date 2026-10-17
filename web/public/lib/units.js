// The units a distance and a width are both written in, and the sphere they
// are measured on.
//
// Shared by distance.js and width.js, which differ only in what a figure that
// rounds down to nought metres is allowed to say.

import { WORDS, SPEAKING } from './words.js';

/** The radius of the globe in metres, as the mean sphere everybody draws on. */
export const EARTH_RADIUS_METRES = 6_371_008.8;

/** Where metres stop being the unit worth writing and kilometres start. */
export const METRES_IN_A_KILOMETRE = 1000;

/** How a tenth of a kilometre is arrived at: hundreds of metres, then back. */
export const METRES_IN_A_TENTH = 100;
export const TENTHS_IN_A_KILOMETRE = 10;

const RADIANS_IN_A_DEGREE = Math.PI / 180;

/** Kilometres are written to one place; two would be reading the noise. */
const TENTHS = new Intl.NumberFormat(SPEAKING, {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
});

/** Haversine: good to a few metres at any distance a family is spread over. */
export function haversineMetres(lat1, lon1, lat2, lon2) {
    const fromLatitude = lat1 * RADIANS_IN_A_DEGREE;
    const toLatitude = lat2 * RADIANS_IN_A_DEGREE;
    const northwards = toLatitude - fromLatitude;
    const eastwards = (lon2 - lon1) * RADIANS_IN_A_DEGREE;

    const a =
        Math.sin(northwards / 2) ** 2
        + Math.cos(fromLatitude) * Math.cos(toLatitude) * Math.sin(eastwards / 2) ** 2;

    // Rounding carries a past one for points on opposite sides of the Earth;
    // capped, so that asin is never asked for what it has no answer to.
    return 2 * EARTH_RADIUS_METRES * Math.asin(Math.sqrt(Math.min(a, 1)));
}

/**
 * A length as it is written: whole metres up to a kilometre and kilometres to
 * one place above it.
 *
 * [minMetres] is the least a figure in metres may say: a width passes one,
 * because a radius of nought is a claim no receiver makes, and a distance
 * passes nought, because two devices on one table are nought metres apart.
 */
export function metresOrKilometres(metres, minMetres) {
    const whole = Math.round(metres);

    if (whole < METRES_IN_A_KILOMETRE) {
        return WORDS.widthMetres(Math.max(whole, minMetres));
    }

    return WORDS.widthKilometres(
        TENTHS.format(Math.round(metres / METRES_IN_A_TENTH) / TENTHS_IN_A_KILOMETRE),
    );
}
