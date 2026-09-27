package io.github.vitalyostanin.geoshare.service

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.Instant

/**
 * The permissions this member has given, as they stand.
 *
 * The grant is where they are kept, so that every phone of the member writes
 * the same ones and a phone picked up tomorrow inherits them rather than
 * starting from nothing. The rules read them from the same document, so
 * withdrawing one takes effect without any phone having to write again.
 */
fun ownSharedWith(
    memberId: String,
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
): Flow<Map<String, Instant>> = callbackFlow {
    val subscription =
        grantsOf(store)
            .document(memberId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)

                    return@addSnapshotListener
                }

                trySend(snapshot?.let(::permissionsOf).orEmpty())
            }

    awaitClose { subscription.remove() }
    // The permissions shown are the ones this phone may still read; refused,
    // it knows of none, and the screen should not offer to withdraw what it
    // cannot see.
}.comingBack("permissions", forgetting = { emptyMap() })

/**
 * The permissions a grant carries: a reader, and the moment their permission
 * ends.
 *
 * A permission without an end is written as a date far enough away to read as
 * never, so that the rules and both clients weigh one kind of value rather
 * than two.
 */
fun permissionsOf(document: DocumentSnapshot): Map<String, Instant> {
    val stated = document.get(GrantField.SHARED_WITH) as? Map<*, *> ?: return emptyMap()

    return stated
        .mapNotNull { (reader, until) ->
            val readerId = reader as? String ?: return@mapNotNull null
            val moment = until as? Timestamp ?: return@mapNotNull null

            readerId to Instant.ofEpochSecond(moment.seconds, moment.nanoseconds.toLong())
        }.toMap()
}
