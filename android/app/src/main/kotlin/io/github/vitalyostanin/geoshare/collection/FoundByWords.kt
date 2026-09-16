package io.github.vitalyostanin.geoshare.collection

import androidx.annotation.StringRes
import io.github.vitalyostanin.geoshare.R

/**
 * What each way of arriving at a position is called on a card.
 *
 * The document carries the word the phone wrote — `Satellites`, `Assistance` —
 * and the card names it in the language of whoever is reading. The name goes
 * through the resources for that reason, and the key stays the entry the phone
 * wrote; the page keeps the same names, so one card reads the same on both.
 *
 * A word this build has never heard of is named as unknown rather than shown
 * as it came: the entries are English identifiers, and a Russian card showing
 * one would be showing a word from another language to say something it has a
 * word for.
 */
@StringRes
fun foundByLabel(foundBy: String?): Int = when (foundBy) {
    FoundBy.Satellites.name -> R.string.found_satellites
    FoundBy.Assistance.name -> R.string.found_assistance
    FoundBy.Unheard.name -> R.string.found_unheard
    FoundBy.OtherRadios.name -> R.string.found_other_radios
    FoundBy.Pretended.name -> R.string.found_pretended
    FoundBy.Unsaid.name -> R.string.found_unsaid
    else -> R.string.map_detail_unknown
}

/**
 * What each switch is called on the screen where it is set.
 *
 * One name per switch rather than per entry of [FoundBy]: the two ways of
 * arriving at a place from the towers and the Wi-Fi are one thing to choose
 * about, and the card is where the difference between them is told.
 */
@StringRes
fun sourceChoiceLabel(choice: SourceChoice): Int = when (choice) {
    SourceChoice.Satellites -> R.string.found_satellites
    SourceChoice.OtherRadios -> R.string.found_other_radios
    SourceChoice.Unheard -> R.string.found_unheard
    SourceChoice.Pretended -> R.string.found_pretended
    SourceChoice.Unsaid -> R.string.found_unsaid
}

/**
 * What each reason for leaving an answer out is called on a card.
 *
 * The same arrangement as [foundByLabel]: the document carries the word the
 * phone wrote — an entry of [JumpReason], or `Source` for an answer the member
 * has told their phone not to count — and each reader says it in its own
 * language. A word this build has never heard of is named as unknown rather
 * than shown as it came.
 */
@StringRes
fun leftOutReasonLabel(reason: String?): Int = when (reason) {
    JumpReason.Drifted.name -> R.string.left_out_drifted
    JumpReason.Hurried.name -> R.string.left_out_hurried
    JumpReason.Contradicted.name -> R.string.left_out_contradicted
    JumpReason.Width.name -> R.string.left_out_width
    JumpReason.Speed.name -> R.string.left_out_speed
    JumpReason.Contradiction.name -> R.string.left_out_contradiction
    JumpReason.FalsePlace.name -> R.string.left_out_false_place
    JumpReason.Unmoved.name -> R.string.left_out_unmoved
    JumpReason.Circling.name -> R.string.left_out_circling
    JumpReason.OneBand.name -> R.string.left_out_one_band
    JumpReason.OneTransmitter.name -> R.string.left_out_one_transmitter
    JumpReason.TimeApart.name -> R.string.left_out_time_apart
    JumpReason.Repeating.name -> R.string.left_out_repeating
    JumpReason.Settling.name -> R.string.left_out_settling
    else -> switchedOffLabel(reason)
}

/**
 * What is left of a reason naming a source the member switched off.
 *
 * Such a reason is written with the switch beside it — `Source:Satellites` —
 * and the screen names the switch where this build knows it; this is the
 * sentence for the cases where it does not: a build older than the switches,
 * or a switch newer than this build.
 */
@StringRes
private fun switchedOffLabel(reason: String?): Int =
    if (reason?.substringBefore(':') == LEFT_OUT_BY_SOURCE) {
        R.string.left_out_by_source
    } else {
        R.string.map_detail_unknown
    }

/**
 * The switch named in a reason, where the reason names one.
 *
 * A source the member switched off is written as `Source:Satellites` rather
 * than as `Source` alone: which of the five it was is the whole of what the
 * member wants back from such a row, and the phone knows it at the moment it
 * leaves the answer out. The older form without a switch is still understood,
 * because a build that wrote it may be the one a phone in the household is
 * running.
 *
 * A switch this build has never heard of comes out as nothing, and the row
 * says only that a source was switched off — which is true, and better than a
 * word of another language on the card.
 */
fun sourceLeftOut(reason: String?): SourceChoice? {
    val named = reason?.substringAfter("$LEFT_OUT_BY_SOURCE:", "").orEmpty()

    return SourceChoice.entries.firstOrNull { it.name == named }
}

/**
 * What the reason field says when the answer was left out because the member
 * switched its source off.
 *
 * Not an entry of [JumpReason], because nothing is wrong with such an answer:
 * the phone was told not to count that source. Named here rather than in the
 * service, so that the writer and the reader spell it once.
 */
const val LEFT_OUT_BY_SOURCE = "Source"
