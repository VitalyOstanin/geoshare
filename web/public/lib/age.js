// How old a position is, in the unit worth naming.
//
// The same rules as the Android screen: the two clients show one set of data,
// and an age that disagreed between them would be read as a bug in whichever
// one the viewer happened to trust less.

import { SPEAKING, WORDS } from './words.js';

/** Past this, a position is old enough that the page says so in grey. */
export const STALE_AFTER_MS = 30 * 60 * 1000;

const MINUTE_MS = 60 * 1000;
const HOUR_MS = 60 * MINUTE_MS;
const DAY_MS = 24 * HOUR_MS;

/**
 * Returns the unit and the whole count in it.
 *
 * A clock running ahead of ours gives a negative age; that is somebody else's
 * phone being wrong, and saying "just now" is closer to the truth than saying
 * anything about the future.
 */
export function positionAge(knownAt, now) {
    const age = now - knownAt;

    if (age < MINUTE_MS) {
        return { unit: 'justNow', count: 0 };
    }

    if (age < HOUR_MS) {
        return { unit: 'minutes', count: Math.floor(age / MINUTE_MS) };
    }

    if (age < DAY_MS) {
        return { unit: 'hours', count: Math.floor(age / HOUR_MS) };
    }

    return { unit: 'days', count: Math.floor(age / DAY_MS) };
}

export function isStale(knownAt, now) {
    return now - knownAt >= STALE_AFTER_MS;
}

/**
 * The two moments a position carries: when the phone last reached the server,
 * and when it last had a position it believed.
 *
 * They part whenever a phone says a position again — which is what a phone
 * does while its satellites are being spoofed and it has nothing new it
 * believes. The stamp is the one clock every member shares, so it is what the
 * news is dated by, and the position is capped at it: the rest of a phone
 * clock running ahead would be somebody's watch rather than the age of
 * anything.
 *
 * The Android screen works out the same two from the same fields.
 */
export function positionMoments(takenAt, writtenAt) {
    const knownAt = writtenAt ?? takenAt;

    return { knownAt, positionAt: Math.min(takenAt, knownAt) };
}

/**
 * The moment itself, for the detail a viewer opens rather than for the list.
 *
 * Written the way the language of the page writes a date rather than the way
 * the browser does: a page a viewer asked to read in Russian should not put
 * the month before the day in the one line it gives an exact time on.
 */
export function timeText(millis) {
    return new Date(millis).toLocaleString(SPEAKING);
}

/** The words come from the one place the page keeps its wording. */
export function ageText(age) {
    return age.unit === 'justNow' ? WORDS.justNow : WORDS.ago(age.count, age.unit);
}
