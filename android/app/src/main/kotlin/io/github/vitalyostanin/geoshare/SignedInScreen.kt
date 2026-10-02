package io.github.vitalyostanin.geoshare

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import io.github.vitalyostanin.geoshare.auth.AuthState
import io.github.vitalyostanin.geoshare.collection.Constellation
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.KeepWatching
import io.github.vitalyostanin.geoshare.map.LocalLink
import io.github.vitalyostanin.geoshare.map.MapScreen
import io.github.vitalyostanin.geoshare.map.PublishProfile
import io.github.vitalyostanin.geoshare.map.SettingsActions
import io.github.vitalyostanin.geoshare.map.SettingsState
import io.github.vitalyostanin.geoshare.map.labelledPositions
import io.github.vitalyostanin.geoshare.map.labelledProfiles
import io.github.vitalyostanin.geoshare.map.publishSharing
import io.github.vitalyostanin.geoshare.map.rememberDatabaseLink
import io.github.vitalyostanin.geoshare.map.rememberGroupRoster
import io.github.vitalyostanin.geoshare.map.rememberMemberGrants
import io.github.vitalyostanin.geoshare.map.rememberMemberPositions
import io.github.vitalyostanin.geoshare.map.rememberMemberProfiles
import io.github.vitalyostanin.geoshare.map.rememberOwnLabels
import io.github.vitalyostanin.geoshare.map.rememberOwnSharedWith
import io.github.vitalyostanin.geoshare.map.rememberSearches
import io.github.vitalyostanin.geoshare.map.rememberWatchers
import io.github.vitalyostanin.geoshare.map.withShare
import io.github.vitalyostanin.geoshare.map.withinAnswerWait
import io.github.vitalyostanin.geoshare.map.writeLabel
import io.github.vitalyostanin.geoshare.map.writePhoneName
import io.github.vitalyostanin.geoshare.project.ConnectLink
import io.github.vitalyostanin.geoshare.project.FamilyProject
import io.github.vitalyostanin.geoshare.project.PendingInvitation
import io.github.vitalyostanin.geoshare.project.PendingInvitationStore
import io.github.vitalyostanin.geoshare.project.WrongAccountDialog
import io.github.vitalyostanin.geoshare.service.LanguageChoice
import io.github.vitalyostanin.geoshare.service.MadeInvitation
import io.github.vitalyostanin.geoshare.service.ThemeChoice
import io.github.vitalyostanin.geoshare.service.askReset
import io.github.vitalyostanin.geoshare.service.callSiren
import io.github.vitalyostanin.geoshare.service.consumePendingInvitation
import io.github.vitalyostanin.geoshare.service.endSearch
import io.github.vitalyostanin.geoshare.service.forgetAidingData
import io.github.vitalyostanin.geoshare.service.forgetPhone
import io.github.vitalyostanin.geoshare.service.foundGroup
import io.github.vitalyostanin.geoshare.service.joinGroup
import io.github.vitalyostanin.geoshare.service.leavePreviousId
import io.github.vitalyostanin.geoshare.service.makeInvitation
import io.github.vitalyostanin.geoshare.service.openSearch
import io.github.vitalyostanin.geoshare.service.phoneReset
import io.github.vitalyostanin.geoshare.service.phoneSiren
import io.github.vitalyostanin.geoshare.service.positionId
import io.github.vitalyostanin.geoshare.service.removeFromGroup
import io.github.vitalyostanin.geoshare.service.renameGroup
import io.github.vitalyostanin.geoshare.service.renewSearch
import io.github.vitalyostanin.geoshare.service.setRunning
import io.github.vitalyostanin.geoshare.service.stopSiren
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * What a signed-in member sees: the map, and everything hung off it — the
 * settings sheet, the group roster, searches, and the dialogs a pending
 * invitation or a link typed into another account can bring up.
 *
 * Split out of [MainActivity.onCreate] for its size alone: every read and
 * write below still goes through [activity], which is why so much of it is
 * [MainActivity]'s own state and methods rather than anything new. The
 * settings a member expects to survive a sign-out — sharing, the theme, the
 * filters — are passed in rather than remembered again here, since a
 * [remember] made inside this composable would reset with it.
 */
