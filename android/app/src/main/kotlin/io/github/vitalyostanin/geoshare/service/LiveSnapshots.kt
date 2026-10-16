package io.github.vitalyostanin.geoshare.service

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * The document as it stands, and again at every change, until the collector
 * goes away.
 *
 * A refusal or a dropped connection ends the flow with the very error the
 * server gave, which is what [comingBack] and the callers that tell a refusal
 * from a lost link read. The library's own `snapshots()` ends it by cancelling
 * the flow instead, and a cancellation is something a collector is meant to
 * pass on without a look. Listeners that need the metadata changes, or that
 * keep going after an error, subscribe by hand.
 */
fun DocumentReference.liveSnapshots(): Flow<DocumentSnapshot> = callbackFlow {
    val subscription =
        addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)

                return@addSnapshotListener
            }

            if (snapshot != null) {
                trySend(snapshot)
            }
        }

    awaitClose { subscription.remove() }
}

/** The same for a collection or a query: the whole result, again at every change. */
fun Query.liveSnapshots(): Flow<QuerySnapshot> = callbackFlow {
    val subscription =
        addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)

                return@addSnapshotListener
            }

            if (snapshot != null) {
                trySend(snapshot)
            }
        }

    awaitClose { subscription.remove() }
}
