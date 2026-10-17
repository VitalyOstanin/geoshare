package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.collection.Fix

/**
 * A position as it goes into the document: where the device was, and whether
 * the cable was in it at the moment that was decided.
 *
 * The pair rather than the fix alone, because the same place written with the
 * cable newly in is a different document, while the same place written with
 * nothing changed is not.
 */
internal data class Written(
    val fix: Fix,
    val charging: Boolean,
    /**
     * Whether the receiver had not settled when the fix was taken.
     *
     * Carried beside the fix rather than worked out again at the write: the
     * count of satellites it rests on belongs to the epoch the fix came out
     * of, and by the time the document is put together the receiver has moved
     * on. It is part of what makes two writes the same write, so a device whose
     * receiver settles while it stands still says so once.
     */
    val settling: Boolean = false,
)

/**
 * What is said when the last position is said again, or nothing where the
 * device has no position to say.
 *
 * The last position written is preferred to the last one ever known, since it
 * is the one the member has been shown. The same position carries the same
 * note about the receiver: it is a statement about the answer, and the answer
 * has not changed. Worked out afresh it would say something about an epoch
 * this position never came out of.
 */
internal fun sayAgainOf(
    lastFix: Fix?,
    lastKnown: Fix?,
    lastWritten: Written?,
    charging: Boolean,
): Written? {
    val known = lastFix ?: lastKnown ?: return null

    return Written(known, charging, lastWritten?.settling == true)
}
