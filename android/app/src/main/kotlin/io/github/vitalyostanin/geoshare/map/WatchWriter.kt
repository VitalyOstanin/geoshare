package io.github.vitalyostanin.geoshare.map

import com.google.firebase.firestore.FieldValue
import io.github.vitalyostanin.geoshare.collection.WATCH_REFRESH
import io.github.vitalyostanin.geoshare.collection.WATCH_RETRY
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.project.GroupConnection
import io.github.vitalyostanin.geoshare.service.WatchField
import io.github.vitalyostanin.geoshare.service.positionId
import io.github.vitalyostanin.geoshare.service.watchOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

private const val TAG = "geoshare.watching"

/**
 * Says that somebody is looking, for as long as somebody is.
 *
 * This is the request half of "fresh on demand": the other phones watch this
 * collection, and seeing a live marker here is what makes them collect every
 * fifteen seconds instead of every fifteen minutes.
 */
class WatchWriter(
    private val memberId: String,
    deviceId: String,
    private val connection: GroupConnection,
) {
    // Per phone rather than per member: leaving the map on one phone must not
    // withdraw the claim the same member's other phone had just made.
    private val document = watchOf(connection).document(positionId(memberId, deviceId))

    /**
     * Keeps the claim alive until the caller stops it.
     *
     * Never returns on its own: it is cancelled with the screen, and the
     * marker it leaves behind expires by itself a few minutes later, which is
     * what covers a phone that is switched off mid-look.
     */
    suspend fun keepLooking(): Nothing {
        while (true) {
            var next = WATCH_REFRESH

            try {
                // The moment of the claim by the server's clock, not an end by
                // this phone's: whoever reads it adds the same few minutes, so
                // a phone whose clock is wrong cannot hold the group longer,
                // and cannot fail to hold it at all.
                document
                    .set(
                        mapOf(
                            WatchField.MEMBER_ID to memberId,
                            WatchField.SINCE to FieldValue.serverTimestamp(),
                        ),
                    )
                    .await()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                // The old claim is still out there and has minutes left on it,
                // so a failed write is worth a line and another try soon, not
                // a message on screen.
                AppLog.w(TAG, "could not say that we are looking", error)
                next = WATCH_RETRY
            }

            delay(next.toMillis())
        }
    }

    /**
     * Withdraws the claim as the screen goes away.
     *
     * Nothing depends on this arriving — the marker expires anyway — but
     * arriving is what lets the other phones go back to their slow mode in
     * seconds rather than minutes.
     */
    suspend fun stopLooking() {
        try {
            document.delete().await()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            AppLog.w(TAG, "could not say that we stopped looking", error)
        }
    }
}
