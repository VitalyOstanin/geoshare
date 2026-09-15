package io.github.vitalyostanin.geoshare.map

import io.github.vitalyostanin.geoshare.collection.SourceChoice
import io.github.vitalyostanin.geoshare.membership.Roster
import io.github.vitalyostanin.geoshare.permission.PermissionStep
import io.github.vitalyostanin.geoshare.service.LanguageChoice
import io.github.vitalyostanin.geoshare.service.ThemeChoice
import java.time.Instant

/**
 * Everything the settings screen shows, as one value.
 *
 * The screen is reached through the map, so all of it used to travel as loose
 * parameters: named in the map's own signature, named again in the call it
 * passes them on with, and named a third time where the screen was built.
 * Three lists to keep in step, none of which the map itself reads.
 */
data class SettingsState(
    /** Whether this phone is sharing its position at all. */
    val sharing: Boolean,
    /** The permission the member has to grant on the settings screen, if any. */
    val permissionNotice: PermissionStep?,
    /** What the application is painted in, as the member last asked for it. */
    val theme: ThemeChoice,
    /** What language it is read in, as the member last asked for it. */
    val language: LanguageChoice,
    /** The permissions this member has given, by the member each was given to. */
    val sharedWith: Map<String, Instant>,
    /** Whether the phone leaves out the positions it cannot have reached. */
    val jumpsFiltered: Boolean,
    /**
     * The sources this phone does not take into account, by the name each is
     * stored under.
     *
     * Empty for a phone counting every answer it gets, which is how one
     * starts.
     */
    val sourcesLeftOut: Set<String>,
    /** Whether the phone leaves the application out of its battery saving. */
    val batteryUnrestricted: Boolean,
    /** Whether this phone has a startup list of its own to open. */
    val startupScreen: Boolean,
    /** Whether the member says that list is already set. */
    val startupSet: Boolean,
    /** Who is in the group, or nothing when this account cannot see a roster. */
    val roster: Roster?,
    /** The invitation code just made, held until the screen is left. */
    val invitation: String?,
)

/** What the settings screen can ask for, as one value, and for the same reason. */
data class SettingsActions(
    val onSharingChange: (Boolean) -> Unit,
    val onJumpsFilteredChange: (Boolean) -> Unit,
    val onSourceUseChange: (SourceChoice, Boolean) -> Unit,
    val onForgetPlace: () -> Unit,
    val onThemeChange: (ThemeChoice) -> Unit,
    val onLanguageChange: (LanguageChoice) -> Unit,
    val onShare: (String, ShareSpan) -> Unit,
    val onOpenSettings: () -> Unit,
    val onAllowBattery: () -> Unit,
    val onOpenStartup: () -> Unit,
    val onStartupSetChange: (Boolean) -> Unit,
    val onFoundGroup: () -> Unit,
    val onMakeInvitation: () -> Unit,
    val onJoinGroup: (String) -> Unit,
    val onRemoveMember: (String) -> Unit,
    val onSignOut: () -> Unit,
)
