package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.collection.FALSE_PLACES_KEPT
import io.github.vitalyostanin.geoshare.collection.FalsePlace
import io.github.vitalyostanin.geoshare.collection.falsePlaceStands
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.project.GroupConnection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val TAG = "geoshare.falseplaces"

/**
 * The places this group says a false signal uses.
 *
 * A subscription rather than a read, and it costs almost nothing: the
 * collection holds a handful of documents that change a few times a year, so
 * after the first read the device is charged only when somebody adds a place.
 * What it buys is a device that starts refusing a new false place within
 * seconds of it being named, rather than at the next release of the
 * application.
 *
 * Whatever cannot be read as a place is dropped rather than thrown: a document
 * somebody typed wrong in the console must not stop the rest of the list from
 * reaching the devices.
 */
fun namedFalsePlaces(connection: GroupConnection): Flow<List<FalsePlace>> =
    falsePlacesOf(connection).liveSnapshots().map { snapshot ->
        val places =
            snapshot.documents
                .mapNotNull { placeOf(it.id, it.data.orEmpty()) }
                .take(FALSE_PLACES_KEPT)

        AppLog.i(TAG, "the group names ${places.size} false places")

        places
    }.comingBack("false places")

/**
 * A document read as a place, or nothing where it is not one. Read from its
 * fields rather than through the typed getters, which throw on a value of
 * another type: the console offers a string first, and a latitude typed as one
 * has to leave this place out, not stop the list.
 */
internal fun placeOf(id: String, fields: Map<String, Any?>): FalsePlace? {
    val latitude = numberIn(fields, FalsePlaceField.LATITUDE) ?: return null
    val longitude = numberIn(fields, FalsePlaceField.LONGITUDE) ?: return null
    val radius = numberIn(fields, FalsePlaceField.RADIUS_METRES) ?: return null
    val place =
        FalsePlace(
            latitude = latitude,
            longitude = longitude,
            radiusMetres = radius,
            named = textIn(fields, FalsePlaceField.NAMED) ?: id,
        )

    if (!falsePlaceStands(place)) {
        AppLog.w(TAG, "a false place is out of range and was left out: $id")

        return null
    }

    return place
}
