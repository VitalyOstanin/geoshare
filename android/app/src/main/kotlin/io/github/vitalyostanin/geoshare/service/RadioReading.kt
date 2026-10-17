package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.collection.Constellation
import io.github.vitalyostanin.geoshare.collection.Counted
import io.github.vitalyostanin.geoshare.collection.DRIFT_LEAST_APART_MS
import io.github.vitalyostanin.geoshare.collection.DRIFT_MOST_APART_MS
import io.github.vitalyostanin.geoshare.collection.Drift
import io.github.vitalyostanin.geoshare.collection.EPOCHS_READ_TOGETHER
import io.github.vitalyostanin.geoshare.collection.Epochs
import io.github.vitalyostanin.geoshare.collection.METRES_IN_A_KILOMETRE
import io.github.vitalyostanin.geoshare.collection.OrbitEpoch
import io.github.vitalyostanin.geoshare.collection.SkyDrift
import io.github.vitalyostanin.geoshare.collection.Slant
import io.github.vitalyostanin.geoshare.collection.driftShapeOf
import io.github.vitalyostanin.geoshare.collection.inOrder
import io.github.vitalyostanin.geoshare.collection.shapeOf
import io.github.vitalyostanin.geoshare.collection.word
import java.time.Duration
import java.time.Instant
import java.util.Locale

// The decisions RadioWatch takes from what the receiver hands it, kept apart
// from the subscriptions so that they can be read, and tested, without a
// device: which window an average is read from, when a line is due, what a
// line says. RadioWatch keeps the state and the callbacks; this is the part
// that depends on nothing but its arguments.

/**
 * How many epochs are held at once.
 *
 * Ten recent against fifty before them, which is what Google's own logger
 * compares. At one epoch a second that is a minute of history: long enough
 * that the earlier half is what the radio was like before whatever is
 * happening started, short enough that walking indoors does not look like the
 * start of an attack for the next quarter of an hour.
 */
internal const val RECENT_EPOCHS = 10
internal const val EARLIER_EPOCHS = 50

/**
 * How old the last epoch may be and still be taken as being about a fix.
 *
 * Five seconds. The status arrives about once a second while the receiver is
 * running, so anything older than this is a receiver that has stopped
 * reporting rather than one reporting about this moment — and the rule read
 * off the epoch, which asks what a fix was built out of, would then be
 * answering about some other minute.
 */
internal val SKY_STILL_CURRENT: Duration = Duration.ofSeconds(5)

/** A band with no history yet, which is what having heard nothing looks like. */
internal val NOTHING = Epochs(recent = null, earlier = null)

/** Appends [value] and lets the oldest go once more than [limit] are held. */
internal fun <T> pushCapped(into: ArrayDeque<T>, value: T, limit: Int) {
    into.addLast(value)

    while (into.size > limit) {
        into.removeFirst()
    }
}

/**
 * The averages a verdict is read from.
 *
 * Nothing is reported until both halves are full: a comparison against three
 * epochs of history would call every device leaving a building an attack.
 */
internal fun windowOf(kept: List<Double>): Epochs {
    if (kept.size < RECENT_EPOCHS + EARLIER_EPOCHS) {
        return NOTHING
    }

    return Epochs(
        recent = kept.takeLast(RECENT_EPOCHS).average(),
        earlier = kept.dropLast(RECENT_EPOCHS).average(),
    )
}

/**
 * Whether the band a transmitter on the ground cannot reach has been silent
 * for the whole window, and the window is full.
 */
internal fun otherBandSilentThroughout(silences: List<Boolean>): Boolean =
    silences.size >= EPOCHS_READ_TOGETHER && silences.all { it }

/** Whether a line that is said at most every [every] is due again at [now]. */
internal fun dueAgain(saidAt: Instant, now: Instant, every: Duration): Boolean =
    Duration.between(saidAt, now) >= every

/** Whether the last epoch, taken at [skyAt], is recent enough to speak for [now]. */
internal fun skyStillCurrent(skyAt: Instant, now: Instant): Boolean =
    Duration.between(skyAt, now).abs() <= SKY_STILL_CURRENT

/** What to do with the epoch just arrived, given when the one kept arrived. */
internal enum class DriftStep {
    /** Keep this epoch as the one to measure against; nothing is read. */
    Restart,

    /** The kept epoch is too recent to say anything; keep it and wait. */
    Wait,

    /** Read the pair, then keep this epoch as the new one. */
    Read,
}

/**
 * Whether one epoch stands far enough from the kept one in time to be read
 * against it.
 *
 * Restarted both where there is nothing kept and where the gap has grown past
 * what the measure is for: a device that stopped collecting and started again
 * has an old epoch in hand, and the difference against it would be about the
 * hour between them rather than about the sky.
 */
