package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.collection.WATCH_AHEAD
import io.github.vitalyostanin.geoshare.collection.Watcher
import io.github.vitalyostanin.geoshare.project.GroupConnection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * Who the other members looking are, and when each of them stops counting.
 *
 * Each viewer writes down the moment it last said it was looking, by the
 * server's clock, and keeps saying so while the map is open. A device that goes
 * silent — a dead battery, a closed lid — therefore stops being a viewer on
 * its own, with no message needed to say so.
 *
 * This is the subscription that makes the whole arrangement work without push
 * messages: the write lands on this device in about a second.
 */
fun watchers(
    /** The document this device writes its own marker into, which is the one to ignore. */
    ownWatchId: String,
    connection: GroupConnection,
): Flow<List<Watcher>> = watchOf(connection)
    .liveSnapshots()
    .map { snapshot ->
        snapshot.documents
            // Our own marker says nothing about who is looking at us; answering
            // it would keep the device awake for its own map. Only this device's
            // marker is dropped: the member's other device looking at the map is
            // somebody to be fresh for, and somebody the screen names.
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
                    // is good for is this device's to decide, from
                    // a clock both sides share.
                    until =
                    Instant
                        .ofEpochSecond(since.seconds, since.nanoseconds.toLong())
                        .plus(WATCH_AHEAD),
                )
            }
    }
    // Nobody is looking as far as a refused device can tell, and the strip
    // under the map should say so rather than name the watchers it last saw.
    .comingBack("watch", forgetting = { emptyList() })
