package io.github.vitalyostanin.geoshare.collection

import kotlin.math.abs

// Whether a satellite's own time runs along with the phone's.
//
// Every raw measurement carries the moment the satellite sent the signal, read
// off the signal itself. Between two epochs that moment has to advance by
// exactly as much as the epochs are apart: the satellite's clock and the
// phone's are both running through the same seconds, and the only thing that
// can make them differ is the flight of the signal, which changes by a few
// microseconds a second at the very most.
//
// This is the first of the three checks SPREE's navigation inspector makes,
// and the only one of them a phone can make at all: the other two want the
// ephemerides and the almanac, which Android does not hand out. What it
// catches is a false sky that is not synchronised with the real one — a replay
// of a recording, a relay holding the signal for a moment before sending it on
// — because the time such a signal carries is not the time it arrives at.
//
// Two clocks are sat against the satellite's, and both are needed:
//
// The receiver's own clock is precise. It is also taken from the satellites,
// so a false sky that moves the two together moves this one with them: a
// disagreement here is worth something, an agreement is worth nothing.
//
// The phone's clock since it was switched on is the opposite. It runs off the
// phone's own oscillator and no signal can touch it, so an agreement here is
// worth something — but the epoch's stamp of it is put on by the layer that
// assembles the epoch rather than by the radio, and on the phones measured it
// jitters by milliseconds. It says whether the receiver's whole scale has been
// walked, not by how many microseconds anything is off.

/** One signal of one satellite, named the way the receiver names it. */
data class Signal(
    val constellation: Constellation,
    val id: Int,
    /** Which of the satellite's signals this is, where the receiver says. */
    val carrierHz: Double?,
)

/** What one epoch said about one signal, beside the two clocks of that epoch. */
data class SatelliteTick(
    val signal: Signal,
    /**
     * The moment the satellite sent this signal, on its own system's scale.
     *
     * Counted from the start of the week for every system but GLONASS, which
     * counts from the start of the day, so it wraps and the wrap has to be
     * undone before two of them are subtracted.
     */
    val sentNanos: Long,
    /** The receiver's clock at the epoch, on the satellites' scale. */
    val receiverNanos: Long,
    /** The phone's own clock since it was switched on, at the same epoch. */
    val sinceBootNanos: Long,
)

/** How far each clock has drifted from the satellite's between two epochs. */
data class ClocksApart(
    /** Against the receiver's clock, in nanoseconds. */
    val receiverNanos: Long,
    /** Against the phone's clock since boot, in nanoseconds. */
    val sinceBootNanos: Long,
)

/** What the pair of clocks says about a signal. */
enum class SatelliteClockVerdict {
    /** Both clocks advanced with the satellite: nothing to report. */
    Agrees,

    /**
     * The receiver's own clock did not advance with the satellite.
     *
     * The receiver is tracking something whose time does not run at the rate
     * its own solution does, which is what a replayed or relayed signal looks
     * like.
     */
    ReceiverApart,

    /**
     * The phone's clock since boot did not advance with the satellite.
     *
     * The heavier of the two: the one clock a false sky cannot reach says the
     * satellite's time went somewhere the phone's did not.
     */
    PhoneApart,

    /** The two epochs cannot be compared at all. */
    Unknown,
}

/** How long a week is, which is what every system but GLONASS counts within. */
const val WEEK_NANOS: Long = 604_800L * 1_000_000_000L

/** How long a day is, which is what GLONASS counts within. */
const val DAY_NANOS: Long = 86_400L * 1_000_000_000L

/**
 * How far the satellite may run from the receiver's clock before it is read as
 * a disagreement.
 *
 * A millisecond. Measured rather than chosen: over two logs of these phones,
 * 1896 pairs of epochs, the halves of a pair disagreed by 13 and 24
 * microseconds in the middle, by 42 and 130 at the nineteenth part in twenty,
 * and at their very worst by 649. A millisecond leaves that whole spread well
 * inside and still catches a relay holding the signal for the 300 metres of
 * light it takes to be worth holding.
 */
const val RECEIVER_APART_NANOS: Long = 1_000_000L

/**
 * The same against the phone's own clock since it was switched on.
 *
 * A hundred milliseconds, and the hundredfold difference is the epoch's stamp
 * rather than the radio: on the same 1896 pairs this one disagreed by 5
 * milliseconds in the middle and by 22 at its worst, because the stamp is put
 * on when the epoch is assembled. It is the coarse check, and it is the one a
 * false sky cannot talk its way around.
 */
const val PHONE_APART_NANOS: Long = 100_000_000L

/**
 * How far apart two epochs may be and still be compared.
 *
 * Five minutes. Further apart than that and the signal was lost and found
 * again in between, so what is being measured is the gap and not the clock.
 */
const val TICKS_APART_NANOS: Long = 5L * 60L * 1_000_000_000L

/** How much the satellite's own time advanced between two epochs. */
fun sentApart(before: SatelliteTick, after: SatelliteTick): Long {
    val period = if (before.signal.constellation == Constellation.Glonass) DAY_NANOS else WEEK_NANOS
    val plain = after.sentNanos - before.sentNanos

    return when {
        plain < -period / 2 -> plain + period
        plain > period / 2 -> plain - period
        else -> plain
    }
}

