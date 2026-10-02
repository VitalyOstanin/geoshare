package io.github.vitalyostanin.geoshare.service

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.Reset
import io.github.vitalyostanin.geoshare.map.resetOf
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "geoshare.resets"

private val ESTIMATE = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE

/** Read with the server's estimate, as a siren is, so the asking screen sees its own request at once. */
private fun resetFrom(document: DocumentSnapshot): Reset? = resetOf(
    phoneId = document.id,
    askedBy = document.getString(ResetField.ASKED_BY),
    askedAt = document.getTimestamp(ResetField.ASKED_AT, ESTIMATE)?.toInstant(),
    doneAt = document.getTimestamp(ResetField.DONE_AT, ESTIMATE)?.toInstant(),
)

/**
 * The request standing for one phone, or null while nobody has asked: followed
 * by the phone itself, to forget, and by the asking screen, to see it done.
 */
fun phoneReset(
    phoneId: String,
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
): Flow<Reset?> = callbackFlow {
    val subscription =
        resetsOf(store).document(phoneId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)

                return@addSnapshotListener
            }

            trySend(snapshot?.takeIf { it.exists() }?.let(::resetFrom))
        }

    awaitClose { subscription.remove() }
}.comingBack("reset", forgetting = { null })

/** Asks one phone to forget its place; written whole, as a siren's call is. */
suspend fun askReset(
    phoneId: String,
    askerId: String,
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    resetsOf(store)
        .document(phoneId)
        .set(
            mapOf(
                ResetField.ASKED_BY to askerId,
                ResetField.ASKED_AT to FieldValue.serverTimestamp(),
            ),
        ).await()

    AppLog.i(TAG, "a phone has been asked to forget its place")
}

/** Marks that the phone named by [phoneId] forgot its place. Written only by that phone. */
suspend fun resetDone(
    phoneId: String,
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    resetsOf(store)
        .document(phoneId)
        .update(mapOf(ResetField.DONE_AT to FieldValue.serverTimestamp()))
        .await()

    AppLog.i(TAG, "the phone said it forgot its place")
}
