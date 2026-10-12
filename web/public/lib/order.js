// The one way both clients put ids in order.

/**
 * Compares two ids the way the Kotlin side does: by the characters themselves.
 *
 * `localeCompare` answers a different question — how a reader of some language
 * would file two words — and for account ids, which mix case and digits, it
 * gives a different order than the device. The panel, the pins and the list of
 * documents all pass through here so that one family sees one order.
 */
export function compareIds(one, other) {
    if (one === other) {
        return 0;
    }

    return one < other ? -1 : 1;
}
