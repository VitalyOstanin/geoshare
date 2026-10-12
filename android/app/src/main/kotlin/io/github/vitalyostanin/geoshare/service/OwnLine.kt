package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.project.GroupConnection
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap

/** What the member's list of devices says about this one. */
internal enum class OwnLine {
    /** The device has its line, as far as the cache or the server knows. */
    Listed,

    /** The server, with nothing of this device's still on its way, has the list without it. */
    Gone,

    /** Nothing to go by: no list at all, or only the cache says it lacks the device. */
    Unknown,
}

/**
 * This device's line in one answer of the list.
 *
 * A list missing altogether is not the device being removed: the member left
 * the group, or was taken off it, and that has its own way of being said.
 * A line missing from the cache, or while a write of this device is still on
 * its way, may only be the device not having written it yet.
 */
internal fun ownLineOf(
    exists: Boolean,
    listed: Set<String>,
    deviceId: String,
    fromServer: Boolean,
): OwnLine = when {
    !exists -> OwnLine.Unknown
    deviceId in listed -> OwnLine.Listed
    fromServer -> OwnLine.Gone
    else -> OwnLine.Unknown
}

/**
 * Says once that the line was there and then went.
 *
 * A device that has never written its line is not one taken out: its first
 * status is still to come.
 */
internal fun Flow<OwnLine>.goneAfterListed(): Flow<Unit> = flow {
    var listed = false

    collect { line ->
        when (line) {
            OwnLine.Listed -> listed = true

            OwnLine.Gone ->
                if (listed) {
                    emit(Unit)

                    listed = false
                }

            OwnLine.Unknown -> Unit
        }
    }
}

/**
 * This device's line in [memberId]'s list of devices in [connection]'s group,
 * whenever the list changes.
 *
 * The list is the devices the member gives out, in the member's grant, and
 * the one way a device goes from it is another device of the member removing
 * it from "My devices". The member's `devices` document would also say so, but
 * every device of the member rewrites its own status line there, and each such
 * write would be delivered to all of them: reads for nothing. The grant
 * changes only when the list or the permissions do, and the service listens
 * to it anyway.
 */
internal fun ownLine(
    connection: GroupConnection,
    memberId: String,
    deviceId: String,
): Flow<OwnLine> = callbackFlow {
    val subscription =
        grantsOf(connection)
            .document(memberId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close()

                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val fromServer =
                        !snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites()

                    trySend(
                        ownLineOf(
                            exists = snapshot.exists(),
                            listed =
                            (snapshot.get(GrantField.DEVICES) as? List<*>)
                                .orEmpty()
                                .filterIsInstance<String>()
                                .toSet(),
                            deviceId = deviceId,
                            fromServer = fromServer,
                        ),
                    )
                }
            }

    awaitClose { subscription.remove() }
}

/**
 * The groups this device is taking itself out of.
 *
 * Signing out or leaving removes the device's line first, and the service
 * hears that the same way as a removal from another device. Noted here before
 * the line goes, and forgotten when the device next links to the group.
 */
internal object LeavingHere {
    private val projects = ConcurrentHashMap.newKeySet<String>()

    fun note(projectId: String) {
        projects.add(projectId)
    }

    fun has(projectId: String): Boolean = projectId in projects

    fun forget(projectId: String) {
        projects.remove(projectId)
    }
}
