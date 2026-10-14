// The positions of the group: subscribing to them and reading what a
// document of the `positions` collection holds. Split out of `store.js`,
// whose `store`, `currentGroup` and `RETRY_AFTER_MS` this imports rather than
// keeping its own copies.

import { doc, onSnapshot } from './vendor/firebase-13.0.0/firebase-firestore.js';
import { positionMoments } from './age.js';
import { currentGroup, refusals, store } from './store.js';
import { memberIdOf } from './phones.js';

/**
 * The positions, as they change.
 *
 * A subscription rather than a read on a timer: a member who moves writes a
 * new position, and the marker follows within a second without the page
 * asking for anything.
 *
 * One subscription per document, because that is the only shape the
 * permissions allow. A document this account may not read answers with a
 * refusal rather than with nothing, and that refusal is the member saying they
 * no longer share — so it drops the marker instead of being reported.
 */
export function watchPositions(documents, onPositions, onRefused) {
    const known = new Map();
    const refused = new Set();
    const show = () => onPositions([...known.values()]);
    const { answered, publish } = onceAllAnswered(documents, show);
    const stops = new Map();
    let wanted = true;

    const sayRefused = () => onRefused?.(refused.size);

    const follow = (id) => {
        stops.set(
            id,
            onSnapshot(
                doc(store, 'groups', currentGroup(), 'positions', id),
                (snapshot) => {
                    const position = snapshot.exists() ? positionOf(snapshot) : null;
                    const first = answered(id);

                    if (refused.delete(id)) {
                        sayRefused();
                    }

                    if (position) {
                        known.set(id, position);
                    } else if (!known.delete(id) && !first) {
                        return;
                    }

                    publish();
                },
                () => {
                    const first = answered(id);

                    if (known.delete(id) || first) {
                        publish();
                    }

                    refused.add(id);
                    sayRefused();
                    askAgainLater(id);
                },
            ),
        );
    };

    // A refused document stays refused until the member says otherwise, and
    // what they say does not reach this page: a permission lives in their
    // grant, which a refused reader cannot see either. So the
    // subscription is made again when a profile changes or, failing that, on
    // a slow timer, and a permission handed back brings the marker back with
    // it instead of waiting for a reload.
    const askAgainLater = (id) => {
        stops.get(id)?.();
        stops.delete(id);

        if (!wanted) {
            return;
        }

        refusals.retryLater(`positions/${id}`, () => {
            if (wanted) {
                follow(id);
            }
        });
    };

    documents.forEach(follow);

    // Said once at the start, so that a group with nothing to draw shows an
    // empty map rather than waiting for an answer that never comes.
    show();

    return () => {
        wanted = false;
        documents.forEach((id) => refusals.forget(`positions/${id}`));
        stops.forEach((stop) => stop());
        stops.clear();
    };
}

/**
 * What a subscription to many documents says, held back until every one of
 * them has answered once, a refusal counting as an answer: the map opens on
 * the freshest of one's own devices, and a list that so far holds only the old
 * one would open it there for good.
 */
function onceAllAnswered(documents, show) {
    const unanswered = new Set(documents);

    return {
        /** Whether this was the document's first answer. */
        answered: (id) => unanswered.delete(id),
        publish: () => {
            if (unanswered.size === 0) {
                show();
            }
        },
    };
}

/** The widest answer still drawn as a circle. The rules say the same number. */
const WIDEST_CIRCLE_METRES = 100_000;

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
function widthOf(metres) {
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
function placeable(degrees, limit) {
    return typeof degrees === 'number' && Number.isFinite(degrees) && degrees >= limit && degrees <= -limit;
}
