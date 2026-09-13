package io.github.vitalyostanin.geoshare.collection

import java.time.Duration
import java.time.Instant

// Telling a jammed receiver from a spoofed one.
//
// The jump filter asks whether a position can be true. These ask something
// else: whether the radio is being interfered with at all, and in which of the
// two ways. The answer does not place the phone anywhere — it says how much
// the provider's own answer is worth.
//
// The two signatures are the ones Android's own guidance names. A receiver
// turns its amplifier down against any strong interference, so the gain falls
// either way; what separates the cases is the noise. Jamming buries the
// satellites in it, and the signal-to-noise ratio falls with the gain. A
// spoofer does the opposite: it hands the receiver a clean, loud signal, and
// the ratio rises while the gain drops.

/** What the radio looks like it is being subjected to. */
enum class Interference {
    /** Nothing worth reporting: neither measure has moved. */
    Quiet,

    /** The gain is down and the satellites with it: noise. */
    Jamming,

    /** The gain is down and the signal is louder than before: a false signal. */
    Spoofing,

    /** Not enough measurements to say anything at all. */
    Unknown,
}

/**
 * Two averages of one measure: the recent epochs against the ones before them.
 *
 * Averages rather than single readings because a single epoch moves on its own
 * — a hand over the phone, a bus going past — and because what is being looked
 * for is a change rather than a level: there is no absolute gain that means
 * interference, only a gain lower than this receiver's own was a minute ago.
 */
data class Epochs(val recent: Double?, val earlier: Double?) {
    /** How far the measure has moved, or nothing when one of the halves is missing. */
    val moved: Double? = if (recent == null || earlier == null) null else recent - earlier
}

/**
 * How far the gain has to fall before the fall is taken for interference.
 *
 * Two decibels. Chosen rather than measured: Android's guidance says which way
 * the measures move and not by how much, so this is a first setting to be
 * corrected against the logs of phones that are actually being jammed.
 */
const val GAIN_FALL_DB: Double = 2.0

/** How far the signal-to-noise ratio has to move to count as having moved. */
const val NOISE_MOVE_DB: Double = 2.0

/**
 * What the two measures say, taken together.
 *
 * Read in that order — the gain first — because the gain falling is what says
 * there is interference at all, and the noise only says which kind.
 */
fun interferenceFrom(gain: Epochs, noise: Epochs): Interference {
    val gainMoved = gain.moved ?: return Interference.Unknown
    val noiseMoved = noise.moved ?: return Interference.Unknown

    if (gainMoved > -GAIN_FALL_DB) {
        return Interference.Quiet
    }

    return when {
        noiseMoved <= -NOISE_MOVE_DB -> Interference.Jamming

        noiseMoved >= NOISE_MOVE_DB -> Interference.Spoofing

        // The gain is down and the satellites are as loud as they were. Not
        // nothing, and not enough: a spoofer that matched the true signal's
        // strength looks like this, and so does a car roof.
        else -> Interference.Quiet
    }
}

/**
 * Whether the clock of the fix and the clock of the phone disagree.
 *
 * A receiver takes its time from the satellites, so a false signal can carry a
 * false clock, and the disagreement shows before anything else does. What it
 * cannot do is stand alone: a phone whose own clock is set from the same
 * satellites moves with the lie, and one without a network has no other clock
 * to be right by.
 */
fun clockDisagrees(takenAt: Instant, phoneSays: Instant, tolerance: Duration): Boolean =
    Duration.between(takenAt, phoneSays).abs() > tolerance

/**
 * How far apart the two clocks may be before it means anything.
 *
 * A fix is written down as it arrives, and the moment it carries is when the
 * receiver worked it out, so a second or two apart is ordinary. Half a minute
 * is not.
 */
val CLOCK_TOLERANCE: Duration = Duration.ofSeconds(30)
