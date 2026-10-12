package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

/**
 * Whether a member's card offers to show them this member's place: they are
 * on this member's map, and this member is not on theirs.
 *
 * Not for oneself — one's own devices see each other without a permission —
 * and not once a permission stands, whatever its span.
 */
fun offersShareBack(
    memberId: String,
    ownMemberId: String,
    sharedWith: Map<String, Instant>,
    now: Instant,
): Boolean = memberId != ownMemberId && sharingOf(sharedWith[memberId], now) == Sharing.Never

/**
 * The line on a member's card that says they cannot see this member, with the
 * spans to let them, the same as "Who sees where I am" offers.
 *
 * On the card rather than only in the settings: the moment somebody is
 * looked at is the moment it is noticed that they are left out, and a trip
 * to the settings to find them in a list is what kept it from being done.
 */
@Composable
internal fun ShareBackLine(
    name: String,
    onShare: (ShareSpan) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth().padding(top = 8.dp, end = 8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.map_share_back, name),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )

            Box {
                TextButton(onClick = { open = true }) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = stringResource(R.string.map_share_back_go))

                        ListTriangle(color = MaterialTheme.colorScheme.primary)
                    }
                }

                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    for (span in ShareSpan.entries.filter { it != ShareSpan.None }) {
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
}
