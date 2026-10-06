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
import androidx.compose.material3.OutlinedTextField
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
import io.github.vitalyostanin.geoshare.project.RefusedJoin
import kotlinx.coroutines.launch

/**
 * A group an administrator took this device off, or — with [refused] — one
 * the invitation it came with was refused for, with the two ways on from it:
 * [onDrop] takes the group off the device, [onJoin] asks for a link to come
 * back by.
 */
data class RemovedGroup(
    val caption: String,
    val onDrop: suspend () -> Unit,
    val onJoin: () -> Unit,
    val refused: RefusedJoin? = null,
)

/**
 * What stands in place of the map while the group shown is one an
 * administrator took this device off.
 *
 * The connection itself stays until the member says otherwise. Drawn over
 * the map rather than instead of it, so the round buttons above
 * and the group chip stay where they are.
 */
@Composable
fun RemovedScreen(removed: RemovedGroup, switcher: Boolean, modifier: Modifier = Modifier) {
    CardOverTheMap(switcher = switcher, modifier = modifier) {
        RemovedNotice(removed = removed, modifier = Modifier.padding(18.dp))
    }
}

/**
 * What stands in place of the map while the device is connected to a project
 * but has no group in it.
 *
 * Offers the two ways to get one: [onFoundGroup] founds a group, with the name
 * typed or `null` when the field is left empty, and [onJoin] asks for a link
 * to join one by.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoGroupScreen(
    switcher: Boolean,
    onFoundGroup: (String?) -> Unit,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var groupName by remember { mutableStateOf("") }

    CardOverTheMap(switcher = switcher, modifier = modifier) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.group_none_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text(
                text = stringResource(R.string.group_none_map_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it.take(GROUP_NAME_LIMIT) },
                label = { Text(text = stringResource(R.string.group_name_optional_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onFoundGroup(groupNameText(groupName)) }) {
                    Text(text = stringResource(R.string.group_found))
                }

                TextButton(onClick = onJoin) {
                    Text(text = stringResource(R.string.group_removed_join))
                }
            }
        }
    }
}

/**
 * The card the screens that stand in place of the map are drawn in: over the
 * map rather than instead of it, so the round buttons above and the group
 * chip stay where they are.
 */
@Composable
private fun CardOverTheMap(
    switcher: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
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
                content()
            }
        }
    }
}

/**
 * What the screens say about [removed], the same on the map and in the
 * settings: that the device was taken off, and what can be done about it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemovedNotice(removed: RemovedGroup, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        val refused = removed.refused
        val title =
            if (refused == null) {
                stringResource(R.string.group_removed_title)
            } else {
                stringResource(R.string.group_join_refused_title, removed.caption)
            }
        val text =
            when {
                refused == null -> stringResource(R.string.group_removed_text, removed.caption)
                refused.inviter == null -> stringResource(R.string.group_join_refused_text)
                else -> stringResource(R.string.group_join_refused_text_by, refused.inviter)
            }

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Text(
            text = text,
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
                            removed.onDrop()
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
            ) {
                Text(text = stringResource(R.string.group_removed_drop))
            }

            TextButton(onClick = removed.onJoin, enabled = !busy) {
                Text(text = stringResource(R.string.group_removed_join))
            }
        }
    }
}
