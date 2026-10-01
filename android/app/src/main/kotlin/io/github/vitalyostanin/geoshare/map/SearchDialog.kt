package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** When a search would end: the day as well as the time, the same as a permission's end. */
private val SEARCH_ENDS_AT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM HH:mm")

/**
 * Opening a search, or renewing one already standing.
 *
 * One dialog for both, the way the page has one window for both: what differs
 * is the title and the word on the button that writes it, and a renewal picks
 * up wherever the standing search already reads from, rather than starting the
 * choice over at the default.
 */
@Composable
fun SearchDialog(
    /** Whose phone [phoneName] names, so a search opened on another member's device says whose. */
    memberName: String,
    phoneName: String,
    renew: Boolean,
    hours: Int,
    onHoursChange: (Int) -> Unit,
    now: Instant,
    /** How the last attempt to write this search came out; null before any. */
    trouble: WriteOutcome?,
    busy: Boolean,
    onGo: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text =
                stringResource(
                    if (renew) R.string.search_renew_title else R.string.search_open_title,
                    phoneName,
                    memberName,
                ),
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.search_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (choice in SEARCH_HOURS) {
                        FilterChip(
                            selected = choice == hours,
                            onClick = { onHoursChange(choice) },
                            label = { Text(text = stringResource(searchHoursLabel(choice))) },
                        )
                    }
                }

                Text(
                    text =
                    stringResource(
                        R.string.search_until,
                        SEARCH_ENDS_AT.format(
                            endIfChosen(hours, now).atZone(ZoneId.systemDefault()),
                        ),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )

                WriteTrouble(trouble)
            }
        },
        confirmButton = {
            val goes = writable(busy, trouble)

            // Red only while it can be pressed: a colour of its own would
            // keep a button that does nothing looking like one that does.
            TextButton(onClick = onGo, enabled = goes) {
                Text(
                    text =
                    stringResource(if (renew) R.string.search_renew else R.string.search_go),
                    color = if (goes) MaterialTheme.colorScheme.error else Color.Unspecified,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(text = stringResource(R.string.search_cancel)) }
        },
    )
}

/** The word one of the three lengths is offered under: the choices the page also gives. */
private fun searchHoursLabel(hours: Int): Int = when (hours) {
    6 -> R.string.search_hours_6
    72 -> R.string.search_hours_72
    else -> R.string.search_hours_24
}
