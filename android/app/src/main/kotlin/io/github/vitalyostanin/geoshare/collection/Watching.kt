package io.github.vitalyostanin.geoshare.collection

import java.time.Duration
import java.time.Instant

/**
 * How long a claim to be looking is worth, counted from the moment the server
 * wrote it down.
 *
 * Longer than the refresh below, so that one write lost to a bad connection
 * does not drop the other phones out of their fast mode; short enough that a
 * viewer who disappears without a word stops being one within minutes.
 *
 * The claim itself carries no end of its own. A viewer saying when their claim
 * runs out would be saying it by their own clock, and a clock a few minutes
 * out either way would hold the group in the fast mode or never put it there
 * at all; the rules refuse anything but the server's own moment, and the three
 * minutes are added here, by whoever reads it.
 */
val WATCH_AHEAD: Duration = Duration.ofMinutes(3)

/** How often the claim is pushed forward while the map is on screen. */
val WATCH_REFRESH: Duration = Duration.ofMinutes(1)

/** A member who says they are looking, and until when the claim counts. */
data class Watcher(val memberId: String, val until: Instant)

/**
 * Who is looking, each of them named once.
 *
 * A member watching from two places — a phone in one hand and the page on a
 * laptop — leaves two claims behind, and the screen has one person to name for
 * them. Claims that have run out are dropped here rather than on the way in,
 * because nothing arrives at the moment one expires: the list is re-read as
 * the clock moves.
 */
fun watchingMembers(watchers: List<Watcher>, now: Instant): List<String> = watchers
    .filter { it.until.isAfter(now) }
    .map { it.memberId }
    .distinct()
    .sorted()
