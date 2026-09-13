package io.github.vitalyostanin.geoshare.map

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.service.PositionField
import io.github.vitalyostanin.geoshare.service.positionId
import io.github.vitalyostanin.geoshare.service.positionsOf
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

private const val TAG = "geoshare.positions"

/** How many phones of one member this screen will follow. The rules say the same. */
private const val DEVICES_PER_MEMBER = 10

/** How long to wait before asking again for a document that was refused. */
private val RETRY_AFTER: Duration = Duration.ofMinutes(1)

/**
 * Which documents hold the positions of the members whose profiles are known.
 *
 * Named one by one rather than asked for as a collection: a permission that
 * runs out cannot be proved by a query, so the collection of positions is
 * closed and a reader walks the devices each profile names instead.
 */
fun positionDocuments(profiles: Map<String, MemberProfile>): List<String> = profiles.values
    .flatMap { profile ->
        profile.devices
            // A subscription each, and the profile is written by somebody
            // else: a profile naming more phones than a person can carry would
            // put this screen out of action. The rules hold the list to the
            // same number; this is what happens to one written before they did.
            .take(DEVICES_PER_MEMBER)
            .map { positionId(profile.memberId, it) }
    }
    .sorted()

/**
 * What the subscriptions have to say: who is on the map, and how many are not.
 *
 * The count is the difference between "nobody is sharing with me" and "the
 * group is unreachable", which look the same on a map with no markers on it.
 */
data class MembersInView(
    val positions: List<MemberPosition> = emptyList(),
    val outOfReach: Int = 0,
)

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
): Flow<MembersInView> = callbackFlow {
    val known = mutableMapOf<String, MemberPosition>()
    val refused = mutableSetOf<String>()
    val subscriptions = mutableMapOf<String, ListenerRegistration>()
    val positions = positionsOf(store)

    fun publish() = trySend(MembersInView(known.values.toList(), refused.size))

    // A refusal stays a refusal until the member says otherwise, and what they
    // say does not reach this screen: a permission lives in their profile, and
    // the profile is the same document it was. So the subscription is made
    // again on a timer, and a permission handed back brings the marker back
    // with it rather than waiting for the screen to be opened afresh.
    fun follow(id: String) {
        subscriptions[id] =
            positions.document(id).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    AppLog.i(TAG, "$id is out of reach: ${error.code}")
                    refused.add(id)
                    known.remove(id)
                    publish()

                    launch {
                        delay(RETRY_AFTER.toMillis())
                        subscriptions.remove(id)?.remove()
                        follow(id)
                    }

                    return@addSnapshotListener
                }

                refused.remove(id)

                // A document that is not there is not a complaint: a member
                // whose profile names a phone they have since reinstalled has
                // one, and so does every device before its first write.
                val position = if (snapshot?.exists() == true) positionOf(snapshot) else null

                if (position == null) {
                    known.remove(id)
                    publish()

                    return@addSnapshotListener
                }

                known[id] = position
                publish()
            }
    }

    documents.forEach { follow(it) }

    // Said once at the start, so that a screen with nothing to draw shows an
    // empty map rather than waiting for an answer that never comes: a member
    // without devices has no document to subscribe to at all.
    publish()

    awaitClose { subscriptions.values.forEach { it.remove() } }
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
        charging = document.getBoolean("charging") ?: false,
    )
}
