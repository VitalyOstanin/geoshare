package io.github.vitalyostanin.geoshare.map

import androidx.compose.ui.unit.dp

/**
 * The widest a card over the map grows: as wide as its own rows need, as on
 * the page (22rem there). Stretched across a tablet it was a band of empty
 * surface with a narrow table in one corner.
 */
internal val CARD_MAX_WIDTH = 360.dp

/** How close to the edge of the map a card may come. */
internal val CARD_MARGIN = 8.dp

/**
 * Where a card's left edge goes so that it stands over the button it is about,
 * in pixels from the left of the map.
 *
 * Centred over [centre] and pushed back inside where that would take it past
 * either edge; a card that does not fit at all keeps to the left margin.
 */
internal fun cardLeft(centre: Float, cardWidth: Float, room: Float, margin: Float): Float =
    (centre - cardWidth / 2)
        .coerceAtMost(room - cardWidth - margin)
        .coerceAtLeast(margin)
