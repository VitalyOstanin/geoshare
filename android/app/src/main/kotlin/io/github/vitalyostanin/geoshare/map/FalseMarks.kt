package io.github.vitalyostanin.geoshare.map

import io.github.vitalyostanin.geoshare.collection.FalsePlace
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.falsePlaceFor
import java.time.Instant

// Positions that lie where a false signal is known to put phones.
//
// The phones refuse an answer that lands in such a place, but a position
// written before the place was named — or before it was drawn wide enough —
// stays in the database until the phone has something true to write over it.
// Under a spoofer that can be hours, and a marker standing in the lake all
// that time is read as where the member is. The screen cannot take the
// position back and does not try: it greys the marker the way it greys one
// gone quiet, says why on the card, and measures nothing from it. The page
// does the same.

/** The positions, each inside one of [places] marked so. */
fun withFalsePlaces(
    positions: List<MemberPosition>,
    places: List<FalsePlace>,
): List<MemberPosition> {
    if (places.isEmpty()) {
        return positions
    }

    return positions.map { position ->
        val fix =
            Fix(
                latitude = position.latitude,
                longitude = position.longitude,
                takenAt = position.takenAt,
            )

        if (falsePlaceFor(fix, places) != null) position.copy(inFalsePlace = true) else position
    }
}

/**
 * Whether a marker is drawn grey: gone quiet, or standing where the lie puts
 * phones. Both say the same to the viewer — the member is not to be looked for
 * here — and one colour for both is one fewer to learn.
 */
fun isGrey(position: MemberPosition, now: Instant): Boolean =
    isStale(position.knownAt, now) || position.inFalsePlace
