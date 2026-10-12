// A request that one device forget where it thinks it is, out of the requests
// lying in the database.
//
// The device's jump filter holds on to the last place it believed, and a wrong
// one makes it refuse every honest answer after it. The device's own settings
// carry a button for that; this is the same button pressed from another
// screen. The device carries out a request no older than RESET_FRESH_MS and
// says so; the asking screen watches the same document to tell a device that
// did from one not heard from since.

/**
 * How long a request waits for its device: one switched back on later the
 * same day still forgets.
 */
export const RESET_FRESH_MS = 24 * 60 * 60_000;

/** How long a request that is over — carried out or never answered — is told of on a row. */
export const RESET_TOLD_MS = 5 * 60_000;

/**
 * One request as the asking screen holds it, out of what the document says,
 * or null for a document that is not one. The times are read the way the
 * store hands them over, in milliseconds.
 */
export function resetOf(phoneId, data) {
    if (
        typeof data?.askedBy !== 'string' ||
        !Number.isFinite(data?.askedAt) ||
        !phoneId.includes('.')
    ) {
        return null;
    }

    return {
        phoneId,
        askedBy: data.askedBy,
        askedAt: data.askedAt,
        doneAt: Number.isFinite(data.doneAt) ? data.doneAt : null,
    };
}

/** `reset`'s state at `now`: carried out, still waiting, or never answered. */
export function resetState(reset, now) {
    if (reset.doneAt !== null) {
        return { kind: 'done', at: reset.doneAt };
    }

    return now < reset.askedAt + RESET_FRESH_MS ? { kind: 'waiting' } : { kind: 'noAnswer' };
}

/**
 * Whether `reset` is still worth a line on its row at `now`: while the device
 * has not answered, and for `RESET_TOLD_MS` after it did or gave up.
 */
export function resetTold(reset, now) {
    const endedAt = reset.doneAt ?? reset.askedAt + RESET_FRESH_MS;

    return now < endedAt + RESET_TOLD_MS;
}
