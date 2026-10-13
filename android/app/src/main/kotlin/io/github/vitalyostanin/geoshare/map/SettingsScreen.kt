package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.BuildConfig
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.collection.Constellation
import io.github.vitalyostanin.geoshare.collection.SourceChoice
import io.github.vitalyostanin.geoshare.collection.constellationLabel
import io.github.vitalyostanin.geoshare.collection.inOrder
import io.github.vitalyostanin.geoshare.collection.sourceChoiceLabel
import io.github.vitalyostanin.geoshare.download.DownloadNetwork
import io.github.vitalyostanin.geoshare.download.DownloadState
import io.github.vitalyostanin.geoshare.permission.PermissionStep
import io.github.vitalyostanin.geoshare.service.AidingErasure
import io.github.vitalyostanin.geoshare.service.LanguageChoice
import io.github.vitalyostanin.geoshare.service.ThemeChoice
import io.github.vitalyostanin.geoshare.service.aidingErasureNote
import io.github.vitalyostanin.geoshare.service.doneResetLine
import java.time.Instant

/**
 * A setting that switches as a whole row: the word beside a switch is where
 * most taps land, and a row where only the switch answers ignores them.
 */
@Composable
private fun SwitchRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    verticalPadding: Dp = 4.dp,
    label: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        label()

        Switch(checked = checked, onCheckedChange = null)
    }
}

/**
 * The way out of a last position that has gone wrong.
 *
 * The filter measures every fix against where the device was last believed to
 * be, so a last position far enough from the truth turns every true fix into a
 * jump and the marker stops moving. A second fix agreeing with the one before
 * it undoes that on its own, but only while fixes keep arriving; this is for
 * the member who is looking at a marker that has stood still too long.
 *
 * Under it, who last pressed the same button from another screen, and when:
 * the notification that said so may long be gone.
 */
@Composable
private fun ForgetPlaceRow(onForgetPlace: () -> Unit) {
    var forgotten by remember { mutableStateOf(false) }
    val label = if (forgotten) R.string.jumps_forgotten else R.string.jumps_forget
    val context = LocalContext.current
    val doneLine = remember { doneResetLine(context) }

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

    doneLine?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
    }
}

/**
 * What this device is called by everybody who sees it, where its owner named
 * it; the model otherwise.
 *
 * Here rather than on the card: the card is about somebody else's device, and
 * the name given there is for the viewer alone.
 */
