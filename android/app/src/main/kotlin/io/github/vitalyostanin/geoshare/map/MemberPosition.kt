package io.github.vitalyostanin.geoshare.map

import java.time.Instant

/**
 * Where one member last said they were.
 *
 * The fields the writer does not guarantee are nullable: a document written by
 * an older build, or one still on its way to the server, is worth showing with
 * the parts it does have rather than dropping.
 */
data class MemberPosition(
    val memberId: String,
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
