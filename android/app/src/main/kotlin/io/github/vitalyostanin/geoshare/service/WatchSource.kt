package io.github.vitalyostanin.geoshare.service

import com.google.firebase.firestore.FirebaseFirestore
import io.github.vitalyostanin.geoshare.log.AppLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.Instant

private const val TAG = "geoshare.watch"

/**
 * When the other members stop looking, as they themselves say.
 *
 * Each viewer writes a moment a little way into the future and keeps pushing it
 * forward while the map is open. A phone that goes silent — a dead battery, a
 * closed lid — therefore stops being a viewer on its own, with no message
 * needed to say so.
 *
 * This is the subscription that makes the whole arrangement work without push
 * messages: the write lands on this phone in about a second.
 */
fun watchersUntil(
    memberId: String,
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
): Flow<List<Instant>> = callbackFlow {
    val subscription =
        store
            .collection("groups")
            .document(GROUP)
            .collection("watch")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    AppLog.w(TAG, "the watch subscription failed", error)

                    return@addSnapshotListener
                }

                val untils =
                    snapshot
                        ?.documents
                        .orEmpty()
                        // Our own marker says nothing about who is looking
                        // at us; answering it would keep the phone awake
                        // for its own map.
                        .filter { it.id != memberId }
                        .mapNotNull { it.getTimestamp("until") }
                        .map { Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong()) }

                trySend(untils)
            }

    awaitClose { subscription.remove() }
}
