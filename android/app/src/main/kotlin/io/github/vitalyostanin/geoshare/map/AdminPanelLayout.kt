package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.vitalyostanin.geoshare.membership.runsTheGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The panel's page: the title, a row per member, the searches standing and the
 * invitation, scrolled and inset the way the settings screen is, and for the
 * same reasons: one row per member runs off a device, and the title has to
 * clear the clock.
 */
@Composable
internal fun AdminPanelBody(
    shown: AdminShown,
    state: AdminPanelState,
    scope: CoroutineScope,
    onSetRunning: (String, Boolean) -> Unit,
    onEndSearch: suspend (phoneId: String) -> Unit,
    onStopSiren: suspend (phoneId: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState()),
        ) {
            AdminHeader(
                caption = shown.caption,
                onRename =
                {
                    state.openRename(shown.roster.name.orEmpty())
                }.takeIf { runsTheGroup(shown.roster, shown.ownMemberId) },
                onHelp = { state.helpOpen = true },
                onClose = onClose,
            )

            HorizontalDivider()

            AdminMembers(shown, state, scope, onSetRunning, onEndSearch, onStopSiren)

            AdminSearchSummaries(
                liveSearches = shown.liveSearches,
                lines = shown.lines,
                profiles = shown.profiles,
                ownMemberId = shown.ownMemberId,
                onRenew = { phone, name -> state.askSearch(phone, name, renew = true) },
                onEndSearch = { phoneId -> scope.launch { onEndSearch(phoneId) } },
            )

            HorizontalDivider()

            val ownName = shown.profiles[shown.ownMemberId]?.displayName

            AdminInvitationSection(
                invitation = shown.invitationSection.invitation,
                invitationLive = shown.invitationSection.stillValid,
                link = InvitationLink(shown.invitationSection.site, ownName, shown.roster.name),
                now = shown.now,
                trouble = shown.invitationSection.trouble,
                onOpenInvitation = state::openInvitation,
            )
        }
    }
}

/** A row per member, each with the questions its buttons put to [state]. */
@Composable
private fun AdminMembers(
    shown: AdminShown,
    state: AdminPanelState,
    scope: CoroutineScope,
    onSetRunning: (String, Boolean) -> Unit,
    onEndSearch: suspend (phoneId: String) -> Unit,
    onStopSiren: suspend (phoneId: String) -> Unit,
) {
    for (line in shown.lines) {
        val lineName = line.displayName ?: unnamedMember(line.memberId)

        AdminMemberRow(
            line = line,
            now = shown.now,
            searches = shown.liveSearches,
            ownPhoneId = shown.ownPhoneId,
            onMakeAdmin = { onSetRunning(line.memberId, true) },
            onStopRunning = {
                if (line.isYou) state.givingUp = true else onSetRunning(line.memberId, false)
            },
            onRemove = { state.removing = line.memberId to lineName },
            onSearch = { phone -> state.askSearch(phone, lineName, renew = false) },
            onEndSearch = { phoneId -> scope.launch { onEndSearch(phoneId) } },
            sirens = shown.live.sirens,
            sirenNow = shown.live.sirenNow,
            onSiren = { phone ->
                state.ringing.ask(SirenAsk(lineName, listOf(phone), phone.phoneId))
            },
            onStopSiren = { phoneId -> scope.launch { onStopSiren(phoneId) } },
            resets = shown.live.resets,
            onReset = { phone -> state.resetting.ask(phone) },
        )
    }
}
