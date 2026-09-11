package io.github.vitalyostanin.geoshare.map

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.service.GROUP
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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
        store
            .collection("groups")
            .document(GROUP)
            .collection("members")
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
    val displayName = document.getString("displayName")?.takeIf { it.isNotBlank() } ?: return null

    return MemberProfile(
        memberId = document.id,
        displayName = displayName,
        pictureUrl = document.getString("photoUrl")?.takeIf { it.isNotBlank() },
    )
}
