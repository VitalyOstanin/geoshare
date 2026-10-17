package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.update.ReleaseNotes

/**
 * What the release changes, read in the app's own dialog instead of sending
 * the member to a browser: the [notes] of [version] in sections, scrolling
 * when they are longer than the screen. The release page stays one tap away
 * through [onOpenPage] for whoever wants the full text.
 */
@Composable
internal fun ReleaseNotesDialog(
    version: String,
    notes: ReleaseNotes,
    onOpenPage: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.update_notes_title, version)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                notes.sections.forEach { section ->
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                    section.items.forEach { item ->
                        Text(
                            text = "• $item",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenPage) {
                Text(text = stringResource(R.string.update_notes_github))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.update_notes_close))
            }
        },
    )
}
