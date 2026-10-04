package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R

/**
 * Asks before signing out, saying what it does to the device.
 *
 * With [severalGroups] the member chooses between the group shown, called
 * [caption], and every group; the first is marked, since it leaves the other
 * groups as they were. With one group there is nothing to choose, and the
 * dialog only says what signing out takes away.
 */
@Composable
fun SignOutDialog(
    caption: String,
    severalGroups: Boolean,
    onSignOut: (everywhere: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var everywhere by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text =
                stringResource(
                    if (severalGroups) {
                        R.string.sign_out_choose_title
                    } else {
                        R.string.sign_out_confirm_title
                    },
                ),
            )
        },
        text = {
            if (severalGroups) {
                Column(modifier = Modifier.selectableGroup()) {
                    SignOutOption(
                        title = stringResource(R.string.sign_out_here, caption),
                        note = stringResource(R.string.sign_out_here_note),
                        selected = !everywhere,
                        onSelect = { everywhere = false },
                    )
                    SignOutOption(
                        title = stringResource(R.string.sign_out_everywhere),
                        note = stringResource(R.string.sign_out_everywhere_note),
                        selected = everywhere,
                        onSelect = { everywhere = true },
                    )
                }
            } else {
                Text(text = stringResource(R.string.sign_out_confirm_text, caption))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onSignOut(everywhere || !severalGroups)
                },
            ) {
                Text(
                    text = stringResource(R.string.sign_out_action),
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

@Composable
private fun SignOutOption(title: String, note: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier =
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 6.dp),
    ) {
        RadioButton(selected = selected, onClick = null)

        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
