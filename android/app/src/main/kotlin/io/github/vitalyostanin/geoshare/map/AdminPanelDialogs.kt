package io.github.vitalyostanin.geoshare.map

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import java.time.Instant

/**
 * Every dialog the panel can raise, drawn over it: the ones that are about the
 * group, those that are about a member, and those that reach into a device.
 */
@Composable
internal fun AdminPanelDialogs(
    state: AdminPanelState,
    shown: AdminShown,
    scope: CoroutineScope,
    writes: AdminWrites,
    onMakeInvitation: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onSetRunning: (String, Boolean) -> Unit,
) {
    AdminGroupDialogs(state, shown.caption, scope, writes.onRenameGroup, onMakeInvitation)
    AdminMemberDialogs(state, shown.ownMemberId, onSetRunning, onRemoveMember)
    AdminDeviceDialogs(state, shown.now, writes)
}

/** Help, the group's own name, and the address to invite. */
@Composable
private fun AdminGroupDialogs(
    state: AdminPanelState,
    caption: String,
    scope: CoroutineScope,
    onRenameGroup: suspend (String) -> WriteOutcome,
    onMakeInvitation: (String) -> Unit,
) {
    if (state.helpOpen) {
        AdminHelpSheet(onClose = { state.helpOpen = false })
    }

    if (state.renaming) {
        RenameGroupDialog(
            text = state.renameText,
            onTextChange = { state.renameText = it },
            trouble = state.renameTrouble,
            busy = state.renameBusy,
            onSave = { state.writeRename(scope, onRenameGroup) },
            onDismiss = { state.renaming = false },
        )
    }

    if (state.inviting) {
        InvitationDialog(
            caption = caption,
            email = state.invitationEmail,
            onEmailChange = { state.invitationEmail = it },
            onCreate = { address ->
                state.inviting = false
                onMakeInvitation(address)
            },
            onDismiss = { state.inviting = false },
        )
    }
}

/** Giving up running the group, and removing a member. */
@Composable
private fun AdminMemberDialogs(
    state: AdminPanelState,
    ownMemberId: String,
    onSetRunning: (String, Boolean) -> Unit,
    onRemoveMember: (String) -> Unit,
) {
    if (state.givingUp) {
        GiveUpDialog(
            onGiveUp = {
                state.givingUp = false
                onSetRunning(ownMemberId, false)
            },
            onDismiss = { state.givingUp = false },
        )
    }

    state.removing?.let { (memberId, name) ->
        RemoveMemberDialog(
            name = name,
            onRemove = {
                state.removing = null
                onRemoveMember(memberId)
            },
            onDismiss = { state.removing = null },
        )
    }
}

/** Ringing a device, asking it to forget its place, and searching for it. */
@Composable
private fun AdminDeviceDialogs(state: AdminPanelState, now: Instant, writes: AdminWrites) {
    val ringing = state.ringing
    val resetting = state.resetting
    val searching = state.searching

    ringing.asked?.let { ask ->
        SirenDialog(
            memberName = ask.name,
            phones = ask.phones,
            selected = ask.selected,
            onSelect = { ringing.change(ask.copy(selected = it)) },
            now = now,
            trouble = ringing.trouble,
            busy = ringing.busy,
            onGo = { ringing.write { writes.onCallSiren(it.selected) } },
            onCancel = ringing::cancel,
        )
    }

    resetting.asked?.let { phone ->
        ResetDialog(
            phoneName = phone.name,
            trouble = resetting.trouble,
            busy = resetting.busy,
            onGo = { resetting.write { writes.onAskReset(it.phoneId) } },
            onCancel = resetting::cancel,
        )
    }

    searching.asked?.let { ask ->
        SearchDialog(
            memberName = ask.memberName,
            phoneName = ask.phone.name,
            renew = ask.renew,
            hours = state.searchHours,
            onHoursChange = { state.searchHours = it },
            now = now,
            trouble = searching.trouble,
            busy = searching.busy,
            onGo = {
                searching.write {
                    writes.onSearch(it.phone.phoneId, it.phone.name, it.renew, state.searchHours)
                }
            },
            onCancel = searching::cancel,
        )
    }
}
