package io.github.vitalyostanin.geoshare.map

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.service.GROUP
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

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
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
): Boolean {
    val profile =
        buildMap {
            put("displayName", displayName)
            // Written as a field rather than left out, so that an account that
            // loses its picture loses it on the other screens too.
            put("photoUrl", pictureUrl)
            put("updatedAt", FieldValue.serverTimestamp())
        }

    try {
        store
            .collection("groups")
            .document(GROUP)
            .collection("members")
            .document(memberId)
            .set(profile)
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
