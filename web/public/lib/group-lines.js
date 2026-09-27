// The group seen whole, as the panel that runs it lists it.
//
// The same arithmetic the phone does in GroupLine.kt, and for the same reason:
// everything here is read from what the group already writes — the roster, the
// profiles every member may read, and the grants this viewer has been given —
// so nothing new has to be asked of anybody for the panel to be drawn.

import { compareIds } from './order.js';
import { NO_LABELS, phoneNames } from './labels.js';
import { mayBeMadeAdmin, mayBeRemoved, mayStopRunning, runsTheGroup } from './membership.js';

/** As much of a phone's id as stands in for a name nobody wrote. */
const PHONE_ID_SHOWN = 8;

/**
 * One member as the panel has to draw them.
 *
 * The phones are counted rather than named: their identities mean nothing to
 * anybody reading, and the question being asked is whether the member is in
 * this at all. The count is of what is visible from here rather than of what
 * the member owns — a grant is closed to everybody it does not name — and the
 * panel says as much beside it.
 */
export function groupOverview(roster, profiles, grants, ownMemberId, labels = NO_LABELS) {
    return roster.members
        .map((memberId) => ({
            memberId,
            displayName: profiles?.get(memberId)?.displayName ?? null,
            runsTheGroup: runsTheGroup(roster, memberId),
            isYou: memberId === ownMemberId,
            phonesShared: grants.get(memberId)?.devices.length ?? 0,
            // Nobody removes themselves here: leaving a group is not the same
            // act as taking somebody out of it, and the rules refuse a roster
            // left with nobody to run it.
            removable: memberId !== ownMemberId && mayBeRemoved(roster, memberId, ownMemberId),
            mayBeMadeAdmin: mayBeMadeAdmin(roster, memberId, ownMemberId),
            mayStopRunning: mayStopRunning(roster, memberId, ownMemberId),
            phones: phoneLines(memberId, grants.get(memberId)?.phones ?? new Map(), labels),
        }))
        .sort(inReadingOrder);
}

/**
 * The person looking at the panel first, and the rest by the name shown.
 *
 * Their own line leads because the panel is read by somebody checking on the
 * others: whose phone this is comes first, and the rest of the list answers the
 * second question. After that the order is by name, so that a list read twice
 * reads the same way — the order of the roster is the order people happened to
 * join in, which no reader can predict.
 */
function inReadingOrder(one, other) {
    const own = Number(other.isYou) - Number(one.isYou);

    if (own !== 0) {
        return own;
    }

    return compareIds(one.displayName ?? one.memberId, other.displayName ?? other.memberId);
}

/**
 * A member's phones as they last said how they are, whether or not this viewer
 * may see where they are: an administrator reads these to tell a phone that
 * went quiet from one whose marker is stuck. Each is called what the map calls
 * it (phoneNames), else by the start of its id.
 */
export function phoneLines(memberId, phones, labels = NO_LABELS) {
    const names = phoneNames(memberId, phones, labels);

    return [...phones]
        .map(([deviceId, phone]) => ({
            // The address the phone's siren is called at.
            phoneId: `${memberId}.${deviceId}`,
            name: names.get(deviceId) ?? deviceId.slice(0, PHONE_ID_SHOWN),
            onlineAt: phone.onlineAt,
            batteryPercent: phone.batteryPercent,
            charging: phone.charging,
        }))
        .sort((one, other) => compareIds(one.name, other.name));
}
