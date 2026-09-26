package io.github.vitalyostanin.geoshare.service

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import io.github.vitalyostanin.geoshare.log.AppLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

private const val TAG = "geoshare.device"

/**
 * Takes back what this phone wrote under the id it went by before, once.
 *
 * A phone that moved from a random id to the system's one would otherwise
 * leave its old position on everybody's map for good, greying, beside the new
 * one; its readers would go on following a document nobody writes, and the
 * list of its phones would name one that is gone.
 *
 * One batch, so the four are taken back together or not at all, and the old
 * id is forgotten only once they are: a phone offline or not yet let into the
 * group tries again the next time it starts. Removing what is already gone
 * does nothing, so the screen and the service may both try at once.
 */
suspend fun leavePreviousId(
    memberId: String,
    device: DeviceIdentity,
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val previous = device.previousId ?: return
    val document = positionId(memberId, previous)

    try {
        store
            .batch()
            .delete(positionsOf(store).document(document))
            .delete(watchOf(store).document(document))
            .set(
                grantsOf(store).document(memberId),
                mapOf(GrantField.DEVICES to FieldValue.arrayRemove(previous)),
                SetOptions.merge(),
            )
            .set(
                phonesOf(store).document(memberId),
                mapOf(previous to FieldValue.delete()),
                SetOptions.merge(),
            )
            .commit()
            .await()

        device.forgetPrevious(previous)
        AppLog.i(TAG, "the documents of the previous id are taken back")
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        AppLog.w(TAG, "could not take back the documents of the previous id", error)
    }
}
