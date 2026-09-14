package io.github.vitalyostanin.geoshare.map

import java.time.Instant

/**
 * Where one phone last said it was.
 *
 * One of these per phone rather than per member: a member can carry two, and
 * each of them says where it is. What ties them together is the owner, which
 * is what the colour, the face and the name are taken from.
 *
 * The fields the writer does not guarantee are nullable: a document written by
 * an older build, or one still on its way to the server, is worth showing with
 * the parts it does have rather than dropping.
 */
data class MemberPosition(
    /** The document this came out of, which is what tells two phones of one member apart. */
    val positionId: String,
    val memberId: String,
    /** What the phone calls itself, shown when a member has more than one on the map. */
    val deviceName: String?,
    val latitude: Double,
    val longitude: Double,
    /** When the fix was taken, by the clock of the phone that took it. */
    val takenAt: Instant,
    /**
     * When the database accepted it. Missing for a moment on the phone that
     * wrote it: the local copy carries the write before the server stamps it.
     */
    val writtenAt: Instant?,
    val mode: String?,
    val batteryPercent: Int?,
    /** Whether the phone was on a charger; false for the builds that wrote no such field. */
    val charging: Boolean = false,
    /**
     * How wide the answer is, in metres, where the phone that wrote it said so.
     *
     * Missing for a build that wrote no such field, and for a fix whose
     * provider named no width at all — both of which are drawn as a marker
     * without a circle rather than as a circle of a made-up size.
     */
    val accuracyMetres: Double? = null,
) {
    /**
     * The moment the age is counted from.
     *
     * The server stamp is preferred because it is the one clock every member
     * shares; the phone that took the fix may be minutes off, and that
     * difference would show up as an age nobody can explain.
     */
    val knownAt: Instant get() = writtenAt ?: takenAt
}
