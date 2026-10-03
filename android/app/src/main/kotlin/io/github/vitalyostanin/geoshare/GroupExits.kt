package io.github.vitalyostanin.geoshare

import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.membership.Leave
import io.github.vitalyostanin.geoshare.project.Connections
import io.github.vitalyostanin.geoshare.project.GroupConnection
import io.github.vitalyostanin.geoshare.service.forgetPhone
import io.github.vitalyostanin.geoshare.service.leaveGroup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

// Leaving a group, removing a device and signing out, each in every group it
// concerns. Every group is its own project with its own account (ADR-0002),
// so each is written under the id that group knows the member by, one after
// another, and a group that does not answer does not hold up the others.

/** The groups this device is signed in to, each with the account it is signed in as there. */
private fun signedIn(): List<Pair<GroupConnection, String>> =
    Connections.all.value.mapNotNull { connection ->
        connection.auth.currentUser?.uid?.let { connection to it }
    }

/** Whether [write] was answered in [MainActivity.SIGN_OUT_WAIT_MS]; a refusal is logged. */
private suspend fun answered(what: String, write: suspend () -> Unit): Boolean = try {
    withTimeoutOrNull(MainActivity.SIGN_OUT_WAIT_MS) { write() } != null
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (refusal: Exception) {
    AppLog.w(MainActivity.TAG, "could not $what", refusal)
    false
}

/**
 * Takes this device out of [connection]'s group and drops the group from it,
 * the way [how] allows: off the roster with everything the member wrote there,
 * or — for somebody the group would be left to nobody by, or who is no longer
 * on its roster — only this device's own documents, as far as the rules still
 * let them go.
 *
 * False when taking the member off the roster went unanswered or was refused:
 * the group stays, and the screen says so. Only this device's documents not
 * going is no reason to keep it, the same as when signing out without a
 * network.
 *
 * The last group going stops the service: there is nowhere left to write.
 */
internal suspend fun MainActivity.leave(connection: GroupConnection, how: Leave): Boolean {
    val memberId = connection.auth.currentUser?.uid

    if (memberId != null) {
        val done =
            when (how) {
                Leave.ROSTER ->
                    answered("leave the group") { leaveGroup(memberId, device.id, connection) }

                Leave.DEVICE_ONLY, Leave.NOT_ON_ROSTER ->
                    answered("take this device out of the group") {
                        forgetPhone(memberId, device.id, connection)
                    }
            }

        if (!done && how == Leave.ROSTER) {
            return false
        }
    }

    if (Connections.all.value.size == 1) {
        stopSharing()
    }

    connection.auth.signOut()
    Connections.remove(this, connection.projectId)

    return true
}

/**
 * Removes one of the member's devices from every group this device is signed
 * in to, under the account each group knows the member by.
 *
 * True only when every group took it: the dialog then closes, and otherwise
 * stays to say so. Trying again is harmless — a removal already done removes
 * nothing.
 */
internal suspend fun forgetDeviceEverywhere(deviceId: String): Boolean = signedIn()
    .map { (connection, memberId) ->
        answered("remove a device from ${connection.projectId}") {
            forgetPhone(memberId, deviceId, connection)
        }
    }
    .all { it }

/**
 * Takes this device out of every group before signing out of each.
 *
 * A device signed out is usually a device changing hands, and what it wrote
 * would otherwise stay behind for good: a marker greying on everybody's map, a
 * line in the lists naming a device the member no longer has. Only the member
 * may remove them, so it has to happen while they are still signed in. The
 * wait is short, and a device without a network signs out all the same; its
 * line is then removed from another of the member's devices or from the page.
 *
 * The same person signs in to every group, so signing out is from all of
 * them at once.
 */
internal suspend fun MainActivity.signOutEverywhere() {
    stopSharing()

    for ((connection, memberId) in signedIn()) {
        val forgotten =
            answered("take this device out of ${connection.projectId}") {
                forgetPhone(memberId, device.id, connection)
            }

        if (!forgotten) {
            AppLog.w(
                MainActivity.TAG,
                "signing out of ${connection.projectId} with this device listed",
            )
        }

        connection.auth.signOut()
    }
}
