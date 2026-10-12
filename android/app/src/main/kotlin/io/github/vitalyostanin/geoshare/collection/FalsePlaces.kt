package io.github.vitalyostanin.geoshare.collection

// Places a false signal is known to put devices at.
//
// A spoofer over a city does not scatter its victims: it hands every receiver
// within reach the same coordinates, and those coordinates are chosen once and
// kept for months. Everybody in the city sees the same island, the same runway,
// the same empty field. That makes the place itself evidence, and evidence the
// speed and width rules cannot reach: they compare a fix with the one before
// it, and the first fix after the service starts has nothing to be compared
// with. A device switched on inside the affected area therefore takes the false
// place as its anchor and believes every step of the road drawn away from it.
//
// Which places those are is a question about one household's surroundings
// rather than about the application, so the list lives in the group's own
// settings in the database and reaches the device from there. Nothing is built
// in: a build carrying somebody's coordinates hands them to everybody who
// downloads it, and a list of places where receivers are being lied to says
// as much about the neighbourhood as about the devices.

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
 * How many places one group may keep.
 *
 * Every one of them is weighed against every fix, and the list is read by a
 * device that has to hold it in memory while it does nothing else. Fifty is far
 * past what a neighbourhood produces and short of anything that costs.
 */
const val FALSE_PLACES_KEPT: Int = 50

/**
 * The widest a place may be.
 *
 * What a place costs is the ground inside its radius: a device genuinely there
 * is a device that cannot be placed at all. Fifty kilometres is the whole
 * surroundings of a city, and past it a place stops being a place.
 */
const val FALSE_PLACE_WIDEST_METRES: Double = 50_000.0

/**
 * Whether a place is one the rules can be asked about.
 *
 * Checked on the way in from the database, because the rules there hold the
 * shape of a document and cannot hold what the numbers mean: a radius of zero
 * catches nothing and a negative one catches everything.
 */
fun falsePlaceStands(place: FalsePlace): Boolean = place.latitude in -90.0..90.0 &&
    place.longitude in -180.0..180.0 &&
    place.radiusMetres > 0.0 &&
    place.radiusMetres <= FALSE_PLACE_WIDEST_METRES

/**
 * The false place a fix has landed on, or nothing where it has landed
 * somewhere devices are actually taken.
 *
 * [places] is empty wherever the group has named none, and a device that has
 * never reached the database is a device with none: the rule then says nothing,
 * which is what it did before any place was ever listed.
 */
fun falsePlaceFor(fix: Fix, places: List<FalsePlace> = emptyList()): FalsePlace? =
    places.firstOrNull { distanceMetres(asFix(it, fix), fix) <= it.radiusMetres }

/** The place as a fix, so that one distance function serves both. */
private fun asFix(place: FalsePlace, at: Fix): Fix =
    Fix(latitude = place.latitude, longitude = place.longitude, takenAt = at.takenAt)
