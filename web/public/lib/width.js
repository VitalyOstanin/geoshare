// How wide a position is: the number the card shows and the ring the map draws.
//
// The same two rules the application holds, written again here rather than
// fetched from it: the two clients share a database and nothing else, and a
// width that read one way on a phone and another in a browser would be read as
// two different answers.

import { WORDS, SPEAKING } from './words.js';

/** Where metres stop being the unit worth writing and kilometres start. */
const METRES_IN_A_KILOMETRE = 1000;

/** How a tenth of a kilometre is arrived at: hundreds of metres, then back. */
const METRES_IN_A_TENTH = 100;
const TENTHS_IN_A_KILOMETRE = 10;

/**
 * Below this a width is written in the calm colour, above it in the ochre one.
 *
 * A hundred metres is where an answer stops naming a building and starts
 * naming a block, which is the difference the colour is there to show.
 */
export const WIDTH_WORTH_NOTICING_METRES = 100;

/** The radius of the globe in metres, as the mean sphere everybody draws on. */
const EARTH_RADIUS_METRES = 6371008.8;

/**
 * How many corners the ring is drawn with.
 *
 * Sixty-four is where the corners stop being visible at the zoom a circle is
 * looked at: a ring of a hundred pixels then has a corner every ten.
 */
const RING_CORNERS = 64;

/** How flat the cosine is allowed to get before the ring stops widening. */
const NARROWEST_PARALLEL = 0.01;

const DEGREES_IN_A_RADIAN = 180 / Math.PI;

/** How far north and south a drawn point may go. */
const POLE = 90;

/** Kilometres are written to one place; two would be reading the noise. */
const TENTHS = new Intl.NumberFormat(SPEAKING, {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
});

/**
 * The width of an answer as it is written on a card.
 *
 * Metres up to a kilometre and kilometres above it, because a width is read to
 * decide whether the marker means a room, a courtyard or a district, and a
 * four-digit count of metres is read one digit at a time.
 */
export function widthText(metres) {
    const whole = Math.round(metres);

    if (whole < METRES_IN_A_KILOMETRE) {
        // A width is a radius, and a radius of nought metres is a claim no
        // receiver makes. Anything that rounds down to it is the narrowest
        // answer there is rather than no answer at all.
        return WORDS.widthMetres(Math.max(whole, 1));
    }

    return WORDS.widthKilometres(
        TENTHS.format(Math.round(metres / METRES_IN_A_TENTH) / TENTHS_IN_A_KILOMETRE),
    );
}

/** Whether a width is wide enough to be worth saying so in the colour. */
export function widthIsWide(metres) {
    return metres >= WIDTH_WORTH_NOTICING_METRES;
}

/**
 * The ring a width draws around a position: a closed list of points.
 *
 * A ring of points rather than a circle of a fixed number of pixels, because
 * the width is a claim in metres: a circle drawn in pixels would say the same
 * thing about a fix eleven metres wide and one seventeen hundred metres wide
 * as soon as the map was zoomed.
 *
 * The parallel narrows towards the poles, so a step east is a wider turn there
 * than at the equator; dividing by its cosine is what keeps the ring round.
 * The cosine is held off nought so that a position at the pole itself gives a
 * ring rather than a division that runs away.
 *
 * Points are [longitude, latitude], and the first is repeated at the end: a
 * ring is closed, and the map expects to be told so.
 */
export function accuracyRing(latitude, longitude, metres, corners = RING_CORNERS) {
    const northwards = (metres / EARTH_RADIUS_METRES) * DEGREES_IN_A_RADIAN;
    const eastwards =
        northwards
        / Math.max(Math.cos(Math.abs(latitude) / DEGREES_IN_A_RADIAN), NARROWEST_PARALLEL);

    const ring = [];

    for (let corner = 0; corner <= corners; corner += 1) {
        const turn = (corner / corners) * 2 * Math.PI;

        // Clamped rather than wrapped: a ring that reaches over the pole is
        // not a ring any drawing of a map can show, and stopping at the pole
        // is the honest end of it.
        const north = Math.min(Math.max(latitude + northwards * Math.cos(turn), -POLE), POLE);
        const east = wrapDegrees(longitude + eastwards * Math.sin(turn));

        ring.push([east, north]);
    }

    return ring;
}

/** The same angle, written as the turn of at most half a circle either way. */
function wrapDegrees(degrees) {
    return (((degrees + 180) % 360 + 360) % 360) - 180;
}
