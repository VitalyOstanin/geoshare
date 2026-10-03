package io.github.vitalyostanin.geoshare

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.vitalyostanin.geoshare.auth.AuthState
import io.github.vitalyostanin.geoshare.collection.Constellation
import io.github.vitalyostanin.geoshare.download.DownloadSetting
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.GroupSwitch
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
import io.github.vitalyostanin.geoshare.map.rememberGroupChoices
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
import io.github.vitalyostanin.geoshare.project.Connections
import io.github.vitalyostanin.geoshare.project.GroupConnection
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
import kotlinx.coroutines.async
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
    connection: GroupConnection,
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
    val grants by rememberMemberGrants(profiles, current.memberId)
    val ownDocument =
        remember(current.memberId) {
            positionId(current.memberId, activity.device.id)
        }
    val onTheMap by rememberMemberPositions(grants, ownDocument, searches)
    val labels by rememberOwnLabels(current.memberId)
    val sharedWith by rememberOwnSharedWith(current.memberId)
    val roster by rememberGroupRoster()
    val link by rememberDatabaseLink()
    val answers = remember { GroupAnswers() }

    // The service does the same; whichever starts
    // first takes the old documents back.
    LaunchedEffect(connection, current.memberId) {
        leavePreviousId(current.memberId, activity.device, connection = connection)
    }

    // Only ConnectState.Done ever leaves one of these
    // behind; an ordinary sign-in finds nothing and
    // this does nothing.
    var wrongAccount by remember {
        mutableStateOf<PendingInvitation?>(null)
    }

    LaunchedEffect(connection, current.memberId) {
        // Kept for the sign-in to the group it invites
        // to, which need not be the one on the screen.
        val pending =
            PendingInvitationStore.read(activity)
                ?.takeIf { it.isFor(connection.projectId) }
                ?: return@LaunchedEffect

        // Plainly another account: said before any
        // write, and the invitation is kept for the
        // right one (ADR-0019).
        if (pending.meantForAnotherAccount(current.email)) {
            wrongAccount = pending

            return@LaunchedEffect
        }

        answers.groupTrouble =
            !activity.withGroup("joined a group from a pending invitation") {
                consumePendingInvitation(
                    activity,
                    current.memberId,
                    pending,
                    connection = connection,
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
        email = current.email,
        pictureUrl = current.pictureUrl,
        deviceId = activity.device.id,
    )

    // Watched rather than read once: the answer to the roaming question is
    // written by a receiver while this screen may be open.
    val everyGroup by Connections.all.collectAsStateWithLifecycle()
    val groupChoices by rememberGroupChoices(everyGroup, activity.device.id)
    val downloadSetting = remember { DownloadSetting(activity) }
    val downloads by remember { downloadSetting.changes() }
        .collectAsStateWithLifecycle(downloadSetting.state())

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
                downloads = downloads,
                roster = roster,
                grants = grants,
                invitation = answers.invitation,
                site = connection.settings.site,
                groupTrouble = answers.groupTrouble,
                phoneName =
                grants[current.memberId]?.phones?.get(activity.device.id)?.name,
                searches = searches,
            ),
            actions =
            settingsActions(
                activity = activity,
                connection = connection,
                current = current,
                answers = answers,
                sharedWith = { sharedWith },
                roster = { roster },
                downloadSetting = downloadSetting,
                onSharingOn = onSharingOn,
                onJumpsFiltered = onJumpsFiltered,
                onSourcesLeftOut = onSourcesLeftOut,
                onTheme = onTheme,
                onStartupSet = onStartupSet,
            ),
            labels = labels,
            onLabel = { kind, key, name ->
                activity.lifecycleScope.launch {
                    writeLabel(current.memberId, kind, key, name, connection = connection)
                }
            },
            groups =
            GroupSwitch(
                choices = groupChoices,
                selected = connection.projectId,
                onSelect = { projectId -> Connections.select(activity, projectId) },
                onJoin = activity.connectFlow::askForLink,
                // In the activity's scope rather than the dialog's: the
                // dialog goes with the screen the moment the group does,
                // and what follows the batch must not go with it.
                onLeave = { ask ->
                    val leaving = Connections.of(ask.projectId)

                    leaving == null ||
                        activity.lifecycleScope
                            .async { activity.leave(leaving, ask.how) }
                            .await()
                },
            ),
        )
    }
}
