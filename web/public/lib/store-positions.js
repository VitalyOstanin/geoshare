// The positions of the group: subscribing to them, with what a document of
// the `positions` collection holds read by `positionOf`. Split out of
// `store.js`, whose `store`, `currentGroup` and `RETRY_AFTER_MS` this imports
// rather than keeping its own copies.

import { doc, onSnapshot } from './vendor/firebase-13.0.0/firebase-firestore.js';
import { currentGroup, refusals, store } from './store.js';
import { positionOf } from './position-data.js';

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

