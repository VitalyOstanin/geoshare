package io.github.vitalyostanin.geoshare.map

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.service.GROUP
import io.github.vitalyostanin.geoshare.service.positionId
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.Instant

private const val TAG = "geoshare.positions"

/**
 * Which documents hold the positions of the members whose profiles are known.
 *
 * Named one by one rather than asked for as a collection: a permission that
 * runs out cannot be proved by a query, so the collection of positions is
 * closed and a reader walks the devices each profile names instead.
 */
fun positionDocuments(profiles: Map<String, MemberProfile>): List<String> = profiles.values
    .flatMap { profile -> profile.devices.map { positionId(profile.memberId, it) } }
    .sorted()

/**
 * Where the members are, as they say so.
 *
 * A subscription rather than a read on a timer: a member who moves writes a
 * new position, and the marker follows within a second without this phone
 * asking for anything.
 *
 * One subscription per document, because that is the only shape the
 * permissions allow. A document this account may not read answers with a
 * refusal rather than with nothing, and that refusal is the member saying they
 * no longer share — so it drops the marker instead of being reported.
 */
fun memberPositions(
    documents: List<String>,
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
): Flow<List<MemberPosition>> = callbackFlow {
    val known = mutableMapOf<String, MemberPosition>()
    val positions = store.collection("groups").document(GROUP).collection("positions")

    fun publish() = trySend(known.values.toList())

    val subscriptions =
        documents.map { id ->
            positions.document(id).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    AppLog.i(TAG, "$id is out of reach: ${error.code}")
                }

                // A document that is not there is not a complaint: a member
                // whose profile names a phone they have since reinstalled has
                // one, and so does every device before its first write.
                val position =
                    if (error == null && snapshot?.exists() == true) positionOf(snapshot) else null

                if (position == null) {
                    if (known.remove(id) != null) {
                        publish()
                    }

                    return@addSnapshotListener
                }

                known[id] = position
                publish()
            }
        }

    // Said once at the start, so that a screen with nothing to draw shows an
    // empty map rather than waiting for an answer that never comes: a member
    // without devices has no document to subscribe to at all.
    publish()

    awaitClose { subscriptions.forEach { it.remove() } }
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
        positionId = document.id,
        // Written into the document by the phone that wrote the position. A
        // document from before positions were kept per phone has no such
        // field, and there the whole id is the account.
        memberId = document.getString("memberId") ?: document.id.substringBefore('.'),
        deviceName = document.getString("deviceName"),
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