/**
 * How far the two clocks fell behind the satellite's between two epochs, or
 * nothing when the pair cannot be read.
 */
fun clocksApart(before: SatelliteTick, after: SatelliteTick): ClocksApart? {
    if (before.signal != after.signal) {
        return null
    }

    val phone = after.sinceBootNanos - before.sinceBootNanos

    if (phone <= 0 || phone > TICKS_APART_NANOS) {
        return null
    }

    val sent = sentApart(before, after)

    return ClocksApart(
        receiverNanos = sent - (after.receiverNanos - before.receiverNanos),
        sinceBootNanos = sent - phone,
    )
}

/**
 * What a pair of epochs says about one signal.
 *
 * The phone's clock is read first and answered first. It is the coarser of the
 * two by a hundredfold, so anything it does catch is past arguing about,
 * whereas the receiver's clock catching something on its own leaves open that
 * the receiver merely re-solved its own time.
 */
fun satelliteClockVerdict(apart: ClocksApart?): SatelliteClockVerdict = when {
    apart == null -> SatelliteClockVerdict.Unknown
    abs(apart.sinceBootNanos) > PHONE_APART_NANOS -> SatelliteClockVerdict.PhoneApart
    abs(apart.receiverNanos) > RECEIVER_APART_NANOS -> SatelliteClockVerdict.ReceiverApart
    else -> SatelliteClockVerdict.Agrees
}

/** The receiver has decoded the satellite's time of week off the signal. */
private const val TOW_DECODED = 1 shl 3

/** It has the time of week from elsewhere, which is as good for this. */
private const val TOW_KNOWN = 1 shl 14

/** The same pair for GLONASS, which broadcasts a time of day instead. */
private const val GLO_TOD_DECODED = 1 shl 7
private const val GLO_TOD_KNOWN = 1 shl 15

/**
 * Whether the measurement carries a satellite time that can be read at all.
 *
 * Without one of these states the field holds a time within the millisecond or
 * the bit, which is a different quantity under the same name: subtracting two
 * of them measures nothing.
 */
fun sentTimeIsKnown(measurement: Measurement): Boolean {
    val wanted =
        if (measurement.constellation == Constellation.Glonass) {
            GLO_TOD_DECODED or GLO_TOD_KNOWN
        } else {
            TOW_DECODED or TOW_KNOWN
        }

    return measurement.state and wanted != 0
}

/**
 * One measurement of one epoch as a tick of the satellite's clock, or nothing
 * when the epoch cannot say where either clock stood.
 *
 * The receiver's clock is the epoch's own time less the bias it reports
 * against the satellites' scale, and the measurement's offset within the epoch
 * belongs to it rather than to the satellite.
 */
fun tickOf(clock: Clock, measurement: Measurement): SatelliteTick? {
    val fullBias = clock.fullBiasNanos ?: return null
    val boot = clock.sinceBootNanos ?: return null

    if (!sentTimeIsKnown(measurement)) {
        return null
    }

    val bias = clock.biasNanos ?: 0.0

    // The whole nanoseconds first and the fractions after. The receiver's
    // clock counts from 1980 and runs to 1.5e18 nanoseconds, which is past
    // where a double can hold whole ones: adding the offset to it before it is
    // rounded loses a quarter of a microsecond of the answer.
    val receiver = (clock.timeNanos - fullBias) + (measurement.timeOffsetNanos - bias).toLong()

    return SatelliteTick(
        signal = Signal(measurement.constellation, measurement.id, measurement.carrierHz),
        sentNanos = measurement.receivedSvTimeNanos,
        receiverNanos = receiver,
        sinceBootNanos = boot,
    )
}

/** One signal read against the epoch before it. */
data class SignalApart(
    val signal: Signal,
    val apart: ClocksApart,
    val verdict: SatelliteClockVerdict,
)

/**
 * What every signal of an epoch said against the clocks of the epoch before
 * it, kept from one epoch to the next.
 *
 * Per signal rather than per satellite: a satellite tracked on two bands
 * reports the two separately, and the second one starting up is not the first
 * one jumping.
 */
class SatelliteClockWatch {
    private val last = mutableMapOf<Signal, SatelliteTick>()

    /**
     * Read an epoch and say how each of its signals stood against the clocks.
     *
     * Signals heard for the first time say nothing and are only remembered:
     * one tick is not a pair.
     */
    fun read(clock: Clock, measurements: List<Measurement>): List<SignalApart> {
        val said = mutableListOf<SignalApart>()

        for (measurement in measurements) {
            val tick = tickOf(clock, measurement) ?: continue
            val before = last[tick.signal]

            last[tick.signal] = tick

            val apart = before?.let { clocksApart(it, tick) } ?: continue

            said += SignalApart(tick.signal, apart, satelliteClockVerdict(apart))
        }

        return said
    }

    /** Forget every signal, as after the receiver was stopped and started again. */
    fun forget() {
        last.clear()
    }
}

/** The signals of a reading that parted from either clock. */
fun parted(said: List<SignalApart>): List<SignalApart> = said.filter {
    it.verdict == SatelliteClockVerdict.ReceiverApart ||
        it.verdict == SatelliteClockVerdict.PhoneApart
}
