package io.github.vitalyostanin.geoshare.collection

// What the receiver can say about the sky, apart from where it thinks it is.
//
// The verdicts in Constellations.kt are read from two averages a minute wide.
// This is the other half: the epoch itself, satellite by satellite, in the
// terms Google's own logger writes down — which is the measure of whether
// anything here is missing. A receiver being handed a false sky differs from
// one under a real one in ways an average cannot carry: every satellite at the
// same strength, every one of them in the fix, half of them without the
// ephemeris that a receiver listening to the real thing would have collected.

/** One satellite as the receiver reported it in one epoch. */
data class Satellite(
    val constellation: Constellation,
    /** The receiver's own number for it, unique within its system. */
    val id: Int,
    /** How loud it is at the antenna, in dB-Hz. Zero means it is not heard. */
    val loudness: Double,
    /**
     * The same before the antenna's own amplifier, where the receiver reports
     * it. The pair is what says whether a signal got louder or the receiver
     * merely turned itself up.
     */
    val basebandLoudness: Double?,
    val elevationDegrees: Double?,
    val azimuthDegrees: Double?,
    /** Which frequency it was heard on, where the receiver says. */
    val carrierHz: Double?,
    /** Whether the position the receiver worked out rests on this satellite. */
    val usedInFix: Boolean,
    /** Whether the receiver holds the orbit this satellite broadcast for itself. */
    val hasEphemeris: Boolean,
    /** Whether it holds the rough orbits of the system as a whole. */
    val hasAlmanac: Boolean,
)

/** The gain the receiver is running one band at, as it reports it. */
data class Gain(val constellation: Constellation, val carrierHz: Double?, val levelDb: Double)

/** How many of a system's satellites are heard, and how many the fix rests on. */
data class Counted(val heard: Int, val usedInFix: Int)

/**
 * A gain of this or below is the receiver saying it has nothing for that band.
 *
 * The phones report -327.67 for a band they are not listening to, which is the
 * smallest number the field can carry rather than a measurement. Averaged in
 * with the real ones it drags the mean down by tens of decibels and every
 * epoch reads as interference; it is the first thing that has to go.
 */
const val NO_GAIN_DB: Double = -300.0

/** The gains of one epoch, the bands with nothing on them left out. */
fun reported(gains: List<Gain>): List<Gain> = gains.filter { it.levelDb > NO_GAIN_DB }

/** The gains of one epoch, averaged within each system separately. */
fun gainPerBand(gains: List<Gain>): Map<Constellation, Double> = reported(gains)
    .groupBy { it.constellation }
    .mapValues { (_, band) -> band.map { it.levelDb }.average() }

/**
 * How loud the satellites are, averaged within each system separately.
 *
 * The ones with nothing on them are left out rather than counted as silence:
 * a receiver lists the satellites it expects as well as the ones it has, and
 * counting the expected ones as zero would make every sky look like jamming.
 *
 * Averaged within the system rather than over all of them at once so that a
 * system with eight satellites in view does not speak for one with three.
 */
fun loudnessPerBand(sky: List<Satellite>): Map<Constellation, Double> =
    sky.filter { it.loudness > 0.0 }
        .groupBy { it.constellation }
        .mapValues { (_, satellites) -> satellites.map { it.loudness }.average() }

/** How many are heard and how many are in the fix, per system. */
fun counted(sky: List<Satellite>): Map<Constellation, Counted> = sky.filter { it.loudness > 0.0 }
    .groupBy { it.constellation }
    .mapValues { (_, satellites) ->
        Counted(
            heard = satellites.size,
            usedInFix = satellites.count { it.usedInFix },
        )
    }

/** The word the log uses for a system. */
val Constellation.word: String
    get() = when (this) {
        Constellation.Gps -> "GPS"
        Constellation.Glonass -> "GLONASS"
        Constellation.Galileo -> "Galileo"
        Constellation.Beidou -> "BeiDou"
        Constellation.Qzss -> "QZSS"
        Constellation.Sbas -> "SBAS"
        Constellation.Irnss -> "IRNSS"
        Constellation.Other -> "other"
    }

