package io.github.vitalyostanin.geoshare.service

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.FoundBy
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.Date

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
    suspend fun write(
        fix: Fix,
        mode: CollectionMode,
        batteryPercent: Int,
        charging: Boolean,
        foundBy: FoundBy,
        // Whether the receiver had not settled since the phone was switched
        // on. Worked out by the caller, where the epoch this fix came out of
        // is to hand; see Settling.kt for what the minutes after a boot look
        // like.
        settling: Boolean = false,
    ) {
        val position =
            mapOf(
                PositionField.MEMBER_ID to memberId,
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
                // How the phone arrived at this, in its own words rather than
                // the provider's: the same answer reads as `gps` whether the
                // receiver measured it or an assistance service sent it over,
                // and those are worth opposite amounts to whoever is looking.
                // Worked out by the caller, which is where the receiver's own
                // last answer is to hand.
                PositionField.FOUND_BY to foundBy.name,
            ) + width(fix) + settling(settling)

        positionsOf(store)
            .document(positionId(memberId, device.id))
            .set(position)
            .await()
    }

    /**
     * Says that answers are arriving and being left out.
     *
     * Written into the phone's own position document, beside a position that
     * is by then out of date: the point of it is precisely that the position
     * is not being renewed. Nothing else in the document is touched — the
     * coordinates, the moment they were taken at and the moment the server
     * stamped all stay as they were, so the marker goes on ageing honestly
     * while the card explains why.
     *
     * An update rather than a write of the whole document, and one that fails
     * where there is nothing to update: a phone whose answers were being
     * thrown out before it ever wrote a position has nowhere to say so, and
     * inventing a document without coordinates would put a marker nowhere on
     * everybody's map. The caller treats a failure as nothing happening.
     */
    suspend fun noteLeftOut(count: Int, reason: String, since: Instant) {
        positionsOf(store)
            .document(positionId(memberId, device.id))
            .update(
                mapOf(
                    PositionField.LEFT_OUT_COUNT to count,
                    PositionField.LEFT_OUT_REASON to reason,
                    PositionField.LEFT_OUT_SINCE to Timestamp(Date.from(since)),
                ),
            )
            .await()
    }

    /**
     * The width of the answer, where there is one.
     *
     * Left out of the document rather than written as null when the provider
     * said nothing: the readers draw a circle for a width they have and leave
     * the marker alone for one they do not, and an absent field says that
     * plainly without every reader having to know that null means absent.
     */
    private fun width(fix: Fix): Map<String, Any> =
        fix.accuracyMetres?.let { mapOf(PositionField.ACCURACY_METRES to it) }.orEmpty()

    /**
     * That the receiver had not settled, where it had not.
     *
     * Left out of the document rather than written as false, for the same
     * reason as the width above: the readers say something about a position
     * whose phone had just been switched on and leave every other one alone,
     * and an absent field says that without a reader having to know that false
     * and absent mean the same thing.
     */
    private fun settling(settling: Boolean): Map<String, Any> =
        if (settling) mapOf(PositionField.SETTLING to true) else emptyMap()
}
