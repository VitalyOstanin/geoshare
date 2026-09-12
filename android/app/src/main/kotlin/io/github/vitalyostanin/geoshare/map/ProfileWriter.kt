package io.github.vitalyostanin.geoshare.map

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.service.GROUP
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.Date

private const val TAG = "geoshare.profile"

/**
 * Says who this phone's member is.
 *
 * A member writes only their own profile — the rules say as much — so this is
 * the only way a name reaches the other clients at all. Answers whether the
 * server took the write, because that, and not the copy the library shows back
 * immediately, is what makes it worth not writing again.
 */
suspend fun publishProfile(
    memberId: String,
    displayName: String,
    pictureUrl: String?,
    deviceId: String,
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
): Boolean {
    val profile =
        buildMap {
            put("displayName", displayName)
            // Written as a field rather than left out, so that an account that
            // loses its picture loses it on the other screens too.
            put("photoUrl", pictureUrl)
            put("updatedAt", FieldValue.serverTimestamp())
            // This phone, added to whatever the other phones of the member
            // have already put there: the readers cannot list the positions,
            // so this list is how they learn which documents to read.
            put("devices", FieldValue.arrayUnion(deviceId))
        }

    try {
        store
            .collection("groups")
            .document(GROUP)
            .collection("members")
            .document(memberId)
            // Merged rather than replaced: the permissions live in this
            // document too, and they are not this write's business.
            .set(profile, SetOptions.merge())
            .await()

        AppLog.i(TAG, "the profile of $memberId is now published")

        return true
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        // The markers fall back to the id, which is what they showed before
        // profiles existed, so this is worth a line rather than a message.
        AppLog.w(TAG, "could not publish the profile", error)

        return false
    }
}

/**
 * Says who this member lets watch them, and until when.
 *
 * Written into the profile rather than straight into the positions: every
 * phone of the member reads it from there and repeats it into its own
 * document, so a phone picked up tomorrow shares with the same people without
 * being told again.
 */
suspend fun publishSharing(
    memberId: String,
    sharedWith: Map<String, Instant>,
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
): Boolean {
    val written = sharedWith.mapValues { (_, until) -> Timestamp(Date.from(until)) }

    try {
        store
            .collection("groups")
            .document(GROUP)
            .collection("members")
            .document(memberId)
            // Merged, and the whole field replaced: a permission taken away is
            // a key that has to disappear, which merging a map does not do on
            // its own.
            .set(mapOf("sharedWith" to written), SetOptions.merge())
            .await()

        AppLog.i(TAG, "the permissions of $memberId are now published")

        return true
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        AppLog.w(TAG, "could not publish the permissions", error)

        return false
    }
}
