package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import io.github.vitalyostanin.geoshare.collection.modesExplained
import io.github.vitalyostanin.geoshare.collection.reasonsExplained

/**
 * The five ways of collecting, read against each other.
 *
 * One place rather than a sentence at a time on the cards: what a mode means
 * is only clear beside the other four, and a member reads this once. The page
 * shows the same table.
 */
@Composable
internal fun ModeHelp(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = onClose) {
                Text(text = stringResource(R.string.map_detail_close))
            }
        },
        title = { Text(text = stringResource(R.string.mode_help_title)) },
        text = {
            Column(
                // Five modes with two sentences each outgrow a short screen,
                // and a dialog clips what does not fit rather than folding it.
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.mode_help_intro),
                    style = MaterialTheme.typography.bodyMedium,
                )

                modesExplained().forEach { explained ->
                    Column {
                        Text(
                            text = stringResource(explained.name),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = stringResource(explained.whenItHappens),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(explained.whatItDoes),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
    )
}

/**
 * What each reason for rejecting a position means.
 *
 * One place rather than a sentence at a time on the cards, as with the modes:
 * the reasons are read against each other — several of them are the same
 * evidence held against different things — and a member reads this once. The
 * page shows the same table.
 */
@Composable
internal fun ReasonHelp(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = onClose) {
                Text(text = stringResource(R.string.map_detail_close))
            }
        },
        title = { Text(text = stringResource(R.string.reason_help_title)) },
        text = {
            Column(
                // Fourteen reasons with two sentences each outgrow any screen,
                // and a dialog clips what does not fit rather than folding it.
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.reason_help_intro),
                    style = MaterialTheme.typography.bodyMedium,
                )

                reasonsExplained().forEach { explained ->
                    Column {
                        Text(
                            text = stringResource(explained.name),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = stringResource(explained.sign),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(explained.means),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
    )
}
