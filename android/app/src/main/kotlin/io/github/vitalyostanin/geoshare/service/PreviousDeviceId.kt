package io.github.vitalyostanin.geoshare.service

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.logFailure
import io.github.vitalyostanin.geoshare.project.GroupConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

private const val TAG = "geoshare.device"

/**
 * Takes back what this device wrote under the id it went by before, once.
 *
 * A device that moved from a random id to the system's one would otherwise
 * leave its old position on everybody's map for good, greying, beside the new
 * one; its readers would go on following a document nobody writes, and the
 * list of its devices would name one that is gone.
 *
 * The old id is forgotten only once the documents are: a device offline or not
 * yet let into the group tries again the next time it starts. Removing what
 * is already gone does nothing, so the screen and the service may both try at
 * once.
 */
suspend fun leavePreviousId(
    memberId: String,
    device: DeviceIdentity,
    connection: GroupConnection,
) {
    val previous = device.previousId ?: return

    try {
        forgetPhone(memberId, previous, connection)
        device.forgetPrevious(previous)
        AppLog.i(TAG, "the documents of the previous id are taken back")
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        logFailure(TAG, "could not take back the documents of the previous id", error)
    }
}

/**
 * Removes one of a member's devices from the group: its position, its claim to
 * be looking, its place among the devices the member gives out and its line in
 * the member's list of devices, with the name the member gave it.
 *
 * What everybody sees goes with it: the marker on the map and the line in the
 * lists. The names other members gave the device stay with them, keyed by the
 * same id, and come back if the device does.
 *
 * One batch, so the four go together or not at all. Only the member may write
 * any of them, so this is done by the member, never for them.
 */
suspend fun forgetPhone(memberId: String, deviceId: String, connection: GroupConnection) {
    val document = positionId(memberId, deviceId)

    connection.store
        .batch()
        .delete(positionsOf(connection).document(document))
        .delete(watchOf(connection).document(document))
        .set(
            grantsOf(connection).document(memberId),
            mapOf(GrantField.DEVICES to FieldValue.arrayRemove(deviceId)),
            SetOptions.merge(),
        )
        .set(
            phonesOf(connection).document(memberId),
            mapOf(deviceId to FieldValue.delete()),
            SetOptions.merge(),
        )
        .commit()
        .await()
}
