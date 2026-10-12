// Documents the rules turned a subscription away from, and when to ask again.
//
// Pure of the database and of the page, so that the waiting can be tested with
// a clock of its own; `store.js` makes the one the page uses.

/**
 * The moments a refused subscription is made again.
 *
 * A refusal is mostly the member saying they no longer share, and a permission
 * handed back is written to their profile as well (the same touch the phones
 * make when sharing starts), so the page asks again when a profile changes
 * rather than polling for it. The timer is the backstop for the cases that
 * touch no profile — a permission that expires, or one granted before the
 * member's phone could write it — and is slow because every ask is a paid read.
 *
 * @param {number} after - the backstop wait, in milliseconds
 * @param {{ schedule?: Function, cancel?: Function }} [timers] - replaceable in tests
 */
export function createRefusals(after, { schedule = setTimeout, cancel = clearTimeout } = {}) {
    const waiting = new Map();

    const forget = (key) => {
        const entry = waiting.get(key);

        if (entry) {
            cancel(entry.timer);
            waiting.delete(key);
        }
    };

    return {
        /** Asks again for `key` after the backstop wait; a later call replaces an earlier one. */
        retryLater(key, again) {
            forget(key);
            waiting.set(key, {
                again,
                timer: schedule(() => {
                    waiting.delete(key);
                    again();
                }, after),
            });
        },

        /** Gives up on `key`, as a subscription being stopped does. */
        forget,

        /** Asks again for everything waiting, now. */
        retryNow() {
            const due = [...waiting.values()];

            waiting.forEach((entry) => cancel(entry.timer));
            waiting.clear();
            due.forEach((entry) => entry.again());
        },
    };
}

/**
 * The latest moment any of the profiles was touched, in milliseconds; null
 * while none has been.
 */
export function lastProfileChange(profiles) {
    let latest = null;

    for (const profile of profiles?.values() ?? []) {
        if (profile.updatedAt !== null && profile.updatedAt !== undefined
            && (latest === null || profile.updatedAt > latest)) {
            latest = profile.updatedAt;
        }
    }

    return latest;
}

/** Whether `current` is a profile change that `previous` has not seen. */
export function profilesChanged(previous, current) {
    return current !== null && previous !== null && current > previous;
}
