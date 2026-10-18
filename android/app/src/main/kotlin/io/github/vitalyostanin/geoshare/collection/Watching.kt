package io.github.vitalyostanin.geoshare.collection

import java.time.Duration
import java.time.Instant

/**
 * How long a claim to be looking is worth, counted from the moment the server
 * wrote it down.
 *
 * Longer than the refresh below, so that one write lost to a bad connection
 * does not drop the other devices out of their fast mode; short enough that a
 * viewer who disappears without a word stops being one within minutes.
 *
 * The claim itself carries no end of its own. A viewer saying when their claim
 * runs out would be saying it by their own clock, and a clock a few minutes
 * out either way would hold the group in the fast mode or never put it there
 * at all; the rules refuse anything but the server's own moment, and the seven
 * minutes are added here, by whoever reads it.
 */
val WATCH_AHEAD: Duration = Duration.ofMinutes(7)

/**
 * How often the claim is pushed forward while the map is on screen.
 *
 * Every device of the group listens to the whole collection, so each push is
 * read once per device: in a group of twenty a push a minute is over a
 * thousand reads an hour for one open map. A viewer who leaves properly
 * withdraws the claim at once, so the length only matters for one who
 * vanished, and those few minutes cost less than the reads did.
 */
val WATCH_REFRESH: Duration = Duration.ofMinutes(5)

/**
 * How soon a push that failed is tried again.
 *
 * Well inside the two minutes [WATCH_AHEAD] leaves over [WATCH_REFRESH]: at
 * this spacing a claim whose push was lost would otherwise run out before the
 * next one, and the devices would drop out of their fast mode while the map is
 * still open.
 */
val WATCH_RETRY: Duration = Duration.ofSeconds(30)

/**
 * How long the map may go untouched before it stops counting as somebody
 * looking.
 *
 * A map on a screen that never goes dark — a device on a charger with the
 * screen kept on, or one nobody locked — would otherwise hold every device of
 * the group in the thirty-second mode around the clock, the most expensive
 * thing the group does. Five minutes, the same as the page (`IDLE_AFTER_MS` in
 * `web/public/lib/store.js`): whoever follows somebody home touches the map
 * now and then, and a forgotten one stops costing soon after.
 */
val IDLE_AFTER: Duration = Duration.ofMinutes(5)

/**
 * How much longer a map last touched at [touchedAt] counts as looked at, at
 * [now]; both on one monotonic clock, in milliseconds. Zero once it no longer
 * does.
 */
fun lookedAtFor(touchedAt: Long, now: Long): Duration =
    Duration.ofMillis((IDLE_AFTER.toMillis() - (now - touchedAt)).coerceAtLeast(0))

/** A member who says they are looking, and until when the claim counts. */
data class Watcher(val memberId: String, val until: Instant)

/**
 * Who is looking, each of them named once.
 *
 * A member watching from two places — a device in one hand and the page on a
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

/**
 * The moments until which somebody who may see this device is looking at it.
 *
 * One open map used to put every device of the group into the fast mode,
 * whether the person looking could see it or not; in a group of twenty that
 * is twenty devices writing for a map that shows three of them. A claim counts
 * here only when it is the owner's own — another device of the same account —
 * or when the owner's grant names the viewer, and then no longer than the
 * permission lasts.
 *
 * [sharedWith] is null until the grant has been read: a device that does not
 * know yet whom it shares with answers everybody, as it did before, rather
 * than nobody for the second or so the subscription takes.
 *
 * [sought] is whether the group is looking for this very device right now:
 * while a search stands the rules open this device's position to everybody in
 * the group, whatever the grant says, and a viewer this device would otherwise
 * leave out of the fast mode is exactly the one the search is for.
 */
fun watchedUntil(
    watchers: List<Watcher>,
    ownerId: String,
    sharedWith: Map<String, Instant>?,
    sought: Boolean = false,
): List<Instant> = watchers.mapNotNull { watcher ->
    when {
        sought || sharedWith == null || watcher.memberId == ownerId -> watcher.until
        else -> sharedWith[watcher.memberId]?.let { minOf(watcher.until, it) }
    }
}

/**
 * The moments the watching of this device runs out, over every group it is
 * in.
 *
 * A device watched in one group is collected for as closely as if every group
 * were watching it: the same positions go to all of them, so a viewer in the
 * family keeps them fresh for the hike as well. The groups are only ever
 * joined here, never weighed against each other.
 */
fun watchedInAnyGroup(perGroup: List<List<Instant>>): List<Instant> = perGroup.flatten().sorted()
