package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.permission.PermissionStep

/**
 * Everything that is not the map.
 *
 * A screen of its own rather than a strip under the map: sharing is set once
 * and then left alone for weeks, and a switch that is always on screen costs
 * the map the room it takes.
 */
@Composable
fun SettingsScreen(
    sharing: Boolean,
    /** The permission the member has to grant on the settings screen, if any. */
    permissionNotice: PermissionStep?,
    onSharingChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onSignOut: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )

                TextButton(onClick = onClose) {
                    Text(text = stringResource(R.string.settings_close))
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.sharing_action), modifier = Modifier.weight(1f))

                Switch(checked = sharing, onCheckedChange = onSharingChange)
            }

            // Shown once the system has stopped asking: from there on the
            // answer is changed on the settings screen, and saying so beats an
            // inert switch.
            if (sharing && permissionNotice != null) {
                Text(
                    text = stringResource(permissionMessage(permissionNotice)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                TextButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Text(text = stringResource(R.string.permission_open_settings))
                }
            }

            HorizontalDivider()

            TextButton(onClick = onSignOut, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(text = stringResource(R.string.sign_out_action))
            }
        }
    }
}

internal fun permissionMessage(step: PermissionStep): Int = when (step) {
    PermissionStep.Location -> R.string.permission_location_needed
    PermissionStep.Notifications -> R.string.permission_notifications_needed
    PermissionStep.BackgroundLocation -> R.string.permission_background_needed
}
