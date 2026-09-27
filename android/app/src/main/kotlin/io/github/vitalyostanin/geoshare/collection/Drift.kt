package io.github.vitalyostanin.geoshare.collection

import kotlin.math.abs

// Whether the satellites move apart from one another as the minutes pass.
//
// Under a real sky each satellite has its own way down to the antenna: one is
// climbing and gaining, another is setting into the roofline and losing, a
// third is passing behind a wall. Their loudness drifts apart, and the size of
// that drifting is decibels within a minute.
//
// A false sky comes from one transmitter, so whatever happens to its signal
// happens to every satellite it names at once. Their loudness still moves —
// the transmitter is not perfectly steady — but it moves as one block: take
// the common part away and what is left is a couple of tenths of a decibel.
//
// So the measure here is not the movement itself but what is left after the
// movement common to all of them is taken out.

/** What one system's satellites did between two epochs. */
data class Drift(
    /** How many of them were heard in both, which is what the reading rests on. */
    val satellites: Int,
    /** How far they moved as a block, in dB: the middle of their changes. */
    val shiftDb: Double,
    /**
     * How far they moved apart from that block, in dB.
     *
     * The middle of how much each satellite's own change differs from the
     * common one. The middle rather than the average because one satellite
     * dropping behind a wall would otherwise decide the reading for the epoch.
     */
    val apartDb: Double,
)

/** How a run of epochs has the satellites moving against one another. */
enum class SkyDrift {
    /** As one block, which is what one transmitter gives. */
    Together,

    /** Each on its own, as satellites at their own heights and behind their own walls do. */
    Apart,

    /** Neither said clearly enough to be worth saying. */
    Unclear,
}

/**
 * How far apart the satellites have to stay, and how close they have to move,
 * before either verdict is given.
 *
 * Read off the logs, at the twenty seconds apart the pairs are read at. Over
 * the phone being spoofed the GPS band drifts apart by 0,2—0,3 dB; over the
 * hours when the same phone still took GLONASS into the fix, by 0,9—1,0 dB;
 * over the phone in another part of the city, by 1,2—1,5 dB. The hours when
 * about half the epochs read flat sit at 0,3—0,5 dB, which is why the limit
 * for a real sky is set above them rather than between: an hour half spoofed
 * is not an hour to call honest.
 */
const val TOGETHER_DRIFT_DB: Double = 0.6
const val APART_DRIFT_DB: Double = 0.9

/**
 * How many satellites of one system have to be heard in both epochs.
 *
 * Eight rather than the five the shape of the sky is read from, and the logs
 * are why. Over the phone in another part of the city, indoors with five
 * satellites in hand, a run of epochs reads as moving together six times in
 * ten — with so few of them the middle of their changes is one satellite's
 * change. Raised to eight, that phone gives no such reading at all on either
 * of its honest recordings, while the two phones under the transmitter, which
 * hear ten to fourteen, keep giving it seven to nine windows in ten.
 */
const val DRIFT_MIN_SATELLITES: Int = 8

/**
 * How far apart in time the two epochs have to stand.
 *
 * Far enough that the satellites have had the chance to move against one
 * another, near enough that the pair is still about one sky. A second apart —
 * which is how often the receiver reports — the honest sky reads as narrow as
 * the false one: the logs give 0,2 dB either way. By fifteen seconds the
 * honest hour is at 0,95 dB and the spoofed ones at 0,3, and the gap only
 * widens after that.
 */
const val DRIFT_LEAST_APART_MS: Long = 15_000L
const val DRIFT_MOST_APART_MS: Long = 60_000L

/**
 * Two epochs read system by system.
 *
 * Only the satellites heard in both: one that appears or disappears between
 * them says something about the sky, but not about how loudly the ones that
 * stayed are speaking. Apart by system for the same reason the slant is —
 * GLONASS arrives quieter than GPS on these phones, and the two together would
 * measure the difference between the systems.
 */
fun driftOf(before: List<Satellite>, now: List<Satellite>): Map<Constellation, Drift> {
    val was = before.filter { it.loudness > 0.0 }.associateBy { it.constellation to it.id }

    return now.filter { it.loudness > 0.0 }
        .mapNotNull { satellite ->
            was[satellite.constellation to satellite.id]?.let { then ->
                satellite.constellation to (satellite.loudness - then.loudness)
            }
        }
        .groupBy({ it.first }, { it.second })
        .filterValues { it.size >= DRIFT_MIN_SATELLITES }
        .mapValues { (_, changes) -> driftOfOne(changes) }
}

/**
 * What a run of epochs says, or [SkyDrift.Unclear] where they do not agree.
 *
 * Read over several for the same reason the shape of the sky is: a single
 * pair of epochs can have every satellite standing still by chance, and a
 * single wall between the antenna and one of them can widen a pair that is
 * otherwise as narrow as the rest.
 */
fun driftShapeOf(epochs: List<Drift>): SkyDrift {
    if (epochs.size < EPOCHS_READ_TOGETHER) {
        return SkyDrift.Unclear
    }

    val apart = middle(epochs.map { it.apartDb })

    return when {
        apart <= TOGETHER_DRIFT_DB -> SkyDrift.Together
        apart >= APART_DRIFT_DB -> SkyDrift.Apart
        else -> SkyDrift.Unclear
    }
}

private fun driftOfOne(changes: List<Double>): Drift {
    val shift = middle(changes)

    return Drift(
        satellites = changes.size,
        shiftDb = shift,
        apartDb = middle(changes.map { abs(it - shift) }),
    )
}
