package io.github.vitalyostanin.geoshare.collection

import java.time.Instant

/** What is made of a fix before it is written or left out. */
data class Judgement(
    /** How the device arrived at the answer. */
    val whence: FoundBy,
    /** Whether the receiver had not settled when the fix was taken. */
    val settling: Boolean,
    /** Why the fix reads as a jump, or nothing where it does not. */
    val jump: JumpReason?,
    /** Whether a jump is confirmed by the fix before it and the other radios. */
    val agreed: Boolean,
    /** Whether a jump is the device answering from a place it keeps coming back to. */
    val home: Boolean,
) {
    /**
     * Whether the fix is left out as a jump: one that none of the ways out of
     * a jump applies to, while the member keeps jumps out at all.
     */
    fun leftOutAsJump(jumpsFiltered: Boolean): Boolean =
        jump != null && jumpsFiltered && !agreed && !home
}

/**
 * Everything the decision about one fix reads, taken at the moment the fix
 * arrives.
 *
 * Gathered in one place so that the decision is a function of what it was
 * given: the service reads the receiver, the sensors and its own memory once,
 * and the rules below never reach back into any of them.
 */
data class Grounds(
    /** The last position written, which a jump is measured from. */
    val lastFix: Fix?,
    /** The last fix left out as a jump, which the next one may agree with. */
    val candidate: Fix?,
    /** The receiver's own last answer, which says how a fused answer was found. */
    val satellites: Fix?,
    /** What the towers and the Wi-Fi around the device last said. */
    val networkSays: Fix?,
    val now: Instant,
    val lately: List<Fix> = emptyList(),
    val falsePlaces: List<FalsePlace> = emptyList(),
    val circleLimits: CircleLimits = CircleLimits(),
    /** The satellites the receiver rests on, read off the epoch the fix came out of. */
    val restsOn: Map<Constellation, Counted> = emptyMap(),
    val skySays: SkySays = SkySays(),
    val standingSince: Instant? = null,
    val putDown: PutDown? = null,
    val carrying: Carrying = Carrying(stirred = false, told = false),
    /** The places positions were written from, which make a place the device's own. */
    val trail: Trail = Trail(),
)

/**
 * Works out where a fix came from, whether the receiver is still settling,
 * and whether it is a jump and, if so, whether one of the ways out of a jump
 * applies.
 *
 * A [trusted] answer did not come from the satellites at all and is never
 * measured against the rules written for them: the towers answer in
 * kilometres, and the width rule would throw away the one source the spoofer
 * cannot reach.
 */
fun judge(fix: Fix, trusted: Boolean, grounds: Grounds): Judgement {
    // How the device arrived at this answer, worked out before anything is
    // decided: it settles both whether this is a source the member allows
    // and what the written document says about itself. The receiver's own
    // last answer is what makes it possible at all — the fused answer
    // being weighed carries no note of its own on these devices.
    val whence = foundBy(fix, grounds.satellites)

    // Whether the receiver has had time to settle since the device was
    // switched on, read off the same epoch as the rules below. Never asked
    // of an answer from the towers: it is a statement about the satellites,
    // and such an answer rests on none.
    val settling = !trusted && stillSettling(fix.sinceBoot, grounds.restsOn)

    val jump =
        if (trusted) {
            null
        } else {
            whyAJump(
                grounds.lastFix,
                fix,
                grounds.standingSince,
                grounds.networkSays,
                grounds.now,
                grounds.lately,
                grounds.falsePlaces,
                grounds.circleLimits,
                restsOn = grounds.restsOn,
                skySays = grounds.skySays,
                putDown = grounds.putDown,
                carrying = grounds.carrying,
                settling = settling,
            )
        }

    // A fix left out does not move the last position, and that is how the
    // marker can be left standing somewhere the device has long left: a
    // wrong last position turns every true fix that follows into a jump.
    // A second fix that agrees with the one before it is the way out of a
    // width nothing can outlive, and of the rules that measure against
    // that last position. Never on the word of the two answers alone,
    // because a spoofed signal draws a road whose fixes all agree: the
    // towers or the Wi-Fi around the device have to name the same place,
    // and past the width only an answer as narrow as the Wi-Fi's will do.
    val agreed =
        jump != null &&
            agreementStands(jump, grounds.candidate, fix, grounds.networkSays, grounds.now)

    // The other way out of an anchor that has gone wrong, and the only one
    // that works against a width or a false place: the device answering
    // from somewhere it has been three times over an hour. Those visits
    // are positions that were written, and while a spoofer runs, nothing
    // is — so this cannot be arranged by feeding the device a signal.
    val home = jump != null && !agreed && comingHome(grounds.trail, grounds.lastFix, fix)

    return Judgement(whence, settling, jump, agreed, home)
}
