package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vitalyostanin.geoshare.R
import kotlinx.coroutines.launch

/**
 * What stands in place of the map while the group shown is one an
 * administrator took this device off.
 *
 * The connection itself stays until the member says otherwise: [onDrop]
 * takes the group off the device, [onJoin] asks for a link to come back by.
 * Drawn over the map rather than instead of it, so the round buttons above
 * and the group chip stay where they are.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemovedScreen(
    caption: String,
    switcher: Boolean,
    onDrop: suspend () -> Unit,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.TopCenter) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 1.dp,
                shadowElevation = 2.dp,
                modifier =
                Modifier
                    .statusBarsPadding()
                    .padding(top = bannerOffset(switcher), start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = stringResource(R.string.group_removed_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    Text(
                        text = stringResource(R.string.group_removed_text, caption),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                busy = true
                                scope.launch {
                                    try {
                                        onDrop()
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                            enabled = !busy,
                        ) {
                            Text(text = stringResource(R.string.group_removed_drop))
                        }

                        TextButton(onClick = onJoin, enabled = !busy) {
                            Text(text = stringResource(R.string.group_removed_join))
                        }
                    }
                }
            }
        }
    }
}
