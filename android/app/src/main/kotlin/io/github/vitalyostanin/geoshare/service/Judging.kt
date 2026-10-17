package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.collection.Answered
import io.github.vitalyostanin.geoshare.collection.Carrying
import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.github.vitalyostanin.geoshare.collection.FalsePlace
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.GPS_PROVIDER
import io.github.vitalyostanin.geoshare.collection.Judgement
import io.github.vitalyostanin.geoshare.collection.PutDown
import io.github.vitalyostanin.geoshare.collection.answeredBy
import io.github.vitalyostanin.geoshare.collection.driftedWhilePutDown
import io.github.vitalyostanin.geoshare.collection.networkAnswerStands
import io.github.vitalyostanin.geoshare.collection.noteOf
import io.github.vitalyostanin.geoshare.collection.saysMoreThan
import io.github.vitalyostanin.geoshare.collection.shouldWrite
import java.time.Duration
import java.time.Instant

/**
 * The run of answers left out as jumps: the one left out last, which the next
 * may agree with, and how many in a row there have been.
 *
 * Kept as a value so that what a judged fix does to it is a function of the
 * fix, and the service only holds the result.
 */
internal data class JumpRun(val candidate: Fix? = null, val inARow: Int = 0) {
    /**
     * The run after a fix has been judged.
     *
     * A fix left out becomes the candidate and lengthens the run. A fix taken
     * from a source the device uses ends it. A fix from a source the member
     * switched off is neither: it is left out for another reason, and says
     * nothing about the satellites either way.
     */
    fun after(fix: Fix, leftOutAsJump: Boolean, used: Boolean): JumpRun = when {
        leftOutAsJump -> JumpRun(fix, inARow + 1)
        used -> JumpRun()
        else -> this
    }
}

/** What becomes of a judged fix. */
internal enum class Outcome {
    /** Left out as a jump; the other radios may be asked. */
    LeftOutAsJump,

    /** Left out because the member said not to take its source into account. */
    LeftOutBySource,

    /** Goes on to the filter and the write. */
    GoesOn,
}

/**
 * What becomes of a fix once it is judged.
 *
 * A jump is looked at first: it is the case that counts towards asking the
 * other radios, and a fix from a switched-off source that is also a jump is
 * still the satellites lying.
 */
internal fun outcomeOf(judged: Judgement, jumpsFiltered: Boolean, used: Boolean): Outcome = when {
    judged.leftOutAsJump(jumpsFiltered) -> Outcome.LeftOutAsJump
    !used -> Outcome.LeftOutBySource
    else -> Outcome.GoesOn
}

/**
 * Whether a fix gets past the write filter. News — the cable going in, a mode
 * beginning — is [always] let through, since the point of it is to be told now
 * rather than at the next threshold.
 */
internal fun passesTheFilter(
    always: Boolean,
    last: Fix?,
    next: Fix,
    mode: CollectionMode,
): Boolean = always || shouldWrite(last, next, mode)

/**
 * Whether an answer is kept for the rule that reads the shape of the run.
 *
 * Only what the satellites answered. An answer the towers gave, and one an
 * assistance service worked out from the Wi-Fi around the device, are answers
 * from outside the transmitter's reach: mixing them into the run would break
 * the very shape the run is kept for.
 */
internal fun belongsInTheShape(fix: Fix, trusted: Boolean): Boolean {
    if (trusted || answeredBy(noteOf(fix.reported?.alongside)) == Answered.Assistance) {
        return false
    }

    return fix.from == null || fix.from == GPS_PROVIDER
}

/** What is made of the answer the towers and the Wi-Fi gave in place of the satellites. */
internal enum class OtherRadiosVerdict {
    /** Nothing usable came back. */
    NothingToPutInItsPlace,

    /** The same place as is written, only wider. */
    RepeatsWhatIsWritten,

    /** A place the sensors say a device nobody touched cannot have gone to. */
    WalkedAway,

    /** Worth standing on. */
    Stands,
}

/**
 * Whether the answer of the other radios is written in place of the
 * satellites'.
 *
 * Asked in the order the cheapest reason to refuse comes first, and the
 * sensors last: the one rule of the filter that is asked of this answer too,
 * since whatever built it, it was not a receiver that had been carried there.
 */
internal fun verdictOnTheOtherRadios(
    asked: Fix?,
    lastFix: Fix?,
    falsePlaces: List<FalsePlace>,
    putDown: PutDown?,
    carrying: Carrying,
    now: Instant,
    awakeFor: Duration,
): OtherRadiosVerdict = when {
    asked == null || !networkAnswerStands(asked, now, falsePlaces, awakeFor) ->
        OtherRadiosVerdict.NothingToPutInItsPlace

    !saysMoreThan(asked, lastFix, now, awakeFor) -> OtherRadiosVerdict.RepeatsWhatIsWritten

    driftedWhilePutDown(putDown, asked, carrying) -> OtherRadiosVerdict.WalkedAway

    else -> OtherRadiosVerdict.Stands
}
