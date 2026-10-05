package io.github.vitalyostanin.geoshare.map

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.project.GroupConnection
import io.github.vitalyostanin.geoshare.service.GrantField
import io.github.vitalyostanin.geoshare.service.ProfileField
import io.github.vitalyostanin.geoshare.service.comingBack
import io.github.vitalyostanin.geoshare.service.grantsOf
import io.github.vitalyostanin.geoshare.service.membersOf
import io.github.vitalyostanin.geoshare.service.permissionsOf
import io.github.vitalyostanin.geoshare.service.phonesOf
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

private const val TAG = "geoshare.profiles"

/**
 * Who the members are, as they say so themselves.
 *
 * A subscription like the positions one, and for the same reason: a member who
 * changes the name on their account should stop being called by the old one
 * without anybody reopening the screen. It costs one read per profile and then
 * nothing, because names change far more rarely than positions do.
 */
fun memberProfiles(connection: GroupConnection): Flow<Map<String, MemberProfile>> = callbackFlow {
    val subscription =
        membersOf(connection)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)

                    return@addSnapshotListener
                }

                trySend(
                    snapshot
                        ?.documents
                        .orEmpty()
                        .mapNotNull(::profileOf)
                        .associateBy { it.memberId },
                )
            }

    awaitClose { subscription.remove() }
    // Nobody to name once the collection is closed to this phone: the names it
    // holds are the members of a group it has been taken out of.
}.comingBack("profiles", forgetting = { emptyMap() })

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
 * What each member gives out, as far as this phone is allowed to know.
 *
 * One subscription per member rather than one over the collection: the grants
 * cannot be listed, because listing them would hand the whole household's
 * phones to a member who was given nothing.
 *
 * A member who shares with this phone answers with their phones; one who does
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

    fun follow(memberId: String) {
        subscriptions[memberId] =
            grants.document(memberId).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    AppLog.i(TAG, "the grant of $memberId is out of reach: ${error.code}")
                    known.remove(memberId)
                    publish()

                    refusals.retryLater(memberId) {
                        subscriptions.remove(memberId)?.remove()
                        follow(memberId)
                    }

                    return@addSnapshotListener
                }

                // A member who has not written a grant yet — a phone that has
                // not been opened since the split — has no document, and that
                // is not a complaint either.
                if (snapshot?.exists() != true) {
                    known.remove(memberId)
                } else {
                    known[memberId] = grantOf(snapshot)
                }

                publish()
            }
    }

    // What the phones say about themselves, followed apart from the grant: an
    // administrator reads these without a permission, and a phone of an
    // older build has not written them at all.
    fun followPhones(memberId: String) {
        val key = "phones:$memberId"

        subscriptions[key] =
            phoneDocuments.document(memberId).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    phones.remove(memberId)
                    publish()

                    refusals.retryLater(key) {
                        subscriptions.remove(key)?.remove()
                        followPhones(memberId)
                    }

                    return@addSnapshotListener
                }

                if (snapshot?.exists() != true) {
                    phones.remove(memberId)
                } else {
                    phones[memberId] = phonesFrom(snapshot.data)
                }

                publish()
            }
    }

    memberIds.forEach {
        follow(it)
        followPhones(it)
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
