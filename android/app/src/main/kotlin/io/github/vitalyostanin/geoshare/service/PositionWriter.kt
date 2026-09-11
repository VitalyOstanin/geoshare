package io.github.vitalyostanin.geoshare.service

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.github.vitalyostanin.geoshare.collection.Fix
import kotlinx.coroutines.tasks.await
import java.util.Date

/** The one group this build shares within; membership is edited outside the application. */
const val GROUP = "family"

/**
 * Where this phone says it is.
 *
 * One document per member, overwritten in place: the history is not kept, so
 * the space a member takes does not grow, and a reader always gets the latest
 * position with a single read.
 */
class PositionWriter(
    private val memberId: String,
    private val store: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun write(fix: Fix, mode: CollectionMode, batteryPercent: Int) {
        val position =
            mapOf(
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
            .document(memberId)
            .set(position)
            .await()
    }
}
