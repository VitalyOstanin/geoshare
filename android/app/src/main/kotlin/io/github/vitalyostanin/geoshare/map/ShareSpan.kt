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
 * Which choice a permission that stands answers to.
 *
 * A permission that has run out reads as none: it is the same thing to the
 * reader, whose marker is gone either way, and the screen should not offer to
 * end something that has already ended.
 *
 * A permission ending at some point today is not one of the choices — it was
 * given hours ago and has been counting down since — so it answers to none of
 * them, and the screen says when it ends instead.
 */
fun spanOf(until: Instant?, now: Instant): ShareSpan? = when {
    until == null || !until.isAfter(now) -> ShareSpan.None
    until.isAfter(ENDLESS_FROM) -> ShareSpan.Forever
    else -> null
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
