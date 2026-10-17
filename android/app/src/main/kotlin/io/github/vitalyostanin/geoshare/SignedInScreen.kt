package io.github.vitalyostanin.geoshare

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.vitalyostanin.geoshare.auth.AuthState
import io.github.vitalyostanin.geoshare.collection.Constellation
import io.github.vitalyostanin.geoshare.collection.Watcher
import io.github.vitalyostanin.geoshare.download.DownloadSetting
import io.github.vitalyostanin.geoshare.download.DownloadState
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.FirstShareQuestion
import io.github.vitalyostanin.geoshare.map.GroupChoice
import io.github.vitalyostanin.geoshare.map.GroupSwitch
import io.github.vitalyostanin.geoshare.map.KeepWatching
import io.github.vitalyostanin.geoshare.map.LabelKind
import io.github.vitalyostanin.geoshare.map.Labels
import io.github.vitalyostanin.geoshare.map.LinkState
import io.github.vitalyostanin.geoshare.map.LocalLink
import io.github.vitalyostanin.geoshare.map.MapScreen
import io.github.vitalyostanin.geoshare.map.MemberGrant
import io.github.vitalyostanin.geoshare.map.MemberProfile
import io.github.vitalyostanin.geoshare.map.MembersInView
import io.github.vitalyostanin.geoshare.map.PublishProfile
import io.github.vitalyostanin.geoshare.map.Search
import io.github.vitalyostanin.geoshare.map.SettingsActions
import io.github.vitalyostanin.geoshare.map.SettingsState
import io.github.vitalyostanin.geoshare.map.labelledPositions
import io.github.vitalyostanin.geoshare.map.labelledProfiles
import io.github.vitalyostanin.geoshare.map.publishSharing
import io.github.vitalyostanin.geoshare.map.rememberDatabaseLink
import io.github.vitalyostanin.geoshare.map.rememberGroupChoices
import io.github.vitalyostanin.geoshare.map.rememberMemberGrants
import io.github.vitalyostanin.geoshare.map.rememberMemberPositions
import io.github.vitalyostanin.geoshare.map.rememberMemberProfiles
import io.github.vitalyostanin.geoshare.map.rememberOwnLabels
import io.github.vitalyostanin.geoshare.map.rememberOwnSharedWith
import io.github.vitalyostanin.geoshare.map.rememberRosterSeen
import io.github.vitalyostanin.geoshare.map.rememberSearches
import io.github.vitalyostanin.geoshare.map.rememberWatchers
import io.github.vitalyostanin.geoshare.map.withShare
import io.github.vitalyostanin.geoshare.map.withinAnswerWait
import io.github.vitalyostanin.geoshare.map.writeLabel
import io.github.vitalyostanin.geoshare.map.writePhoneName
import io.github.vitalyostanin.geoshare.membership.RosterSeen
import io.github.vitalyostanin.geoshare.project.ConnectLink
import io.github.vitalyostanin.geoshare.project.Connections
import io.github.vitalyostanin.geoshare.project.GroupConnection
import io.github.vitalyostanin.geoshare.project.PendingInvitation
import io.github.vitalyostanin.geoshare.project.PendingInvitationStore
import io.github.vitalyostanin.geoshare.project.WrongAccountDialog
import io.github.vitalyostanin.geoshare.project.refusedJoinOf
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
    kept: KeptChoices,
) {
    val reads = rememberGroupReads(activity, current.memberId)
    val answers = rememberSaveable(saver = GroupAnswers.Saver) { GroupAnswers() }
    // A project with no group of this account's own in it yet refuses the
    // profile, the claim to be looking and the first share alike; they wait
    // for the group rather than be written and refused on every return.
    val outside = reads.rosterSeen.value.outside(current.memberId)

    SignedInEffects(activity, connection, current, answers, outside)

    SignedInMap(activity, connection, current, kept, answers, reads)
}

/**
 * What [SignedInScreen] watches in the group, each as the [State] it comes
 * from: the map reads them where it draws, and a callback handed down reads
 * the latest rather than the value of the composition that made it.
 */
