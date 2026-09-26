package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.github.vitalyostanin.geoshare.R

/**
 * Asks for a name, with what it is called now to start from.
 *
 * One dialog for the three names there are — a phone and a person for this
 * member alone, and this phone for everybody — so they read and behave alike.
 * [onSave] gets null for an empty name, which puts back whatever the thing is
 * called without one; the reset button says so in a word.
 */
@Composable
fun NameDialog(
    title: String,
    /** What the name stands in for, shown above the field. */
    note: String,
    current: String?,
    onSave: (String?) -> Unit,
    onClose: () -> Unit,
) {
    var typed by rememberSaveable { mutableStateOf(current.orEmpty()) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(text = title) },
        text = {
            Column {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it.take(LABEL_LIMIT) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(labelText(typed)) }) {
                Text(text = stringResource(R.string.name_save))
            }
        },
        dismissButton = {
            Row {
                if (current != null) {
                    TextButton(onClick = { onSave(null) }) {
                        Text(text = stringResource(R.string.name_reset))
                    }
                }

                TextButton(onClick = onClose) {
                    Text(text = stringResource(R.string.name_cancel))
                }
            }
        },
    )
}
