package io.github.vitalyostanin.geoshare.map

import com.google.firebase.firestore.FieldValue
import io.github.vitalyostanin.geoshare.collection.WATCH_REFRESH
import io.github.vitalyostanin.geoshare.collection.WATCH_RETRY
import io.github.vitalyostanin.geoshare.project.GroupConnection
import io.github.vitalyostanin.geoshare.service.WatchField
import io.github.vitalyostanin.geoshare.service.positionId
import io.github.vitalyostanin.geoshare.service.watchOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

private const val TAG = "geoshare.watching"

/**
 * Says that somebody is looking, for as long as somebody is.
 *
 * This is the request half of "fresh on demand": the other devices watch this
 * collection, and seeing a live marker here is what makes them collect every
 * thirty seconds instead of every fifteen minutes.
 */
class WatchWriter(
    private val memberId: String,
    deviceId: String,
    private val connection: GroupConnection,
) {
    // Per device rather than per member: leaving the map on one device must not
    // withdraw the claim the same member's other device had just made.
    private val document = watchOf(connection).document(positionId(memberId, deviceId))

    /**
     * Keeps the claim alive until the caller stops it.
     *
     * Never returns on its own: it is cancelled with the screen, and the
     * marker it leaves behind expires by itself a few minutes later, which is
     * what covers a device that is switched off mid-look.
     */
    suspend fun keepLooking(): Nothing {
        while (true) {
            var next = WATCH_REFRESH

            runCatchingFailure {
                // The moment of the claim by the server's clock, not an end by
                // this device's: whoever reads it adds the same few minutes, so
                // a device whose clock is wrong cannot hold the group longer,
                // and cannot fail to hold it at all.
                document
                    .set(
                        mapOf(
                            WatchField.MEMBER_ID to memberId,
                            WatchField.SINCE to FieldValue.serverTimestamp(),
                        ),
                    )
                    .await()
            }.onFailure { error ->
                // The old claim is still out there and has minutes left on it,
                // so a failed write is worth a line and another try soon, not
                // a message on screen.
                logFailure(TAG, "could not say that we are looking", error)
                next = WATCH_RETRY
            }

            delay(next.toMillis())
        }
    }

    /**
     * Withdraws the claim as the screen goes away.
     *
     * Nothing depends on this arriving — the marker expires anyway — but
     * arriving is what lets the other devices go back to their slow mode in
     * seconds rather than minutes.
     *
     * Nothing is written once the group no longer has this member signed in:
     * the screen goes away because of the sign-out, which has taken the claim
     * down with the rest of the device's documents, and the rules would refuse
     * a write without the account anyway.
     */
    suspend fun stopLooking() {
        if (connection.auth.currentUser?.uid != memberId) {
            return
        }

        runCatchingFailure { document.delete().await() }.onFailure { error ->
            logFailure(TAG, "could not say that we stopped looking", error)
        }
    }
}
