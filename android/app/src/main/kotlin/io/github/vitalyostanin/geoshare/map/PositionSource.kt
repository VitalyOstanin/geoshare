package io.github.vitalyostanin.geoshare.map

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.project.GroupConnection
import io.github.vitalyostanin.geoshare.service.PositionField
import io.github.vitalyostanin.geoshare.service.memberIdOf
import io.github.vitalyostanin.geoshare.service.positionId
import io.github.vitalyostanin.geoshare.service.positionsOf
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import java.time.Duration

private const val TAG = "geoshare.positions"

/** How many devices of one member this screen will follow. The rules say the same. */
private const val DEVICES_PER_MEMBER = 10

/**
 * How long to wait before asking again for a document that was refused.
 *
 * Shared with the grants, which are refused for the same reason and come back
 * the same way: the member hands the permission back, and nothing about that
 * reaches a client whose subscription has already been turned away.
 *
 * Only a fallback: a member who gives a permission touches their profile, and
 * that retries at once (Refusals.retryNow). Every retry is reads the rules
 * charge the project for, so the timer is a quarter of an hour, not a minute.
 */
internal val RETRY_AFTER: Duration = Duration.ofMinutes(15)

/** The widest answer still drawn as a circle. The rules say the same number. */
internal const val WIDEST_CIRCLE_METRES = 100_000.0

/**
 * Which documents hold the positions of the members who let this device watch,
 * together with any this device may read regardless: its own, and every device
 * a search stands for right now.
 *
 * Named one by one rather than asked for as a collection: a permission that
 * runs out cannot be proved by a query, so the collection of positions is
 * closed and a reader walks the devices each grant names instead.
 */
fun positionDocuments(
    grants: Map<String, MemberGrant>,
    own: String? = null,
    /** The devices the group is looking for right now: their places open without a grant. */
    sought: Collection<String> = emptyList(),
): List<String> = grants.values
    .flatMap { grant ->
        grant.devices
            // A subscription each, and the grant is written by somebody
            // else: a grant naming more devices than a person can carry
            // would put this screen out of action. The rules hold the list
            // to the same number; this is what happens to one written
            // before they did.
            .take(DEVICES_PER_MEMBER)
            .map { positionId(grant.memberId, it) }
    }
    // This device's own document is followed whether or not its grant says
    // so yet: the grant is written after the first position, and until it
    // reaches the server the device would show nobody at all, itself
    // included. The database hands a subscriber its own write before the
    // server has it, so the marker is there as soon as the device has a
    // position, with the network or without it.
    .plus(listOfNotNull(own))
    // A standing search opens its one device's place over whatever the
    // grant says, and to readers the grant never named at all: the rules
    // read the search where they used to read the grant, so the reader
    // has to ask for the document the same way, grant or none.
    .plus(sought)
    .distinct()
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
 * new position, and the marker follows within a second without this device
 * asking for anything.
 *
 * One subscription per document, because that is the only shape the
 * permissions allow. A document this account may not read answers with a
 * refusal rather than with nothing, and that refusal is the member saying they
 * no longer share — so it drops the marker instead of being reported.
 */