/** The systems in the order the log names them, whichever of them are there. */
fun inOrder(systems: Set<Constellation>): List<Constellation> = listOf(
    Constellation.Gps,
    Constellation.Glonass,
    Constellation.Galileo,
    Constellation.Beidou,
    Constellation.Qzss,
    Constellation.Sbas,
    Constellation.Irnss,
    Constellation.Other,
).filter { it in systems }

/**
 * The receiver's own clock, as it reports it with every epoch.
 *
 * A receiver takes its time from the satellites, so a transmitter that hands
 * out a false sky hands out a false time with it, and the receiver's clock
 * steps to meet it. The step shows here before it shows anywhere else: in the
 * bias against the phone's own clock, in the drift, and in the count of
 * discontinuities, which the receiver raises whenever its own time stopped
 * running forward.
 */
data class Clock(
    val timeNanos: Long,
    val leapSecond: Int?,
    val timeUncertaintyNanos: Double?,
    val fullBiasNanos: Long?,
    val biasNanos: Double?,
    val biasUncertaintyNanos: Double?,
    val driftNanosPerSecond: Double?,
    val driftUncertaintyNanosPerSecond: Double?,
    val discontinuities: Int,
    /**
     * The phone's own clock since it was switched on, at the moment of the
     * epoch.
     *
     * The one clock in the epoch a false sky cannot touch. Everything else
     * here is taken from the satellites and steps when they say to step; this
     * runs off the phone's own oscillator, so the pair of them is what shows a
     * step happening at all.
     */
    val sinceBootNanos: Long?,
    val sinceBootUncertaintyNanos: Double?,
)

/**
 * One raw measurement of one signal, before a position is worked out of it.
 *
 * What the status says is how loud a satellite is; this is what the receiver
 * measured off it — how far away the signal says the satellite is, how fast
 * that distance is changing, and how sure the receiver is of either. A false
 * sky has to be self-consistent in these to be believed, and the ones that are
 * not show up as distances that do not agree with the rates.
 */
data class Measurement(
    val constellation: Constellation,
    val id: Int,
    val timeOffsetNanos: Double,
    val state: Int,
    val receivedSvTimeNanos: Long,
    val receivedSvTimeUncertaintyNanos: Long,
    val loudness: Double,
    val basebandLoudness: Double?,
    val pseudorangeRateMps: Double,
    val pseudorangeRateUncertaintyMps: Double,
    val deltaRangeState: Int,
    val deltaRangeMetres: Double,
    val deltaRangeUncertaintyMetres: Double,
    val carrierHz: Double?,
    val multipath: Int,
    val gainDb: Double?,
    /**
     * Signal against noise where the receiver reports it, in dB.
     *
     * Not the same number as [loudness], which is carrier against noise
     * density: this one carries the receiver's own bandwidth in it. Few
     * receivers report it, and the ones that do report it beside the other,
     * which is what makes the pair worth having.
     */
    val snrDb: Double?,
    /**
     * Which signal of the satellite this is — "C" for the coarse GPS code,
     * "Q" and "I" for the halves of a Galileo or BeiDou one.
     *
     * A false sky is normally one signal per satellite, because imitating the
     * second one costs as much again for nothing; a real one has receivers
     * tracking several.
     */
    val codeType: String?,
    /**
     * The delay the receiver applies for this signal against its own reference
     * signal, and how sure it is of it.
     *
     * Reported from Android 11. Worth keeping because a receiver mixing a
     * false signal with real ones has to reconcile them somewhere, and this is
     * where it says by how much.
     */
    val fullInterSignalBiasNanos: Double?,
    val fullInterSignalBiasUncertaintyNanos: Double?,
    /** The same delay as the satellite itself broadcasts it. */
    val satelliteInterSignalBiasNanos: Double?,
    val satelliteInterSignalBiasUncertaintyNanos: Double?,
    /**
     * Whether the receiver was tracking every signal it can rather than the
     * cheapest set that gives a position.
     *
     * A property of the epoch rather than of the one measurement, written on
     * each row the way Google's own logger writes it. It says how much of a
     * silence is the receiver economising and how much is the sky: a receiver
     * in the cheap mode hears fewer signals by design.
     */
    val fullTracking: Boolean?,
)