private class GroupReads(
    val profiles: State<Map<String, MemberProfile>?>,
    val searches: State<Map<String, Search>>,
    val grants: State<Map<String, MemberGrant>>,
    val onTheMap: State<MembersInView>,
    val labels: State<Labels>,
    val sharedWith: State<Map<String, Instant>>,
    val rosterSeen: State<RosterSeen>,
    val link: State<LinkState>,
    val watching: State<List<Watcher>>,
    val groupChoices: State<List<GroupChoice>>,
    val downloadSetting: DownloadSetting,
    val downloads: State<DownloadState>,
)

/** Starts every read of the group that the signed-in screen shows. */
@Composable
private fun rememberGroupReads(activity: MainActivity, memberId: String): GroupReads {
    val profiles = rememberMemberProfiles()
    val searches = rememberSearches()
    val grants = rememberMemberGrants(profiles.value, memberId)
    val ownDocument = remember(memberId) { positionId(memberId, activity.device.id) }
    // Watched rather than read once: the answer to the roaming question is
    // written by a receiver while this screen may be open.
    val everyGroup by Connections.all.collectAsStateWithLifecycle()
    val downloadSetting = remember { DownloadSetting(activity) }

    return GroupReads(
        profiles = profiles,
        searches = searches,
        grants = grants,
        onTheMap =
        rememberMemberPositions(grants.value, ownDocument, searches.value, profiles.value),
        labels = rememberOwnLabels(memberId),
        sharedWith = rememberOwnSharedWith(memberId),
        rosterSeen = rememberRosterSeen(),
        link = rememberDatabaseLink(),
        // The other half of the same collection: this device says it is
        // looking, and reads who else is.
        watching = rememberWatchers(memberId, activity.device.id),
        groupChoices = rememberGroupChoices(everyGroup, activity.device.id),
        downloadSetting = downloadSetting,
        downloads = remember { downloadSetting.changes() }
            .collectAsStateWithLifecycle(downloadSetting.state()),
    )
}

/** The map with the settings sheet and the first-share question over it. */
@Composable
private fun SignedInMap(
    activity: MainActivity,
    connection: GroupConnection,
    current: AuthState.SignedIn,
    kept: KeptChoices,
    answers: GroupAnswers,
    reads: GroupReads,
) {
    val profiles = reads.profiles.value
    val searches = reads.searches.value
    val labels = reads.labels.value
    val onTheMap = reads.onTheMap.value

    // Whether the database answers, told to every screen below the map
    // rather than passed down through each of them.
    CompositionLocalProvider(LocalLink provides reads.link.value) {
        MapScreen(
            // Called what this member called them, where
            // they did; the rest of the screen never
            // needs to know which name is whose.
            positions =
            labelledPositions(onTheMap.positions, labels, reads.grants.value, searches),
            // Those whose position this device is refused:
            // an empty map with a reason is not the same
            // thing as an empty map without one.
            outOfReach = onTheMap.outOfReach,
            profiles = labelledProfiles(profiles.orEmpty(), labels),
            watchers = reads.watching.value,
            ownMemberId = current.memberId,
            ownPhoneId = positionId(current.memberId, activity.device.id),
            ownDisplayName = current.displayName,
            ownPictureUrl = current.pictureUrl,
            searches = searches,
            settings = settingsStateFrom(activity, connection, current, kept, answers, reads),
            actions = settingsActionsFrom(activity, connection, current, kept, answers, reads),
            labels = labels,
            onLabel = labelWriter(activity, current.memberId, connection),
            groups = groupSwitch(activity, connection, reads.groupChoices.value),
        )

        // Once per group, over the map: whom to show this member's place to,
        // while nobody has been given it yet.
        if (!reads.rosterSeen.value.outside(current.memberId)) {
            AskFirstShare(
                activity,
                connection,
                current.memberId,
                profiles,
                reads.sharedWith.value,
            )
        }
    }
}

/** [settingsStateOf] for the values [reads] holds now. */
private fun settingsStateFrom(
    activity: MainActivity,
    connection: GroupConnection,
    current: AuthState.SignedIn,
    kept: KeptChoices,
    answers: GroupAnswers,
    reads: GroupReads,
): SettingsState = settingsStateOf(
    activity = activity,
    connection = connection,
    memberId = current.memberId,
    kept = kept,
    answers = answers,
    sharedWith = reads.sharedWith.value,
    downloads = reads.downloads.value,
    rosterSeen = reads.rosterSeen.value,
    grants = reads.grants.value,
    searches = reads.searches.value,
)