@Composable
private fun PhoneNameRow(name: String?, onPhoneNameChange: (String?) -> Unit) {
    var asking by remember { mutableStateOf(false) }

    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.phone_name_title))
            Text(
                text = name ?: stringResource(R.string.phone_name_none),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        TextButton(onClick = { asking = true }) {
            Text(text = stringResource(R.string.phone_name_change))
        }
    }

    if (asking) {
        NameDialog(
            title = stringResource(R.string.phone_name_title),
            note = stringResource(R.string.phone_name_for_all),
            current = name,
            onSave = { typed ->
                onPhoneNameChange(typed)
                asking = false
            },
            onClose = { asking = false },
        )
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
    /** The way to the screen where the group is run, offered to whoever runs it. */
    onOpenAdmin: () -> Unit,
    onOpenMyPhones: () -> Unit,
    /** The screen with what decides how true the position is: jumps, sources, downloads. */
    onOpenAccuracy: () -> Unit,
    /** The licences of the libraries inside the application. */
    onOpenLicences: () -> Unit,
    /** The screen for telling what went wrong, with the log it would send. */
    onOpenReport: () -> Unit,
    /** Leaving the group, offered here while it is the only one; null while the list offers it. */
    onLeaveGroup: (() -> Unit)?,
    /** What the group shown is called, for the question signing out asks. */
    groupCaption: String,
    /** Whether the device is in more than one group, so signing out can be from this one alone. */
    severalGroups: Boolean,
    /** The group shown, when the device was taken off it; its section then says so. */
    removed: RemovedGroup?,
    modifier: Modifier = Modifier,
) {
    var signingOut by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize()) {
        // Scrolled rather than fitted: the permissions are one row per member,
        // and a group large enough to need them is large enough to run off a
        // device screen. The insets are taken outside the scrolling, so that
        // the content stops at the clock and the gesture bar instead of passing
        // under them.
        Column(
            modifier =
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsHeader(onClose = onClose)

            HorizontalDivider()

            SharingSection(settings = settings, actions = actions)

            HorizontalDivider()

            BackgroundWorkSection(
                batteryUnrestricted = settings.batteryUnrestricted,
                sirenThroughSilence = settings.sirenThroughSilence,
                startupScreen = settings.startupScreen,
                startupSet = settings.startupSet,
                onAllowBattery = actions.onAllowBattery,
                onAllowSirenThroughSilence = actions.onAllowSirenThroughSilence,
                onOpenStartup = actions.onOpenStartup,
                onStartupSetChange = actions.onStartupSetChange,
            )

            HorizontalDivider()

            GroupSection(
                roster = settings.roster,
                ownMemberId = ownMemberId,
                profiles = profiles,
                trouble = settings.groupTrouble,
                onFoundGroup = actions.onFoundGroup,
                readProjectMark = actions.onReadProjectMark,
                onJoinGroup = actions.onJoinGroup,
                onPasteLink = actions.onPasteLink,
                onOpenAdmin = onOpenAdmin,
                onOpenMyPhones = onOpenMyPhones,
                onLeaveGroup = onLeaveGroup,
                removed = removed,
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

            ThisDeviceSection(settings = settings, actions = actions)

            HorizontalDivider()

            AccuracyRow(settings = settings, onOpen = onOpenAccuracy)

            HorizontalDivider()

            TextButton(
                onClick = { signingOut = true },
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(text = stringResource(R.string.sign_out_action))
            }

            HorizontalDivider()

            ReportRow(onOpen = onOpenReport)

            HorizontalDivider()

            // Last, under signing out: read once if ever, and the version
            // under it is what somebody reporting a fault is asked for.
            TextButton(
                onClick = onOpenLicences,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(text = stringResource(R.string.licences_open))
            }

            Text(
                text = stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }

    if (signingOut) {
        SignOutDialog(
            caption = groupCaption,
            severalGroups = severalGroups,
            onSignOut = actions.onSignOut,
            onDismiss = { signingOut = false },
        )
    }
}

/** The title of the screen and the way back to the map. */
@Composable
private fun SettingsHeader(onClose: () -> Unit) {
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
}

/**
 * What belongs to this device alone rather than to the group: its name, how
 * the application looks, and in which language it speaks.
 */
@Composable
private fun ThisDeviceSection(settings: SettingsState, actions: SettingsActions) {
    Text(
        text = stringResource(R.string.settings_this_device),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
    )

    PhoneNameRow(name = settings.phoneName, onPhoneNameChange = actions.onPhoneNameChange)

    AppearanceRow(theme = settings.theme, onThemeChange = actions.onThemeChange)

    LanguageRow(language = settings.language, onLanguageChange = actions.onLanguageChange)
}

/**
 * The line under "Position accuracy" that a section behind it has to be read
 * for: the first warning any of those sections shows, or null while none does.
 *
 * Lifted onto the row because the sections were taken off the main screen, and
 * a satellite system heard to no use or a download waiting for an answer is
 * worth less if it is only found by opening a screen nobody has a reason to open.
 */
@Composable
private fun accuracyWarning(settings: SettingsState): String? {
    if (settings.aidingDoubted.isNotEmpty()) {
        val named =
            inOrder(settings.aidingDoubted)
                .map { system -> stringResource(constellationLabel(system)) }
                .joinToString(", ")

        return stringResource(R.string.aiding_wasted, named)
    }

    if (settings.downloads.waitingInRoaming) {
        return stringResource(R.string.downloads_waiting)
    }

    return null
}

/** The way to the screen for reporting a problem. */
@Composable
private fun ReportRow(onOpen: () -> Unit) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.report_open), modifier = Modifier.weight(1f))

        Text(text = "\u203A", style = MaterialTheme.typography.titleLarge)
    }
}

/** The way to the position accuracy screen, in red while something there needs the member. */
@Composable
private fun AccuracyRow(settings: SettingsState, onOpen: () -> Unit) {
    val warning = accuracyWarning(settings)

    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.accuracy_open))
            Text(
                text = warning ?: stringResource(R.string.accuracy_open_note),
                style = MaterialTheme.typography.bodySmall,
                color =
                if (warning != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        Text(text = "\u203A", style = MaterialTheme.typography.titleLarge)
    }
}

/**
 * Jumps, the sources of a position, the satellite data and the downloads.
 *
 * Drawn over the settings it is opened from, like the device list, so that
 * either way back is the settings screen.
 */
@Composable
fun AccuracyScreen(
    settings: SettingsState,
    actions: SettingsActions,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.accuracy_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )

                TextButton(onClick = onClose) {
                    Text(text = stringResource(R.string.accuracy_back))
                }
            }

            HorizontalDivider()

            JumpsSection(settings = settings, actions = actions)

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

            DownloadsSection(
                downloads = settings.downloads,
                onDownloadNetworkChange = actions.onDownloadNetworkChange,
            )
        }
    }
}

