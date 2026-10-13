// What the devices of a member say about themselves, apart from where they are.
//
// A device writes these into a document of its member's own, beside the
// position rather than inside it: whether it is in touch, how charged it is
// and whether it is on a cable are not a place, and the filter that holds a
// position back when the answers look forged has nothing to say about them.

/**
 * The member a device's id belongs to: the part before the dot. An id with no
 * dot in it, from before positions were kept per device, is the member's own.
 */
export function memberIdOf(phoneId) {
    const dot = phoneId.indexOf('.');

    return dot < 0 ? phoneId : phoneId.slice(0, dot);
}

/** The device a device's id stands for: everything after the first dot. */
export function deviceIdOf(phoneId) {
    return phoneId.slice(phoneId.indexOf('.') + 1);
}

/** How many devices of one member this page will follow. The rules say the same. */
const PHONES_PER_MEMBER = 10;

/** As much of a name as fits on a card. The devices cut it to the same. */
const NAME_LIMIT = 40;

/**
 * The devices in one document, by the id of each, with whatever in an entry is
 * not what a device writes left out: the rules hold the number of devices and
 * not their fields.
 */
export function phonesOf(data) {
    const phones = new Map();
    // The entries that are not devices are dropped before the count, so that
    // one of them does not take the place of a real device.
    const entries = Object.entries(data ?? {})
        .filter(([, entry]) => entry !== null && typeof entry === 'object')
        .slice(0, PHONES_PER_MEMBER);

    for (const [deviceId, entry] of entries) {
        phones.set(deviceId, {
            model: shortText(entry.model),
            name: shortText(entry.name),
            onlineAt: typeof entry.onlineAt?.toMillis === 'function' ? entry.onlineAt.toMillis() : null,
            mode: typeof entry.mode === 'string' ? entry.mode : null,
            batteryPercent: percent(entry.batteryPercent),
            charging: entry.charging === true,
        });
    }

    return phones;
}

function shortText(value) {
    return typeof value === 'string' && value !== '' ? value.slice(0, NAME_LIMIT) : null;
}

function percent(value) {
    return typeof value === 'number' && value >= 0 && value <= 100 ? Math.round(value) : null;
}

/**
 * The grants as the rest of the page reads them: which devices of a member this
 * viewer may follow, and what those devices said about themselves.
 *
 * The devices to follow are the ones the grant names together with the ones
 * the member's own document names, and only where the grant could be read:
 * the grant is what says this viewer may watch, and an administrator reads the
 * devices of everybody without being allowed to see where any of them is. A
 * grant is still read for its list because a device of an older build names
 * itself there and nowhere else.
 */
export function combinedGrants(named, phones) {
    const grants = new Map();

    for (const memberId of new Set([...named.keys(), ...phones.keys()])) {
        const own = phones.get(memberId) ?? new Map();
        const listed = named.get(memberId);
        const devices = listed === undefined ? [] : [...new Set([...listed, ...own.keys()])];

        grants.set(memberId, { memberId, devices, phones: own });
    }

    return grants;
}

/**
 * A position with what its device said about itself laid over it, where the
 * device said so after the position was written.
 *
 * The later of the two wins: a position carries the charge it was written
 * with, and while the answers are being thrown away that is the charge of an
 * hour ago. The moment the device was last in touch moves on with it, which is
 * what keeps the marker of a device that is working from going grey merely
 * because its position is being held back.
 */
export function withPhones(positions, grants) {
    return positions.map((position) => {
        const deviceId = deviceIdOf(position.positionId);
        const phone = grants.get(position.memberId)?.phones?.get(deviceId);

        if (!phone || phone.onlineAt === null || phone.onlineAt <= position.knownAt) {
            return position;
        }

        return {
            ...position,
            knownAt: phone.onlineAt,
            mode: phone.mode ?? position.mode,
            batteryPercent: phone.batteryPercent ?? position.batteryPercent,
            charging: phone.charging,
        };
    });
}
