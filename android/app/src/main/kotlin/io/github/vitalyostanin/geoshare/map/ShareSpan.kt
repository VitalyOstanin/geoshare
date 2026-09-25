package io.github.vitalyostanin.geoshare.map

import java.time.Duration
import java.time.Instant

/**
 * A permission that never ends, written as a moment far enough away to read as
 * never.
 *
 * One kind of value rather than two: the rules weigh a timestamp against the
 * clock, and an absent one would be a case of its own in every rule and in
 * both clients.
 */
val FOREVER: Instant = Instant.parse("9999-12-31T00:00:00Z")

/** Anything past this reads as a permission without an end rather than as a date. */
private val ENDLESS_FROM: Instant = Instant.parse("9000-01-01T00:00:00Z")

/**
 * How long a member lets somebody watch them.
 *
 * The choices are spans rather than moments: nobody picks a date to be visible
 * until, they pick "for the walk" or "for the day", and a permission that runs
 * out on its own is what makes giving one cheap.
 */
enum class ShareSpan {
    /** Not shared at all; the reader's marker disappears within seconds. */
    None,
    Hour,
    EightHours,
    Day,
    Forever,
}

/** When a permission given now would end, or null when nothing is given. */
fun shareUntil(span: ShareSpan, now: Instant): Instant? = when (span) {
    ShareSpan.None -> null
    ShareSpan.Hour -> now.plus(Duration.ofHours(1))
    ShareSpan.EightHours -> now.plus(Duration.ofHours(8))
    ShareSpan.Day -> now.plus(Duration.ofDays(1))
    ShareSpan.Forever -> FOREVER
}

/**
 * How a permission stands, which is not the same set of answers as the spans
 * it can be given for.
 *
 * A permission that has run out reads as none: it is the same thing to the
 * reader, whose marker is gone either way, and the screen should not offer to
 * end something that has already ended. A permission ending at some point
 * today is not one of the spans either — it was given hours ago and has been
 * counting down since — so it carries the moment it ends, and the screen says
 * that instead.
 *
 * Three answers of its own rather than a span that might be missing: a `when`
 * over spans had to keep a branch for the three that cannot arrive, which is a
 * branch nothing can be said about and nothing can test.
 *
 * Named for the sharing rather than for a standing: a standing is where an
 * account stands with respect to a group, which is a different question
 * answered in `membership`.
 */
sealed interface Sharing {
    /** Not shared, or shared until a moment that has passed. */
    data object Never : Sharing

    /** Shared with no end named. */
    data object Endless : Sharing

    /** Shared until this moment, which is still ahead. */
    data class Until(val moment: Instant) : Sharing
}

fun sharingOf(until: Instant?, now: Instant): Sharing = when {
    until == null || !until.isAfter(now) -> Sharing.Never
    until.isAfter(ENDLESS_FROM) -> Sharing.Endless
    else -> Sharing.Until(until)
}

/**
 * The permissions as they stand once one reader has been given a span.
 *
 * A permission taken away is a key that disappears rather than a moment
 * written in the past: the field is replaced whole on every write, and a key
 * nobody reads is one no rule has to weigh.
 */
fun withShare(
    sharedWith: Map<String, Instant>,
    readerId: String,
    span: ShareSpan,
    now: Instant,
): Map<String, Instant> {
    val until = shareUntil(span, now) ?: return sharedWith - readerId

    return sharedWith + (readerId to until)
}
