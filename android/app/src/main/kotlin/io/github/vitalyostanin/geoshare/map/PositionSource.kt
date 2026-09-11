package io.github.vitalyostanin.geoshare.map

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.service.GROUP
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.Instant

private const val TAG = "geoshare.positions"

/**
 * Where the members are, as they say so.
 *
 * A subscription rather than a read on a timer: a member who moves writes a
 * new position, and the marker follows within a second without this phone
 * asking for anything.
 */
fun memberPositions(
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
): Flow<List<MemberPosition>> = callbackFlow {
    val subscription =
        store
            .collection("groups")
            .document(GROUP)
            .collection("positions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    AppLog.w(TAG, "the positions subscription failed", error)

                    return@addSnapshotListener
                }

                trySend(snapshot?.documents.orEmpty().mapNotNull(::positionOf))
            }

    awaitClose { subscription.remove() }
}

/**
 * A document turns into a position, or into nothing.
 *
 * A document without coordinates is not a position, and the map is better off
 * leaving the member out than drawing them off the coast of Africa.
 */
private fun positionOf(document: DocumentSnapshot): MemberPosition? {
    val latitude = document.getDouble("latitude")
    val longitude = document.getDouble("longitude")
    val takenAt = document.getTimestamp("takenAt")

    if (latitude == null || longitude == null || takenAt == null) {
        AppLog.w(TAG, "the position of ${document.id} has no place or time in it")

        return null
    }

    return MemberPosition(
        memberId = document.id,
        latitude = latitude,
        longitude = longitude,
        takenAt = Instant.ofEpochSecond(takenAt.seconds, takenAt.nanoseconds.toLong()),
        writtenAt =
        document
            .getTimestamp("writtenAt")
            ?.let { Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong()) },
        mode = document.getString("mode"),
        batteryPercent = document.getLong("batteryPercent")?.toInt(),
    )
}
