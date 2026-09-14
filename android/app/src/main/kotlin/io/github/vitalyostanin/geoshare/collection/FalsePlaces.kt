package io.github.vitalyostanin.geoshare.collection

// Places a false signal is known to put phones at.
//
// A spoofer over a city does not scatter its victims: it hands every receiver
// within reach the same coordinates, and those coordinates are chosen once and
// kept for months. Everybody in the city sees the same island, the same runway,
// the same empty field. That makes the place itself evidence, and evidence the
// speed and width rules cannot reach: they compare a fix with the one before
// it, and the first fix after the service starts has nothing to be compared
// with. A phone switched on inside the affected area therefore takes the false
// place as its anchor and believes every step of the road drawn away from it.

/**
 * A place a false signal lands on, and how far around it to distrust.
 *
 * The radius is not the spread of the answers — those sit within metres of each
 * other — but the length of the road the receiver draws away from the point
 * while it is being fed. Anything inside it is treated as the lie rather than
 * as a position.
 */
data class FalsePlace(
    val latitude: Double,
    val longitude: Double,
    val radiusMetres: Double,
    /** What the place is, for the log to say something a person can read. */
    val named: String,
)

/**
 * The places this build knows about.
 *
 * Kept in the build rather than read from the database, because the point
 * moves once in months, while reading it would need a phone that wakes to
 * fetch it — the very thing the service does not do. A place goes on this list
 * only where nobody has business being: the island is water and forest, and a
 * family standing on it is a case worth losing to catch the lie. The airport
 * the same spoofer used until the end of April is deliberately left off — it
 * has flights, staff and passengers, and calling it false would lose real
 * positions.
 *
 * The list is expected to grow. What one costs is the ground inside its
 * radius: a phone genuinely there is a phone this build refuses to place, so
 * a new entry is read off the log of a phone being fed the signal, given a
 * radius that covers the spread seen in that log and no more, and named for
 * somebody reading the log afterwards. Two entries are cheaper than one wide
 * one whenever the ground between them is ground a family can stand on.
 */
val KNOWN_FALSE_PLACES: List<FalsePlace> = listOf(
    // Every phone in the household that could see the satellites at all was
    // put here through the evening of 13 September 2026.
    FalsePlace(
        latitude = 61.428,
        longitude = 5.420,
        radiusMetres = 4_000.0,
        named = "a place a false signal uses",
    ),
    // Thirteen kilometres north-east of the island, and outside its radius:
    // the third phone, which has no mobile network and is placed by whatever
    // its satellites say, was held here on the same evening. One point so far,
    // so the radius is the smallest that covers it with room for the road the
    // receiver draws.
    FalsePlace(
        latitude = 61.49782,
        longitude = 5.60881,
        radiusMetres = 5_000.0,
        named = "a second place a false signal uses",
    ),
)

/**
 * The false place a fix has landed on, or nothing where it has landed
 * somewhere phones are actually taken.
 */
fun falsePlaceFor(fix: Fix, places: List<FalsePlace> = KNOWN_FALSE_PLACES): FalsePlace? =
    places.firstOrNull { distanceMetres(asFix(it, fix), fix) <= it.radiusMetres }

/** The place as a fix, so that one distance function serves both. */
private fun asFix(place: FalsePlace, at: Fix): Fix =
    Fix(latitude = place.latitude, longitude = place.longitude, takenAt = at.takenAt)
