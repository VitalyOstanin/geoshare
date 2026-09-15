package io.github.vitalyostanin.geoshare.collection

// How a position was arrived at, in words of this application's own.
//
// The name of the provider is not the answer. It says which of the phone's
// ways of knowing handed the answer over, and on these phones the two come
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
// What the phones actually hand over was checked rather than assumed. The
// satellite and fused answers carry a note; an answer from the network carries
// no note at all — `Location[network … hAcc=699.999]` and nothing else — so
// there is nothing in it to tell an answer worked out from the Wi-Fi around
// the phone from one worked out from a tower. Both are named as what they are,
// the two radios together, and the width beside the word is what separates a
// flat from a district.

/** The platform's name for the provider that answers from the towers and the Wi-Fi. */
const val NETWORK_PROVIDER = "network"

/** How a position was arrived at, as far as the phone that wrote it could tell. */
enum class FoundBy {
    /** Worked out from satellites the receiver was hearing at the time. */
    Satellites,

    /**
     * Handed over by an assistance service: computed somewhere else, from the
     * towers and the Wi-Fi around the phone, and sent over the network.
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

    /** From the towers and the Wi-Fi around the phone, asked as one radio. */
    OtherRadios,

    /** Put there by another application on the phone rather than measured. */
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
 * an answer from the network, which carries no note on these phones, is what
 * the name is naming.
 */
fun foundBy(fix: Fix): FoundBy {
    if (fix.reported?.pretended == true) {
        return FoundBy.Pretended
    }

    return when (answeredBy(noteOf(fix.reported?.alongside))) {
        Answered.Satellites -> FoundBy.Satellites

        Answered.Assistance -> FoundBy.Assistance

        Answered.Unheard -> FoundBy.Unheard

        Answered.Unsaid ->
            if (fix.from == NETWORK_PROVIDER) FoundBy.OtherRadios else FoundBy.Unsaid
    }
}

/** The word the log uses for it; the clients have their own, translated. */
val FoundBy.said: String
    get() = when (this) {
        FoundBy.Satellites -> "the satellites"
        FoundBy.Assistance -> "an assistance service"
        FoundBy.Unheard -> "satellites none of which were heard"
        FoundBy.OtherRadios -> "the towers and the Wi-Fi"
        FoundBy.Pretended -> "another application on the phone"
        FoundBy.Unsaid -> "a means the provider did not say"
    }
