package io.github.vitalyostanin.geoshare.collection

import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

// How loudness stands against height, which is the one thing a transmitter on
// the ground cannot imitate.
//
// A satellite near the horizon is heard through far more atmosphere than one
// overhead, and it is reflected off far more of the ground on the way: under a
// real sky the low ones are the quiet ones, and the loudness of an epoch is
// spread over ten decibels or more. A false sky comes from one transmitter at
// one distance, so every satellite it names arrives at the same strength
// whatever height it claims — the spread collapses to a decibel and the line
// through the points goes flat.
//
// Both halves of that are measured here: how wide the loudness of the epoch is
// spread at all, and how much of the spread follows the height. The first is
// the plainer of the two and separates the logs better; the second is what
// says the first is about the sky rather than about a thin one.

/** What one system's satellites said in one epoch, read as loudness against height. */
data class Slant(
    /** How many of them were heard and had a place in the sky to be heard at. */
    val satellites: Int,
    /** Their loudness, averaged, in dB-Hz. */
    val meanDb: Double,
    /**
     * How far their loudness is spread about that average, in dB.
     *
     * The whole set's own spread rather than an estimate of some larger one:
     * the question is what this epoch looked like, not what a sky it was drawn
     * from would look like.
     */
    val spreadDb: Double,
    /**
     * How much louder a satellite overhead is than one on the horizon, in dB,
     * as the straight line through the epoch says.
     *
     * Against the sine of the height rather than the height itself, because
     * that is what the atmosphere a signal crosses goes as. Nothing where the
     * satellites heard are all at about the same height, which is a sky with
     * no answer to give rather than a flat one.
     */
    val slopeDb: Double?,
    /**
     * The same said the plainer way: the high satellites' average less the low
     * ones'. Nothing unless the epoch has both.
     */
    val highLessLowDb: Double?,
)

/** How an epoch's loudness stands against the height of the satellites it came from. */
enum class SkyShape {
    /** Every satellite at one strength however high it claims to be. */
    Flat,

    /** Loud overhead, quiet near the horizon, as a sky heard through air is. */
    Sloped,

    /** Neither said clearly enough to be worth saying. */
    Unclear,
}

/** Above this counts as overhead when the two halves are compared. */
const val HIGH_DEGREES: Double = 45.0

/** Below this counts as near the horizon. */
const val LOW_DEGREES: Double = 20.0

/**
 * How many satellites of one system have to be heard before the epoch is read.
 *
 * Five. Fewer than that and one reflection off a window decides both the
 * spread and the line, and the devices here hear eight to twelve of the GPS
 * band at a time, so the limit costs nothing where the question is being
 * asked.
 */
const val SLANT_MIN_SATELLITES: Int = 5

/**
 * How flat the loudness has to be, and how flat the line through it, before
 * the sky is called flat.
 *
 * Read off the logs rather than chosen. Over the device being spoofed the
 * spread of the GPS band sits at 0,7—1,2 dB an epoch and the line within a
 * decibel of level; over the device in another part of the city the spread is
 * 4,5—7,6 dB and the line runs to several. The hours when GLONASS was still
 * being taken into the fix read the same way as the other device: spread
 * 2,8—3,9 dB. Nothing between the two pairs of limits is called anything.
 */
const val FLAT_SPREAD_DB: Double = 1.5
const val FLAT_SLOPE_DB: Double = 1.0
const val SLOPED_SPREAD_DB: Double = 2.5
const val SLOPED_SLOPE_DB: Double = 2.0

/**
 * How much the heights have to differ before a line through them means
 * anything.
 *
 * The spread of their sines, summed: satellites gathered in one quarter of the
 * sky give a line whose slope is mostly the noise of the epoch divided by a
 * small number, and that is how a thin sky comes to look steep.
 */
private const val HEIGHTS_APART_ENOUGH: Double = 0.05

/**
 * One epoch read system by system.
 *
 * Apart by system for the same reason the averages in [loudnessPerBand] are:
 * GLONASS arrives some decibels quieter than GPS on these devices, and the two
 * mixed together would show a spread of the difference between the systems
 * rather than of the sky.
 *
 * Satellites the receiver lists without hearing are left out, and so are the
 * ones it hears without knowing where they are: both have no height to stand
 * against. A receiver reports the second as zero elevation and zero azimuth
 * at once, which is not a place a satellite can be.
 */
fun slantOf(sky: List<Satellite>): Map<Constellation, Slant> =
    sky.filter { it.loudness > 0.0 && it.hasAPlace }
        .groupBy { it.constellation }
        .mapValues { (_, satellites) -> slantOfOne(satellites) }

/**
 * What a run of epochs says, or [SkyShape.Unclear] where they do not agree.
 *
 * Read over several rather than one because a single epoch moves on its own:
 * over the device being spoofed one epoch in three still reads as sloped, and
 * over the honest one every tenth epoch reads flat. The middle of ten epochs
 * separates the two sets of logs without either being read as the other.
 *
 * Both limits have to be met for either verdict, so that a sky answering
 * plainly in one measure and not in the other is called nothing at all.
 */
fun shapeOf(epochs: List<Slant>): SkyShape {
    val read = epochs.filter { it.satellites >= SLANT_MIN_SATELLITES && it.slopeDb != null }

    if (read.size < EPOCHS_READ_TOGETHER) {
        return SkyShape.Unclear
    }

    val spread = middle(read.map { it.spreadDb })
    val slope = middle(read.mapNotNull { it.slopeDb?.let(::abs) })

    return when {
        spread <= FLAT_SPREAD_DB && slope <= FLAT_SLOPE_DB -> SkyShape.Flat
        spread >= SLOPED_SPREAD_DB && slope >= SLOPED_SLOPE_DB -> SkyShape.Sloped
        else -> SkyShape.Unclear
    }
}

/** How many epochs are read together before either verdict is given. */
const val EPOCHS_READ_TOGETHER: Int = 10

/** Whether the receiver says where in the sky this satellite is. */
private val Satellite.hasAPlace: Boolean
    get() {
        val elevation = elevationDegrees ?: return false

        return elevation != 0.0 || (azimuthDegrees ?: 0.0) != 0.0
    }

private fun slantOfOne(satellites: List<Satellite>): Slant {
    val loudness = satellites.map { it.loudness }
    val mean = loudness.average()
    val heights = satellites.map { sin(Math.toRadians(it.elevationDegrees ?: 0.0)) }
    val meanHeight = heights.average()
    val apart = heights.sumOf { (it - meanHeight) * (it - meanHeight) }
    val high = satellites.filter { (it.elevationDegrees ?: 0.0) > HIGH_DEGREES }
    val low = satellites.filter { (it.elevationDegrees ?: 0.0) < LOW_DEGREES }

    return Slant(
        satellites = satellites.size,
        meanDb = mean,
        spreadDb = sqrt(loudness.sumOf { (it - mean) * (it - mean) } / loudness.size),
        slopeDb =
        if (apart < HEIGHTS_APART_ENOUGH) {
            null
        } else {
            heights.zip(loudness) { height, loud -> (height - meanHeight) * (loud - mean) }
                .sum() / apart
        },
        highLessLowDb =
        if (high.isEmpty() || low.isEmpty()) {
            null
        } else {
            high.map { it.loudness }.average() - low.map { it.loudness }.average()
        },
    )
}

/** The middle of a set of readings, which one wild epoch cannot move. */
internal fun middle(values: List<Double>): Double {
    val sorted = values.sorted()
    val half = sorted.size / 2

    return if (sorted.size % 2 == 1) {
        sorted[half]
    } else {
        (sorted[half - 1] + sorted[half]) / 2
    }
}
