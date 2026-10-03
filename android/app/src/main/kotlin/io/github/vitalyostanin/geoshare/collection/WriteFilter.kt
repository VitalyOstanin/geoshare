package io.github.vitalyostanin.geoshare.collection

import java.time.Duration
import java.time.Instant
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class Fix(
    val latitude: Double,
    val longitude: Double,
    val takenAt: Instant,
    /**
     * How wide the provider says its answer is, in metres, or nothing at all
     * where it did not say.
     *
     * Carried because a position is a claim of two parts — where, and how
     * sure — and a phone whose satellites are being jammed answers with the
     * second part telling the truth long after the first has stopped.
     */
    val accuracyMetres: Double? = null,
    /**
     * Which of the phone's ways of knowing answered, in the platform's own
     * word for it: `gps` for the satellites, `network` for the towers and the
     * Wi-Fi, `fused` for Play Services having picked one of them itself.
     *
     * Carried because the question every other rule here is in aid of — is
     * this position a lie the satellites were told — cannot be asked of an
     * answer that never came from a satellite. A tower fix a kilometre wide
     * and a spoofed fix a kilometre away read the same on a map and mean
     * opposite things, and this is the field that tells them apart.
     */
    val from: String? = null,
    /**
     * How long the phone had been awake when the fix was taken, or nothing
     * where that is not known.
     *
     * A second clock, because the first one moves. The wall clock of a phone
     * is set from the network and from the satellites, and both of those can
     * step it: a step of a few seconds between two fixes turns a phone
     * standing still into one that travelled. This count runs from the last
     * boot and nothing sets it back.
     *
     * It is null for a fix read back from the trail file, and that is the
     * point: the count starts again at every boot, so two of them may only be
     * compared within one run of the process.
     */
    val sinceBoot: Duration? = null,
    /** The rest of what the provider said, kept for the record. */
    val reported: Reported? = null,
)

/**
 * Whether a fix is worth a write.
 *
 * Writes are what wake the other side's subscription, so the threshold is not
 * only about the free quota: a phone lying on a table would otherwise keep
 * everyone's screen busy redrawing the same marker.
 *
 * The thresholds belong to the mode rather than to this file. A phone asking
 * the chip four times a minute because somebody is watching should be saying
 * what it finds on the same terms; a phone asking once a quarter of an hour
 * should not.
 */
fun shouldWrite(last: Fix?, next: Fix, mode: CollectionMode): Boolean {
    if (last == null) {
        return true
    }

    if (distanceMetres(last, next) >= mode.writeDistance) {
        return true
    }

    return apart(last, next) >= mode.writeSilence
}

/**
 * How old a fix is at a given moment, counted on the steadier clock where both
 * sides of the comparison carry it.
 *
 * The same reasoning as [apart], asked of one fix against now rather than of
 * two fixes against each other: a clock stepped between the fix and the
 * moment it is judged at makes a fresh answer look stale, or a stale one
 * fresh. [nowSinceBoot] is how long the phone has been awake at that moment,
 * and it is absent wherever the caller has no such count to offer — a test
 * holding a fixed wall clock, or a fix read back from the trail file, which
 * carries none of its own.
 */
fun since(fix: Fix, now: Instant, nowSinceBoot: Duration? = null): Duration {
    val fixBoot = fix.sinceBoot

    if (fixBoot != null && nowSinceBoot != null) {
        return nowSinceBoot.minus(fixBoot)
    }

    return Duration.between(fix.takenAt, now)
}

/**
 * How much time passed between two fixes, counted on the steadier of the two
 * clocks they carry.
 *
 * The wall clock of a phone is set from the network and from the satellites,
 * and either can step it between one fix and the next. A step of a minute
 * backwards turns a phone standing still into one that crossed the city, and
 * a step forwards hides a jump that did happen — and under a false signal the
 * wall clock is part of the lie, since the time it is set from comes off the
 * same satellites. Where both fixes know how long the phone had been awake,
 * that count answers instead: it runs from the last boot and nothing sets it
 * back.
 *
 * The count starts again at every boot and is absent for a fix read back from
 * the trail file, so the wall clock still answers whenever the two fixes do
 * not both carry it. Two counts from either side of a reboot are never
 * compared, because a fix only carries one while the process that took it is
 * still running.
 */
fun apart(from: Fix, to: Fix): Duration {
    val fromBoot = from.sinceBoot
    val toBoot = to.sinceBoot

    if (fromBoot != null && toBoot != null) {
        return toBoot.minus(fromBoot)
    }

    return Duration.between(from.takenAt, to.takenAt)
}

/**
 * How long after the first of [run] each of its fixes was taken, by [apart].
 *
 * One clock for the whole run, so that a period found between two of its
 * fixes can be laid off from any other without crossing to the wall clock.
 */
fun sinceFirst(run: List<Fix>): List<Duration> = run.map { apart(run.first(), it) }

/**
 * Whether [next] is the answer [before] already was, handed over a second time.
 *
 * Opening a subscription makes the provider deliver the position it last
 * found, which is the one a moment-old single request has just returned. Read
 * twice, it goes through the rules twice, and a fix left out comes back as the
 * fix before it that agrees with it.
 */
fun sameAnswer(before: Fix?, next: Fix): Boolean = before != null &&
    before.takenAt == next.takenAt &&
    before.latitude == next.latitude &&
    before.longitude == next.longitude

/**
 * The radius the distances here are measured on, in metres: the mean sphere,
 * the same one the page measures on.
 */
const val EARTH_RADIUS_METRES: Double = 6_371_008.8

/** Haversine: the fixes compared here are metres apart, not continents. */
fun distanceMetres(from: Fix, to: Fix): Double {
    val fromLatitude = Math.toRadians(from.latitude)
    val toLatitude = Math.toRadians(to.latitude)
    val deltaLatitude = toLatitude - fromLatitude
    val deltaLongitude = Math.toRadians(to.longitude - from.longitude)

    val a =
        sin(deltaLatitude / 2).pow(2) +
            cos(fromLatitude) * cos(toLatitude) * sin(deltaLongitude / 2).pow(2)

    // Rounding carries a past one for points on opposite sides of the Earth;
    // capped, so that asin is never asked for what it has no answer to.
    return 2 * EARTH_RADIUS_METRES * asin(sqrt(min(a, 1.0)))
}
