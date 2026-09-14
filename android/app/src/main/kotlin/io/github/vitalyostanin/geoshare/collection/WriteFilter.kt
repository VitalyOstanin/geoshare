package io.github.vitalyostanin.geoshare.collection

import java.time.Duration
import java.time.Instant
import kotlin.math.asin
import kotlin.math.cos
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

    return Duration.between(last.takenAt, next.takenAt) >= mode.writeSilence
}

/** The radius the distances here are measured on, in metres. */
const val EARTH_RADIUS_METRES: Double = 6_371_000.0

/** Haversine: the fixes compared here are metres apart, not continents. */
fun distanceMetres(from: Fix, to: Fix): Double {
    val fromLatitude = Math.toRadians(from.latitude)
    val toLatitude = Math.toRadians(to.latitude)
    val deltaLatitude = toLatitude - fromLatitude
    val deltaLongitude = Math.toRadians(to.longitude - from.longitude)

    val a =
        sin(deltaLatitude / 2).pow(2) +
            cos(fromLatitude) * cos(toLatitude) * sin(deltaLongitude / 2).pow(2)

    return 2 * EARTH_RADIUS_METRES * asin(sqrt(a))
}
