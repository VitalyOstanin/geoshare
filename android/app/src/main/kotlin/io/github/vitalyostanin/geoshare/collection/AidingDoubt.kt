package io.github.vitalyostanin.geoshare.collection

import io.github.vitalyostanin.geoshare.R
import java.time.Duration

// A constellation heard and never counted.
//
// A receiver takes a satellite into its answer only once it holds that
// satellite's own orbit — the ephemeris — and it collects those either off the
// air, half a minute per satellite, or over the network in one go. What it
// cannot do is tell aiding data that has gone wrong from aiding data that is
// merely old, and it will sit on the wrong kind for as long as it is left to.
//
// A device under observation spent a whole day in exactly that state: GLONASS
// was heard in three hundred and forty-two epochs out of six hundred and
// eighteen and not one of those satellites had an ephemeris or entered a
// single fix, and BeiDou was heard in three hundred and nine with six. Eleven
// minutes after the aiding data was erased every heard satellite of every
// system had its ephemeris, and GLONASS, Galileo and BeiDou were all in the
// answer.
//
// Nothing in the collecting rules would ever have said so: the fixes were
// ordinary GPS fixes of ordinary width, and the sky file carried the fact on
// every row without anything reading it. This reads it.

/**
 * How long a constellation has to be wasted before it is worth saying so.
 *
 * Half an hour. A receiver that has just been switched on, or one that has
 * just had its aiding data erased, collects the ephemerides off the air in
 * about a quarter of an hour, so a shorter window would announce a receiver
 * doing its work. The trouble this is written for lasts days.
 */
val AIDING_DOUBT_AFTER: Duration = Duration.ofMinutes(30)

/**
 * How many epochs of hearing it for nothing are needed alongside the half
 * hour.
 *
 * Sixty. The clock alone is not enough, because a constellation low on the
 * horizon is heard in a handful of epochs an hour and would pass the half hour
 * without the receiver ever having had a chance at it: a device heard BeiDou in
 * fourteen epochs out of five hundred and eleven over two and a half hours,
 * which is a sky and not a fault. On the device that did have the fault the
 * count was three hundred and forty-two, so the two are nowhere near each
 * other.
 *
 * Counted in epochs rather than in minutes of hearing because the receiver
 * reports about once a second while it is running and not at all while the
 * device sleeps, and a span in minutes would mean one thing in Viewing and
 * another in Idle.
 */
const val AIDING_DOUBT_EPOCHS: Int = 60

/** What one constellation is worth to the receiver in one epoch. */
data class ConstellationUse(
    /** How many of its satellites are heard at all. */
    val heard: Int,
    /** How many of those the receiver holds an orbit for. */
    val withEphemeris: Int,
    /** How many of them the position rests on. */
    val usedInFix: Int,
)

/** The same epoch [counted] reads, with the orbits the receiver holds added. */
fun useOf(sky: List<Satellite>): Map<Constellation, ConstellationUse> =
    sky.filter { it.loudness > 0.0 }
        .groupBy { it.constellation }
        .mapValues { (_, satellites) ->
            ConstellationUse(
                heard = satellites.size,
                withEphemeris = satellites.count { it.hasEphemeris },
                usedInFix = satellites.count { it.usedInFix },
            )
        }

/**
 * Whether this epoch is one where the constellation was heard and gave
 * nothing.
 *
 * Read off the orbits alone, and deliberately not off the fix. An orbit held
 * is the receiver saying it has decoded that satellite's own broadcast, which
 * is precisely what the stuck aiding data prevents; a fix is the receiver's
 * own bookkeeping, and these devices put a satellite in a fix while saying they
 * hold no orbit for it — a device under observation did so in a hundred and
 * thirty epochs of one night, every one of them a single GLONASS satellite
 * with no ephemeris. Counting those as the constellation having come good
 * would have cleared the count twice an hour throughout the fault.
 *
 * A constellation out of the answer with its orbits in hand is the other
 * fault: a receiver preferring other satellites, which is what a false sky
 * looks like and what [restsOnOneBand] is for.
 */
