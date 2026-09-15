package io.github.vitalyostanin.geoshare.collection

// Which sources of a position this phone takes into account.
//
// The phone can now say how it got an answer — see [FoundBy] — and the answers
// are not worth the same. A place worked out from satellites the receiver was
// hearing is a measurement; one handed over by an assistance service is
// somebody else's guess about where the phone probably is, arrived at from the
// towers and the Wi-Fi around it, and where those are being lied to it is a
// guess built on the lie. A member who knows that about their own
// neighbourhood should be able to say so, once, rather than watching the
// marker jump and wondering.
//
// A source switched off here is a source this phone does not take into account
// when it works out its point: an answer arriving that way is left out the way
// a jump is, so it moves no marker, becomes no last position and reaches no
// other phone. It is still asked for, weighed by every rule and written to the
// log, because the rules need the run of answers and the log is the evidence —
// what stops is its counting towards where the phone is.

/**
 * A source as the member sees it: one switch, and the ways the platform has of
 * naming the same thing.
 *
 * The switches are not the entries of [FoundBy] one for one, because two of
 * those entries are one source to anybody looking at a phone. An answer from
 * an assistance service and an answer from the network provider are both a
 * place worked out from the towers and the Wi-Fi around the phone; what
 * separates them is who did the arithmetic and whose name the answer arrived
 * under, and neither of those is a reason to switch one off and keep the
 * other. The difference is kept where it is worth something — in the document,
 * on the card and in the log — and left out of the choice.
 *
 * [FoundBy.Satellites] has no switch at all, deliberately: leaving it out
 * would leave the phone with no source of a point, which is what the sharing
 * switch is for. A setting that quietly amounts to switching off is a setting
 * that will be blamed for something else.
 */
enum class SourceChoice(val covers: List<FoundBy>) {
    /** Worked out from the towers and the Wi-Fi, by the phone or by a service. */
    OtherRadios(listOf(FoundBy.Assistance, FoundBy.OtherRadios)),

    /** Credited to satellites the receiver measured nothing off. */
    Unheard(listOf(FoundBy.Unheard)),

    /** Put there by another application on the phone. */
    Pretended(listOf(FoundBy.Pretended)),

    /** Arrived with nothing said about how it was reached. */
    Unsaid(listOf(FoundBy.Unsaid)),
}

/** The switch an answer falls under, or nothing where it falls under none. */
fun choiceFor(whence: FoundBy): SourceChoice? =
    SourceChoice.entries.firstOrNull { whence in it.covers }

/**
 * Whether a position from this source is one this phone takes into account.
 *
 * [leftOut] holds the names of the switches the member has turned off, as
 * [SourceChoice] spells them: a name from a build older or newer than this one
 * is simply a name that matches nothing, which leaves the position counted
 * rather than silently dropped.
 */
fun usesWhatWasFoundBy(whence: FoundBy, leftOut: Set<String>): Boolean =
    choiceFor(whence)?.name !in leftOut
