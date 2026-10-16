package io.github.vitalyostanin.geoshare.service

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.Siren
import io.github.vitalyostanin.geoshare.map.sirenOf
import io.github.vitalyostanin.geoshare.project.GroupConnection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Instant

private const val TAG = "geoshare.sirens"

/**
 * A document turns into a siren, or into nothing.
 *
 * Read with the server's estimate rather than its confirmed answer: a caller
 * who has just called sees its own write at once, with `calledAt` guessed at
 * this device's clock until the server answers, rather than with no call at
 * all for the second or so that takes.
 */
private fun sirenFrom(document: DocumentSnapshot): Siren? = sirenOf(
    phoneId = document.id,
    calledBy = document.getString(SirenField.CALLED_BY),
    calledAt = document.getTimestamp(SirenField.CALLED_AT, ESTIMATE)?.toInstant(),
    heardAt = document.getTimestamp(SirenField.HEARD_AT, ESTIMATE)?.toInstant(),
    stoppedBy = document.getString(SirenField.STOPPED_BY),
    stoppedAt = document.getTimestamp(SirenField.STOPPED_AT, ESTIMATE)?.toInstant(),
)

private val ESTIMATE = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE

/**
 * The siren standing for one device, or null while nobody has called it.
 *
 * Followed by two readers at once: the device named by [phoneId], to know it
 * is being called and to mark when it heard, and the caller's screen, to see
 * whether the device heard or gave it up as switched off.
 */
fun phoneSiren(phoneId: String, connection: GroupConnection): Flow<Siren?> =
    sirensOf(connection).document(phoneId).liveSnapshots().map { snapshot ->
        snapshot.takeIf { it.exists() }?.let(::sirenFrom)
    }.comingBack("siren", forgetting = { null })

/**
 * Calls a siren on one device.
 *
 * Written whole with exactly the two fields the rules allow on a call, rather
 * than merged in: a `set` this narrow also wipes any `heardAt` or `stopped*`
 * left over from a call this device already answered, so the new call starts
 * from nothing.
 */
suspend fun callSiren(phoneId: String, callerId: String, connection: GroupConnection) {
    sirensOf(connection)
        .document(phoneId)
        .set(
            mapOf(
                SirenField.CALLED_BY to callerId,
                SirenField.CALLED_AT to FieldValue.serverTimestamp(),
            ),
        ).await()

    AppLog.i(TAG, "a siren has been called")
}

/** Marks that the device named by [phoneId] heard its own call. Written only by that device. */
suspend fun heardSiren(phoneId: String, connection: GroupConnection) {
    sirensOf(connection)
        .document(phoneId)
        .update(mapOf(SirenField.HEARD_AT to FieldValue.serverTimestamp()))
        .await()

    AppLog.i(TAG, "a siren has been heard")
}

/** Stops a standing call before its device answers or its freshness runs out. */
suspend fun stopSiren(phoneId: String, stopperId: String, connection: GroupConnection) {
    sirensOf(connection)
        .document(phoneId)
        .update(
            mapOf(
                SirenField.STOPPED_BY to stopperId,
                SirenField.STOPPED_AT to FieldValue.serverTimestamp(),
            ),
        ).await()

    AppLog.i(TAG, "a siren has been stopped")
}
