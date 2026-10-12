package io.github.vitalyostanin.geoshare.service

import com.google.firebase.firestore.DocumentSnapshot
import io.github.vitalyostanin.geoshare.collection.FALSE_PLACES_KEPT
import io.github.vitalyostanin.geoshare.collection.FalsePlace
import io.github.vitalyostanin.geoshare.collection.falsePlaceStands
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.project.GroupConnection
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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
fun namedFalsePlaces(connection: GroupConnection): Flow<List<FalsePlace>> = callbackFlow {
    val subscription =
        falsePlacesOf(connection).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)

                return@addSnapshotListener
            }

            val places =
                snapshot?.documents.orEmpty()
                    .mapNotNull(::placeOf)
                    .take(FALSE_PLACES_KEPT)

            AppLog.i(TAG, "the group names ${places.size} false places")

            trySend(places)
        }

    awaitClose { subscription.remove() }
}.comingBack("false places")

/** A document read as a place, or nothing where it is not one. */
private fun placeOf(snapshot: DocumentSnapshot): FalsePlace? {
    val latitude = snapshot.getDouble(FalsePlaceField.LATITUDE) ?: return null
    val longitude = snapshot.getDouble(FalsePlaceField.LONGITUDE) ?: return null
    val radius = snapshot.getDouble(FalsePlaceField.RADIUS_METRES) ?: return null
    val place =
        FalsePlace(
            latitude = latitude,
            longitude = longitude,
            radiusMetres = radius,
            named = snapshot.getString(FalsePlaceField.NAMED) ?: snapshot.id,
        )

    if (!falsePlaceStands(place)) {
        AppLog.w(TAG, "a false place is out of range and was left out: ${snapshot.id}")

        return null
    }

    return place
}
