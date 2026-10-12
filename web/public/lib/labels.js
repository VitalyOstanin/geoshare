// What this member calls the devices and the people of the group, for
// themselves alone: "new device" rather than a model number, "grandma" rather
// than the name on her account.
//
// A device is named by its full address, account and device joined by a dot, so
// that two devices of one model are two names. The application keeps the same
// document and takes the names in the same order.

import { compareIds } from './order.js';
import { deviceIdOf } from './phones.js';
import { keep, readKept } from './kept.js';

/** As much of a name as fits on a card. The application cuts it to the same. */
export const LABEL_LIMIT = 40;

/** How many names of each kind are kept. The rules say the same. */
const PHONE_LABELS = 500;
const MEMBER_LABELS = 50;

/** Where the copy of the names is kept, one entry per account. */
const LABELS_KEY = 'geoshare.labels.';

/** No names at all: what a member has before they give any. */
export const NO_LABELS = Object.freeze({ phones: new Map(), members: new Map() });

/** What is kept of a name somebody typed: nothing for an empty one, which resets the name. */
export function labelText(typed) {
    const text = String(typed ?? '').trim().slice(0, LABEL_LIMIT);

    return text === '' ? null : text;
}

function namesFrom(value, limit) {
    const names = new Map();

    if (!value || typeof value !== 'object' || Array.isArray(value)) {
        return names;
    }

    for (const [key, name] of Object.entries(value)) {
        const text = typeof name === 'string' ? labelText(name) : null;

        if (text !== null && names.size < limit) {
            names.set(key, text);
        }
    }

    return names;
}

/**
 * The names out of the document, with whatever is not a name left out: the
 * rules hold how many there are and not what each one is.
 */
export function labelsFrom(data) {
    return {
        phones: namesFrom(data?.devices, PHONE_LABELS),
        members: namesFrom(data?.members, MEMBER_LABELS),
    };
}

/**
 * The copy of the names this browser kept, so that the markers are named from
 * the first drawing rather than from the database's first answer. A browser
 * that keeps nothing starts without names and waits for the database.
 */
export function readKeptLabels(memberId, store = globalThis.localStorage) {
    const kept = readKept(LABELS_KEY + memberId, store);

    try {
        return kept ? labelsFrom(JSON.parse(kept)) : NO_LABELS;
    } catch {
        // A copy that no longer parses is no copy at all.
        return NO_LABELS;
    }
}

export function keepLabels(memberId, labels, store = globalThis.localStorage) {
    keep(LABELS_KEY + memberId, JSON.stringify({
        devices: Object.fromEntries(labels.phones),
        members: Object.fromEntries(labels.members),
    }), store);
}

/** The profiles with the names this member gave the people put in place of their own. */
export function labelledProfiles(profiles, labels) {
    if (!profiles) {
        return profiles;
    }

    return new Map([...profiles].map(([memberId, profile]) => {
        const named = labels.members.get(memberId);

        return [memberId, named ? { ...profile, displayName: named } : profile];
    }));
}

/**
 * A member's devices, each called what this member called it, else what its
 * owner called it, else its model; null for a device that has none of them.
 *
 * Devices left with one name between them — two of one model nobody named —
 * are told apart by a number after it, counted in the order of their ids, so
 * that the map, the panel and the application all number them alike.
 *
 * `phones` is what the member's devices say about themselves, by device id;
 * `seen` is a last name for a device known only from its position, or null.
 */
export function phoneNames(memberId, phones, labels, seen = new Map()) {
    const names = new Map();

    for (const deviceId of new Set([...phones.keys(), ...seen.keys()])) {
        const phone = phones.get(deviceId);

        names.set(deviceId, labels.phones.get(`${memberId}.${deviceId}`)
            ?? phone?.name
            ?? phone?.model
            ?? seen.get(deviceId)
            ?? null);
    }

    const sharing = new Map();

    for (const [deviceId, name] of names) {
        if (name !== null) {
            sharing.set(name, [...(sharing.get(name) ?? []), deviceId]);
        }
    }

    // A number is skipped where it would give a name another device already
    // has: "Pixel (1)" named so by its owner stays the only one.
    const taken = new Set([...sharing].filter(([, ids]) => ids.length === 1).map(([name]) => name));

    for (const [name, deviceIds] of sharing) {
        if (deviceIds.length > 1) {
            let number = 0;

            for (const deviceId of deviceIds.sort(compareIds)) {
                do {
                    number += 1;
                } while (taken.has(`${name} (${number})`));

                taken.add(`${name} (${number})`);
                names.set(deviceId, `${name} (${number})`);
            }
        }
    }

    return names;
}

/**
 * The positions with each device called what phoneNames calls it.
 *
 * A position carries no name of its own: the model lives in the device's own
 * document, and a device that has not written one yet stays unnamed.
 */
export function labelledPositions(positions, labels, grants) {
    const seen = new Map();

    for (const position of positions) {
        const phones = seen.get(position.memberId) ?? new Map();

        phones.set(deviceIdOf(position.positionId), null);
        seen.set(position.memberId, phones);
    }

    const names = new Map([...seen].map(([memberId, unnamed]) => [
        memberId,
        phoneNames(memberId, grants.get(memberId)?.phones ?? new Map(), labels, unnamed),
    ]));

    return positions.map((position) => {
        const named = names.get(position.memberId).get(deviceIdOf(position.positionId));

        return named === null || named === position.deviceName
            ? position
            : { ...position, deviceName: named };
    });
}
