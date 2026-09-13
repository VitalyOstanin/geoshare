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
 * One document per phone, overwritten in place: the history is not kept, so
 * the space a member takes does not grow, and a reader always gets the latest
 * position with a single read.
 *
 * Per phone rather than per member, because a member can carry two: they used
 * to write into one document, and the marker showed whichever of them wrote
 * last. The owner is written into the document as well, since that is what the
 * readers group the markers by.
 *
 * Who may read it is not written here. The permissions live in the owner's
 * profile, where the owner writes them, and that is where the rules read them
 * from: a copy kept here would go on granting what has been taken back until
 * the phone happened to write again — and a phone that is off, lost or no
 * longer has the application never writes again at all.
 */
class PositionWriter(
    private val memberId: String,
    private val device: DeviceIdentity,
    private val store: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun write(fix: Fix, mode: CollectionMode, batteryPercent: Int, charging: Boolean) {
        val position =
            mapOf(
                PositionField.MEMBER_ID to memberId,
                PositionField.DEVICE_NAME to device.name,
                PositionField.LATITUDE to fix.latitude,
                PositionField.LONGITUDE to fix.longitude,
                PositionField.TAKEN_AT to Timestamp(Date.from(fix.takenAt)),
                // The reader compares this against its own clock to age the
                // marker, and phone clocks disagree.
                PositionField.WRITTEN_AT to FieldValue.serverTimestamp(),
                PositionField.MODE to mode.name,
                PositionField.BATTERY_PERCENT to batteryPercent,
                // A percentage on its own says nothing about which way it is
                // going: fifteen percent on a charger is a phone about to be
                // fine, and fifteen percent off one is a phone about to go
                // quiet.
                PositionField.CHARGING to charging,
            )

        positionsOf(store)
            .document(positionId(memberId, device.id))
            .set(position)
            .await()
    }
}
