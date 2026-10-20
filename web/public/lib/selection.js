// The rules for whose card is open, kept apart from the map and from the strip
// of members the card stands above.
//
// Which member is being looked at is a question the map, the strip and the
// circle of doubt all ask, and none of them owns the answer: it is plain data
// about what the viewer picked, written here so that it can be checked without
// a browser and read the same way from every one of them.
//
// A selection is either `null` — nothing open — or `{ key, positionId }`:
//   - `key` is the marker's key on the map. Members standing close together
//     share one marker, and its key is their ids joined by a comma.
//   - `positionId` is which member's card is shown, or `null` when the marker
//     covers several members and what is shown instead is the list of them.

/**
 * What tapping a marker on the map does to the current selection.
 *
 * A second tap on the very same marker closes its card, rather than
 * reopening the same thing the viewer just looked at. Otherwise a marker
 * with exactly one member under it jumps straight to that member's card: a
 * list of one would just be an extra tap to reach the only member on it.
 * With several members there is no single card to jump to, so the list of
 * them opens instead, and a marker somehow holding nobody opens nothing at
 * all — an empty list would tell the viewer nothing.
 *
 * @param selection what is open now
 * @param key the marker that was tapped
 * @param positionIds the members standing under that marker
 */
export function markerTapped(selection, key, positionIds) {
    if (selection !== null && selection.key === key) {
        return null;
    }

    if (positionIds.length === 0) {
        return null;
    }

    return { key, positionId: positionIds.length === 1 ? positionIds[0] : null };
}

/**
 * Whether tapping a member in the strip closes their card instead of opening
 * it: it does when that very member's card is the one open, the same as a
 * second tap on their marker.
 *
 * @param selection what is open now
 * @param positionId the member whose entry was tapped
 */
export function memberTapCloses(selection, positionId) {
    return selection !== null && selection.positionId === positionId;
}

/**
 * Opens one member's card directly, by naming the member rather than the
 * marker.
 *
 * Used when a member is picked from the strip or from a marker's list, where
 * the marker's key is already known and there is no ambiguity to resolve.
 *
 * The member whose card is open is taken out of the marker they shared (see
 * clusterMembers), so the list they were picked from is kept by its key: the
 * way back to it is a marker that only comes back once the card is left.
 *
 * @param key the marker the member is shown under
 * @param positionId the member whose card is wanted
 * @param list the marker for several the member was picked from, if any
 */
export function openMember(key, positionId, list = null) {
    return list === null ? { key, positionId } : { key, positionId, list };
}

/**
 * Steps back from one member's card to the list of everyone under the same
 * marker.
 *
 * The marker is the one the member was picked from, kept in `list`: while
 * their card was open they stood on a pin of their own, and the marker for
 * all of them comes back with the next drawing.
 *
 * @param selection what is open now
 */
export function listShown(selection) {
    return selection === null ? null : { key: selection.list ?? selection.key, positionId: null };
}

/**
 * Which member ids the strip should highlight as "their card is open".
 *
 * With the list open there is no single member to point at, so every member
 * under that marker is highlighted instead: the list names all of them, and
 * the strip says the same thing about the same people. Who stands under the
 * marker is handed in rather than looked up here, because that belongs to the
 * map — this module knows what was picked, not where anybody is.
 *
 * @param selection what is open now
 * @param positionIds the members under the marker the selection names
 */
export function shownPositionIds(selection, positionIds) {
    if (selection === null) {
        return new Set();
    }

    return selection.positionId === null ? new Set(positionIds) : new Set([selection.positionId]);
}

/**
 * The member whose accuracy circle should be drawn, or `null` for none.
 *
 * With the list open this is `null` on purpose: the list exists precisely
 * because it is not yet decided which of several members is meant, so there
 * is nobody the circle could honestly be drawn around.
 *
 * @param selection what is open now
 */
export function selectedPositionId(selection) {
    return selection === null ? null : selection.positionId;
}
