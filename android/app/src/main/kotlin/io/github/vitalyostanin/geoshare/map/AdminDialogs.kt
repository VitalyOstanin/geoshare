package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.membership.ADDRESS_LIMIT
import io.github.vitalyostanin.geoshare.membership.looksLikeAnAddress

/** What the icons of the group's screen mean. */
@Composable
internal fun AdminHelpSheet(onClose: () -> Unit) {
    HelpSheet(
        title = stringResource(R.string.help_title_screen),
        entries =
        listOf(
            HelpEntry(
                R.drawable.ic_place_reset,
                HelpLook.Plain,
                stringResource(R.string.help_reset),
            ),
            HelpEntry(R.drawable.ic_siren, HelpLook.Alarm, stringResource(R.string.help_siren)),
            HelpEntry(
                R.drawable.ic_search_alarm,
                HelpLook.Alarm,
                stringResource(R.string.help_search),
            ),
            HelpEntry(
                R.drawable.ic_search_alarm,
                HelpLook.AlarmFilled,
                stringResource(R.string.help_search_standing),
            ),
        ),
        onClose = onClose,
    )
}

/**
 * Asks for the group's own name.
 *
 * Changing it (ADR-0016) is a write of its own, so a roster write refused
 * elsewhere on the panel says nothing about whether this one would be too.
 */
@Composable
internal fun RenameGroupDialog(
    text: String,
    onTextChange: (String) -> Unit,
    trouble: WriteOutcome?,
    busy: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val nameToSave = groupNameText(text)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.group_name_label)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { onTextChange(it.take(GROUP_NAME_LIMIT)) },
                    singleLine = true,
                )

                WriteTrouble(trouble)
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = nameToSave != null && writable(busy, trouble),
            ) {
                Text(text = stringResource(R.string.group_name_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.search_cancel))
            }
        },
    )
}

/**
 * Asks for the address an invitation is made for.
 *
 * Who may spend the code is asked for up front (ADR-0015) rather than left to
 * the address the link happens to be sent to: the rules check the account it
 * names, and a code with none would let in whoever holds it.
 */
@Composable
internal fun InvitationDialog(
    caption: String,
    email: String,
    onEmailChange: (String) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.group_invite_title, caption)) },
        text = {
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = { onEmailChange(it.take(ADDRESS_LIMIT)) },
                    label = { Text(text = stringResource(R.string.group_invite_address)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )

                Text(
                    text = stringResource(R.string.group_invite_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(email.trim()) },
                // A keyboard leaves a space after a suggested word; the
                // address is what is between the spaces.
                enabled = looksLikeAnAddress(email.trim()),
            ) {
                Text(text = stringResource(R.string.group_invite_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.search_cancel))
            }
        },
    )
}

/**
 * Asks before giving up one's own role, which cannot be undone from here: only
 * another administrator can hand it back.
 */
@Composable
internal fun GiveUpDialog(onGiveUp: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.group_give_up_title)) },
        text = { Text(text = stringResource(R.string.group_give_up_text)) },
        confirmButton = {
            TextButton(onClick = onGiveUp) {
                Text(text = stringResource(R.string.group_give_up))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.group_keep_running))
            }
        },
    )
}

/**
 * Asks before removing a member, which is not undone from here either: the
 * member comes back only by a new invitation.
 */
@Composable
internal fun RemoveMemberDialog(name: String, onRemove: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.group_remove_title, name)) },
        text = { Text(text = stringResource(R.string.group_remove_text)) },
        confirmButton = {
            TextButton(onClick = onRemove) {
                Text(
                    text = stringResource(R.string.group_remove_go),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.search_cancel))
            }
        },
    )
}
