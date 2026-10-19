package io.github.vitalyostanin.geoshare.service

import android.content.Context
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.cutToLimit
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.Search
import io.github.vitalyostanin.geoshare.map.idTail
import io.github.vitalyostanin.geoshare.map.logFailure
import io.github.vitalyostanin.geoshare.map.runCatchingFailure
import io.github.vitalyostanin.geoshare.map.searchOf
import io.github.vitalyostanin.geoshare.project.GroupConnection
import io.github.vitalyostanin.geoshare.trimText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val TAG = "geoshare.searches"

/** How much of a device's name is kept in a search document. The rules say the same. */
private const val PHONE_NAME_LIMIT = 100

/**
 * The group's searches, and again whenever an administrator opens, renews or
 * ends one, or the device looked for says it was found.
 *
 * Everybody in the group reads the collection, the same as the roster and the
 * profiles: every map marks the device being looked for, and a search names
 * nobody's place. A refusal is read as no searches, which is what the device
 * showed before any of this existed.
 */
fun groupSearches(connection: GroupConnection): Flow<Map<String, Search>> =
    searchesOf(connection).liveSnapshots().map { snapshot ->
        snapshot.documents.mapNotNull(::searchFrom).associateBy { it.phoneId }
    }.comingBack("searches", forgetting = { emptyMap() })

/**
 * A document turns into a search, or into nothing.
 *
 * Read with the server's estimate rather than its confirmed answer: an
 * administrator who has just opened or renewed a search sees its own write at
 * once, with `renewedAt` guessed at this device's clock until the server
 * answers, rather than with no search at all for the second or so that takes
 * — [searchOf] drops a document with no `renewedAt`, and a write still on its
 * way otherwise reads as one.
 */
private fun searchFrom(document: DocumentSnapshot): Search? = searchOf(
    phoneId = document.id,
    phoneName = document.getString(SearchField.PHONE_NAME),
    openedBy = document.getString(SearchField.OPENED_BY),
    renewedBy = document.getString(SearchField.RENEWED_BY),
    startedAt = document.getTimestamp(SearchField.STARTED_AT, ESTIMATE)?.toInstant(),
    renewedAt = document.getTimestamp(SearchField.RENEWED_AT, ESTIMATE)?.toInstant(),
    hours = document.getLong(SearchField.HOURS)?.toInt(),
)

private val ESTIMATE = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE

/**
 * The search standing for one device, or null while nobody is looking for it:
 * the device's own copy of what [groupSearches] holds, read from one document
 * rather than the whole collection.
 *
 * This is what the service reads: it cares whether it itself is being looked
 * for, not who else in the group is, and one document costs it far less than
 * the collection every screen already follows.
 */
fun ownSearch(phoneId: String, connection: GroupConnection): Flow<Search?> =
    searchesOf(connection).document(phoneId).liveSnapshots().map { snapshot ->
        snapshot.takeIf { it.exists() }?.let(::searchFrom)
    }.comingBack("own search", forgetting = { null })

/** What is kept of a device's name at the moment a search is opened or renewed: nothing for none. */
internal fun phoneNameField(phoneName: String?): Map<String, Any> = phoneName
    ?.let { trimText(it) }
    ?.let { cutToLimit(it, PHONE_NAME_LIMIT) }
    ?.ifEmpty { null }
    ?.let { mapOf(SearchField.PHONE_NAME to it) }
    .orEmpty()

/**
 * Opens a search for a device: its place is open to the whole group for the
 * hours chosen. Written whole, so that one opened again after an earlier one
 * ran out starts from now and names whoever opened it this time.
 */
suspend fun openSearch(
    phoneId: String,
    /** The device's name as this administrator sees it, carried along for whoever has no other. */
    phoneName: String?,
    adminId: String,
    hours: Int,
    connection: GroupConnection,
) {
    searchesOf(connection)
        .document(phoneId)
        .set(
            mapOf(
                SearchField.OPENED_BY to adminId,
                SearchField.STARTED_AT to FieldValue.serverTimestamp(),
                SearchField.RENEWED_BY to adminId,
                SearchField.RENEWED_AT to FieldValue.serverTimestamp(),
                SearchField.HOURS to hours,
            ) + phoneNameField(phoneName),
        ).await()

    AppLog.i(TAG, "a search has been opened")
}

/** Moves the end of a standing search on to the hours chosen from now, keeping who opened it. */
suspend fun renewSearch(
    phoneId: String,
    /** The device's name as this administrator sees it now, which may have changed since. */
    phoneName: String?,
    adminId: String,
    hours: Int,
    connection: GroupConnection,
) {
    searchesOf(connection)
        .document(phoneId)
        .update(
            mapOf(
                SearchField.RENEWED_BY to adminId,
                SearchField.RENEWED_AT to FieldValue.serverTimestamp(),
                SearchField.HOURS to hours,
            ) + phoneNameField(phoneName),
        ).await()

    AppLog.i(TAG, "a search has been renewed")
}

/** Ends a search: the device was found, and its place closes to all but whom it shares with. */
suspend fun endSearch(phoneId: String, connection: GroupConnection) {
    searchesOf(connection).document(phoneId).delete().await()

    AppLog.i(TAG, "a search has been ended")
}

/**
 * What a member is called, read once rather than followed: the notification
 * that says who opened a search is shown for as long as the search stands,
 * and a name that changed in the meantime is not worth a subscription of its
 * own for.
 *
 * Null where the member has published no name, or it could not be read:
 * the caller says so in its own words, which this file cannot read.
 */
suspend fun memberDisplayName(memberId: String, connection: GroupConnection): String? =
    runCatchingFailure {
        membersOf(connection)
            .document(memberId)
            .get()
            .await()
            .getString(ProfileField.DISPLAY_NAME)
            ?.takeIf { it.isNotBlank() }
    }.getOrElse { refusal ->
        logFailure(TAG, "could not read the name of $memberId", refusal)
        null
    }

/** What a member without a published name is called in a notification. */
fun unnamedMember(context: Context, memberId: String): String =
    context.getString(R.string.member_unnamed, idTail(memberId))
