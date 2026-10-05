package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.membership.Invitation
import io.github.vitalyostanin.geoshare.membership.Roster
import io.github.vitalyostanin.geoshare.membership.Standing
import io.github.vitalyostanin.geoshare.membership.invitationFrom
import io.github.vitalyostanin.geoshare.membership.runsTheGroup
import io.github.vitalyostanin.geoshare.membership.standingIn
import io.github.vitalyostanin.geoshare.project.ConnectLink
import io.github.vitalyostanin.geoshare.project.QrScanDialog

/**
 * Who is in the group, and what the person looking may do about it.
 *
 * Four positions to stand in, and the screen shows a different thing in each:
 * somebody with no group offers to make one or to enter a code, a member sees
 * the roster, the one running the group is offered the screen where it is run,
 * and a group nobody runs says so rather than offering controls whose every
 * write would be refused.
 *
 * Running the group is not done here. Handing out a code and taking somebody
 * off the roster are acts of a different kind from the switches around them —
 * they change what the household is rather than how this phone behaves — and
 * they are on a screen of their own, reached from the line below.
 */
@Composable
fun GroupSection(
    roster: Roster?,
    ownMemberId: String,
    /** The names to show instead of account ids, as far as they are known. */
    profiles: Map<String, MemberProfile>,
    /** Whether the last write to the group was refused. */
    trouble: Boolean,
    onFoundGroup: (String?) -> Unit,
    onJoinGroup: (Invitation) -> Unit,
    /** Hands pasted text carrying an invitation link to the same path a messenger's link takes. */
    onPasteLink: (String) -> Unit,
    onOpenAdmin: () -> Unit,
    /** The way to this member's own phones, to ring one of them. */
    onOpenMyPhones: () -> Unit,
    /** Leaving the group, or null when it is offered elsewhere. */
    onLeaveGroup: (() -> Unit)?,
    /**
     * The group, when an administrator took this device off it. Its roster
     * no longer has this account, which would otherwise read as no group at
     * all and offer to found one.
     */
    removed: RemovedGroup? = null,
    modifier: Modifier = Modifier,
) {
    val standing = standingIn(roster, ownMemberId)

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.group_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
        )

        when {
            removed != null ->
                RemovedNotice(removed = removed, modifier = Modifier.padding(horizontal = 16.dp))

            standing == Standing.Ungrouped || standing == Standing.Outsider ->
                WayIn(
                    onFoundGroup = onFoundGroup,
                    onJoinGroup = onJoinGroup,
                    onPasteLink = onPasteLink,
                )

            else ->
                TheGroup(
                    roster = roster ?: Roster(),
                    profiles = profiles,
                    standing = standing,
                    onOpenAdmin = onOpenAdmin,
                    onOpenMyPhones = onOpenMyPhones,
                    onLeaveGroup = onLeaveGroup,
                )
        }

        // Under both of them, because every one of the writes can be refused
        // and none of them says which reason it was.
        if (trouble) {
            Text(
                text = stringResource(R.string.group_refused),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

/**
 * What [WayIn]'s field names once its text is typed or pasted, decided once so
 * the button's enabled state and its click agree on it.
 *
 * A link is checked first: [ConnectLink.parse] answers for the owner's own
 * link too, whose [ConnectLink.invitation] is null, and that one names
 * nothing to join here — pasting it into this field is mistaking it for an
 * invitation, not making one.
 */
internal sealed interface WayInPaste {
    /** A link carrying an invitation, to be handed to `ConnectFlow.pasteLink` whole. */
    data class Link(val text: String) : WayInPaste

    /** A "group.code" invitation, the shape read by [invitationFrom]. */
    data class Code(val invitation: Invitation) : WayInPaste

    /** Neither, including nothing typed at all. */
    data object Neither : WayInPaste
}

/** [WayInPaste] for [typed], the field's whole text at the moment it is read. */
internal fun wayInPaste(typed: String): WayInPaste {
    val link = ConnectLink.parse(typed)

    if (link?.invitation != null) {
        return WayInPaste.Link(typed)
    }

    val invitation = invitationFrom(typed)

    return if (invitation != null) WayInPaste.Code(invitation) else WayInPaste.Neither
}

/**
 * The two ways into a group, offered together.
 *
 * Together because the phone cannot tell which of them applies: a group that
 * has not been made and a group this account is not on both read as no roster
 * at all. Founding one when a group already exists is refused by the rules,
 * which is the answer to the question the screen could not ask.
 */
@Composable
private fun WayIn(
    onFoundGroup: (String?) -> Unit,
    onJoinGroup: (Invitation) -> Unit,
    onPasteLink: (String) -> Unit,
) {
    var typed by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    val pasted = wayInPaste(typed)

    Text(
        text = stringResource(R.string.group_none),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )

    // Optional: a group founded with the field left empty gets no name of its
    // own and is shown by its founder's instead (ADR-0016).
    OutlinedTextField(
        value = groupName,
        onValueChange = { groupName = it.take(GROUP_NAME_LIMIT) },
        label = { Text(text = stringResource(R.string.group_name_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        TextButton(onClick = { onFoundGroup(groupNameText(groupName)) }) {
            Text(text = stringResource(R.string.group_found))
        }
    }

    // Nothing is capitalised for the person typing: half of a "group.code"
    // pair is an account id, where an upper case letter and a lower case one
    // belong to two different accounts, and a pasted link is case-sensitive
    // throughout. The code's own half of the older shape is put into upper
    // case where it is read, which is after [wayInPaste] has told the two
    // apart.
    //
    // The icon at the end reads the link off another screen's QR code and
    // puts it here, to be joined by the button below like a pasted one.
    OutlinedTextField(
        value = typed,
        onValueChange = { typed = it },
        label = { Text(text = stringResource(R.string.group_code_label)) },
        trailingIcon = {
            IconButton(onClick = { scanning = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_qr_scan),
                    contentDescription = stringResource(R.string.connect_scan_qr),
                )
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    )

    if (scanning) {
        QrScanDialog(
            onFound = { link ->
                scanning = false
                typed = link
            },
            onDismiss = { scanning = false },
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        TextButton(
            onClick = {
                when (pasted) {
                    is WayInPaste.Link -> onPasteLink(pasted.text)
                    is WayInPaste.Code -> onJoinGroup(pasted.invitation)
                    WayInPaste.Neither -> Unit
                }
                typed = ""
            },
            enabled = pasted != WayInPaste.Neither,
        ) {
            Text(text = stringResource(R.string.group_join))
        }
    }
}

/** The roster itself, with what the person looking at it may do. */
@Composable
private fun TheGroup(
    roster: Roster,
    profiles: Map<String, MemberProfile>,
    standing: Standing,
    onOpenAdmin: () -> Unit,
    onOpenMyPhones: () -> Unit,
    onLeaveGroup: (() -> Unit)?,
) {
    for (memberId in roster.members) {
        MemberRow(
            name = profiles[memberId]?.displayName ?: unnamedMember(memberId),
            runsTheGroup = runsTheGroup(roster, memberId),
        )
    }

    when (standing) {
        Standing.Administrator ->
            TextButton(
                onClick = onOpenAdmin,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
            ) {
                Text(text = stringResource(R.string.admin_title))
            }

        Standing.Unrun ->
            Text(
                text = stringResource(R.string.group_unrun),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

        else -> Unit
    }

    // Everybody in the group, not only whoever runs it: the phone lost at
    // home is most often one's own other phone.
    TextButton(
        onClick = onOpenMyPhones,
        modifier = Modifier.padding(start = 8.dp),
    ) {
        Text(text = stringResource(R.string.my_phones_open))
    }

    if (onLeaveGroup != null) {
        TextButton(
            onClick = onLeaveGroup,
            modifier = Modifier.padding(start = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.group_leave),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** One person on the roster, named and nothing more. */
@Composable
private fun MemberRow(name: String, runsTheGroup: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge)

            if (runsTheGroup) {
                Text(
                    text = stringResource(R.string.group_runs_it),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