/**
 * [settingsActions] for the group [reads] holds. The shared-with list is read
 * when an action runs; the roster is the one this composition saw.
 */
private fun settingsActionsFrom(
    activity: MainActivity,
    connection: GroupConnection,
    current: AuthState.SignedIn,
    kept: KeptChoices,
    answers: GroupAnswers,
    reads: GroupReads,
): SettingsActions {
    val roster = reads.rosterSeen.value.roster

    return settingsActions(
        activity = activity,
        connection = connection,
        current = current,
        answers = answers,
        sharedWith = { reads.sharedWith.value },
        roster = { roster },
        downloadSetting = reads.downloadSetting,
        onSharingOn = kept.onSharingOn,
        onJumpsFiltered = kept.onJumpsFiltered,
        onSourcesLeftOut = kept.onSourcesLeftOut,
        onTheme = kept.onTheme,
        onStartupSet = kept.onStartupSet,
    )
}

/** Keeps a name this member gives a person or a device, in the activity's scope so a closed dialog does not drop it. */
private fun labelWriter(
    activity: MainActivity,
    memberId: String,
    connection: GroupConnection,
): (LabelKind, String, String?) -> Unit = { kind, key, name ->
    activity.lifecycleScope.launch {
        writeLabel(memberId, kind, key, name, connection = connection)
    }
}

/** What the settings screen shows of this group, this account and this device. */
private fun settingsStateOf(
    activity: MainActivity,
    connection: GroupConnection,
    memberId: String,
    kept: KeptChoices,
    answers: GroupAnswers,
    sharedWith: Map<String, Instant>,
    downloads: DownloadState,
    rosterSeen: RosterSeen,
    grants: Map<String, MemberGrant>,
    searches: Map<String, Search>,
): SettingsState = SettingsState(
    sharing = kept.sharingOn,
    permissionNotice = activity.permissionNotice,
    theme = kept.theme,
    language = kept.language,
    sharedWith = sharedWith,
    batteryUnrestricted = activity.batteryFree,
    sirenThroughSilence = activity.sirenLoud,
    startupScreen = activity.startupAvailable,
    startupSet = kept.startupSet,
    jumpsFiltered = kept.jumpsFiltered,
    sourcesLeftOut = kept.sourcesLeftOut,
    aidingDoubted = kept.aidingDoubted,
    downloads = downloads,
    roster = rosterSeen.roster,
    rosterAnswered = rosterSeen.answered,
    grants = grants,
    invitation = answers.invitation,
    site = connection.settings.site,
    groupTrouble = answers.groupTrouble,
    refusedJoin = answers.refusedJoin,
    phoneName =
    grants[memberId]?.phones?.get(activity.device.id)?.name,
    searches = searches,
)

/**
 * The settings a member expects to survive a sign-out — sharing, the theme,
 * the filters — with the ways to change them, kept above the screen of one
 * group: a [remember] made inside [SignedInScreen] would reset with it.
 */
internal class KeptChoices(
    val theme: ThemeChoice,
    val onTheme: (ThemeChoice) -> Unit,
    val language: LanguageChoice,
    val sharingOn: Boolean,
    val onSharingOn: (Boolean) -> Unit,
    val startupSet: Boolean,
    val onStartupSet: (Boolean) -> Unit,
    val jumpsFiltered: Boolean,
    val onJumpsFiltered: (Boolean) -> Unit,
    val sourcesLeftOut: Set<String>,
    val onSourcesLeftOut: (Set<String>) -> Unit,
    val aidingDoubted: Set<Constellation>,
)

/**
 * What the signed-in screen sets going in the background: the old documents
 * taken back, a pending invitation answered, sharing carried on, and — once
 * the account has a group of its own here — the claim to be looking and the
 * profile.
 */
