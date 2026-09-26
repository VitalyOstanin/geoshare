package io.github.vitalyostanin.geoshare.service

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import io.github.vitalyostanin.geoshare.collection.CollectionMode
import kotlinx.coroutines.tasks.await

/**
 * What this phone says about itself, apart from where it is.
 *
 * Written into the member's document of phones, under this phone's id, rather
 * than into the position. None of it is a place: the moment the phone was in
 * touch, the mode it is in, how charged it is and whether it is on a cable are
 * true whatever the receiver is being told, and the filter that holds a
 * position back when the answers look forged has no business holding these
 * back with it. A phone put on a cable under a transmitter used to write
 * nothing at all, because the cable was news only inside a position.
 *
 * Merged rather than replaced, and merged into this phone's entry alone: the
 * other phones of the member write their own entries into the same document,
 * and the name the member gives the phone is written elsewhere.
 */
class PhoneStatusWriter(
    private val memberId: String,
    private val device: DeviceIdentity,
    private val store: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun write(mode: CollectionMode, batteryPercent: Int, charging: Boolean) {
        val entry =
            mapOf(
                PhoneField.MODEL to device.name,
                // By the server's clock, which is the one the readers can
                // trust to say how long ago the phone was last heard from.
                PhoneField.ONLINE_AT to FieldValue.serverTimestamp(),
                PhoneField.MODE to mode.name,
                PhoneField.BATTERY_PERCENT to batteryPercent,
                PhoneField.CHARGING to charging,
            )

        phonesOf(store)
            .document(memberId)
            .set(mapOf(device.id to entry), SetOptions.merge())
            .await()
    }
}
