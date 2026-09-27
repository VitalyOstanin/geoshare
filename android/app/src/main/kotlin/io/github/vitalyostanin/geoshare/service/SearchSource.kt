package io.github.vitalyostanin.geoshare.service

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.Search
import io.github.vitalyostanin.geoshare.map.searchOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant

private const val TAG = "geoshare.searches"

/**
 * The group's searches, and again whenever an administrator opens, renews or
 * ends one, or the member looked for says they were found.
 *
 * Everybody in the group reads the collection, the same as the roster and the
 * profiles: every map marks the member being looked for, and a search names
 * nobody's place. A refusal is read as no searches, which is what the phone
 * showed before any of this existed.
 */
fun groupSearches(
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
): Flow<Map<String, Search>> = callbackFlow {
    val subscription =
        searchesOf(store).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)

                return@addSnapshotListener
            }

            trySend(
                snapshot
                    ?.documents
                    .orEmpty()
                    .mapNotNull(::searchFrom)
                    .associateBy { it.memberId },
            )
        }

    awaitClose { subscription.remove() }
}.comingBack("searches", forgetting = { emptyMap() })

/**
 * A document turns into a search, or into nothing.
 *
 * Read with the server's estimate rather than its confirmed answer: an
 * administrator who has just opened or renewed a search sees its own write at
 * once, with `renewedAt` guessed at this phone's clock until the server
 * answers, rather than with no search at all for the second or so that takes
 * — [searchOf] drops a document with no `renewedAt`, and a write still on its
 * way otherwise reads as one.
 */
private fun searchFrom(document: DocumentSnapshot): Search? = searchOf(
    memberId = document.id,
    openedBy = document.getString(SearchField.OPENED_BY),
    renewedBy = document.getString(SearchField.RENEWED_BY),
    startedAt = document.getTimestamp(SearchField.STARTED_AT, ESTIMATE)?.toInstant(),
    renewedAt = document.getTimestamp(SearchField.RENEWED_AT, ESTIMATE)?.toInstant(),
    hours = document.getLong(SearchField.HOURS)?.toInt(),
)

private val ESTIMATE = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE

private fun Timestamp.toInstant(): Instant = Instant.ofEpochSecond(seconds, nanoseconds.toLong())

/**
 * The search standing for one member, or null while nobody is looking for
 * them: the phone's own copy of what [groupSearches] holds, read from one
 * document rather than the whole collection.
 *
 * This is what the service reads: it cares whether it itself is being looked
 * for, not who else in the group is, and one document costs it far less than
 * the collection every screen already follows.
 */
fun ownSearch(
    memberId: String,
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
): Flow<Search?> = callbackFlow {
    val subscription =
        searchesOf(store).document(memberId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)

                return@addSnapshotListener
            }

            trySend(snapshot?.takeIf { it.exists() }?.let(::searchFrom))
        }

    awaitClose { subscription.remove() }
}.comingBack("own search", forgetting = { null })

/**
 * Opens a search for a member: their place is open to the whole group for the
 * hours chosen. Written whole, so that one opened again after an earlier one
 * ran out starts from now and names whoever opened it this time.
 */
suspend fun openSearch(
    memberId: String,
    adminId: String,
    hours: Int,
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    searchesOf(store)
        .document(memberId)
        .set(
            mapOf(
                SearchField.OPENED_BY to adminId,
                SearchField.STARTED_AT to FieldValue.serverTimestamp(),
                SearchField.RENEWED_BY to adminId,
                SearchField.RENEWED_AT to FieldValue.serverTimestamp(),
                SearchField.HOURS to hours,
            ),
        ).await()

    AppLog.i(TAG, "a search has been opened")
}

/** Moves the end of a standing search on to the hours chosen from now, keeping who opened it. */
suspend fun renewSearch(
    memberId: String,
    adminId: String,
    hours: Int,
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    searchesOf(store)
        .document(memberId)
        .update(
            mapOf(
                SearchField.RENEWED_BY to adminId,
                SearchField.RENEWED_AT to FieldValue.serverTimestamp(),
                SearchField.HOURS to hours,
            ),
        ).await()

    AppLog.i(TAG, "a search has been renewed")
}

/** Ends a search: the member was found, and their place closes to all but whom they share with. */
suspend fun endSearch(
    memberId: String,
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    searchesOf(store).document(memberId).delete().await()

    AppLog.i(TAG, "a search has been ended")
}

/**
 * What a member is called, read once rather than followed: the notification
 * that says who opened a search is shown for as long as the search stands,
 * and a name that changed in the meantime is not worth a subscription of its
 * own for.
 *
 * The account id, shortened, where the member has published no name — the
 * same fallback every screen falls back to.
 */
suspend fun memberDisplayName(
    memberId: String,
    store: FirebaseFirestore = FirebaseFirestore.getInstance(),
): String = try {
    membersOf(store)
        .document(memberId)
        .get()
        .await()
        .getString(ProfileField.DISPLAY_NAME)
        ?.takeIf { it.isNotBlank() }
        ?: memberId.take(SHORTENED_ID_LENGTH)
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (refusal: Exception) {
    AppLog.w(TAG, "could not read the name of $memberId", refusal)
    memberId.take(SHORTENED_ID_LENGTH)
}

/** How much of an account id stands in for a name nobody has published. */
private const val SHORTENED_ID_LENGTH = 6