@Composable
private fun SignedInEffects(
    activity: MainActivity,
    connection: GroupConnection,
    current: AuthState.SignedIn,
    answers: GroupAnswers,
    outside: Boolean,
) {
    // The service does the same; whichever starts
    // first takes the old documents back.
    LaunchedEffect(connection, current.memberId) {
        leavePreviousId(current.memberId, activity.device, connection = connection)
    }

    PendingInvitationAnswer(activity, connection, current, answers)

    // A service left without a session stops after a
    // minute, and the account signed in after that —
    // the device handed to somebody else — would share
    // nothing until the screen was next opened.
    LaunchedEffect(current.memberId) {
        activity.continueSharingSetup()
    }

    if (!outside) {
        KeepWatching(current.memberId, activity.device.id)
    }

    if (!outside) {
        PublishProfile(
            memberId = current.memberId,
            displayName = current.displayName,
            email = current.email,
            pictureUrl = current.pictureUrl,
            deviceId = activity.device.id,
        )
    }
}

/**
 * Takes up an invitation kept from before the sign-in, for this group and
 * account, and asks about one that was plainly meant for another account.
 */
@Composable
private fun PendingInvitationAnswer(
    activity: MainActivity,
    connection: GroupConnection,
    current: AuthState.SignedIn,
    answers: GroupAnswers,
) {
    // Only ConnectState.Done ever leaves one of these
    // behind; an ordinary sign-in finds nothing and
    // this does nothing.
    var wrongAccount by remember {
        mutableStateOf<PendingInvitation?>(null)
    }

    LaunchedEffect(connection, current.memberId) {
        // Kept for the sign-in to the group it invites
        // to, which need not be the one on the screen.
        val kept = PendingInvitationStore.read(activity, connection.projectId)

        // One the rules already turned down is not tried again; the map
        // goes on saying so until the group is dropped or another link
        // is opened.
        answers.refusedJoin = refusedJoinOf(kept, connection.projectId)

        val pending =
            kept
                ?.takeIf { !it.refused }
                ?: return@LaunchedEffect

        // Plainly another account: said before any
        // write, and the invitation is kept for the
        // right one (ADR-0019).
        if (pending.meantForAnotherAccount(current.email)) {
            wrongAccount = pending

            return@LaunchedEffect
        }

        val joined =
            activity.withGroup("joined a group from a pending invitation") {
                consumePendingInvitation(
                    activity,
                    current.memberId,
                    pending,
                    connection = connection,
                )
            }

        // Refused by the rules: the invitation is marked so, and the map
        // says it in its own words. Anything else — no network — is the
        // general trouble line, and the invitation waits for another try.
        answers.refusedJoin = refusedJoinOf(
            PendingInvitationStore.read(activity, connection.projectId),
            connection.projectId,
        )
        answers.groupTrouble = !joined && answers.refusedJoin == null
    }

    wrongAccount?.let { pending ->
        WrongAccountDialog(
            invitedAddress = pending.invitedAddress.orEmpty(),
            signedInAddress =
            ConnectLink.shortenedAddress(current.email.orEmpty()),
            onLater = {
                PendingInvitationStore.clear(activity, connection.projectId)
                wrongAccount = null
            },
            onSwitchAccount = {
                wrongAccount = null
                activity.signIn.signOut()
            },
        )
    }
}

/** The groups this device is in, to switch between, join and leave. */
private fun groupSwitch(
    activity: MainActivity,
    connection: GroupConnection,
    groupChoices: List<GroupChoice>,
): GroupSwitch = GroupSwitch(
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
)

/** Once per group, over the map: whom to show this member's place to, while nobody has been given it yet. */
@Composable
private fun AskFirstShare(
    activity: MainActivity,
    connection: GroupConnection,
    memberId: String,
    profiles: Map<String, MemberProfile>?,
    sharedWith: Map<String, Instant>,
) {
    FirstShareQuestion(
        connection = connection,
        memberId = memberId,
        profiles = profiles,
        onShare = { readers, span ->
            activity.lifecycleScope.launch {
                val now = Instant.now()

                publishSharing(
                    memberId = memberId,
                    sharedWith =
                    readers.fold(sharedWith) { given, reader ->
                        withShare(given, reader, span, now)
                    },
                    connection = connection,
                )
            }
        },
    )
}
