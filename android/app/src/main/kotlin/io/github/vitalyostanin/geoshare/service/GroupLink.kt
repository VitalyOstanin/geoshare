package io.github.vitalyostanin.geoshare.service

import com.google.firebase.auth.FirebaseAuth
import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.FoundBy
import io.github.vitalyostanin.geoshare.collection.watchedUntil
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.stands
import io.github.vitalyostanin.geoshare.project.GroupConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

private const val TAG = "geoshare.link"

/**
 * What the service does for one group: who it writes as there, and what it
 * listens to there.
 *
 * One per group the device is signed in to. The position is worked out once
 * for all of them; each link only carries it into its own group. The account
 * is the group's own — each project signs in on its own (ADR-0002), so the
 * same person has a different id in every group, and so does this device's
 * position.
 */
class GroupLink(val connection: GroupConnection, val memberId: String, device: DeviceIdentity) {
    /** This device's position, siren and search address in this group. */
    val phoneId: String = positionId(memberId, device.id)

    val writer = PositionWriter(memberId, device, connection)
    val status = PhoneStatusWriter(memberId, device, connection)

    /**
     * Whether this link is the one [other] signed in as [uid] needs, so that
     * a list of groups published again with nothing changed in it does not
     * tear down what listens to them.
     */
    fun serves(other: GroupConnection, uid: String): Boolean =
        connection.app === other.app && connection.group == other.group && memberId == uid

    /**
     * The moments this group's watching of the device runs out, and again
     * whenever that changes.
     *
     * Who is looking is the screens' business; the service needs only the
     * moments the claims of those allowed to see this device run out. While
     * the group is looking for this very device every claim counts, whatever
     * the grant says: the rules already answer every viewer, and a device
     * still gating them by who it shares with would be fast for fewer of the
     * group than can actually see it.
     */
    fun watching(): Flow<List<Instant>> {
        val claims = watchers(phoneId, connection).onStart { emit(emptyList()) }
        val permissions: Flow<Map<String, Instant>?> = ownSharedWith(memberId, connection)
        // Said again, as none, at the moment the search runs out: nothing
        // arrives from the database then, and without it every viewer would
        // go on counting until the claims themselves happened to change.
        val search =
            channelFlow {
                ownSearch(phoneId, connection).collectLatest { found ->
                    send(found)

                    if (found != null && found.stands(Instant.now())) {
                        delay(Duration.between(Instant.now(), found.endsAt).toMillis())
                        send(null)
                    }
                }
            }.onStart { emit(null) }

        return combine(
            claims,
            permissions.unknownAfter(GRANT_AWAITED),
            search,
        ) { seen, shared, standing ->
            val sought = standing?.stands(Instant.now()) == true

            watchedUntil(seen, memberId, shared, sought)
        }
    }

    override fun toString(): String = "GroupLink(${connection.projectId}, ${connection.group})"
}

/**
 * Who [connection]'s project says is signed in, now and whenever that
 * changes: the account id, or null for nobody.
 */
fun signedInTo(connection: GroupConnection): Flow<String?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser?.uid) }

    connection.auth.addAuthStateListener(listener)
    awaitClose { connection.auth.removeAuthStateListener(listener) }
}.distinctUntilChanged()

/**
 * The writes of the service, carried into every group it is linked to.
 *
 * A group whose write is refused or does not answer is written down and
 * skipped; the others are not held up by it. Only when no group took the
 * write does it count as failed, which is what the service treats as the
 * position not having been written at all.
 */
class EveryGroup(private val links: () -> List<GroupLink>) {
    suspend fun write(
        fix: Fix,
        mode: CollectionMode,
        batteryPercent: Int,
        charging: Boolean,
        foundBy: FoundBy,
        settling: Boolean = false,
    ) = each("write the position") { link ->
        link.writer.write(fix, mode, batteryPercent, charging, foundBy, settling)
    }

    suspend fun noteLeftOut(count: Int, reason: String, since: Instant) =
        each("say answers are left out") { link -> link.writer.noteLeftOut(count, reason, since) }

    suspend fun tellStatus(mode: CollectionMode, batteryPercent: Int, charging: Boolean) =
        each("say how the device is") { link -> link.status.write(mode, batteryPercent, charging) }

    /** Writes into every linked group and answers the projects that took it. */
    private suspend fun each(what: String, write: suspend (GroupLink) -> Unit): List<String> {
        val linked = links()
        val taken = mutableListOf<String>()
        var last: Exception? = null

        for (link in linked) {
            try {
                write(link)
                taken += link.connection.projectId
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (refusal: Exception) {
                AppLog.w(TAG, "could not $what in ${link.connection.projectId}", refusal)
                last = refusal
            }
        }

        if (taken.isEmpty()) {
            throw last ?: IllegalStateException("no group to $what in")
        }

        return taken
    }
}

/**
 * How long the first decision waits for the grant before counting every
 * claim. Read from the cache it is there in a few hundredths of a second, and
 * deciding before it arrives costs a round of collecting started and dropped
 * again, a status written into every group included, on every start.
 */
private val GRANT_AWAITED: Duration = Duration.ofSeconds(1)

/**
 * This flow, preceded by a null standing for "not known yet" only when it has
 * said nothing within [wait].
 */
internal fun <T> Flow<T?>.unknownAfter(wait: Duration): Flow<T?> = channelFlow {
    val unknown =
        launch {
            delay(wait.toMillis())
            send(null)
        }

    collect { value ->
        unknown.cancel()
        send(value)
    }
}