fun heardForNothing(use: ConstellationUse): Boolean = use.heard > 0 && use.withEphemeris == 0

/**
 * The constellations the receiver keeps hearing and keeps making nothing of.
 *
 * Read epoch by epoch and kept as two counts per constellation: when it was
 * last worth something, and how many epochs it has been heard for nothing
 * since. Both are needed — see [AIDING_DOUBT_EPOCHS] for the sky that passes
 * one of them without the fault.
 *
 * The clock is the device's own count since it was switched on, for the reason
 * every other span in the collecting is measured by it: the wall clock steps
 * when the satellites say it should, and these satellites are the ones being
 * doubted.
 *
 * A constellation that comes good is forgiven at once, counts and all, so a
 * receiver that decodes an orbit in the twenty-ninth minute starts its half
 * hour again rather than announcing itself in the thirtieth.
 */
class AidingDoubt(
    private val after: Duration = AIDING_DOUBT_AFTER,
    private val epochs: Int = AIDING_DOUBT_EPOCHS,
) {
    private val wastedSince = mutableMapOf<Constellation, Duration>()
    private val wastedEpochs = mutableMapOf<Constellation, Int>()

    /**
     * Read one epoch and say which constellations stand doubted.
     *
     * [sinceBoot] is the device's own count since it was switched on, taken at
     * the epoch.
     */
    fun read(use: Map<Constellation, ConstellationUse>, sinceBoot: Duration): Set<Constellation> {
        for (constellation in WORTH_DOUBTING) {
            val epoch = use[constellation] ?: continue

            // Silence says nothing either way: a constellation below the
            // horizon is not being wasted, and one that has just set should
            // not have its count cleared by the setting.
            if (epoch.heard == 0) {
                continue
            }

            if (!heardForNothing(epoch)) {
                wastedSince.remove(constellation)
                wastedEpochs.remove(constellation)

                continue
            }

            wastedSince.putIfAbsent(constellation, sinceBoot)
            wastedEpochs[constellation] = (wastedEpochs[constellation] ?: 0) + 1
        }

        return doubted(sinceBoot)
    }

    /** What stands doubted now, without reading an epoch. */
    fun doubted(sinceBoot: Duration): Set<Constellation> = wastedSince
        .filterKeys { constellation -> (wastedEpochs[constellation] ?: 0) >= epochs }
        .filterValues { since -> sinceBoot.minus(since) >= after }
        .keys
        .toSet()

    private companion object {
        /**
         * The four systems whose waste is worth a word.
         *
         * QZSS, SBAS and India's system are left out for the reason they are
         * left out of [SHARE_THE_GPS_BAND]: on these devices they are heard a
         * satellite or two at a time or not at all, and a member told that
         * QZSS was being wasted would be told about a system that was never
         * going to carry an answer here.
         */
        val WORTH_DOUBTING =
            setOf(
                Constellation.Gps,
                Constellation.Glonass,
                Constellation.Galileo,
                Constellation.Beidou,
            )
    }
}

/**
 * The name a constellation is read under on a screen.
 *
 * Beside the log's own [word] rather than instead of it: the log is read in
 * one language by whoever is looking into a fault, and this is read by a
 * member in theirs. GLONASS is the one that differs in Russian; the rest are
 * spelled the way their own operators spell them.
 */
fun constellationLabel(constellation: Constellation): Int = when (constellation) {
    Constellation.Gps -> R.string.constellation_gps
    Constellation.Glonass -> R.string.constellation_glonass
    Constellation.Galileo -> R.string.constellation_galileo
    Constellation.Beidou -> R.string.constellation_beidou
    Constellation.Qzss -> R.string.constellation_qzss
    Constellation.Sbas -> R.string.constellation_sbas
    Constellation.Irnss -> R.string.constellation_irnss
    Constellation.Other -> R.string.constellation_other
}
