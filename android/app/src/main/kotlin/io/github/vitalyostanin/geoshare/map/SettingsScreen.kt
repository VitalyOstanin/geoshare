package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import io.github.vitalyostanin.geoshare.collection.Constellation
import io.github.vitalyostanin.geoshare.collection.SourceChoice
import io.github.vitalyostanin.geoshare.collection.constellationLabel
import io.github.vitalyostanin.geoshare.collection.inOrder
import io.github.vitalyostanin.geoshare.collection.sourceChoiceLabel
import io.github.vitalyostanin.geoshare.permission.PermissionStep
import io.github.vitalyostanin.geoshare.service.AidingErasure
import io.github.vitalyostanin.geoshare.service.LanguageChoice
import io.github.vitalyostanin.geoshare.service.ThemeChoice
import io.github.vitalyostanin.geoshare.service.aidingErasureNote
import java.time.Instant

/**
 * The way out of a last position that has gone wrong.
 *
 * The filter measures every fix against where the phone was last believed to
 * be, so a last position far enough from the truth turns every true fix into a
 * jump and the marker stops moving. A second fix agreeing with the one before
 * it undoes that on its own, but only while fixes keep arriving; this is for
 * the member who is looking at a marker that has stood still too long.
 */
@Composable
private fun ForgetPlaceRow(onForgetPlace: () -> Unit) {
    var forgotten by remember { mutableStateOf(false) }
    val label = if (forgotten) R.string.jumps_forgotten else R.string.jumps_forget

    TextButton(
        onClick = {
            forgotten = true
            onForgetPlace()
        },
        enabled = !forgotten,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
    ) {
        Text(text = stringResource(label))
    }
}

/**
 * Everything that is not the map.
 *
 * A screen of its own rather than a strip under the map: sharing is set once
 * and then left alone for weeks, and a switch that is always on screen costs
 * the map the room it takes.
 */
@Composable
fun SettingsScreen(
    settings: SettingsState,
    actions: SettingsActions,
    /** Everyone with a profile, so that the permissions can be given by name. */
    profiles: Map<String, MemberProfile>,
    ownMemberId: String,
    /** The clock the screen reads, so that a permission runs out while it is open. */
    now: Instant,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        // Scrolled rather than fitted: the permissions are one row per member,
        // and a group large enough to need them is large enough to run off a
        // phone screen. The insets are taken inside the scrolling, so that the
        // title clears the clock and the last row clears the gesture bar
        // without the scrolled content being cut short of either.
        Column(
            modifier =
            Modifier
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
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

                Switch(checked = settings.sharing, onCheckedChange = actions.onSharingChange)
            }

            // Shown once the system has stopped asking: from there on the
            // answer is changed on the settings screen, and saying so beats an
            // inert switch.
            if (settings.sharing && settings.permissionNotice != null) {
                Text(
                    text = stringResource(permissionMessage(settings.permissionNotice)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                TextButton(
                    onClick = actions.onOpenSettings,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Text(text = stringResource(R.string.permission_open_settings))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.jumps_title), modifier = Modifier.weight(1f))

                Switch(
                    checked = settings.jumpsFiltered,
                    onCheckedChange = actions.onJumpsFilteredChange,
                )
            }

            // Said under the switch rather than left to the title: what it
            // does is easy to read as "be more accurate", and what it really
            // does is throw answers away.
            Text(
                text = stringResource(R.string.jumps_explained),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )

            if (settings.jumpsFiltered) {
                ForgetPlaceRow(onForgetPlace = actions.onForgetPlace)
            }

            HorizontalDivider()

            SourcesSection(
                leftOut = settings.sourcesLeftOut,
                onSourceUseChange = actions.onSourceUseChange,
            )

            HorizontalDivider()

            AidingSection(
                doubted = settings.aidingDoubted,
                onForgetAiding = actions.onForgetAiding,
            )

            HorizontalDivider()

            GroupSection(
                roster = settings.roster,
                ownMemberId = ownMemberId,
                profiles = profiles,
                invitation = settings.invitation,
                trouble = settings.groupTrouble,
                onFoundGroup = actions.onFoundGroup,
                onMakeInvitation = actions.onMakeInvitation,
                onJoinGroup = actions.onJoinGroup,
                onRemoveMember = actions.onRemoveMember,
            )

            HorizontalDivider()

            VisibilitySection(
                profiles = profiles,
                ownMemberId = ownMemberId,
                sharedWith = settings.sharedWith,
                now = now,
                onShare = actions.onShare,
            )

            HorizontalDivider()

            BackgroundWorkSection(
                batteryUnrestricted = settings.batteryUnrestricted,
                startupScreen = settings.startupScreen,
                startupSet = settings.startupSet,
                onAllowBattery = actions.onAllowBattery,
                onOpenStartup = actions.onOpenStartup,
                onStartupSetChange = actions.onStartupSetChange,
            )

            HorizontalDivider()

            AppearanceRow(theme = settings.theme, onThemeChange = actions.onThemeChange)

            LanguageRow(language = settings.language, onLanguageChange = actions.onLanguageChange)

            HorizontalDivider()

            TextButton(
                onClick = actions.onSignOut,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(text = stringResource(R.string.sign_out_action))
            }
        }
    }
}

