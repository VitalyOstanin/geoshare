package io.github.vitalyostanin.geoshare.collection

import androidx.annotation.StringRes
import io.github.vitalyostanin.geoshare.R

/**
 * What each way of collecting is called on a card.
 *
 * The document carries the name of the entry the phone was in — `Standby`,
 * `Viewing` — which says nothing to whoever opens the card: "Idle" does not
 * explain that the phone asked for nothing because it had not left the place
 * it was. The page keeps the same phrases, so one card reads the same on both.
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