@Composable
internal fun SignedInScreen(
    activity: MainActivity,
    current: AuthState.SignedIn,
    theme: ThemeChoice,
    onTheme: (ThemeChoice) -> Unit,
    language: LanguageChoice,
    sharingOn: Boolean,
    onSharingOn: (Boolean) -> Unit,
    startupSet: Boolean,
    onStartupSet: (Boolean) -> Unit,
    jumpsFiltered: Boolean,
    onJumpsFiltered: (Boolean) -> Unit,
    sourcesLeftOut: Set<String>,
    onSourcesLeftOut: (Set<String>) -> Unit,
    aidingDoubted: Set<Constellation>,
) {
    val profiles by rememberMemberProfiles()
    val searches by rememberSearches()
    val grants by rememberMemberGrants(profiles)
    val ownDocument =
        remember(current.memberId) {
            positionId(current.memberId, activity.device.id)
        }
    val onTheMap by rememberMemberPositions(grants, ownDocument, searches)
    val labels by rememberOwnLabels(current.memberId)
    val sharedWith by rememberOwnSharedWith(current.memberId)
    val roster by rememberGroupRoster()
    val link by rememberDatabaseLink()
    // Kept on the screen rather than in the database:
    // the code is written where nobody may read it
    // back, this phone included, so the only copy to
    // show is the one just made.
    var invitation by
        remember { mutableStateOf<MadeInvitation?>(null) }
    // Whether the last write to the group was refused.
    // Held beside the invitation and for the same
    // reason: nothing in the database says it, and the
    // person who pressed the button is owed an answer.
    var groupTrouble by remember { mutableStateOf(false) }

    // The service does the same; whichever starts
    // first takes the old documents back.
    LaunchedEffect(current.memberId) {
        leavePreviousId(current.memberId, activity.device)
    }

    // Only the restart ConnectState.Done led to ever
    // leaves one of these behind; an ordinary sign-in
    // finds nothing and this does nothing.
    var wrongAccount by remember {
        mutableStateOf<PendingInvitation?>(null)
    }

    LaunchedEffect(current.memberId) {
        val pending = PendingInvitationStore.read(activity)
            ?: return@LaunchedEffect

        // Plainly another account: said before any
        // write, and the invitation is kept for the
        // right one (ADR-0019).
        if (pending.meantForAnotherAccount(current.email)) {
            wrongAccount = pending

            return@LaunchedEffect
        }

        groupTrouble =
            !activity.withGroup("joined a group from a pending invitation") {
                consumePendingInvitation(
                    activity,
                    current.memberId,
                    pending,
                )
            }
    }

    wrongAccount?.let { pending ->
        WrongAccountDialog(
            invitedAddress = pending.invitedAddress.orEmpty(),
            signedInAddress =
            ConnectLink.shortenedAddress(current.email.orEmpty()),
            onLater = {
                PendingInvitationStore.clear(activity)
                wrongAccount = null
            },
            onSwitchAccount = {
                wrongAccount = null
                activity.signIn.signOut()
            },
        )
    }

    // A service left without a session stops after a
    // minute, and the account signed in after that —
    // the phone handed to somebody else — would share
    // nothing until the screen was next opened.
    LaunchedEffect(current.memberId) {
        activity.continueSharingSetup()
    }

    KeepWatching(current.memberId, activity.device.id)

    // The other half of the same collection: this
    // phone says it is looking, and reads who else is.
    val watching by rememberWatchers(current.memberId, activity.device.id)

    PublishProfile(
        memberId = current.memberId,
        displayName = current.displayName,
        pictureUrl = current.pictureUrl,
        deviceId = activity.device.id,
    )

    // Whether the database answers, told to every screen below the map
    // rather than passed down through each of them.
    CompositionLocalProvider(LocalLink provides link) {
        MapScreen(
            // Called what this member called them, where
            // they did; the rest of the screen never
            // needs to know which name is whose.
            positions =
            labelledPositions(onTheMap.positions, labels, grants, searches),
            // Those whose position this phone is refused:
            // an empty map with a reason is not the same
            // thing as an empty map without one.
            outOfReach = onTheMap.outOfReach,
            profiles = labelledProfiles(profiles.orEmpty(), labels),
            watchers = watching,
            ownMemberId = current.memberId,
            ownPhoneId = positionId(current.memberId, activity.device.id),
            ownDisplayName = current.displayName,
            ownPictureUrl = current.pictureUrl,
            searches = searches,
            settings =
            SettingsState(
                sharing = sharingOn,
                permissionNotice = activity.permissionNotice,
                theme = theme,
                language = language,
                sharedWith = sharedWith,
                batteryUnrestricted = activity.batteryFree,
                sirenThroughSilence = activity.sirenLoud,
                startupScreen = activity.startupAvailable,
                startupSet = startupSet,
                jumpsFiltered = jumpsFiltered,
                sourcesLeftOut = sourcesLeftOut,
                aidingDoubted = aidingDoubted,
                roster = roster,
                grants = grants,
                invitation = invitation,
                site = FamilyProject.site,
                groupTrouble = groupTrouble,
                phoneName =
                grants[current.memberId]?.phones?.get(activity.device.id)?.name,
                searches = searches,
            ),
            actions =
            SettingsActions(
                onSharingChange = { wanted ->
                    onSharingOn(wanted)
                    activity.applySharing(wanted)
                },
                // Read by the service out of the same
                // store, which is where it looks on every
                // fix that arrives.
                onJumpsFilteredChange = { wanted ->
                    onJumpsFiltered(wanted)
                    activity.jumps.enabled = wanted
                },
                // Left in the same store for the service
                // to pick up and spend: the request has
                // to outlive this screen, since the phone
                // may not have a fix to answer it with
                // until minutes from now.
                // The service reads the same store on
                // every answer it gets, so the choice
                // takes effect at the next fix rather
                // than at the next restart.
                onSourceUseChange = { source, counted ->
                    activity.sources.use(source, counted)
                    onSourcesLeftOut(activity.sources.leftOut)
                },
                onForgetPlace = { activity.jumps.askToForget() },
                // Done here and not in the service:
                // the command is answered by the
                // receiver on the spot, and what it
                // answered is what the row shows.
                onForgetAiding = {
                    forgetAidingData(activity)
                },
                onThemeChange = { wanted ->
                    onTheme(wanted)
                    activity.appearance.choice = wanted
                },
                // Handed to the platform and not kept
                // here: it stores the answer, tells the
                // system settings about it and builds the
                // screens again in the new language.
                onLanguageChange = { wanted -> activity.speak(wanted) },
                // Written into the grant, and left to be
                // read back: the phones of a member share
                // the same permissions, so the answer has
                // to come from where all of them look.
                onShare = { readerId, span ->
                    activity.lifecycleScope.launch {
                        publishSharing(
                            memberId = current.memberId,
                            sharedWith =
                            withShare(
                                sharedWith = sharedWith,
                                readerId = readerId,
                                span = span,
                                now = Instant.now(),
                            ),
                        )
                    }
                },
                // Read back through the phones of this
                // member, like every other phone reads it.
                onPhoneNameChange = { name ->
                    activity.lifecycleScope.launch {
                        writePhoneName(current.memberId, activity.device.id, name)
                    }
                },
                onOpenSettings = activity::openApplicationSettings,
                onAllowBattery = activity::askBatteryExemption,
                // Read back in onStart, as the battery row is.
                onAllowSirenThroughSilence = activity::askSirenThroughSilence,
                onOpenStartup = activity::openStartupScreen,
                onStartupSetChange = { set ->
                    onStartupSet(set)
                    activity.backgroundWork.startupSet = set
                },
                // Refused when a group already exists,
                // which is the answer to the question the
                // screen cannot ask: a phone outside a
                // group cannot read the roster, so it
                // cannot tell an empty household from one
                // it is not in.
                onFoundGroup = { name ->
                    activity.lifecycleScope.launch {
                        groupTrouble =
                            !activity.withGroup("found a group") {
                                foundGroup(
                                    activity,
                                    current.memberId,
                                    name,
                                )
                            }
                    }
                },
                onMakeInvitation = { email ->
                    activity.lifecycleScope.launch {
                        groupTrouble =
                            !activity.withGroup("made an invitation") {
                                invitation =
                                    makeInvitation(
                                        current.memberId,
                                        email,
                                    )
                            }
                    }
                },
                onJoinGroup = { invited ->
                    activity.lifecycleScope.launch {
                        groupTrouble =
                            !activity.withGroup("joined a group") {
                                joinGroup(
                                    activity,
                                    current.memberId,
                                    invited,
                                )
                            }
                    }
                },
                // A link typed or pasted here is the same
                // thing a messenger hands over as an
                // intent, so it takes the same path:
                // ConnectFlow reads it, and the dialog
                // above the map shows what it asks for.
                onPasteLink = activity.connectFlow::pasteLink,
                onRemoveMember = { memberId ->
                    activity.lifecycleScope.launch {
                        groupTrouble =
                            !activity.withGroup("removed a member") {
                                roster?.let {
                                    removeFromGroup(it, memberId)
                                }
                            }
                    }
                },
                onSetRunning = { memberId, runs ->
                    activity.lifecycleScope.launch {
                        groupTrouble =
                            !activity.withGroup("changed who runs the group") {
                                setRunning(memberId, runs)
                            }
                    }
                },
                // Called from the dialog's own
                // coroutine, as onSearch and
                // onCallSiren are: it has to stay
                // open on a refusal and say so.
                onRenameGroup = { name ->
                    activity.writeOutcome("the group could not be renamed") {
                        renameGroup(name)
                    }
                },
                onSignOut = {
                    activity.lifecycleScope.launch {
                        activity.forgetThisPhone(current.memberId)
                        activity.signIn.signOut()
                    }
                },
                // A suspend function called from the
                // dialog's own coroutine rather than
                // wrapped in lifecycleScope.launch here:
                // the dialog has to stay open on a
                // refusal and say so, which nothing
                // fire-and-forget could tell it.
                onSearch = { phoneId, phoneName, renew, hours ->
                    activity.writeOutcome("a search could not be written") {
                        if (renew) {
                            renewSearch(
                                phoneId,
                                phoneName,
                                current.memberId,
                                hours,
                            )
                        } else {
                            openSearch(
                                phoneId,
                                phoneName,
                                current.memberId,
                                hours,
                            )
                        }
                    }
                },
                sirenOf = ::phoneSiren,
                onCallSiren = { phoneId ->
                    activity.writeOutcome("a siren could not be called") {
                        callSiren(phoneId, current.memberId)
                    }
                },
                resetOf = ::phoneReset,
                onAskReset = { phoneId ->
                    activity.writeOutcome("a place reset could not be asked") {
                        askReset(phoneId, current.memberId)
                    }
                },
                onStopSiren = { phoneId ->
                    try {
                        withinAnswerWait {
                            stopSiren(phoneId, current.memberId)
                        }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (refusal: Exception) {
                        AppLog.w(
                            MainActivity.TAG,
                            "a siren could not be stopped",
                            refusal,
                        )
                    }
                },
                onForgetPhone = { phoneId ->
                    try {
                        forgetPhone(
                            current.memberId,
                            phoneId.substringAfter('.'),
                        )
                        true
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (refusal: Exception) {
                        AppLog.w(
                            MainActivity.TAG,
                            "a phone could not be removed",
                            refusal,
                        )
                        false
                    }
                },
                onEndSearch = { phoneId ->
                    try {
                        withinAnswerWait { endSearch(phoneId) }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (refusal: Exception) {
                        AppLog.w(
                            MainActivity.TAG,
                            "a search could not be ended",
                            refusal,
                        )
                    }
                },
            ),
            labels = labels,
            onLabel = { kind, key, name ->
                activity.lifecycleScope.launch {
                    writeLabel(current.memberId, kind, key, name)
                }
            },
        )
    }
}
