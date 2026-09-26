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
    /**
     * How the phone that wrote it says it arrived at the answer, in the word
     * it wrote rather than in this reader's language.
     *
     * Missing for a build that wrote no such field, which is a position whose
     * source is not known rather than one arrived at by no means at all.
     */
    val foundBy: String? = null,
    /**
     * Whether the phone that wrote this said its receiver had not settled
     * since it was switched on.
     *
     * False for a build that wrote no such field and for every position of a
     * phone that has been up a while, which are the same thing to a reader:
     * nothing to say about how the answer was arrived at beyond what the other
     * fields say. True is a position worth less than its width suggests — see
     * the Android client's Settling.kt for the minutes this covers.
     */
    val settling: Boolean = false,
    /**
     * How many answers this phone has left out since it wrote this position.
     *
     * Nought — and for the builds that wrote no such field, missing — means
     * nothing is being thrown away and the position is simply the latest one.
     * A number here says the phone is working, getting answers and refusing
     * them, which is what being lied to looks like from the outside; without
     * it such a phone is indistinguishable from one in a drawer.
     */
    val leftOutCount: Int? = null,
    /** Why the last of them was left out, in the word the phone wrote. */
    val leftOutReason: String? = null,
    /** When the run of left-out answers began. */
    val leftOutSince: Instant? = null,
    /**
     * Whether the position lies where a false signal is known to put phones.
     *
     * Not a field of the document: the screen works it out against the
     * group's list of such places, because a position written before a place
     * was named stays in the database until the phone has something true to
     * write over it. See FalseMarks.kt.
     */
    val inFalsePlace: Boolean = false,
) {
    /**
     * The moment the age is counted from.
     *
     * The server stamp is preferred because it is the one clock every member
     * shares; the phone that took the fix may be minutes off, and that
     * difference would show up as an age nobody can explain.
     */
    val knownAt: Instant get() = writtenAt ?: takenAt

    /**
     * How old the position itself is, as opposed to how long ago the phone
     * last spoke.
     *
     * The two part whenever a phone says a position again, which is what a
     * phone does while its satellites are being spoofed and it has nothing new
     * it believes: the stamp moves and the position does not. Capped at the
     * server stamp, because the rest of the difference would be somebody's
     * clock rather than the age of anything.
     */
    val positionAt: Instant get() = minOf(takenAt, knownAt)
}
