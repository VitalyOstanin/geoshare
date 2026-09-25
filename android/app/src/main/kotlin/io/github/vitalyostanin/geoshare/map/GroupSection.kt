package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.membership.Invitation
import io.github.vitalyostanin.geoshare.membership.Roster
import io.github.vitalyostanin.geoshare.membership.Standing
import io.github.vitalyostanin.geoshare.membership.invitationFrom
import io.github.vitalyostanin.geoshare.membership.standingIn

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
    onFoundGroup: () -> Unit,
    onJoinGroup: (Invitation) -> Unit,
    onOpenAdmin: () -> Unit,
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
                    profiles = profiles,
                    standing = standing,
                    onOpenAdmin = onOpenAdmin,
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
 * The two ways into a group, offered together.
 *
 * Together because the phone cannot tell which of them applies: a group that
 * has not been made and a group this account is not on both read as no roster
 * at all. Founding one when a group already exists is refused by the rules,
 * which is the answer to the question the screen could not ask.
 */
@Composable
private fun WayIn(onFoundGroup: () -> Unit, onJoinGroup: (Invitation) -> Unit) {
    var typed by remember { mutableStateOf("") }
    val invitation = invitationFrom(typed)

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

    // Nothing is capitalised for the person typing: half of what they enter is
    // an account id, where an upper case letter and a lower case one belong to
    // two different accounts. The code's own half is put into upper case where
    // it is read, which is after the two have been told apart.
    OutlinedTextField(
        value = typed,
        onValueChange = { typed = it },
        label = { Text(text = stringResource(R.string.group_code_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        TextButton(
            onClick = {
                invitation?.let(onJoinGroup)
                typed = ""
            },
            enabled = invitation != null,
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
) {
    for (memberId in roster.members) {
        MemberRow(
            name = profiles[memberId]?.displayName ?: memberId.take(NAME_FALLBACK_LENGTH),
            runsTheGroup = memberId in roster.admins,
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

/** How much of an account id stands in for a name nobody has published. */
private const val NAME_FALLBACK_LENGTH = 6
