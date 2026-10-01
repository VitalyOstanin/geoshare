package io.github.vitalyostanin.geoshare.collection

// Who the phone asks for its position.
//
// Play Services' fused provider is the default wherever it exists: it batches,
// it blends the satellites with the Wi-Fi and it spends the least. What it
// will not do is be told where to look. It picks its sources itself, so a
// member who switched the towers off still had the towers asked, and their
// answers were only thrown away on arrival. A phone without Play Services
// has no fused provider of that kind at all, and until this asked nothing.
//
// The platform's own providers are asked by name instead, and only the ones
// the member left on. Names are the platform's: "fused" is its own blend,
// present on the newer releases and on some phones without Play Services.

/** The platform's blend of every source it has. */
const val FUSED_PROVIDER: String = "fused"

/** The receiver, and nothing but the receiver. */
const val GPS_PROVIDER: String = "gps"

/** Who to ask, as decided by [whomToAsk]. */
sealed interface Asked {
    /** Play Services' fused provider, which chooses its own sources. */
    data object PlayServices : Asked

    /** The platform's own providers by name, the preferred one first. */
    data class Platform(val providers: List<String>) : Asked
}

/**
 * Who to ask for a position with this accuracy, or nobody.
 *
 * Nobody when the accuracy asks for nothing, and when everything the member
 * left on is something this phone does not have: an answer from a source that
 * is switched off would be left out anyway, so asking for it only spends the
 * battery.
 *
 * Without a blend of its own the platform is asked by source. The receiver is
 * the only way to the accuracy the fast mode wants, and the network beside it
 * is what answers indoors; the slower modes ask the network alone, because the
 * receiver woken every quarter of an hour costs more than the whole rest of
 * the collection.
 */
fun whomToAsk(
    accuracy: Accuracy,
    playServices: Boolean,
    leftOut: Set<String>,
    present: Set<String>,
): Asked? {
    if (accuracy == Accuracy.None) {
        return null
    }

    val sky = SourceChoice.Satellites.name !in leftOut
    val radios = SourceChoice.OtherRadios.name !in leftOut

    if (playServices && sky && radios) {
        return Asked.PlayServices
    }

    val wanted =
        when {
            sky && radios && FUSED_PROVIDER in present -> listOf(FUSED_PROVIDER)
            sky && radios && accuracy == Accuracy.High -> listOf(GPS_PROVIDER, NETWORK_PROVIDER)
            sky && radios -> listOf(NETWORK_PROVIDER, GPS_PROVIDER).filter { it in present }.take(1)
            sky -> listOf(GPS_PROVIDER)
            radios -> listOf(NETWORK_PROVIDER)
            else -> emptyList()
        }

    val providers = wanted.filter { it in present }

    return if (providers.isEmpty()) null else Asked.Platform(providers)
}
