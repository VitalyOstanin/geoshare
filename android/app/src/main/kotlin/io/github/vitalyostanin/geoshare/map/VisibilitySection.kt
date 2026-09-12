package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * When a permission ends: the day as well as the time.
 *
 * The longest span is a day, so a permission given in the evening runs out
 * tomorrow, and an hour alone would read as one that ends this morning. The
 * year is left out: nothing here reaches one.
 */
private val ENDS_AT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM HH:mm")

/**
 * Who may watch this member, and for how long.
 *
 * Being in the group is not the same as being allowed to watch: a group of
 * people on a trip is not a household, and a permission that runs out on its
 * own is what lets somebody be visible for the walk and not for the week after
 * it.
 *
 * One control per member rather than a switch and a span beside it: there is
 * no such thing as sharing for an hour with somebody turned off, and a single
 * list cannot express one.
 */
@Composable
fun VisibilitySection(
    /** Everyone with a profile, this member included; they are not offered themselves. */
    profiles: Map<String, MemberProfile>,
    ownMemberId: String,
    /** The permissions as they stand, by the member each was given to. */
    sharedWith: Map<String, Instant>,
    onShare: (String, ShareSpan) -> Unit,
    modifier: Modifier = Modifier,
) {
    val readers = profiles.values.filter { it.memberId != ownMemberId }.sortedBy { it.displayName }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.visibility_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
        )

        if (readers.isEmpty()) {
            Text(
                text = stringResource(R.string.visibility_empty),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            return@Column
        }

        for (reader in readers) {
            ReaderRow(
                reader = reader,
                until = sharedWith[reader.memberId],
                onShare = { span -> onShare(reader.memberId, span) },
            )
        }
    }
}

@Composable
private fun ReaderRow(reader: MemberProfile, until: Instant?, onShare: (ShareSpan) -> Unit) {
    var open by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = reader.displayName, modifier = Modifier.weight(1f))

        // The list is anchored to the button rather than to the row: a menu
        // placed beside the button opens at the start of the row instead,
        // over the names, and which reader is being changed stops being
        // obvious at exactly the moment it matters.
        Box {
            TextButton(onClick = { open = true }) {
                Text(text = standingText(until))
            }

            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                for (span in ShareSpan.entries) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(spanLabel(span))) },
                        onClick = {
                            open = false
                            onShare(span)
                        },
                    )
                }
            }
        }
    }
}

/**
 * What the row says a permission is now.
 *
 * A permission given hours ago is none of the choices any more, so it is shown
 * as the time it ends rather than as the span it was given for.
 */
@Composable
private fun standingText(until: Instant?): String {
    val now = Instant.now()

    return when (spanOf(until, now)) {
        ShareSpan.Forever -> stringResource(R.string.visibility_forever)

        ShareSpan.None -> stringResource(R.string.visibility_none)

        null ->
            stringResource(
                R.string.visibility_until,
                ENDS_AT.format(until?.atZone(ZoneId.systemDefault())),
            )

        else -> stringResource(R.string.visibility_none)
    }
}

private fun spanLabel(span: ShareSpan): Int = when (span) {
    ShareSpan.None -> R.string.visibility_none
    ShareSpan.Hour -> R.string.visibility_hour
    ShareSpan.EightHours -> R.string.visibility_eight_hours
    ShareSpan.Day -> R.string.visibility_day
    ShareSpan.Forever -> R.string.visibility_forever
}
