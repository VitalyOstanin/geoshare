// What a document of the `positions` collection holds, read into a position
// the map can draw. Kept apart from `store-positions.js`, which subscribes to
// the documents, so that the tests reach it without Firebase.

import { positionMoments } from './age.js';
import { memberIdOf } from './phones.js';

/** The widest answer still drawn as a circle. The rules say the same number. */
export const WIDEST_CIRCLE_METRES = 100_000;

export function positionOf(snapshot) {
    const data = snapshot.data();

    // A document without usable coordinates is not a position, and the map is
    // better off leaving the member out than drawing them off the coast of
    // Africa — or, for a number far outside the globe, than asking the map to
    // place a point that is not on it.
    if (!placeable(data.latitude, -90) || !placeable(data.longitude, -180) || !data.takenAt) {
        return null;
    }

    return {
        // One document per device: the id is the account and the device joined
        // by a dot, and the owner is repeated inside. A document from before
        // positions were kept per device has no owner field, and there the
        // whole id is the account.
        positionId: snapshot.id,
        memberId: typeof data.memberId === 'string' ? data.memberId : memberIdOf(snapshot.id),
        latitude: data.latitude,
        longitude: data.longitude,
        takenAt: data.takenAt.toMillis(),
        // The server stamp dates the news and the fix dates the position; the
        // two part under a spoof, and which is which is worked out in one
        // place the tests can reach.
        ...positionMoments(data.takenAt.toMillis(), data.writtenAt?.toMillis() ?? null),
        mode: data.mode ?? null,
        batteryPercent: typeof data.batteryPercent === 'number' ? data.batteryPercent : null,
        // Absent in what the older builds wrote, which is a device not known to
        // be charging rather than a device known not to be.
        charging: data.charging === true,
        accuracyMetres: widthOf(data.accuracyMetres),
        // How the device says it arrived at the answer, in the word it wrote.
        // Absent in what the older builds wrote, which is a source not known
        // rather than a position arrived at by no means at all.
        foundBy: typeof data.foundBy === 'string' ? data.foundBy : null,
        // How many answers this device has thrown away since it wrote this
        // position, and why the last of them went. A device under a
        // transmitter writes nothing at all, and without these its marker
        // simply gets older — which is what a device in a drawer does too.
        leftOutCount: typeof data.leftOutCount === 'number' ? data.leftOutCount : null,
        leftOutReason: typeof data.leftOutReason === 'string' ? data.leftOutReason : null,
        // Whether the device said its receiver had not settled since it was
        // switched on. Absent in what the older builds wrote and in every
        // position of a device that has been up a while, which are one thing
        // to a reader: nothing to say beyond what the other fields say.
        settling: data.settling === true,
    };
}

/**
 * The width of the answer, where the document holds one worth drawing.
 *
 * A width is a radius, so nought and less are not widths at all; and one wider
 * than a hundred kilometres is a circle covering everything the map can show,
 * which says less about where the device is than the marker alone does. The
 * rules refuse both, and this is what happens to a document written before
 * they did.
 */
export function widthOf(metres) {
    if (typeof metres !== 'number' || !Number.isFinite(metres)) {
        return null;
    }

    return metres > 0 && metres <= WIDEST_CIRCLE_METRES ? metres : null;
}

/**
 * Whether a coordinate is a number the globe has a place for.
 *
 * Written by another member's device, so it is worth as much as that device: a
 * field of the wrong type, an infinity, or a degree count far outside the
 * globe all reach the clients, and the rules do not stop them.
 */
export function placeable(degrees, limit) {
    return typeof degrees === 'number' && Number.isFinite(degrees) && degrees >= limit && degrees <= -limit;
}
