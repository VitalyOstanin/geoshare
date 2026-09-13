package io.github.vitalyostanin.geoshare.map

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.service.ProfileField
import io.github.vitalyostanin.geoshare.service.membersOf
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.Instant

private const val TAG = "geoshare.profiles"

/**
 * Who the members are, as they say so themselves.
 *
 * A subscription like the positions one, and for the same reason: a member who
 * changes the name on their account should stop being called by the old one
 * without anybody reopening the screen. It costs one read per profile and then
 * nothing, because names change far more rarely than positions do.
 */
fun memberProfiles(
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
): Flow<Map<String, MemberProfile>> = callbackFlow {
    val subscription =
        membersOf(store)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    AppLog.w(TAG, "the profiles subscription failed", error)

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
}

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
        pictureUrl = document.getString(ProfileField.PHOTO_URL)?.takeIf { it.isNotBlank() },
        devices =
        (document.get(ProfileField.DEVICES) as? List<*>).orEmpty().filterIsInstance<String>(),
        sharedWith = permissionsOf(document),
    )
}

/**
 * The permissions this member has given, as they stand.
 *
 * The profile is where they are kept, so that every phone of the member writes
 * the same ones and a phone picked up tomorrow inherits them rather than
 * starting from nothing. The service copies them into each position it writes,
 * because that is where a rule can weigh them without paying for a read.
 */
fun ownSharedWith(
    memberId: String,
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
): Flow<Map<String, Instant>> = callbackFlow {
    val subscription =
        membersOf(store)
            .document(memberId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    AppLog.w(TAG, "the permissions subscription failed", error)

                    return@addSnapshotListener
                }

                trySend(snapshot?.let(::permissionsOf).orEmpty())
            }

    awaitClose { subscription.remove() }
}

/**
 * The permissions a profile carries: a reader, and the moment their permission
 * ends.
 *
 * A permission without an end is written as a date far enough away to read as
 * never, so that the rules and both clients weigh one kind of value rather
 * than two.
 */
private fun permissionsOf(document: DocumentSnapshot): Map<String, Instant> {
    val stated = document.get(ProfileField.SHARED_WITH) as? Map<*, *> ?: return emptyMap()

    return stated
        .mapNotNull { (reader, until) ->
            val readerId = reader as? String ?: return@mapNotNull null
            val moment = until as? Timestamp ?: return@mapNotNull null

            readerId to Instant.ofEpochSecond(moment.seconds, moment.nanoseconds.toLong())
        }.toMap()
}