fun memberPositions(
    documents: List<String>,
    connection: GroupConnection,
    /** Says when to ask again at once for every document still refused. */
    retryNow: Flow<Unit> = emptyFlow(),
): Flow<MembersInView> = callbackFlow {
    val known = mutableMapOf<String, MemberPosition>()
    val refused = mutableSetOf<String>()
    val subscriptions = mutableMapOf<String, ListenerRegistration>()
    val positions = positionsOf(connection)
    val refusals = Refusals(this)

    // Held back until every document has answered once, a refusal counting
    // as an answer: the map opens on the freshest of one's own devices, and a
    // list that so far holds only the old one would open it there for good.
    val unanswered = documents.toMutableSet()

    fun show() = trySend(MembersInView(known.values.toList(), refused.size))

    fun publish() {
        if (unanswered.isEmpty()) {
            show()
        }
    }

    // A refusal stays a refusal until the member says otherwise, and what they
    // say does not reach this screen: a permission lives in their grant, which
    // a refused client cannot read either. So the subscription is made
    // again on a timer, and a permission handed back brings the marker back
    // with it rather than waiting for the screen to be opened afresh.
    fun follow(id: String) {
        val document = positions.document(id)

        subscriptions[id] =
            // With the metadata changes as well: whether this device's own
            // write is still waiting is not part of the document, and a listener
            // told only of the data would hear of it only as a side effect.
            document.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                unanswered.remove(id)

                if (error != null) {
                    AppLog.i(TAG, "$id is out of reach: ${error.code}")
                    refused.add(id)
                    known.remove(id)
                    publish()

                    refusals.retryLater(id) {
                        subscriptions.remove(id)?.remove()
                        follow(id)
                    }

                    return@addSnapshotListener
                }

                refused.remove(id)

                // A document that is not there is not a complaint: a member
                // whose grant names a device they have since reinstalled has
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

    launch { retryNow.collect { refusals.retryNow() } }

    // Said once at the start, so that a screen with nothing to draw shows an
    // empty map rather than waiting for an answer that never comes: a member
    // whose grant this device may not read has no document to subscribe to at
    // all.
    show()

    awaitClose { subscriptions.values.forEach { it.remove() } }
}

/**
 * A document turns into a position, or into nothing.
 *
 * A document without usable coordinates is not a position, and the map is
 * better off leaving the member out than drawing them off the coast of Africa
 * — or, for a number far outside the globe, than being asked to place a point
 * that is not on it.
 */
private fun positionOf(document: DocumentSnapshot): MemberPosition? {
    val latitude = document.getDouble(PositionField.LATITUDE)
    val longitude = document.getDouble(PositionField.LONGITUDE)
    val takenAt = document.getTimestamp(PositionField.TAKEN_AT)

    if (latitude == null || longitude == null || takenAt == null ||
        !placeable(latitude, longitude)
    ) {
        AppLog.w(TAG, "the position of ${document.id} has no place or time in it")

        return null
    }

    return MemberPosition(
        positionId = document.id,
        // Written into the document by the device that wrote the position. A
        // document from before positions were kept per device has no such
        // field, and there the whole id is the account.
        memberId = document.getString(PositionField.MEMBER_ID) ?: memberIdOf(document.id),
        deviceName = null,
        latitude = latitude,
        longitude = longitude,
        takenAt = takenAt.toInstant(),
        writtenAt =
        document
            .getTimestamp(PositionField.WRITTEN_AT)
            ?.toInstant(),
        mode = document.getString(PositionField.MODE),
        batteryPercent = document.getLong(PositionField.BATTERY_PERCENT)?.toInt(),
        charging = document.getBoolean(PositionField.CHARGING) ?: false,
        accuracyMetres = widthOf(document),
        foundBy = document.getString(PositionField.FOUND_BY),
        settling = document.getBoolean(PositionField.SETTLING) ?: false,
        leftOutCount = document.getLong(PositionField.LEFT_OUT_COUNT)?.toInt(),
        leftOutReason = document.getString(PositionField.LEFT_OUT_REASON),
        leftOutSince =
        document
            .getTimestamp(PositionField.LEFT_OUT_SINCE)
            ?.toInstant(),
        unsent = document.metadata.hasPendingWrites(),
    )
}

/**
 * The width of the answer, where the document holds one worth drawing.
 *
 * A width is a radius, so nought and less are not widths at all; and one wider
 * than a hundred kilometres is a circle covering everything the map can show,
 * which says less about where the device is than the marker alone does. The
 * rules refuse both, and this is what happens to a document written before
 * they did.
 */
private fun widthOf(document: DocumentSnapshot): Double? = document
    .getDouble(PositionField.ACCURACY_METRES)
    ?.takeIf { it.isFinite() && it > 0.0 && it <= WIDEST_CIRCLE_METRES }
