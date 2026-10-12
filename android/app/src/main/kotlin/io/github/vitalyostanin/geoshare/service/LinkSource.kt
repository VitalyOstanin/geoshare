package io.github.vitalyostanin.geoshare.service

import com.google.firebase.firestore.MetadataChanges
import io.github.vitalyostanin.geoshare.map.DatabaseLink
import io.github.vitalyostanin.geoshare.map.LinkState
import io.github.vitalyostanin.geoshare.project.GroupConnection
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

/**
 * Whether the database is answering, judged by the snapshots of the group
 * document (see [DatabaseLink]) and told again whenever that changes.
 *
 * The group document is the one every signed-in device is on. A refusal is
 * the server answering, so it is read as a confirmation; the listener is
 * dropped by the client after one, and a link judged answering stays so.
 */
fun databaseLink(connection: GroupConnection): Flow<LinkState> = callbackFlow {
    var link = DatabaseLink.from(Instant.now())
    var judging: Job? = null

    fun judge() {
        val now = Instant.now()

        val silent = LinkState(since = link.cachedSince, knownAt = link.confirmedAt)

        trySend(if (link.lost(now)) silent else LinkState())

        judging?.cancel()
        judging =
            link.lostAt()?.takeIf { it.isAfter(now) }?.let { at ->
                launch {
                    delay(Duration.between(now, at).toMillis())
                    judge()
                }
            }
    }

    val subscription =
        connection.groupDocument.addSnapshotListener(MetadataChanges.INCLUDE) {
                snapshot,
                error,
            ->
            link =
                link.next(
                    fromCache = error == null && snapshot?.metadata?.isFromCache == true,
                    now = Instant.now(),
                )
            judge()
        }

    judge()

    awaitClose { subscription.remove() }
}.distinctUntilChanged()
