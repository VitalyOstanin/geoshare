package io.github.vitalyostanin.geoshare.service

import android.content.Context
import io.github.vitalyostanin.geoshare.collection.Answered
import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.answeredBy
import io.github.vitalyostanin.geoshare.collection.noteOf
import io.github.vitalyostanin.geoshare.collection.secondOpinionDue
import io.github.vitalyostanin.geoshare.collection.secondOpinionEvery
import io.github.vitalyostanin.geoshare.collection.secondOpinionWithEachFix
import io.github.vitalyostanin.geoshare.log.AppLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.Instant
import java.util.Locale

private const val TAG = "geoshare.service"

/**
 * How long the network is given to answer when Play Services will not.
 *
 * A tower answers in seconds and a scan of the Wi-Fi around the device takes a
 * few more; half a minute is long enough for both and short enough to be over
 * before the next round of asking.
 */
private val NETWORK_ANSWER_WAIT: Duration = Duration.ofSeconds(30)

/**
 * What the towers and the Wi-Fi say about where the device is, kept beside the
 * satellites' own answer.
 */
class NetworkOpinion(private val context: Context) {
    /**
     * The last thing the towers and the Wi-Fi said, whoever asked them.
     *
     * Kept because it is the one opinion a transmitter drawing a false road
     * cannot reach, and because the rule that uses it has to run while nothing
     * looks wrong: a road drawn slowly breaks no other rule, so by the time
     * another rule fires there would be nothing left to compare.
     */
    var said: Fix? = null

    /** When the second opinion was last asked for, by the clock or by a position. */
    private var askedAt: Instant? = null

    /**
     * Keeps an answer the device did not measure, for the rule that weighs the
     * satellites against something they cannot reach.
     *
     * An assistance service answers over the network, from a list of the Wi-Fi
     * around the device rather than from the sky, and arrives under the name of
     * the satellites like everything else the provider hands over. The name is
     * therefore not what it is taken by — the note is, and a note admitting
     * that nothing was measured is a note from beyond the transmitter's reach.
     *
     * It matters most where there is no second opinion otherwise: a device with
     * no SIM has no towers to ask, and these answers are the only ones it ever
     * gets that the spoofer did not write.
     */
    fun remember(fix: Fix) {
        if (answeredBy(noteOf(fix.reported?.alongside)) != Answered.Assistance) {
            return
        }

        said = fix
    }

    /**
     * Keeps an answer from the towers and the Wi-Fi standing beside the
     * satellites' own.
     *
     * The only rule here that has to run while everything looks right. A
     * transmitter drawing a false road slowly breaks no other rule — not the
     * speed, not the width, not the sensors of a device in a moving car — and
     * the first moment anything is visibly wrong is the moment the road ends,
     * hours later. So the second opinion is fetched on a clock rather than on
     * suspicion, and the cost of that is one scan every [secondOpinionEvery].
     *
     * The slow modes fetch it with each position instead, through [freshen].
     *
     * What comes back is never written, only compared: an answer from the
     * towers is worth a district, and the rules that decide what goes in the
     * document are asked about it elsewhere.
     */
    suspend fun keep(mode: CollectionMode) {
        if (secondOpinionWithEachFix(mode)) {
            return
        }

        while (true) {
            ask()
            delay(secondOpinionEvery(mode).toMillis())
        }
    }

    /**
     * Makes sure a position about to be judged in a slow mode has an answer
     * from the other radios beside it.
     *
     * What the platform already knows is taken first, for nothing; the towers
     * and the Wi-Fi are asked only when that is too old to compare and the
     * mode's own round has passed since they were last asked. Nobody is
     * watching in these modes, so the half minute the asking may take delays
     * nothing that anybody sees.
     */
    suspend fun freshen(mode: CollectionMode) {
        if (!secondOpinionWithEachFix(mode)) {
            return
        }

        lastNetworkPosition(context)?.let { known ->
            val held = said

            if (held == null || known.takenAt.isAfter(held.takenAt)) {
                said = known
            }
        }

        if (secondOpinionDue(mode, said, askedAt, Instant.now(), awakeFor())) {
            ask()
        }
    }

    private suspend fun ask() {
        askedAt = Instant.now()

        answerNow()?.let { said = it }
    }

    /** Asks the towers and the Wi-Fi, falling back on what the platform last heard from them. */
    suspend fun answerNow(): Fix? = withTimeoutOrNull(NETWORK_ANSWER_WAIT.toMillis()) {
        networkPosition(context)
    } ?: lastNetworkPosition(context)

    /**
     * What the towers and the Wi-Fi have to say when Play Services has nothing.
     *
     * Written down rather than used: the question being answered here is
     * whether a device whose satellites are jammed could be placed by its other
     * radios at all, and that has to be known before the collection is moved
     * onto them.
     */
    suspend fun noteWhatItKnows() {
        val asked =
            withTimeoutOrNull(NETWORK_ANSWER_WAIT.toMillis()) {
                networkPosition(context)
            }
        val known = lastNetworkPosition(context)

        AppLog.i(TAG, "the network says ${saying(asked)}, and knew ${saying(known)} before that")
    }
}

/** How a fix reads in a line of the log, and how its absence reads. */
internal fun saying(fix: Fix?, now: Instant = Instant.now()): String = when (fix) {
    null -> "nothing"

    else ->
        String.format(
            Locale.ROOT,
            "%.5f,%.5f ±%.0f m, %d s old",
            fix.latitude,
            fix.longitude,
            fix.accuracyMetres ?: 0.0,
            Duration.between(fix.takenAt, now).seconds,
        )
}
