package io.github.vitalyostanin.geoshare.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitalyostanin.geoshare.membership.Roster
import io.github.vitalyostanin.geoshare.service.MadeInvitation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * What the admin panel holds between its redraws: which dialog is open and what
 * has been typed into it, and the questions put about one device.
 *
 * Kept apart from the panel so that the panel itself only lays out, and so that
 * the sections and the dialogs, which are drawn from different places, read one
 * and the same state.
 */
@Stable
internal class AdminPanelState(
    /** Which device is being asked about, in which of the two ways. */
    val searching: AskedWrite<SearchAsk>,
    val ringing: AskedWrite<SirenAsk>,
    val resetting: AskedWrite<PhoneLine>,
) {
    var givingUp by mutableStateOf(false)
    var helpOpen by mutableStateOf(false)

    // The dialog for the group's own name: open or not, what is typed in it
    // so far, and how the last write to it came out.
    var renaming by mutableStateOf(false)
    var renameText by mutableStateOf("")
    var renameTrouble by mutableStateOf<WriteOutcome?>(null)
    var renameBusy by mutableStateOf(false)

    // The dialog that asks for the address to invite: open or not, and what
    // has been typed into it so far. Reopened by "make another invitation" as
    // well as by the button that offers the first one, so it is held apart
    // from the invitation itself rather than inferred from it.
    var inviting by mutableStateOf(false)
    var invitationEmail by mutableStateOf("")

    /** The member about to be removed and the name the dialog calls them by. */
    var removing by mutableStateOf<Pair<String, String>?>(null)

    /**
     * The length picked so far for the search being asked about -- held apart
     * from whether the write is refused, so a refusal on one device's search
     * does not linger once a different one is opened.
     */
    var searchHours by mutableStateOf(DEFAULT_SEARCH_HOURS)

    fun askSearch(phone: PhoneLine, memberName: String, renew: Boolean) {
        searching.ask(SearchAsk(phone, memberName, renew))
        searchHours = DEFAULT_SEARCH_HOURS
    }

    fun openRename(current: String) {
        renameText = current
        renameTrouble = null
        renaming = true
    }

    fun openInvitation() {
        invitationEmail = ""
        inviting = true
    }

    /** Writes the typed name; the dialog closes only when the write is done. */
    fun writeRename(scope: CoroutineScope, rename: suspend (String) -> WriteOutcome) {
        val name = groupNameText(renameText) ?: return

        renameBusy = true
        scope.launch {
            val outcome = rename(name)

            renameBusy = false

            if (outcome == WriteOutcome.DONE) {
                renaming = false
                renameTrouble = null
            } else {
                renameTrouble = outcome
            }
        }
    }
}

/** The panel's [AdminPanelState]: nothing in it depends on which devices are shown. */
@Composable
internal fun rememberAdminPanelState(): AdminPanelState {
    val searching = rememberAskedWrite<SearchAsk>()
    val ringing = rememberAskedWrite<SirenAsk>()
    val resetting = rememberAskedWrite<PhoneLine>()

    return remember { AdminPanelState(searching, ringing, resetting) }
}

/** The calls and the requests to forget a place standing on the group's devices, by device. */
internal class AdminLive(
    val sirens: Map<String, Siren>,
    val sirenNow: Instant,
    val resets: Map<String, Reset>,
)

/**
 * The sirens and the reset requests of every device the panel offers to ring,
 * followed while it is open: a call rung from anywhere else shows on its row as
 * well. Rebuilt with the devices, which is why it is not part of the state that
 * outlives them.
 */
@Composable
internal fun rememberAdminLive(
    lines: List<GroupLine>,
    ownPhoneId: String?,
    sirenOf: (phoneId: String) -> Flow<Siren?>,
    resetOf: (phoneId: String) -> Flow<Reset?>,
): AdminLive {
    val ringable =
        remember(lines, ownPhoneId) {
            lines.flatMap { it.phones }.map { it.phoneId }.filter { it != ownPhoneId }.toSet()
        }
    val sirens by remember(ringable) { sirensOf(ringable, sirenOf) }
        .collectAsStateWithLifecycle(initialValue = emptyMap())
    val resets by remember(ringable) { resetsOf(ringable, resetOf) }
        .collectAsStateWithLifecycle(initialValue = emptyMap())
    val sirenNow by sirenClock(sirens)

    return AdminLive(sirens, sirenNow, resets)
}

/** Which device is being asked about, whose member it is, and in which of the two ways. */
internal data class SearchAsk(val phone: PhoneLine, val memberName: String, val renew: Boolean)

/** Whose device is about to be rung, and which of their devices is picked. */
internal data class SirenAsk(val name: String, val phones: List<PhoneLine>, val selected: String)

/** What the panel draws: the group as read from here, at the moment [now]. */
internal class AdminShown(
    val roster: Roster,
    val profiles: Map<String, MemberProfile>,
    val lines: List<GroupLine>,
    /** The searches standing at [now], by the device each is looking for. */
    val standing: Map<String, Search>,
    val caption: String,
    val ownMemberId: String,
    /** This device's own `{uid}.{deviceId}`: the one device nobody here rings. */
    val ownPhoneId: String?,
    val now: Instant,
    val invite: AdminInvite,
    val live: AdminLive,
)

/** The writes that wait on an answer, each answering how the write came out. */
internal class AdminWrites(
    val onRenameGroup: suspend (String) -> WriteOutcome,
    val onSearch: suspend (
        phoneId: String,
        phoneName: String?,
        renew: Boolean,
        hours: Int,
    ) -> WriteOutcome,
    val onCallSiren: suspend (phoneId: String) -> WriteOutcome,
    val onAskReset: suspend (phoneId: String) -> WriteOutcome,
)

/** What the invitation section needs: the invitation just made and the link it is built against. */
internal class AdminInvite(
    /** The invitation just made, held until the screen is left. */
    val invitation: MadeInvitation?,
    /** Whether an invitation made here still stands, followed for as long as asked. */
    val standing: (code: String) -> Flow<Boolean?>,
    /** This device's Hosting site, which the invitation link is built against. */
    val site: String?,
    /** Whether the last write to the group was refused. */
    val trouble: Boolean,
)