internal fun driftStepFor(keptAt: Instant?, now: Instant): DriftStep {
    val apart = keptAt?.let { Duration.between(it, now).toMillis() }

    return when {
        apart == null || apart > DRIFT_MOST_APART_MS -> DriftStep.Restart
        apart < DRIFT_LEAST_APART_MS -> DriftStep.Wait
        else -> DriftStep.Read
    }
}

/**
 * The line saying the radio has nothing to go on, with how much it does have.
 *
 * The counts are the longest history of each reading: [gain] and [shared] are
 * the most any system of the GPS band holds, [glonass] that of GLONASS.
 */
internal fun nothingToGoOn(gain: Int, shared: Int, glonass: Int): String =
    "the radio has nothing to go on: $gain epochs of gain, " +
        "$shared of the GPS band, $glonass of GLONASS, ${RECENT_EPOCHS + EARLIER_EPOCHS} needed"

/** The verdict with the moves behind it, in the words of the log. */
internal fun verdictLine(
    verdict: String,
    gain: Pair<Epochs, Epochs>,
    loudness: Pair<Epochs, Epochs>,
): String = String.format(
    Locale.ROOT,
    "the radio says %s (gain: the GPS band %s, GLONASS %s; " +
        "loudness: the GPS band %s, GLONASS %s)",
    verdict.lowercase(),
    moved(gain.first),
    moved(gain.second),
    moved(loudness.first),
    moved(loudness.second),
)

/** The satellites an epoch held off their orbits, or null where none was held. */
internal fun orbitsLine(last: OrbitEpoch?): String? {
    last ?: return null

    val off = last.off.joinToString(", ") {
        String.format(
            Locale.ROOT,
            "%s %d at %.0f° and %+.0f km",
            it.constellation.word,
            it.id,
            it.elevationDegrees,
            it.offMetres / METRES_IN_A_KILOMETRE,
        )
    }

    return "${last.off.size} of ${last.held.size} satellites off their orbits" +
        if (off.isEmpty()) "" else ": $off"
}

/**
 * What the log says when the answer about the orbits changed, or null where it
 * did not: it hears of a change, not every second.
 */
internal fun orbitsChange(wasOff: Boolean, isOff: Boolean, saying: () -> String?): String? = when {
    isOff == wasOff -> null
    isOff -> "the sky is off its orbits: ${saying()}"
    else -> "the sky is back on its orbits"
}

/** How many satellites each system has in the sky and how many the fix rests on. */
internal fun satellitesLine(counts: Map<Constellation, Counted>): String? {
    if (counts.isEmpty()) {
        return null
    }

    val said = inOrder(counts.keys).joinToString(", ") { system ->
        val count = counts[system] ?: Counted(heard = 0, usedInFix = 0)

        "${system.word} ${count.usedInFix} of ${count.heard}"
    }

    return "the fix rests on $said"
}

/** The shape of each system's sky, for those that have a full window. */
internal fun skyLine(shapes: Map<Constellation, List<Slant>>): String? {
    val read = inOrder(shapes.keys).mapNotNull { system ->
        val epochs = shapes[system].orEmpty()

        if (epochs.size < EPOCHS_READ_TOGETHER) {
            return@mapNotNull null
        }

        val last = epochs.last()

        String.format(
            Locale.ROOT,
            "%s %s (spread %.1f dB, slope %s, %d satellites)",
            system.word,
            shapeOf(epochs).toString().lowercase(),
            last.spreadDb,
            last.slopeDb?.let { String.format(Locale.ROOT, "%+.1f dB", it) }
                ?: "not measurable",
            last.satellites,
        )
    }

    if (read.isEmpty()) {
        return null
    }

    return "the sky reads ${read.joinToString(", ")}"
}

/** How each system's satellites moved against one another, for full windows. */
internal fun driftLine(drifts: Map<Constellation, List<Drift>>): String? {
    val read = inOrder(drifts.keys).mapNotNull { system ->
        val epochs = drifts[system].orEmpty()

        if (epochs.size < EPOCHS_READ_TOGETHER) {
            return@mapNotNull null
        }

        val last = epochs.last()

        String.format(
            Locale.ROOT,
            "%s %s (apart %.1f dB, shift %+.1f dB, %d satellites)",
            system.word,
            when (driftShapeOf(epochs)) {
                SkyDrift.Together -> "as one block"
                SkyDrift.Apart -> "each on its own"
                SkyDrift.Unclear -> "neither way"
            },
            last.apartDb,
            last.shiftDb,
            last.satellites,
        )
    }

    if (read.isEmpty()) {
        return null
    }

    return "the satellites move ${read.joinToString(", ")}"
}

/**
 * What the log says when the set of systems heard for nothing changed, or null
 * where it did not.
 *
 * Whichever way it changed is said, so that a system that has come good clears
 * the line on the screen as well as raising it.
 */
internal fun aidingChange(said: Set<Constellation>, doubted: Set<Constellation>): String? = when {
    said == doubted -> null
    said.isEmpty() -> "every system heard is being made use of"
    else -> "heard for nothing: ${inOrder(said).joinToString(", ") { it.word }}"
}
