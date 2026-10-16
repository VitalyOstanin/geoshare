package io.github.vitalyostanin.geoshare.map

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.project.GroupConnection
import io.github.vitalyostanin.geoshare.service.LabelField
import io.github.vitalyostanin.geoshare.service.PhoneField
import io.github.vitalyostanin.geoshare.service.comingBack
import io.github.vitalyostanin.geoshare.service.labelsOf
import io.github.vitalyostanin.geoshare.service.liveSnapshots
import io.github.vitalyostanin.geoshare.service.phonesOf
import io.github.vitalyostanin.geoshare.service.readString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

private const val TAG = "geoshare.labels"

/**
 * The copy of this member's names kept on the device.
 *
 * The names are in the database, and the database answers a second or a
 * minute after the screen is up, or not at all without a network: without the
 * copy the markers would be called by their models until it did. The same
 * reason the false places are kept on the device, and the same terms: a failure
 * is logged and costs one subscription's worth of waiting.
 */
class LabelCache(context: Context) {
    private val store = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun read(memberId: String): Labels = try {
        store.readString(memberId, null)?.let { kept ->
            val json = JSONObject(kept)

            labelsFrom(
                entriesOf(json.optJSONObject(LabelField.PHONES)),
                entriesOf(json.optJSONObject(LabelField.MEMBERS)),
            )
        } ?: Labels()
    } catch (error: Exception) {
        AppLog.w(TAG, "the names kept on the device could not be read", error)

        Labels()
    }

    fun write(memberId: String, labels: Labels) {
        val json =
            JSONObject()
                .put(LabelField.PHONES, JSONObject(labels.phones))
                .put(LabelField.MEMBERS, JSONObject(labels.members))

        store.edit().putString(memberId, json.toString()).apply()
    }

    private fun entriesOf(json: JSONObject?): Map<String, Any?> {
        json ?: return emptyMap()

        return json.keys().asSequence().associateWith { json.opt(it) }
    }

    private companion object {
        const val FILE = "labels"
    }
}

/**
 * This member's names for the others: first the copy on the device, then the
 * document, and the document again whenever it changes — on this device or on
 * the member's other one, or on the page.
 */
fun ownLabels(memberId: String, cache: LabelCache, connection: GroupConnection): Flow<Labels> =
    labelsOf(connection)
        .document(memberId)
        .liveSnapshots()
        .map { snapshot ->
            labelsFrom(snapshot.get(LabelField.PHONES), snapshot.get(LabelField.MEMBERS))
        }
        .onEach { cache.write(memberId, it) }
        // Refused outright, the device is out of the group, and the names it gave
        // people there have no one left to name.
        .comingBack("labels", forgetting = { Labels() })
        .onStart { emit(cache.read(memberId)) }

/** Which of the two maps a name goes into. */
enum class LabelKind(val field: String) {
    PHONE(LabelField.PHONES),
    MEMBER(LabelField.MEMBERS),
}

/**
 * Gives a device or a person a name for this member alone, or takes it away
 * when [name] is null.
 *
 * Merged into the one map, so that the other names stay as they were. The
 * address of a device has a dot in it, which a merge takes as it is: only a
 * field path written as a string splits on dots.
 */
suspend fun writeLabel(
    memberId: String,
    kind: LabelKind,
    key: String,
    name: String?,
    connection: GroupConnection,
): Boolean = trying("could not keep the name") {
    labelsOf(connection)
        .document(memberId)
        .set(mapOf(kind.field to mapOf(key to (name ?: FieldValue.delete()))), SetOptions.merge())
        .await()
}

/**
 * Gives this device a name everybody who sees it sees, or takes it away when
 * [name] is null.
 *
 * Into the device's own entry of its member's document, beside what it says
 * about itself: that writer merges its fields without this one, so neither
 * undoes the other.
 */
suspend fun writePhoneName(
    memberId: String,
    deviceId: String,
    name: String?,
    connection: GroupConnection,
): Boolean = trying("could not name the device") {
    phonesOf(connection)
        .document(memberId)
        .set(
            mapOf(deviceId to mapOf(PhoneField.NAME to (name ?: FieldValue.delete()))),
            SetOptions.merge(),
        )
        .await()
}

private suspend fun trying(failure: String, write: suspend () -> Unit): Boolean = try {
    write()

    true
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (error: Exception) {
    logFailure(TAG, failure, error)

    false
}
