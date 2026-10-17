package io.github.vitalyostanin.geoshare.download

import java.time.Duration
import java.time.Instant

// What the device may download on its own, and over which networks.
//
// Only the downloads the application starts by itself fall under this: the
// satellite data the spoofing check is made from, and whatever else it comes
// to fetch in the background later. The map is fetched because the member
// opened it, and the places shared with the group are a few kilobytes that
// the whole application exists for; neither waits on a network choice.
// An update is downloaded because the member pressed for it, but tens of
// megabytes over a network they switched off are asked about first.

/** The kinds of network the member chooses between. */
enum class DownloadNetwork { WiFi, Mobile, Roaming }

/** What the device stands on right now, as far as a download is concerned. */
enum class NetworkKind {
    None,

    /** Wi-Fi, or a cable: a network nobody counts the bytes of. */
    WiFi,

    /** A mobile network at home. */
    Mobile,

    /** A mobile network away from home. */
    Roaming,
}

/** Whether a download due now may start. */
enum class DownloadVerdict {
    Go,

    /** Roaming, and roaming is switched off: the member is asked first. */
    AskFirst,

    /** No network, or one the member switched off: tried again later. */
    Wait,
}

/** How it starts: Wi-Fi and the mobile network at home, but not roaming. */
val DEFAULT_DOWNLOAD_NETWORKS: Set<DownloadNetwork> =
    setOf(DownloadNetwork.WiFi, DownloadNetwork.Mobile)

/**
 * How long "not now" holds before the member is asked again.
 *
 * A day, because the satellite data is fetched once a day: asking sooner
 * would be asking about the same download twice.
 */
val ASK_AGAIN_AFTER: Duration = Duration.ofDays(1)

fun downloadVerdict(on: NetworkKind, allowed: Set<DownloadNetwork>): DownloadVerdict = when (on) {
    NetworkKind.None -> DownloadVerdict.Wait

    NetworkKind.WiFi -> if (DownloadNetwork.WiFi in
        allowed
    ) {
        DownloadVerdict.Go
    } else {
        DownloadVerdict.Wait
    }

    NetworkKind.Mobile -> if (DownloadNetwork.Mobile in
        allowed
    ) {
        DownloadVerdict.Go
    } else {
        DownloadVerdict.Wait
    }

    NetworkKind.Roaming -> if (DownloadNetwork.Roaming in
        allowed
    ) {
        DownloadVerdict.Go
    } else {
        DownloadVerdict.AskFirst
    }
}

/** Whether the member may be asked again, having last said "not now" at [declinedAt]. */
fun mayAskAgain(declinedAt: Instant?, now: Instant): Boolean =
    declinedAt == null || !now.isBefore(declinedAt.plus(ASK_AGAIN_AFTER))

/**
 * Whether the member is asked before an update they pressed for comes down
 * over [on] (ADR-0039): over a network they switched off, roaming included.
 * The press is theirs, but the size is not on the button. With no network
 * there is nothing to ask about; the download fails on its own.
 */
fun asksBeforeUpdate(on: NetworkKind, allowed: Set<DownloadNetwork>): Boolean =
    on != NetworkKind.None && downloadVerdict(on, allowed) != DownloadVerdict.Go
