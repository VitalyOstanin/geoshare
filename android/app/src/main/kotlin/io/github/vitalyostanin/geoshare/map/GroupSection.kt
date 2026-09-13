package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.membership.Roster
import io.github.vitalyostanin.geoshare.membership.Standing
import io.github.vitalyostanin.geoshare.membership.looksLikeACode
import io.github.vitalyostanin.geoshare.membership.mayBeRemoved
import io.github.vitalyostanin.geoshare.membership.standingIn
import io.github.vitalyostanin.geoshare.membership.tidyCode

/**
 * Who is in the group, and what the person looking may do about it.
 *
 * Four positions to stand in, and the screen shows a different thing in each:
 * somebody with no group offers to make one or to enter a code, a member sees
 * the roster, the one running the group hands out codes and takes members off,
 * and a group nobody runs says so rather than offering controls whose every
 * write would be refused.
 */
@Composable
fun GroupSection(
    roster: Roster?,
    ownMemberId: String,
    /** The names to show instead of account ids, as far as they are known. */
    profiles: Map<String, MemberProfile>,
    /** The code just made, held until the screen is left. */
    invitation: String?,
    onFoundGroup: () -> Unit,
    onMakeInvitation: () -> Unit,
    onJoinGroup: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val standing = standingIn(roster, ownMemberId)

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.group_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
        )

        when (standing) {
            Standing.Ungrouped, Standing.Outsider ->
                WayIn(onFoundGroup = onFoundGroup, onJoinGroup = onJoinGroup)

            else ->
                TheGroup(
                    roster = roster ?: Roster(),
                    ownMemberId = ownMemberId,
                    profiles = profiles,
                    standing = standing,
                    invitation = invitation,
                    onMakeInvitation = onMakeInvitation,
                    onRemoveMember = onRemoveMember,
                )
        }
    }
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
private fun WayIn(onFoundGroup: () -> Unit, onJoinGroup: (String) -> Unit) {
    var typed by remember { mutableStateOf("") }
    val code = tidyCode(typed)

    Text(
        text = stringResource(R.string.group_none),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        TextButton(onClick = onFoundGroup) {
            Text(text = stringResource(R.string.group_found))
        }
    }

    OutlinedTextField(
        value = typed,
        onValueChange = { typed = it },
        label = { Text(text = stringResource(R.string.group_code_label)) },
        singleLine = true,
        keyboardOptions =
        KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        TextButton(
            onClick = {
                onJoinGroup(code)
                typed = ""
            },
            enabled = looksLikeACode(code),
        ) {
            Text(text = stringResource(R.string.group_join))
        }
    }
}

/** The roster itself, with what the person looking at it may do. */
@Composable
private fun TheGroup(
    roster: Roster,
    ownMemberId: String,
    profiles: Map<String, MemberProfile>,
    standing: Standing,
    invitation: String?,
    onMakeInvitation: () -> Unit,
    onRemoveMember: (String) -> Unit,
) {
    for (memberId in roster.members) {
        MemberRow(
            memberId = memberId,
            name = profiles[memberId]?.displayName ?: memberId.take(NAME_FALLBACK_LENGTH),
            runsTheGroup = memberId in roster.admins,
            // Nobody removes themselves here: leaving is not the same act as
            // taking somebody out, and mixing the two into one row is how a
            // tap in the wrong place empties a group.
            removable =
            standing == Standing.Administrator &&
                memberId != ownMemberId &&
                mayBeRemoved(roster, memberId),
            onRemove = { onRemoveMember(memberId) },
        )
    }

    when (standing) {
        Standing.Administrator ->
            Invitation(invitation = invitation, onMakeInvitation = onMakeInvitation)

        Standing.Unrun ->
            Text(
                text = stringResource(R.string.group_unrun),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

        else -> Unit
    }
}

/** One person on the roster. */
@Composable
private fun MemberRow(
    memberId: String,
    name: String,
    runsTheGroup: Boolean,
    removable: Boolean,
    onRemove: () -> Unit,
) {
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

        if (removable) {
            TextButton(onClick = onRemove) {
                Text(text = stringResource(R.string.group_remove))
            }
        }
    }
}

/**
 * The code, made on demand and shown until the screen is left.
 *
 * Shown in a face whose characters are all the same width: a code is read off
 * this screen and typed into another one, often over the telephone, and the
 * letters that are read for each other are already left out of it.
 */
@Composable
private fun Invitation(invitation: String?, onMakeInvitation: () -> Unit) {
    if (invitation != null) {
        Text(
            text = invitation,
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Text(
            text = stringResource(R.string.group_code_lasts),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        TextButton(onClick = onMakeInvitation) {
            Text(
                text =
                stringResource(
                    if (invitation == null) R.string.group_invite else R.string.group_invite_again,
                ),
            )
        }
    }
}

/** How much of an account id stands in for a name nobody has published. */
private const val NAME_FALLBACK_LENGTH = 6
