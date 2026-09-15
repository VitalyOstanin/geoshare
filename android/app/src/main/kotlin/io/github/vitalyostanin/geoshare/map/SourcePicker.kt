package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import io.github.vitalyostanin.geoshare.collection.SourceChoice
import io.github.vitalyostanin.geoshare.collection.sourceChoiceLabel

/**
 * Which sources of a position this phone counts, reached from the map.
 *
 * The same setting as the one on the settings screen, over the same store, and
 * on the map because this is what changes hour to hour while a transmitter is
 * running: the answers stop being worth anything, the member switches that
 * source off, and the marker stops being dragged about. A setting that lives
 * only in the settings reads as one to be set once.
 *
 * A button and a menu rather than a row of chips: five chips take a strip of
 * map from everybody, including the members who never touch this, while the
 * button says in one line how many sources are counted and asks for a tap
 * only from whoever wants to change that. The count is what carries the news —
 * a phone counting fewer than all of them is a phone leaving answers out, and
 * that is worth seeing without opening anything.
 */
@Composable
fun SourcePicker(
    leftOut: Set<String>,
    onSourceUseChange: (SourceChoice, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val all = SourceChoice.entries
    val counted = all.count { it.name !in leftOut }

    Surface(
        modifier = modifier,
        shape = CircleShape,
        // Over a map rather than over a page: without a ground of its own the
        // label sits on whatever the tiles happen to be.
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
    ) {
        AssistChip(
            onClick = { open = true },
            label = { Text(text = stringResource(R.string.sources_button, counted, all.size)) },
            leadingIcon = {
                // Only when something is being left out: a mark that is always
                // there is a mark nobody reads.
                if (counted < all.size) {
                    Surface(
                        modifier = Modifier.size(8.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error,
                    ) {}
                }
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        )
    }

    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        all.forEach { source ->
            val on = source.name !in leftOut

            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.width(260.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(sourceChoiceLabel(source)),
                            modifier = Modifier.weight(1f),
                        )

                        Switch(
                            checked = on,
                            onCheckedChange = { wanted -> onSourceUseChange(source, wanted) },
                        )
                    }
                },
                // The row itself switches too: a menu where only the switch
                // answers is a menu half of whose taps do nothing.
                onClick = { onSourceUseChange(source, !on) },
            )
        }
    }
}
