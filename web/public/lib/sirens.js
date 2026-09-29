// A call that rings one phone in the group, out of the sirens lying in the
// database.
//
// A phone answers a call that is newer than the last one it answered and no
// older than SIREN_FRESH_MS, sounding for SIREN_SECONDS through the alarm
// stream; the caller's own screen watches the same document to tell an
// answered phone from one switched off, and gives up waiting after the same
// SIREN_FRESH_MS.

/** How long a phone sounds once it starts, in seconds. */
export const SIREN_SECONDS = 22;

/** How long a call waits for its phone, and how long the caller's screen waits for an answer. */
export const SIREN_FRESH_MS = 60_000;

/** How long a call that is over — stopped, unanswered or sounded out — is told of on a row. */
export const SIREN_TOLD_MS = 5 * 60_000;

/**
 * One call as the phone or the caller's screen holds it, out of what the
 * document says, or null for a document that is not one.
 *
 * The times are read the way the store hands them over, in milliseconds; the
 * member id is read out of `phoneId` the way a position's is, since a siren
 * is addressed the same way: `{uid}.{deviceId}`.
 */
export function sirenOf(phoneId, data) {
    if (
        typeof data?.calledBy !== 'string' ||
        !Number.isFinite(data?.calledAt) ||
        !phoneId.includes('.')
    ) {
        return null;
    }

    return {
        phoneId,
        memberId: phoneId.slice(0, phoneId.indexOf('.')),
        calledBy: data.calledBy,
        calledAt: data.calledAt,
        heardAt: Number.isFinite(data.heardAt) ? data.heardAt : null,
        stoppedBy: typeof data.stoppedBy === 'string' ? data.stoppedBy : null,
        stoppedAt: Number.isFinite(data.stoppedAt) ? data.stoppedAt : null,
    };
}

/**
 * Whether the phone should sound for `siren` at `now`: not stopped, newer
 * than `lastAnswered`, and still within `SIREN_FRESH_MS` of `calledAt` — a
 * call whose own clock ran a little ahead counts too, since `calledAt` is
 * the server's own stamp and the phone reads it no faster than the server
 * wrote it.
 */
export function shouldSound(siren, lastAnswered, now) {
    return (
        siren.stoppedBy === null &&
        (lastAnswered === null || siren.calledAt > lastAnswered) &&
        now < siren.calledAt + SIREN_FRESH_MS
    );
}

/**
 * `siren`'s state at `now`, for the caller's own screen to show, in the
 * order the document's own fields settle it: a stop wins over everything
 * else, an answer over silence, and silence past `SIREN_FRESH_MS` means
 * nobody is coming.
 */
export function sirenState(siren, now) {
    if (siren.stoppedBy !== null) {
        return { kind: 'stopped', by: siren.stoppedBy };
    }

    if (siren.heardAt !== null) {
        const until = siren.heardAt + SIREN_SECONDS * 1000;

        return now < until ? { kind: 'sounding', until } : { kind: 'over' };
    }

    return now < siren.calledAt + SIREN_FRESH_MS ? { kind: 'waiting' } : { kind: 'noAnswer' };
}

/**
 * Whether `siren` is still worth a line on its row at `now`: always while it
 * is being called or sounding, and for `SIREN_TOLD_MS` after it ended. A stop
 * without its time — a write still on its way — counts from the call.
 */
export function sirenTold(siren, now) {
    let endedAt;

    if (siren.stoppedBy !== null) {
        endedAt = siren.stoppedAt ?? siren.calledAt;
    } else if (siren.heardAt !== null) {
        endedAt = siren.heardAt + SIREN_SECONDS * 1000;
    } else {
        endedAt = siren.calledAt + SIREN_FRESH_MS;
    }

    return now < endedAt + SIREN_TOLD_MS;
}