/**
 * The one lever the phone has over a constellation it hears and never counts.
 *
 * On the settings screen rather than on the card that shows the trouble: the
 * card is read on the map, where a receiver restarting itself in the middle of
 * a walk is the last thing wanted, and this is answered once and left alone
 * for months. The row says nothing until the receiver has answered, since
 * "nothing has been asked" and "asked and refused" are different facts.
 */
@Composable
private fun AidingSection(doubted: Set<Constellation>, onForgetAiding: () -> AidingErasure) {
    var asking by remember { mutableStateOf(false) }
    var erasure by remember { mutableStateOf(AidingErasure.Untouched) }
    val note = aidingErasureNote(erasure)

    Text(
        text = stringResource(R.string.aiding_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
    )

    Text(
        text = stringResource(R.string.aiding_explained),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
    )

    // The one row on this screen that is not an answer the member gave: the
    // service watched half an hour of epochs to write it, and it is here
    // rather than on the map because the button that answers it is here.
    if (doubted.isNotEmpty()) {
        // Named through a mapping rather than inside joinToString: the words
        // come from the resources, and only an inline call keeps that
        // readable to the compose compiler.
        val named =
            inOrder(doubted)
                .map { system -> stringResource(constellationLabel(system)) }
                .joinToString(", ")

        Text(
            text = stringResource(R.string.aiding_wasted, named),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { asking = true }) {
            Text(text = stringResource(R.string.aiding_forget))
        }

        if (note != null) {
            Text(
                text = stringResource(note),
                style = MaterialTheme.typography.bodyMedium,
                color =
                if (erasure == AidingErasure.Refused) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }

    // Asked before it is done rather than undone afterwards: the receiver
    // cannot be given its orbits back, and the minutes without a position
    // that follow are what a member has to agree to beforehand.
    if (asking) {
        AlertDialog(
            onDismissRequest = { asking = false },
            title = { Text(text = stringResource(R.string.aiding_confirm_title)) },
            text = { Text(text = stringResource(R.string.aiding_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        asking = false
                        erasure = onForgetAiding()
                    },
                ) {
                    Text(text = stringResource(R.string.aiding_forget))
                }
            },
            dismissButton = {
                TextButton(onClick = { asking = false }) {
                    Text(text = stringResource(R.string.aiding_keep))
                }
            },
        )
    }
}

/**
 * Which sources of a position this phone takes into account.
 *
 * One switch per source rather than a single "only trust the satellites",
 * because the sources are not a scale: an answer from the towers is a wide
 * honest guess, one from an assistance service is a guess built on whatever
 * the towers around the phone were told, and one another application put there
 * is not a guess at all. Which of them are worth counting is a fact about the
 * neighbourhood the phone is in, and the member is the one who knows it.
 *
 * Answers from a source switched off are still collected, weighed and logged;
 * what stops is their counting towards where the phone is — see
 * [io.github.vitalyostanin.geoshare.collection.SourceChoice] for what each
 * switch covers and why the satellites have none.
 */
@Composable
private fun SourcesSection(
    leftOut: Set<String>,
    onSourceUseChange: (SourceChoice, Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.sources_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
    )

    Text(
        text = stringResource(R.string.sources_explained),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
    )

    SourceChoice.entries.forEach { source ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = stringResource(sourceChoiceLabel(source)), modifier = Modifier.weight(1f))

            Switch(
                checked = source.name !in leftOut,
                onCheckedChange = { counted -> onSourceUseChange(source, counted) },
            )
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

        // A tick rather than a switch, and a word beside it: the switches on
        // this screen change what the phone does, and this one only records
        // that the member has been to a screen nothing here can read.
        Text(
            text = stringResource(R.string.background_startup_done),
            style = MaterialTheme.typography.bodyMedium,
        )

        Checkbox(checked = startupSet, onCheckedChange = onStartupSetChange)
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

/**
 * The language the application is read in.
 *
 * Beside the theme rather than on a screen of its own: both are answers about
 * this phone rather than about the member, and both are given once. A phone
 * running Android 13 or later offers the same choice in its own settings, and
 * either place changes the other.
 */
@Composable
private fun LanguageRow(language: LanguageChoice, onLanguageChange: (LanguageChoice) -> Unit) {
    var open by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.language_title), modifier = Modifier.weight(1f))

        Box {
            TextButton(onClick = { open = true }) {
                Text(text = stringResource(languageLabel(language)))
            }

            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                for (choice in LanguageChoice.entries) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(languageLabel(choice))) },
                        onClick = {
                            open = false
                            onLanguageChange(choice)
                        },
                    )
                }
            }
        }
    }
}

private fun languageLabel(choice: LanguageChoice): Int = when (choice) {
    LanguageChoice.Russian -> R.string.language_russian
    LanguageChoice.English -> R.string.language_english
    LanguageChoice.System -> R.string.language_system
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
