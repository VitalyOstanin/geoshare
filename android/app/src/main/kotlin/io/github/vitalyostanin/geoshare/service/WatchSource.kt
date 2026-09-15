package io.github.vitalyostanin.geoshare.service

import com.google.firebase.firestore.FirebaseFirestore
import io.github.vitalyostanin.geoshare.collection.WATCH_AHEAD
import io.github.vitalyostanin.geoshare.collection.Watcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.Instant

/**
 * Who the other members looking are, and when each of them stops counting.
 *
 * Each viewer writes down the moment it last said it was looking, by the
 * server's clock, and keeps saying so while the map is open. A phone that goes
 * silent — a dead battery, a closed lid — therefore stops being a viewer on
 * its own, with no message needed to say so.
 *
 * This is the subscription that makes the whole arrangement work without push
 * messages: the write lands on this phone in about a second.
 */
fun watchers(
    /** The document this phone writes its own marker into, which is the one to ignore. */
    ownWatchId: String,
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
): Flow<List<Watcher>> = callbackFlow {
    val subscription =
        watchOf(store)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)

                    return@addSnapshotListener
                }

                val looking =
                    snapshot
                        ?.documents
                        .orEmpty()
                        // Our own marker says nothing about who is looking
                        // at us; answering it would keep the phone awake
                        // for its own map. Only this phone's marker is
                        // dropped: the member's other phone looking at the
                        // map is somebody to be fresh for, and somebody the
                        // screen names.
                        .filter { it.id != ownWatchId }
                        .mapNotNull { document ->
                            val since = document.getTimestamp(WatchField.SINCE)
                            val member = document.getString(WatchField.MEMBER_ID)

                            if (since == null || member == null) {
                                return@mapNotNull null
                            }

                            Watcher(
                                memberId = member,
                                // The claim says when it was made; how long it
                                // is good for is this phone's to decide, from a
                                // clock both sides share.
                                until =
                                Instant
                                    .ofEpochSecond(since.seconds, since.nanoseconds.toLong())
                                    .plus(WATCH_AHEAD),
                            )
                        }

                trySend(looking)
            }

    awaitClose { subscription.remove() }
}.comingBack("watch")
