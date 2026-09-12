package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import io.github.vitalyostanin.geoshare.permission.PermissionStep
import io.github.vitalyostanin.geoshare.service.ThemeChoice
import java.time.Instant

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
    /** What the application is painted in, as the member last asked for it. */
    theme: ThemeChoice,
    /** Everyone with a profile, so that the permissions can be given by name. */
    profiles: Map<String, MemberProfile>,
    ownMemberId: String,
    /** The permissions this member has given, by the member each was given to. */
    sharedWith: Map<String, Instant>,
    /** Whether the phone leaves the application out of its battery saving. */
    batteryUnrestricted: Boolean,
    /** Whether this phone has a startup list of its own to open. */
    startupScreen: Boolean,
    /** Whether the member says that list is already set. */
    startupSet: Boolean,
    onSharingChange: (Boolean) -> Unit,
    onThemeChange: (ThemeChoice) -> Unit,
    onShare: (String, ShareSpan) -> Unit,
    onOpenSettings: () -> Unit,
    onAllowBattery: () -> Unit,
    onOpenStartup: () -> Unit,
    onStartupSetChange: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        // Scrolled rather than fitted: the permissions are one row per member,
        // and a group large enough to need them is large enough to run off a
        // phone screen.
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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

            VisibilitySection(
                profiles = profiles,
                ownMemberId = ownMemberId,
                sharedWith = sharedWith,
                onShare = onShare,
            )

            HorizontalDivider()

            BackgroundWorkSection(
                batteryUnrestricted = batteryUnrestricted,
                startupScreen = startupScreen,
                startupSet = startupSet,
                onAllowBattery = onAllowBattery,
                onOpenStartup = onOpenStartup,
                onStartupSetChange = onStartupSetChange,
            )

            HorizontalDivider()

            AppearanceRow(theme = theme, onThemeChange = onThemeChange)

            HorizontalDivider()

            TextButton(onClick = onSignOut, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(text = stringResource(R.string.sign_out_action))
            }
        }
    }
}

/**
 * What stands between the service and the phone that would rather it stopped.
 *
 * Two rows that are not the same kind of thing. The battery restriction is a
 * state the system will answer for, so the row says how it stands and offers
 * to change it. The manufacturer's startup list cannot be read by anyone, so
 * that row is a reminder with a way to the screen and a mark the member sets
 * themselves once they have been there.
 */
@Composable
private fun BackgroundWorkSection(
    batteryUnrestricted: Boolean,
    startupScreen: Boolean,
    startupSet: Boolean,
    onAllowBattery: () -> Unit,
    onOpenStartup: () -> Unit,
    onStartupSetChange: (Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.background_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.background_battery), modifier = Modifier.weight(1f))

        if (batteryUnrestricted) {
            Text(
                text = stringResource(R.string.background_battery_lifted),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            TextButton(onClick = onAllowBattery) {
                Text(text = stringResource(R.string.background_battery_allow))
            }
        }
    }

    if (!startupScreen) {
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.background_startup), modifier = Modifier.weight(1f))

        TextButton(onClick = onOpenStartup) {
            Text(text = stringResource(R.string.background_startup_open))
        }

        Switch(checked = startupSet, onCheckedChange = onStartupSetChange)
    }

    // Said under the row rather than in a dialog: it explains why this one row
    // is answered by hand while everything above it answers itself.
    Text(
        text = stringResource(R.string.background_startup_explained),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
    )
}

/**
 * The theme, as one row of the list rather than a row of buttons: the screen
 * is a list of "what" and "how it stands", and the answer is read far more
 * often than it is changed.
 */
@Composable
private fun AppearanceRow(theme: ThemeChoice, onThemeChange: (ThemeChoice) -> Unit) {
    var open by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.appearance_title), modifier = Modifier.weight(1f))

        // Anchored to the button, as on the reader rows: a menu declared
        // beside it opens at the start of the row instead, over the label.
        Box {
            TextButton(onClick = { open = true }) {
                Text(text = stringResource(themeLabel(theme)))
            }

            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                for (choice in ThemeChoice.entries) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(themeLabel(choice))) },
                        onClick = {
                            open = false
                            onThemeChange(choice)
                        },
                    )
                }
            }
        }
    }
}

private fun themeLabel(choice: ThemeChoice): Int = when (choice) {
    ThemeChoice.Light -> R.string.appearance_light
    ThemeChoice.Dark -> R.string.appearance_dark
    ThemeChoice.System -> R.string.appearance_system
}

internal fun permissionMessage(step: PermissionStep): Int = when (step) {
    PermissionStep.Location -> R.string.permission_location_needed
    PermissionStep.Notifications -> R.string.permission_notifications_needed
    PermissionStep.BackgroundLocation -> R.string.permission_background_needed
}
