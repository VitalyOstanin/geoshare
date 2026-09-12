package io.github.vitalyostanin.geoshare.service

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.github.vitalyostanin.geoshare.collection.Fix
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.Date

/** The one group this build shares within; membership is edited outside the application. */
const val GROUP = "family"

/**
 * Where this phone says it is.
 *
 * One document per phone, overwritten in place: the history is not kept, so
 * the space a member takes does not grow, and a reader always gets the latest
 * position with a single read.
 *
 * Per phone rather than per member, because a member can carry two: they used
 * to write into one document, and the marker showed whichever of them wrote
 * last. The owner is written into the document as well, since that is what the
 * readers group the markers by.
 */
class PositionWriter(
    private val memberId: String,
    private val device: DeviceIdentity,
    private val store: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun write(
        fix: Fix,
        mode: CollectionMode,
        batteryPercent: Int,
        sharedWith: Map<String, Instant>,
    ) {
        val position =
            mapOf(
                "memberId" to memberId,
                "deviceName" to device.name,
                // Copied from the profile rather than decided here: the member
                // says once who may watch them, and every phone of theirs
                // repeats it into its own document, which is where a rule can
                // read it without paying for a second document.
                "sharedWith" to sharedWith.mapValues { (_, until) -> Timestamp(Date.from(until)) },
                "latitude" to fix.latitude,
                "longitude" to fix.longitude,
                "takenAt" to Timestamp(Date.from(fix.takenAt)),
                // The reader compares this against its own clock to age the
                // marker, and phone clocks disagree.
                "writtenAt" to FieldValue.serverTimestamp(),
                "mode" to mode.name,
                "batteryPercent" to batteryPercent,
            )

        store
            .collection("groups")
            .document(GROUP)
            .collection("positions")
            .document(positionId(memberId, device.id))
            .set(position)
            .await()
    }
}
