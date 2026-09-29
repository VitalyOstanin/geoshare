package io.github.vitalyostanin.geoshare.map

import io.github.vitalyostanin.geoshare.collection.Constellation
import io.github.vitalyostanin.geoshare.collection.SourceChoice
import io.github.vitalyostanin.geoshare.membership.Invitation
import io.github.vitalyostanin.geoshare.membership.Roster
import io.github.vitalyostanin.geoshare.permission.PermissionStep
import io.github.vitalyostanin.geoshare.service.AidingErasure
import io.github.vitalyostanin.geoshare.service.LanguageChoice
import io.github.vitalyostanin.geoshare.service.ThemeChoice
import kotlinx.coroutines.flow.Flow
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
    /**
     * The constellations the receiver has been hearing for nothing, as the
     * collecting service last left them.
     *
     * Empty on a phone whose receiver is making use of everything it hears,
     * which is how one should stand: a name here is the receiver sitting on
     * aiding data it believes and cannot use.
     */
    val aidingDoubted: Set<Constellation>,
    /** Whether the phone leaves the application out of its battery saving. */
    val batteryUnrestricted: Boolean,
    /** Whether the siren may loosen "Do Not Disturb" set to total silence. */
    val sirenThroughSilence: Boolean,
    /** Whether this phone has a startup list of its own to open. */
    val startupScreen: Boolean,
    /** Whether the member says that list is already set. */
    val startupSet: Boolean,
    /** Who is in the group, or nothing when this account cannot see a roster. */
    val roster: Roster?,
    /**
     * What each member gives out, as far as this phone has been let see it.
     *
     * Carried for the panel where the group is seen whole: a member who shares
     * with this phone is counted there by the phones they list, and one who
     * does not is simply missing from this map.
     */
    val grants: Map<String, MemberGrant>,
    /**
     * The invitation just made, held until the screen is left: the code with
     * the name of the group in front of it, which is what gets handed over.
     */
    val invitation: String?,
    /**
     * Whether the last write to the group was refused.
     *
     * Kept as a plain yes or no because the refusal says no more than that:
     * the rules weigh a code that has run out, a group already made and a
     * roster somebody else has changed all in one breath, and the screen would
     * be guessing if it named one of them.
     */
    val groupTrouble: Boolean,
    /** What this phone's owner named it for everybody, if they did. */
    val phoneName: String?,
    /**
     * The group's searches, by the phone each one is looking for.
     *
     * Carried whole rather than narrowed to what still stands: the panel lists
     * a search until it is called off, and what still stands is worked out
     * against the clock wherever it is read, the same as everywhere else this
     * is used.
     */
    val searches: Map<String, Search> = emptyMap(),
)

/** What the settings screen can ask for, as one value, and for the same reason. */
data class SettingsActions(
    val onSharingChange: (Boolean) -> Unit,
    val onJumpsFilteredChange: (Boolean) -> Unit,
    val onSourceUseChange: (SourceChoice, Boolean) -> Unit,
    val onForgetPlace: () -> Unit,
    /**
     * Tell the receiver to throw its aiding data away, and say how that
     * went: the answer is the receiver's and arrives at once, so the row
     * reads it back rather than waiting for a state to travel round.
     */
    val onForgetAiding: () -> AidingErasure,
    val onThemeChange: (ThemeChoice) -> Unit,
    val onLanguageChange: (LanguageChoice) -> Unit,
    val onShare: (String, ShareSpan) -> Unit,
    val onOpenSettings: () -> Unit,
    val onAllowBattery: () -> Unit,
    /** Opens the system screen that lets the application change "Do Not Disturb". */
    val onAllowSirenThroughSilence: () -> Unit,
    val onOpenStartup: () -> Unit,
    val onStartupSetChange: (Boolean) -> Unit,
    val onFoundGroup: () -> Unit,
    val onMakeInvitation: () -> Unit,
    val onJoinGroup: (Invitation) -> Unit,
    val onRemoveMember: (String) -> Unit,
    /** Hands a member the running of the group, or takes it back. */
    val onSetRunning: (String, Boolean) -> Unit,
    val onPhoneNameChange: (String?) -> Unit,
    val onSignOut: () -> Unit,
    /**
     * Opens a search for one phone, or renews one already standing; answers
     * whether the database allowed it.
     *
     * A suspend function returning the answer rather than a fire-and-forget
     * callback, unlike the actions above: the dialog that calls this has to
     * stay open on a refusal and say so, which a callback with nothing to
     * return could not tell it.
     */
    val onSearch: suspend (phoneId: String, name: String?, renew: Boolean, hours: Int) -> Boolean,
    /** Ends a search: the phone has been found, or its owner has said so themselves. */
    val onEndSearch: suspend (phoneId: String) -> Unit,
    /** A phone's siren as the database has it, for the screen that rang it. */
    val sirenOf: (phoneId: String) -> Flow<Siren?>,
    /** Rings a phone; answers whether the database allowed it, as [onSearch] does. */
    val onCallSiren: suspend (phoneId: String) -> Boolean,
    /** Stops a phone's siren before its twenty seconds are up. */
    val onStopSiren: suspend (phoneId: String) -> Unit,
    /** Takes one of the member's own phones out of the group; answers whether it went. */
    val onForgetPhone: suspend (phoneId: String) -> Boolean,
)
