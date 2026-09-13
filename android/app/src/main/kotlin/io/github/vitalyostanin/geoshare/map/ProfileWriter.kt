package io.github.vitalyostanin.geoshare.map

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.service.ProfileField
import io.github.vitalyostanin.geoshare.service.membersOf
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
            put(ProfileField.DISPLAY_NAME, displayName)
            // Written as a field rather than left out, so that an account that
            // loses its picture loses it on the other screens too.
            put(ProfileField.PHOTO_URL, pictureUrl)
            put(ProfileField.UPDATED_AT, FieldValue.serverTimestamp())
            // This phone, added to whatever the other phones of the member
            // have already put there: the readers cannot list the positions,
            // so this list is how they learn which documents to read.
            put(ProfileField.DEVICES, FieldValue.arrayUnion(deviceId))
        }

    try {
        membersOf(store)
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
        membersOf(store)
            .document(memberId)
            // The field named rather than merged: a plain merge joins the two
            // maps key by key, and a permission taken away would survive its
            // own removal. Naming the field replaces it whole and leaves the
            // rest of the profile — the name, the picture, the phones — alone.
            .set(
                mapOf(ProfileField.SHARED_WITH to written),
                SetOptions.mergeFields(ProfileField.SHARED_WITH),
            )
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
