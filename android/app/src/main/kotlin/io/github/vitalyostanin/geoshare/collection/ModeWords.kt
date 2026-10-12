package io.github.vitalyostanin.geoshare.collection

import androidx.annotation.StringRes
import io.github.vitalyostanin.geoshare.R

/**
 * What each way of collecting is called on a card.
 *
 * The document carries the entry the device was in — `Standby`, `Viewing` — and
 * the card gives that mode its name rather than a sentence about it. The five
 * differ by what they ask the receiver for and how often they send, which is a
 * table to be read once against itself in the help rather than a phrase to be
 * read again on every card. The name goes through the resources, so that a
 * translation names the modes in its own language while the key stays the
 * entry the device wrote. The page keeps the same names, so one card reads the
 * same on both.
 */
@StringRes
fun modeLabel(mode: String?): Int = when (mode) {
    CollectionMode.Idle.name -> R.string.mode_idle
    CollectionMode.Standby.name -> R.string.mode_standby
    CollectionMode.Moving.name -> R.string.mode_moving
    CollectionMode.Viewing.name -> R.string.mode_viewing
    CollectionMode.Night.name -> R.string.mode_night
    else -> R.string.map_detail_unknown
}
