package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.service.MadeInvitation
import io.github.vitalyostanin.geoshare.service.deviceIdOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant

/** The group's name at the top of the panel; [onRename] is null for a member who does not run it. */
@Composable
internal fun AdminHeader(
    caption: String,
    onRename: (() -> Unit)?,
    onHelp: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = caption,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )

        onRename?.let {
            TextButton(onClick = it) {
                Text(text = stringResource(R.string.group_name_rename))
            }
        }

        HelpButton(onClick = onHelp)

        TextButton(onClick = onClose) {
            Text(text = stringResource(R.string.settings_close))
        }
    }
}

/**
 * Every search standing now, under the rows rather than in them, with the one
 * thing the device's own row does not offer: renewing it without ending it
 * first.
 */
@Composable
internal fun AdminSearchSummaries(
    standing: Map<String, Search>,
    lines: List<GroupLine>,
    profiles: Map<String, MemberProfile>,
    ownMemberId: String,
    onRenew: (PhoneLine, String) -> Unit,
    onEndSearch: (phoneId: String) -> Unit,
) {
    for (search in standing.values) {
        val phone = phoneLine(search, lines)
        val soughtName =
            lines.firstOrNull { it.memberId == search.memberId }?.displayName
                ?: unnamedMember(search.memberId)

        AdminSearchSummary(
            search = search,
            phoneName = phone.name,
            memberName = soughtName,
            openedByYou = search.openedBy == ownMemberId,
            openedByName = profiles[search.openedBy]?.displayName ?: unnamedMember(search.openedBy),
            onRenew = { onRenew(phone, soughtName) },
            onEndSearch = { onEndSearch(search.phoneId) },
        )
    }
}

/**
 * The device a standing search is about, out of what this screen already knows
 * of the member's devices — or, failing that, out of the search document
 * itself, for a device whose own status has not reached this screen.
 */
private fun phoneLine(search: Search, lines: List<GroupLine>): PhoneLine = lines
    .firstOrNull { it.memberId == search.memberId }
    ?.phones
    ?.firstOrNull { it.phoneId == search.phoneId }
    ?: PhoneLine(
        phoneId = search.phoneId,
        name = search.phoneName ?: shortName(deviceIdOf(search.phoneId)),
        onlineAt = null,
        batteryPercent = null,
        charging = false,
    )

/** What the invitation link is made of besides the invitation itself. */
internal class InvitationLink(val site: String?, val inviter: String?, val groupName: String?)

/**
 * The invitation just made, whether it still stands, and the line saying the
 * last write to the group was refused.
 */
@Composable
internal fun AdminInvitationSection(
    invitation: MadeInvitation?,
    invitationStanding: (code: String) -> Flow<Boolean?>,
    link: InvitationLink,
    now: Instant,
    trouble: Boolean,
    onOpenInvite: () -> Unit,
) {
    // Keyed as a whole: collectAsStateWithLifecycle keeps the last value across a
    // new flow, and a fresh code would go on reading as the spent one.
    val invitationLeft by
        key(invitation?.code) {
            remember {
                invitation?.let { invitationStanding(it.code) } ?: flowOf(null)
            }.collectAsStateWithLifecycle(initialValue = null)
        }

    AdminInvitation(
        invitation = invitation,
        standing = invitationLeft,
        now = now,
        link =
        invitation?.let {
            invitationLinkText(
                made = it,
                site = link.site,
                inviter = link.inviter,
                groupName = link.groupName,
            )
        },
        onOpenInvite = onOpenInvite,
    )

    if (trouble) {
        Text(
            text = stringResource(R.string.group_refused),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
