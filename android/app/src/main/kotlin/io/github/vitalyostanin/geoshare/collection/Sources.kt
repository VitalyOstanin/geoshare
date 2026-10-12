package io.github.vitalyostanin.geoshare.collection

// Which sources of a position this device takes into account.
//
// The device can now say how it got an answer — see [FoundBy] — and the answers
// are not worth the same. A place worked out from satellites the receiver was
// hearing is a measurement; one handed over by an assistance service is
// somebody else's guess about where the device probably is, arrived at from the
// towers and the Wi-Fi around it, and where those are being lied to it is a
// guess built on the lie. A member who knows that about their own
// neighbourhood should be able to say so, once, rather than watching the
// marker jump and wondering.
//
// A source switched off here is a source this device does not take into account
// when it works out its point: an answer arriving that way is left out the way
// a jump is, so it moves no marker, becomes no last position and reaches no
// other device. The satellites and the other radios, switched off, are not
// asked at all — see [whomToAsk]; the remaining kinds cannot be asked for by
// name, so they still arrive, are weighed by every rule and written to the
// log, and what stops is their counting towards where the device is.

/**
 * A source as the member sees it: one switch, and the ways the platform has of
 * naming the same thing.
 *
 * The switches are not the entries of [FoundBy] one for one, because two of
 * those entries are one source to anybody looking at a device. An answer from
 * an assistance service and an answer from the network provider are both a
 * place worked out from the towers and the Wi-Fi around the device; what
 * separates them is who did the arithmetic and whose name the answer arrived
 * under, and neither of those is a reason to switch one off and keep the
 * other. The difference is kept where it is worth something — in the document,
 * on the card and in the log — and left out of the choice.
 *
 * The satellites have a switch like everything else, and that is the point of
 * the list rather than an oversight in it: where a transmitter is running, the
 * satellite answer is the one that lies — a kilometre out, six kilometres out —
 * while the towers around the device go on saying roughly the right district. A
 * member in that neighbourhood wants exactly the setting that leaves the wide
 * honest answer and drops the narrow false one.
 *
 * Switching off every one of them leaves the device with nothing to place
 * itself by, and nothing is written at all. That is a thing a member may
 * legitimately want for an hour, and the button says how many are left so it
 * cannot be done by accident and forgotten.
 */
enum class SourceChoice(val covers: List<FoundBy>) {
    /** Measured off satellites the receiver was hearing at the time. */
    Satellites(listOf(FoundBy.Satellites)),

    /** Worked out from the towers and the Wi-Fi, by the device or by a service. */
    OtherRadios(listOf(FoundBy.Assistance, FoundBy.OtherRadios)),

    /** Credited to satellites the receiver measured nothing off. */
    Unheard(listOf(FoundBy.Unheard)),

    /** Put there by another application on the device. */
    Pretended(listOf(FoundBy.Pretended)),

    /** Arrived with nothing said about how it was reached. */
    Unsaid(listOf(FoundBy.Unsaid)),
}

/** The switch an answer falls under, or nothing where it falls under none. */
fun choiceFor(whence: FoundBy): SourceChoice? =
    SourceChoice.entries.firstOrNull { whence in it.covers }

/**
 * Whether a position from this source is one this device takes into account.
 *
 * [leftOut] holds the names of the switches the member has turned off, as
 * [SourceChoice] spells them: a name from a build older or newer than this one
 * is simply a name that matches nothing, which leaves the position counted
 * rather than silently dropped.
 */
fun usesWhatWasFoundBy(whence: FoundBy, leftOut: Set<String>): Boolean =
    choiceFor(whence)?.name !in leftOut