/** The switch that shares the position, and the way to the permission the system stopped asking for. */
@Composable
private fun SharingSection(settings: SettingsState, actions: SettingsActions) {
    SwitchRow(
        checked = settings.sharing,
        onCheckedChange = actions.onSharingChange,
        verticalPadding = 12.dp,
    ) {
        Text(text = stringResource(R.string.sharing_action), modifier = Modifier.weight(1f))
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
}

/** The switch that leaves out answers which jump, what that means, and the way to forget a wrong place. */
@Composable
private fun JumpsSection(settings: SettingsState, actions: SettingsActions) {
    SwitchRow(
        checked = settings.jumpsFiltered,
        onCheckedChange = actions.onJumpsFilteredChange,
    ) {
        Text(text = stringResource(R.string.jumps_title), modifier = Modifier.weight(1f))
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
}

/**
 * The one lever the device has over a constellation it hears and never counts.
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
 * Which sources of a position this device takes into account.
 *
 * One switch per source rather than a single "only trust the satellites",
 * because the sources are not a scale: an answer from the towers is a wide
 * honest guess, one from an assistance service is a guess built on whatever
 * the towers around the device were told, and one another application put there
 * is not a guess at all. Which of them are worth counting is a fact about the
 * neighbourhood the device is in, and the member is the one who knows it.
 *
 * Answers from a source switched off are still collected, weighed and logged;
 * what stops is their counting towards where the device is — see
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
        SwitchRow(
            checked = source.name !in leftOut,
            onCheckedChange = { counted -> onSourceUseChange(source, counted) },
        ) {
            Text(text = stringResource(sourceChoiceLabel(source)), modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Over which networks the device downloads on its own.
 *
 * Roaming switched off does not mean "never": the member is asked each time,
 * which the row under the switch says, and a download already waiting for
 * that answer is shown below.
 */
@Composable
private fun DownloadsSection(
    downloads: DownloadState,
    onDownloadNetworkChange: (DownloadNetwork, Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.downloads_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
    )

    Text(
        text = stringResource(R.string.downloads_explained),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
    )

    DownloadNetwork.entries.forEach { network ->
        val allowed = network in downloads.allowed

        SwitchRow(
            checked = allowed,
            onCheckedChange = { wanted -> onDownloadNetworkChange(network, wanted) },
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(downloadNetworkLabel(network)))

                downloadNetworkNote(network, allowed)?.let { note ->
                    Text(
                        text = stringResource(note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (downloads.waitingInRoaming) {
        Text(
            text = stringResource(R.string.downloads_waiting),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
    }
}

private fun downloadNetworkLabel(network: DownloadNetwork): Int = when (network) {
    DownloadNetwork.WiFi -> R.string.downloads_wifi
    DownloadNetwork.Mobile -> R.string.downloads_mobile
    DownloadNetwork.Roaming -> R.string.downloads_roaming
}

private fun downloadNetworkNote(network: DownloadNetwork, allowed: Boolean): Int? = when {
    network == DownloadNetwork.Mobile -> R.string.downloads_mobile_note
    network == DownloadNetwork.Roaming && !allowed -> R.string.downloads_roaming_off_note
    else -> null
}

/**
 * What stands between the service and the device that would rather it stopped.
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
    sirenThroughSilence: Boolean,
    startupScreen: Boolean,
    startupSet: Boolean,
    onAllowBattery: () -> Unit,
    onAllowSirenThroughSilence: () -> Unit,
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

    // Optional, and said so: without it the siren is still heard through
    // every "Do Not Disturb" setting but total silence.
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.siren_silence_title))
            Text(
                text = stringResource(R.string.siren_silence_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (sirenThroughSilence) {
            Text(
                text = stringResource(R.string.siren_silence_granted),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            TextButton(onClick = onAllowSirenThroughSilence) {
                Text(text = stringResource(R.string.siren_silence_allow))
            }
        }
    }

    if (startupScreen) {
        StartupSection(
            startupSet = startupSet,
            onOpenStartup = onOpenStartup,
            onStartupSetChange = onStartupSetChange,
        )
    }
}

/**
 * The manufacturer's screen that lets the application start on its own, which
 * nothing here can read, and the tick that says the member has been to it.
 */
@Composable
private fun StartupSection(
    startupSet: Boolean,
    onOpenStartup: () -> Unit,
    onStartupSetChange: (Boolean) -> Unit,
) {
    // The title on a line of its own: beside a button, a word and a tick it
    // was left a few letters' width on a device and broke inside words.
    Text(
        text = stringResource(R.string.background_startup),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onOpenStartup) {
            Text(text = stringResource(R.string.background_startup_open))
        }

        Spacer(modifier = Modifier.weight(1f))

        // A tick rather than a switch, and a word beside it: the switches on
        // this screen change what the device does, and this one only records
        // that the member has been to a screen nothing here can read. The
        // word ticks it too.
        Row(
            modifier =
            Modifier.toggleable(
                value = startupSet,
                role = Role.Checkbox,
                onValueChange = onStartupSetChange,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.background_startup_done),
                style = MaterialTheme.typography.bodyMedium,
            )

            Checkbox(checked = startupSet, onCheckedChange = null)
        }
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
 * this device rather than about the member, and both are given once. A device
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
