package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
 * Which sources of a position this device counts, reached from the map.
 *
 * The same setting as the one on the settings screen, over the same store, and
 * on the map because this is what changes hour to hour while a transmitter is
 * running: the answers stop being worth anything, the member switches that
 * source off, and the marker stops being dragged about. A setting that lives
 * only in the settings reads as one to be set once.
 *
 * A button and a menu rather than a row of chips: five chips take a strip of
 * map from everybody, including the members who never touch this, while the
 * button carries how many sources are counted and asks for a tap only from
 * whoever wants to change that. A round one with the count on it, like the
 * other controls over the map, rather than a line of words that stood apart
 * from them.
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

    val label = stringResource(R.string.sources_button, counted, all.size)

    // The menu opens where the box is, which is the button's own corner.
    Box(modifier = modifier) {
        MapRoundButton(icon = R.drawable.ic_sources, label = label, onClick = { open = true })

        // The count stays on the button: a device counting fewer than all of
        // them is leaving answers out, and that is worth seeing without
        // opening anything. Red only then; a mark that is always loud is a
        // mark nobody reads.
        Surface(
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp),
            shape = CircleShape,
            color =
            if (counted < all.size) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        ) {
            Text(
                text = "$counted/${all.size}",
                style = MaterialTheme.typography.labelSmall,
                color =
                if (counted < all.size) {
                    MaterialTheme.colorScheme.onError
                } else {
                    MaterialTheme.colorScheme.onPrimary
                },
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
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
}
