package io.github.vitalyostanin.geoshare.map

import androidx.annotation.StringRes
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
 * One entry of a help table: the name, a muted line saying when it applies, and a
 * line saying what it means. The modes and the reasons name these three
 * differently, so each caller maps its own record onto this one.
 */
private data class HelpEntry(
    @param:StringRes val name: Int,
    @param:StringRes val subtitle: Int,
    @param:StringRes val meaning: Int,
)

/**
 * A dialog that reads a table of entries against each other: an intro, then
 * every entry as a name, a muted line and a body line. The mode and reason
 * help differ only in their texts and their entries, so they share this.
 */
@Composable
private fun HelpDialog(
    @StringRes titleRes: Int,
    @StringRes introRes: Int,
    entries: List<HelpEntry>,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = onClose) {
                Text(text = stringResource(R.string.map_detail_close))
            }
        },
        title = { Text(text = stringResource(titleRes)) },
        text = {
            Column(
                // A table of entries with two sentences each outgrows a short
                // screen, and a dialog clips what does not fit rather than
                // folding it.
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(introRes),
                    style = MaterialTheme.typography.bodyMedium,
                )

                entries.forEach { entry ->
                    Column {
                        Text(
                            text = stringResource(entry.name),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = stringResource(entry.subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(entry.meaning),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
    )
}

/**
 * The five ways of collecting, read against each other.
 *
 * One place rather than a sentence at a time on the cards: what a mode means
 * is only clear beside the other four, and a member reads this once. The page
 * shows the same table.
 */
@Composable
internal fun ModeHelp(onClose: () -> Unit) {
    HelpDialog(
        titleRes = R.string.mode_help_title,
        introRes = R.string.mode_help_intro,
        entries = modesExplained().map { HelpEntry(it.name, it.whenItHappens, it.whatItDoes) },
        onClose = onClose,
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
    HelpDialog(
        titleRes = R.string.reason_help_title,
        introRes = R.string.reason_help_intro,
        entries = reasonsExplained().map { HelpEntry(it.name, it.sign, it.means) },
        onClose = onClose,
    )
}
