package io.github.vitalyostanin.geoshare.map

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.project.ConnectLink
import io.github.vitalyostanin.geoshare.project.GroupConnection
import io.github.vitalyostanin.geoshare.service.GrantField
import io.github.vitalyostanin.geoshare.service.ProfileField
import io.github.vitalyostanin.geoshare.service.grantsOf
import io.github.vitalyostanin.geoshare.service.membersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.Date

private const val TAG = "geoshare.profile"

/**
 * The name a member goes by in the group: the account's own, or, for an
 * account without one, its shortened address — the same the admin saw on the
 * invitation, and more use to them than the start of an id.
 */
fun profileName(displayName: String?, email: String?): String? =
    displayName?.trim()?.takeIf { it.isNotEmpty() }
        ?: email?.trim()?.takeIf { it.isNotEmpty() }?.let(ConnectLink::shortenedAddress)

/**
 * Says who this device's member is.
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
    connection: GroupConnection,
): Boolean {
    val profile =
        mapOf(
            ProfileField.DISPLAY_NAME to displayName,
            // Written as a field rather than left out, so that an account that
            // loses its picture loses it on the other screens too. Held to
            // the account host, as the rules hold it, so that a picture kept
            // anywhere else costs the picture and not the whole profile.
            ProfileField.PHOTO_URL to accountPicture(pictureUrl),
            ProfileField.UPDATED_AT to FieldValue.serverTimestamp(),
        )

    try {
        membersOf(connection)
            .document(memberId)
            // Merged rather than replaced, so that a field added here later is
            // not wiped by a build that predates it.
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
 * Adds this device to the list its member's readers subscribe to.
 *
 * Written into the grant rather than into the profile: a reader who has been
 * given a permission needs the list, and the rest of the household has no
 * business knowing how many devices somebody carries.
 *
 * The list is joined rather than replaced, because the other devices of the
 * same member put themselves in it the same way and none of them has the whole
 * list to write.
 */
suspend fun publishDevice(
    memberId: String,
    deviceId: String,
    connection: GroupConnection,
): Boolean {
    try {
        grantsOf(connection)
            .document(memberId)
            // Merged rather than replaced: the permissions live in this
            // document too, and they are not this write's business.
            .set(mapOf(GrantField.DEVICES to FieldValue.arrayUnion(deviceId)), SetOptions.merge())
            .await()

        AppLog.i(TAG, "the phone of $memberId is now on the list its readers follow")

        return true
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        // Nothing on this device stops working: it goes on writing its own
        // position, and it is the readers who will not find the document.
        AppLog.w(TAG, "could not publish the phone", error)

        return false
    }
}

/**
 * Touches this member's profile, so that the members tell a changed grant at once.
 *
 * A reader the grant used to refuse cannot hear it change, and would ask again
 * only on its timer; the profiles every member follows can be heard. Says
 * nothing of whom the place went to. Worth a line when it fails, since the
 * timer still brings the marker, only later.
 */
private suspend fun announceSharing(memberId: String, connection: GroupConnection) {
    try {
        membersOf(connection)
            .document(memberId)
            .update(ProfileField.UPDATED_AT, FieldValue.serverTimestamp())
            .await()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        AppLog.w(TAG, "could not say that the permissions changed", error)
    }
}

/**
 * Says who this member lets watch them, and until when.
 *
 * Written into the grant rather than straight into the positions: every device
 * of the member reads it from there, so a device picked up tomorrow shares with
 * the same people without being told again, and the rules weigh the permission
 * where its owner writes it rather than where a device last copied it.
 */
suspend fun publishSharing(
    memberId: String,
    sharedWith: Map<String, Instant>,
    connection: GroupConnection,
): Boolean {
    val written = sharedWith.mapValues { (_, until) -> Timestamp(Date.from(until)) }

    try {
        grantsOf(connection)
            .document(memberId)
            // The field named rather than merged: a plain merge joins the two
            // maps key by key, and a permission taken away would survive its
            // own removal. Naming the field replaces it whole and leaves the
            // list of devices alone.
            .set(
                mapOf(GrantField.SHARED_WITH to written),
                SetOptions.mergeFields(GrantField.SHARED_WITH),
            )
            .await()

        AppLog.i(TAG, "the permissions of $memberId are now published")
        announceSharing(memberId, connection)

        return true
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        AppLog.w(TAG, "could not publish the permissions", error)

        return false
    }
}
