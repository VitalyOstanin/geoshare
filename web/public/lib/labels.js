// What this member calls the phones and the people of the group, for
// themselves alone: "new phone" rather than a model number, "grandma" rather
// than the name on her account.
//
// A phone is named by its full address, account and phone joined by a dot, so
// that two phones of one model are two names. The application keeps the same
// document and takes the names in the same order.

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
    try {
        const kept = store.getItem(LABELS_KEY + memberId);

        return kept ? labelsFrom(JSON.parse(kept)) : NO_LABELS;
    } catch {
        return NO_LABELS;
    }
}

export function keepLabels(memberId, labels, store = globalThis.localStorage) {
    try {
        store.setItem(LABELS_KEY + memberId, JSON.stringify({
            devices: Object.fromEntries(labels.phones),
            members: Object.fromEntries(labels.members),
        }));
    } catch {
        // Only the next visit is worse off: it waits for the database.
    }
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
 * The positions with each phone called what this member called it, else what
 * its owner called it, else its model.
 */
export function labelledPositions(positions, labels, grants) {
    return positions.map((position) => {
        const deviceId = position.positionId.slice(position.positionId.indexOf('.') + 1);
        const named = labels.phones.get(position.positionId)
            ?? grants.get(position.memberId)?.phones?.get(deviceId)?.name
            ?? null;

        return named === null || named === position.deviceName
            ? position
            : { ...position, deviceName: named };
    });
}
