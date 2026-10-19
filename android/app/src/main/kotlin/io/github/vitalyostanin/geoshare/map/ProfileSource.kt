package io.github.vitalyostanin.geoshare.map

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.project.GroupConnection
import io.github.vitalyostanin.geoshare.service.GrantField
import io.github.vitalyostanin.geoshare.service.ProfileField
import io.github.vitalyostanin.geoshare.service.comingBack
import io.github.vitalyostanin.geoshare.service.grantsOf
import io.github.vitalyostanin.geoshare.service.liveSnapshots
import io.github.vitalyostanin.geoshare.service.membersOf
import io.github.vitalyostanin.geoshare.service.permissionsOf
import io.github.vitalyostanin.geoshare.service.phonesOf
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val TAG = "geoshare.profiles"

/**
 * Whether the server says this member's own profile is gone from the group.
 *
 * Only an answer from the server counts, and only one with no write of this
 * device still on its way: a write the server refuses shows up in the cache
 * first and vanishes afterwards, and that is not the profile being taken away.
 * Nothing is said where the document cannot be read — a device not yet let into
 * the group has no profile there to lose.
 */
fun ownProfileGone(connection: GroupConnection, memberId: String): Flow<Boolean> = callbackFlow {
    val subscription =
        membersOf(connection)
            .document(memberId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close()

                    return@addSnapshotListener
                }

                if (snapshot != null &&
                    !snapshot.metadata.isFromCache &&
                    !snapshot.metadata.hasPendingWrites()
                ) {
                    trySend(!snapshot.exists())
                }
            }

    awaitClose { subscription.remove() }
}

/**
 * Who the members are, as they say so themselves.
 *
 * A subscription like the positions one, and for the same reason: a member who
 * changes the name on their account should stop being called by the old one
 * without anybody reopening the screen. It costs one read per profile and then
 * nothing, because names change far more rarely than positions do.
 */
fun memberProfiles(connection: GroupConnection): Flow<Map<String, MemberProfile>> =
    membersOf(connection)
        .liveSnapshots()
        .map { snapshot -> snapshot.documents.mapNotNull(::profileOf).associateBy { it.memberId } }
        // Nobody to name once the collection is closed to this device: the names it
        // holds are the members of a group it has been taken out of.
        .comingBack("profiles", forgetting = { emptyMap() })

/**
 * A document turns into a profile, or into nothing.
 *
 * A profile without a name says nothing the id does not already say, and the
 * screens fall back to the id on their own, so there is nothing to carry.
 */
private fun profileOf(document: DocumentSnapshot): MemberProfile? {
    val displayName =
        document.getString(ProfileField.DISPLAY_NAME)?.takeIf { it.isNotBlank() } ?: return null

    return MemberProfile(
        memberId = document.id,
        displayName = displayName,
        pictureUrl = accountPicture(document.getString(ProfileField.PHOTO_URL)),
        updatedAt = document.getTimestamp(ProfileField.UPDATED_AT)?.toInstant(),
    )
}

/**
 * What each member gives out, as far as this device is allowed to know.
 *
 * One subscription per member rather than one over the collection: the grants
 * cannot be listed, because listing them would hand the whole household's
 * devices to a member who was given nothing.
 *
 * A member who shares with this device answers with their devices; one who does
 * not answers with a refusal, and a refusal is simply an absence here — there
 * is no marker to drop and nothing to report, since the same member is already
 * missing from the map. The subscription is made again on a timer, so a
 * permission handed back arrives without the screen being reopened.
 */
fun memberGrants(
    memberIds: List<String>,
    connection: GroupConnection,
    /** Says when to ask again at once for every grant still refused. */
    retryNow: Flow<Unit> = emptyFlow(),
): Flow<Map<String, MemberGrant>> = callbackFlow {
    val known = mutableMapOf<String, MemberGrant>()
    val phones = mutableMapOf<String, Map<String, PhoneStatus>>()
    val subscriptions = mutableMapOf<String, ListenerRegistration>()
    val grants = grantsOf(connection)
    val phoneDocuments = phonesOf(connection)
    val refusals = Refusals(this)

    fun publish() = trySend(combinedGrants(known, phones))

    // One document of one member, kept in `into` for as long as it is readable.
    // Both documents of a member are followed the same way: a refusal drops what
    // was held and asks again on a timer, a missing document is an absence.
    fun <T : Any> followDocument(
        key: String,
        memberId: String,
        document: DocumentReference,
        into: MutableMap<String, T>,
        read: (DocumentSnapshot) -> T,
    ) {
        subscriptions[key] =
            document.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    AppLog.i(TAG, "the document $key is out of reach: ${error.code}")
                    into.remove(memberId)
                    publish()

                    refusals.retryLater(key) {
                        subscriptions.remove(key)?.remove()
                        followDocument(key, memberId, document, into, read)
                    }

                    return@addSnapshotListener
                }

                // A member who has not written a document yet — a device that
                // has not been opened since the split — has none, and that is
                // not a complaint either.
                if (snapshot?.exists() != true) {
                    into.remove(memberId)
                } else {
                    into[memberId] = read(snapshot)
                }

                publish()
            }
    }

    memberIds.forEach { memberId ->
        followDocument(memberId, memberId, grants.document(memberId), known, ::grantOf)

        // What the devices say about themselves, followed apart from the grant:
        // an administrator reads these without a permission, and a device of an
        // older build has not written them at all.
        followDocument(
            "phones:$memberId",
            memberId,
            phoneDocuments.document(memberId),
            phones,
        ) { phonesFrom(it.data) }
    }

    launch { retryNow.collect { refusals.retryNow() } }

    // Said once, so that a screen with nobody to follow draws an empty map
    // instead of waiting for an answer that is not coming.
    publish()

    awaitClose { subscriptions.values.forEach { it.remove() } }
}

/** A document turns into what its member gives out. */
private fun grantOf(document: DocumentSnapshot): MemberGrant = MemberGrant(
    memberId = document.id,
    devices = (document.get(GrantField.DEVICES) as? List<*>).orEmpty().filterIsInstance<String>(),
    sharedWith = permissionsOf(document),
)
