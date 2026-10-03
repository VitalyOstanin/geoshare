package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.collection.CARRIED_STEPS
import io.github.vitalyostanin.geoshare.collection.Carrying
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.FoundBy
import io.github.vitalyostanin.geoshare.collection.Moving
import io.github.vitalyostanin.geoshare.collection.PutDown
import io.github.vitalyostanin.geoshare.collection.STIRS_KEPT
import io.github.vitalyostanin.geoshare.collection.Stir
import io.github.vitalyostanin.geoshare.collection.carryingSince
import io.github.vitalyostanin.geoshare.collection.distanceMetres
import io.github.vitalyostanin.geoshare.collection.pinnedWhileWalking
import io.github.vitalyostanin.geoshare.collection.putDownAt
import io.github.vitalyostanin.geoshare.collection.walkNote
import io.github.vitalyostanin.geoshare.collection.walkingNow
import io.github.vitalyostanin.geoshare.log.AppLog
import java.time.Instant
import java.util.Locale

private const val TAG = "geoshare.service"

/**
 * What the phone's own sensors say about it being carried, kept for the rules
 * that weigh each answer against it.
 *
 * @param sensorsTell whether this phone has sensors to ask at all
 */
class BeingCarried(private val sensorsTell: Boolean) {
    /**
     * Since when the phone's own sensors say it has not been carried anywhere,
     * or nothing while it is moving and while they have said nothing yet.
     *
     * Kept as the moment the stillness began rather than as a flag, because
     * what a fix is weighed against is whether the stillness covers the way
     * from the last position to this one.
     */
    var standingSince: Instant? = null
        private set

    /**
     * Where the phone was standing when it was last put down.
     *
     * The place the answers are weighed against while nothing touches the
     * phone, which is the one comparison a slow creep cannot walk away from
     * unnoticed: the position written before each answer creeps along with the
     * lie, and this does not.
     */
    var putDown: PutDown? = null
        private set

    /**
     * What the phone's own motion sensors have reported lately, oldest first.
     *
     * Kept as moments rather than as a flag because they arrive late: the step
     * counter is read in batches, and a batch landing a minute after the fact
     * still has to undo an anchor that covered the minute it describes.
     */
    private val stirsHeard = mutableListOf<Stir>()

    /** What the step counter last stood at, and when anything last stirred. */
    private var stepsNow: Long? = null
    private var lastStir: Instant? = null

    /**
     * When the stretch of walking the phone is in the middle of began, and
     * where it said it was at the time.
     *
     * The pair is the other rule's anchor turned around: there a place is kept
     * while the phone lies still, here a place is kept while the phone is
     * carried, so that an answer refusing to follow the walking can be seen.
     * Only a narrow answer measured off the sky is kept — see
     * [pinnedWhileWalking] for why the others say nothing.
     */
    private var walkStarted: Instant? = null

    private var walkedFrom: Fix? = null

    /** What has stirred the phone since it was put down. */
    fun carrying(): Carrying = carryingSince(putDown, stirsHeard, sensorsTell)

    /**
     * Keeps what the motion sensors have said, so that the anchor can be
     * undone by a report that arrives after the fact.
     */
    fun hear(stir: Stir, now: Instant) {
        stirsHeard += stir
        lastStir = maxOf(lastStir ?: stir.at, stir.at)
        stir.steps?.let { counted -> stepsNow = maxOf(stepsNow ?: counted, counted) }

        val old = now.minus(STIRS_KEPT)

        stirsHeard.removeAll { it.at.isBefore(old) }
    }

    /**
     * Keeps the moment the phone was last carried anywhere.
     *
     * The stillness is dated from the first report of it and left alone by
     * every report that repeats it: the timer is reviewed on a clock, so the
     * same answer arrives again every few minutes, and taking the latest of
     * them would keep the stillness permanently one review old and cover
     * nothing.
     */
    fun hear(moving: Moving, now: Instant) {
        standingSince =
            when {
                !moving.told -> null
                moving.going -> null
                else -> standingSince ?: now
            }
    }

    /**
     * Moves the anchor to where the phone has just said it is, where the
     * anchor it had is no longer about a phone lying untouched.
     *
     * An anchor is left where it is for as long as nothing has stirred: that
     * is the whole of its value, since an anchor that followed the answers
     * would follow a creep as readily as the last position does. It is moved
     * once something has been carried somewhere and then put down again, and
     * [putDownAt] is what decides that the phone has settled.
     */
    fun anchorWhereItLies(believed: Fix, now: Instant) {
        val carrying = carrying()

        if (putDown != null && !carrying.stirred && (carrying.steps ?: 0) < CARRIED_STEPS) {
            return
        }

        val settled = putDownAt(believed, lastStir, sensorsTell, now, stepsNow)

        // Said once per anchor rather than per fix: what this line answers is
        // whether the rule is awake at all on a given phone, and a phone
        // standing still would otherwise repeat it every few minutes.
        if (settled?.at != putDown?.at) {
            AppLog.i(
                TAG,
                settled?.let { "the phone is standing where it was put down, at ${theSpot(it)}" }
                    ?: "nothing to anchor on: the phone has been moved too recently",
            )
        }

        putDown = settled
    }

    /**
     * Says so when the phone has been carried somewhere and the answer has
     * stayed behind.
     *
     * Nothing is decided by it yet: the line goes in the log, the position is
     * treated exactly as it would have been, and what the line is for is the
     * measurement — how often this happens over a day of ordinary carrying,
     * and what it looks like when it does. The threshold of `PINNED_STEPS` was
     * chosen to be far beyond a room rather than measured, which is the other
     * way round from every other threshold here, and it should be measured
     * before anything is left out on the strength of it.
     *
     * The place a walk is measured from is taken from a narrow satellite
     * answer and no other, because only such an answer claims to know where
     * the phone is closely enough for standing still to mean anything.
     */
    fun noticeAnAnswerThatWillNotFollow(fix: Fix, whence: FoundBy) {
        val walk = walkingNow(stirsHeard) ?: return

        if (walk.steps == 0L) {
            return
        }

        if (whence != FoundBy.Satellites) {
            AppLog.keep(TAG, walkNote(walk, whence, null, fix))

            return
        }

        if (walkStarted != walk.since || walkedFrom == null) {
            walkStarted = walk.since
            walkedFrom = fix
            AppLog.keep(TAG, walkNote(walk, whence, null, fix))

            return
        }

        val from = walkedFrom ?: return

        AppLog.keep(TAG, walkNote(walk, whence, from, fix))

        if (!pinnedWhileWalking(walk, from, fix, sensorsTell)) {
            return
        }

        val away = "%.0f m from where the walk began, ±%.0f m, at %.5f,%.5f"
            .format(
                Locale.ROOT,
                distanceMetres(from, fix),
                fix.accuracyMetres ?: 0.0,
                fix.latitude,
                fix.longitude,
            )

        // Kept apart: PINNED_STEPS is waiting on a day of the phone being
        // carried, and the ordinary log does not hold a day.
        AppLog.keep(TAG, "carried ${walk.steps} steps and the answer has not followed: $away")
    }

    /** Where an anchor stands, for the one line that says it was set. */
    private fun theSpot(anchor: PutDown): String =
        "%.5f,%.5f".format(Locale.ROOT, anchor.at.latitude, anchor.at.longitude)
}
