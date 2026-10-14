package io.github.vitalyostanin.geoshare.collection

import java.time.Duration

// How a position was arrived at, in words of this application's own.
//
// The name of the provider is not the answer. It says which of the device's
// ways of knowing handed the answer over, and on these devices the two come
// apart at exactly the moment the difference matters: a position arrives under
// the name `gps`, having been read off a list an assistance service sent over
// the network, with not one satellite heard at the time. A member looking at a
// marker cannot be shown the word `gps` for that.
//
// So the note the provider attaches decides wherever it says anything, and the
// name of the provider is fallen back on only where it says nothing. That is
// the same order every rule here reads them in — see [answeredBy] — and the
// reason is the same: the loudness is a measurement and the name is a label.
//
// What the devices actually hand over was checked rather than assumed. The
// satellite and fused answers carry a note; an answer from the network carries
// no note at all — `Location[network … hAcc=699.999]` and nothing else — so
// there is nothing in it to tell an answer worked out from the Wi-Fi around
// the device from one worked out from a tower. Both are named as what they are,
// the two radios together, and the width beside the word is what separates a
// flat from a district.

/** The platform's name for the provider that answers from the towers and the Wi-Fi. */
const val NETWORK_PROVIDER = "network"

/**
 * How far apart in time two answers may stand and still be the same answer.
 *
 * Half a minute. The satellites are asked for directly while the receiver is
 * on anyway, and the fused provider hands over what it has: the two are the
 * same fix under two names, seconds apart, or they are not the same fix at
 * all.
 */
val SAME_ANSWER_WITHIN: Duration = Duration.ofSeconds(30)

/** How a position was arrived at, as far as the device that wrote it could tell. */
enum class FoundBy {
    /** Worked out from satellites the receiver was hearing at the time. */
    Satellites,

    /**
     * Handed over by an assistance service: computed somewhere else, from the
     * towers and the Wi-Fi around the device, and sent over the network.
     */
    Assistance,

    /**
     * Credited to satellites, none of which were heard.
     *
     * The receiver counted what should be overhead and measured nothing off
     * any of it. Whatever worked this position out, it was not the sky, and it
     * is worth saying apart from an assistance answer that admits as much.
     */
    Unheard,

    /** From the towers and the Wi-Fi around the device, asked as one radio. */
    OtherRadios,

    /** Put there by another application on the device rather than measured. */
    Pretended,

    /** The provider said nothing that separates the others. */
    Unsaid,
}

/**
 * How a fix was arrived at.
 *
 * Asked of a fix rather than of a provider name, because the answer is in the
 * note where there is one. The order is what makes it honest: another
 * application pretending settles it outright, the note decides what it can,
 * and the provider's own name is read only where the note was silent — where
 * an answer from the network, which carries no note on these devices, is what
 * the name is naming.
 *
 * [satellites] is the receiver's own last answer, which is what settles the
 * case the positions actually get written from. The fused provider hands over
 * no note at all on these devices — checked in the sky file: every `fused` row
 * has an empty note beside `gps` rows carrying `SourceType`, `maxCn0` and a
 * satellite count — so without it the answer would be "not said" for nearly
 * every position this application writes. Where the two turn out to be one
 * answer, the receiver's note speaks for both.
 */
fun foundBy(fix: Fix, satellites: Fix? = null): FoundBy {
    if (fix.reported?.pretended == true) {
        return FoundBy.Pretended
    }

    return when (answeredBy(noteOf(fix.reported?.alongside))) {
        Answered.Satellites -> FoundBy.Satellites

        Answered.Assistance -> FoundBy.Assistance

        Answered.Unheard -> FoundBy.Unheard

        Answered.Unsaid ->
            if (fix.from == NETWORK_PROVIDER) {
                FoundBy.OtherRadios
            } else {
                whatTheReceiverSaid(fix, satellites)
            }
    }
}

/**
 * The note of the receiver's own answer, where that answer is this one.
 *
 * The fused provider says nothing about how it arrived at a position, and the
 * receiver, asked directly, says a great deal: on the device this was written
 * for the two stand a second apart on the same spot, and the receiver's note
 * reads `QUICKGPS=true|SourceType=128` — an assistance service answering over
 * the network under the name of the satellites. That is precisely what a
 * member needs told, and the fused answer alone cannot tell them.
 *
 * So the note is carried across where the two are one answer, and nowhere
 * else. Where they are not, the word stays "not said": a source invented from
 * the width would be read as a fact.
 */
private fun whatTheReceiverSaid(fix: Fix, satellites: Fix?): FoundBy {
    if (!theSameAnswer(fix, satellites)) {
        return FoundBy.Unsaid
    }

    return when (answeredBy(noteOf(satellites?.reported?.alongside))) {
        Answered.Satellites -> FoundBy.Satellites
        Answered.Assistance -> FoundBy.Assistance
        Answered.Unheard -> FoundBy.Unheard
        Answered.Unsaid -> FoundBy.Unsaid
    }
}

/**
 * Whether the receiver's own answer and the one being written are one answer.
 *
 * Not a guess from the width and not the name of a provider: the receiver is
 * asked directly, and if its answer stands at the same moment and inside the
 * doubt of the two together, then the fix that came with no note is that same
 * answer handed over under another name.
 *
 * The doubt is taken as it stands rather than with the margin the second
 * opinion grants. That margin is there for accusing the satellites of lying,
 * which is a serious thing to do on the strength of a database entry; naming
 * a source is the opposite case, and a claim that two answers are the same
 * answer should be held to the tighter test, not the looser one.
 */
private fun theSameAnswer(fix: Fix, satellites: Fix?): Boolean {
    if (satellites == null) {
        return false
    }

    if (apart(satellites, fix).abs() > SAME_ANSWER_WITHIN) {
        return false
    }

    val doubt = (fix.accuracyMetres ?: 0.0) + (satellites.accuracyMetres ?: 0.0)

    return distanceMetres(fix, satellites) <= doubt
}

/** The word the log uses for it; the clients have their own, translated. */
val FoundBy.said: String
    get() = when (this) {
        FoundBy.Satellites -> "the satellites"
        FoundBy.Assistance -> "an assistance service"
        FoundBy.Unheard -> "satellites none of which were heard"
        FoundBy.OtherRadios -> "the towers and the Wi-Fi"
        FoundBy.Pretended -> "another application on the device"
        FoundBy.Unsaid -> "a means the provider did not say"
    }
