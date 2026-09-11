package io.github.vitalyostanin.geoshare.map

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.service.GROUP
import io.github.vitalyostanin.geoshare.service.positionId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.time.Duration
import java.util.Date

private const val TAG = "geoshare.watching"

/**
 * How far ahead a viewer claims to be looking.
 *
 * Longer than the refresh below, so that one write lost to a bad connection
 * does not drop the other phones out of their fast mode; short enough that a
 * viewer who disappears without a word stops being one within minutes.
 */
val WATCH_AHEAD: Duration = Duration.ofMinutes(3)

/** How often the claim is pushed forward while the map is on screen. */
val WATCH_REFRESH: Duration = Duration.ofMinutes(1)

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
    private val store: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    // Per phone rather than per member: leaving the map on one phone must not
    // withdraw the claim the same member's other phone had just made.
    private val document =
        store
            .collection("groups")
            .document(GROUP)
            .collection("watch")
            .document(positionId(memberId, deviceId))

    /**
     * Keeps the claim alive until the caller stops it.
     *
     * Never returns on its own: it is cancelled with the screen, and the
     * marker it leaves behind expires by itself a few minutes later, which is
     * what covers a phone that is switched off mid-look.
     */
    suspend fun keepLooking(): Nothing {
        while (true) {
            val until = Timestamp(Date(System.currentTimeMillis() + WATCH_AHEAD.toMillis()))

            try {
                document.set(mapOf("memberId" to memberId, "until" to until)).await()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                // The old claim is still out there and has minutes left on it,
                // so a failed write is worth a line and another try, not a
                // message on screen.
                AppLog.w(TAG, "could not say that we are looking", error)
            }

            delay(WATCH_REFRESH.toMillis())
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
