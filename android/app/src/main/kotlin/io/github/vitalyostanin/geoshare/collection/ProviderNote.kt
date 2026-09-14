package io.github.vitalyostanin.geoshare.collection

// What the provider attached to a position, read rather than merely kept.
//
// The name of the provider says which of the phone's ways of knowing handed
// the answer over, and that is all it says. It does not say how the answer was
// arrived at, and on these phones the two come apart: a fix arrives under the
// name `gps`, having been read off a list an assistance service sent over the
// network, with not one satellite heard at the time. Both look the same to
// every rule here, and they are worth opposite amounts — one is the receiver's
// own measurement and the other is a guess from somewhere else entirely.
//
// The note is where the difference is admitted. It is the provider's own
// business what goes in it, so nothing here may depend on a key being present;
// what it costs to read is a string split, and what it buys is the one
// statement the platform makes about whether a position rests on satellites.

/** How the provider arrived at a position, as far as its own note admits. */
enum class Answered {
    /** Worked out from satellites the receiver was hearing at the time. */
    Satellites,

    /** Handed over by an assistance service rather than measured. */
    Assistance,

    /**
     * Said to be the receiver's own, with nothing audible behind it.
     *
     * The satellites are counted and their loudness is zero: the receiver
     * knows which ones should be up there and is hearing none of them. A
     * position under this heading was not measured off the sky whatever the
     * note calls it.
     */
    Unheard,

    /** The note says nothing that separates the others. */
    Unsaid,
}

/**
 * The fields of the note that have a meaning here, as far as they were there.
 *
 * Every one of them is optional twice over: the provider need not write the
 * key at all, and what it writes need not be a number. A key that is missing
 * and a key that is unreadable are the same thing from here — nothing said.
 */
data class ProviderNote(
    /**
     * Which kind of answer the provider calls this one.
     *
     * The numbers are the ones these phones use rather than anything the
     * platform documents: [MEASURED_SOURCE] beside a real satellite count and
     * [ASSISTED_SOURCE] beside a silent one. Read as a hint and never on its
     * own — the loudness below is the measurement, and this is the label.
     */
    val sourceType: Int? = null,
    /** The provider saying in as many words that this came from assistance. */
    val quickGps: Boolean? = null,
    /** How many satellites the provider says the answer rests on. */
    val satellites: Int? = null,
    /** The loudest satellite behind the answer, in dB-Hz. */
    val maxLoudness: Double? = null,
    /** The average loudness behind the answer, in dB-Hz. */
    val meanLoudness: Double? = null,
)

/** The value of [ProviderNote.sourceType] seen beside satellites being heard. */
const val MEASURED_SOURCE: Int = 0

/** The value seen beside an answer an assistance service handed over. */
const val ASSISTED_SOURCE: Int = 128

/**
 * The note as the provider wrote it, split into the fields that are read here.
 *
 * The keys are matched without regard to case: what a provider calls its
 * fields is its own business, and two phones of the same make have already
 * been seen to differ in the shape of the word rather than the word itself.
 */
fun noteOf(alongside: String?): ProviderNote {
    val pairs = pairsOf(alongside)

    return ProviderNote(
        sourceType = pairs["sourcetype"]?.trim()?.toIntOrNull(),
        quickGps = pairs["quickgps"]?.trim()?.lowercase()?.let { said ->
            when (said) {
                "true" -> true
                "false" -> false
                else -> null
            }
        },
        satellites = pairs["satellites"]?.trim()?.toIntOrNull(),
        maxLoudness = pairs["maxcn0"]?.trim()?.toDoubleOrNull(),
        meanLoudness = pairs["meancn0"]?.trim()?.toDoubleOrNull(),
    )
}

/**
 * What the note says about where the answer came from.
 *
 * Read in the order the statements are worth. The provider saying outright
 * that an assistance service answered settles it; after that comes the
 * loudness, which is a measurement and cannot be argued with; and the label
 * only decides the cases the loudness left open.
 */
fun answeredBy(note: ProviderNote): Answered {
    if (note.quickGps == true) {
        return Answered.Assistance
    }

    if (note.sourceType == ASSISTED_SOURCE) {
        return Answered.Assistance
    }

    val loudest = note.maxLoudness

    return when {
        // Nothing measurable was written down. The label alone is not taken
        // for a measurement: the one case where it stood alone turned out to
        // be an assistance answer wearing the name of the satellites.
        loudest == null -> Answered.Unsaid

        loudest > 0.0 -> Answered.Satellites

        // Counted and silent. The receiver has a list of what should be
        // overhead and is hearing none of it, so whatever worked this position
        // out, it was not these satellites.
        else -> Answered.Unheard
    }
}

/**
 * Whether a position was measured off the sky, and so is worth asking the
 * questions about a false signal of at all.
 *
 * The questions cost nothing to ask of the wrong position and everything to
 * answer wrongly: an assistance answer that disagrees with the last written
 * one is not a jump, and a silent receiver is not a quiet radio.
 */
fun restsOnSatellites(alongside: String?): Boolean =
    answeredBy(noteOf(alongside)) == Answered.Satellites

/** The word the log uses for what the note said. */
val Answered.word: String
    get() = when (this) {
        Answered.Satellites -> "measured off the satellites"
        Answered.Assistance -> "handed over by assistance"
        Answered.Unheard -> "credited to satellites none of which were heard"
        Answered.Unsaid -> "by a means the provider did not say"
    }

/**
 * The note split into its pairs, whatever else is in it.
 *
 * A part without a separator, or with nothing before one, is dropped rather
 * than guessed at; a key repeated keeps its last value, which is the only
 * choice that does not depend on the order a bundle happened to be read in.
 */
private fun pairsOf(alongside: String?): Map<String, String> = alongside
    .orEmpty()
    .split('|')
    .mapNotNull { part ->
        val at = part.indexOf('=')

        if (at <= 0) null else part.take(at).trim().lowercase() to part.substring(at + 1)
    }
    .toMap()
