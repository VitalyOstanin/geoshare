package io.github.vitalyostanin.geoshare.service

import android.content.Context
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.PublishedProfile
import io.github.vitalyostanin.geoshare.membership.Presence
import io.github.vitalyostanin.geoshare.membership.presenceBeforeAnswer
import io.github.vitalyostanin.geoshare.project.Connections
import io.github.vitalyostanin.geoshare.project.GroupConnection
import io.github.vitalyostanin.geoshare.project.RemovedHere
import io.github.vitalyostanin.geoshare.project.SeenOnRoster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

private const val TAG = "geoshare.service"

/**
 * The groups the device is signed in to, each with what writes into it and
 * listens to it, whenever that changes.
 *
 * Followed rather than read once: the answer at the moment the service starts
 * is not the answer a second later, a group can be joined or left while it
 * runs, and a service holding up a notification that says it is sharing has
 * to be either sharing or gone.
 */
internal class GroupLinks(
    private val context: Context,
    private val scope: CoroutineScope,
    private val sirens: SirenAnswer,
    private val jumps: JumpFilterSetting,
    /** Told the links before and after every change, to start or stop the collection. */
    private val changed: (before: List<GroupLink>, after: List<GroupLink>) -> Unit,
) {
    private val current = MutableStateFlow<List<GroupLink>>(emptyList())

    /** The groups linked now, and again whenever that changes. */
    val links: StateFlow<List<GroupLink>> = current

    /** What listens to each group for a siren, a search or a reset, by link. */
    private val listening = mutableMapOf<GroupLink, Job>()

    /**
     * Follows the groups and who is signed in to each, and keeps a link to
     * every group with a session: made when a session turns up, taken down
     * when it ends or the group is left.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun follow() {
        Connections.all.flatMapLatest { connections ->
            if (connections.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(connections.map(::signedInHere)) { it.toList() }
            }
        }.collect { signedIn -> relink(signedIn) }
    }

    /** Takes down every link at once, as the service goes. */
    fun unlinkAll() {
        unlink(current.value)
        current.value = emptyList()
    }

    /**
     * Who is signed in to [connection] and still stands in its group, or the
     * group with nobody.
     *
     * A group the device was taken off, or has not got into yet, refuses every
     * write; it is left alone rather than written into and refused once a
     * minute. Founding or joining one republishes the groups, and this is
     * asked again from the start. Until the first answer only a group once
     * stood in is written into; any other is held unlinked rather than left
     * out of the combination, which would hold up every group with it.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun signedInHere(connection: GroupConnection): Flow<Pair<GroupConnection, String?>> =
        signedInTo(connection).flatMapLatest { uid ->
            if (uid == null) {
                flowOf(connection to null)
            } else {
                val wasIn = SeenOnRoster.has(context, connection.projectId, uid)

                presenceIn(context, connection, uid)
                    .map<Presence, Presence?> { it }
                    .onStart { emit(presenceBeforeAnswer(wasIn)) }
                    .map { presence ->
                        connection to
                            uid.takeIf { presence == Presence.IN || presence == Presence.UNKNOWN }
                    }
            }
        }

    private fun relink(signedIn: List<Pair<GroupConnection, String?>>) {
        val device = DeviceIdentity(context)
        val kept = current.value
        val next =
            signedIn.mapNotNull { (connection, uid) ->
                uid?.let {
                    kept.firstOrNull { link -> link.serves(connection, uid) }
                        ?: GroupLink(connection, uid, device)
                }
            }

        unlink(kept.filter { it !in next })
        current.value = next
        next.filter { it !in kept }.forEach(::listenTo)

        changed(kept, next)
    }

    /**
     * What the service does for one group besides writing into it: hears a
     * siren, a search or a request to forget the place there, and takes back
     * what the device's previous id left in it.
     */
    private fun listenTo(link: GroupLink) {
        AppLog.i(TAG, "writing into $link")

        listening[link] =
            scope.launch {
                val device = DeviceIdentity(context)

                LeavingHere.forget(link.connection.projectId)
                launch { signOutWhenRemoved(link, device) }
                // Once, after the move to the system's id: the old position
                // would otherwise stay on the map beside the new one.
                launch { leavePreviousId(link.memberId, device, link.connection) }
                launch { keepSearchNotification(context, link.phoneId, link.connection) }
                launch { sirens.answer(link.memberId, link.phoneId, link.connection) }
                launch {
                    answerResets(context, link.memberId, link.phoneId, jumps, link.connection)
                }
            }
    }

    /**
     * Signs out of [link]'s group once another device of the member has
     * removed this one from "My devices".
     *
     * Writing on would undo the removal: the next status puts the device's
     * line back, and with it the marker on everybody's map. A device removed
     * that way has usually changed hands, so it stops there rather than asks.
     */
    private suspend fun signOutWhenRemoved(link: GroupLink, device: DeviceIdentity) {
        val projectId = link.connection.projectId

        ownLine(link.connection, link.memberId, device.id).goneAfterListed().firstOrNull() ?: return

        if (LeavingHere.has(projectId)) {
            return
        }

        AppLog.i(TAG, "removed from $link by another device, signing out of it")
        RemovedHere.note(context, projectId)
        PublishedProfile(context).forget()
        link.connection.auth.signOut()
    }

    private fun unlink(gone: List<GroupLink>) {
        for (link in gone) {
            AppLog.i(TAG, "no longer writing into $link")
            listening.remove(link)?.cancel()
        }
    }
}
