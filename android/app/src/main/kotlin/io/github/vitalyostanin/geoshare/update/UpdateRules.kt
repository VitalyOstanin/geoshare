package io.github.vitalyostanin.geoshare.update

import java.time.Duration
import java.time.Instant

/** How often the app asks GitHub for a newer release on its own. */
val CHECK_INTERVAL: Duration = Duration.ofHours(24)

private val VERSION = Regex("""(\d+)\.(\d+)\.(\d+)""")

/**
 * Reads `X.Y.Z` into its three numbers, or null for anything else. [text]
 * may carry a build suffix after the number (`0.1.1-debug`), which says
 * nothing about the release it was built from and is ignored.
 */
internal fun parseVersion(text: String): List<Int>? {
    val match = VERSION.matchEntire(text.substringBefore('-').substringBefore('+'))
        ?: return null

    return match.groupValues.drop(1).map { it.toIntOrNull() ?: return null }
}

/**
 * Whether [found] is a later release than [current], compared number by
 * number so that `0.1.10` follows `0.1.9`. A version that does not parse
 * is never newer: an odd tag on GitHub must not nag anyone to update.
 */
fun isNewerVersion(found: String, current: String): Boolean {
    val foundParts = parseVersion(found) ?: return false
    val currentParts = parseVersion(current) ?: return false

    for ((a, b) in foundParts.zip(currentParts)) {
        if (a != b) return a > b
    }

    return false
}

/**
 * Whether a notification for [found] is due: it is newer than [current] and
 * was not announced already ([lastNotified] is the version the last
 * notification named, null if none was shown). One notification per
 * version, however many daily checks find it.
 */
fun shouldNotify(lastNotified: String?, found: String, current: String): Boolean =
    isNewerVersion(found, current) && found != lastNotified

/**
 * Whether a background check is due at [now], given when the last one ran.
 * A [lastCheck] in the future (the clock was set back) counts as due,
 * otherwise checks would stay blocked until the clock caught up.
 */
fun isCheckDue(lastCheck: Instant?, now: Instant): Boolean =
    lastCheck == null || lastCheck.isAfter(now) || !now.isBefore(lastCheck.plus(CHECK_INTERVAL))
