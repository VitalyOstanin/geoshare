package io.github.vitalyostanin.geoshare.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.collection.Watcher
import io.github.vitalyostanin.geoshare.collection.watchingMembers
import io.github.vitalyostanin.geoshare.membership.Leave
import io.github.vitalyostanin.geoshare.membership.Roster
import io.github.vitalyostanin.geoshare.membership.Standing
import io.github.vitalyostanin.geoshare.membership.howToLeave
import io.github.vitalyostanin.geoshare.membership.standingIn
import io.github.vitalyostanin.geoshare.update.UpdateBoard
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.roundToInt

/** Above this a surface counts as pale, below it as dark. */
internal const val HALF_LIT = 0.5f

/** As much of the screen as the full list may take before it starts scrolling instead. */
internal val LIST_MAX_HEIGHT = 220.dp

/** Keeps the positions under an opened marker across a recreated screen, as the card is kept. */
private val PositionIdsSaver = listSaver<List<String>, String>(
    save = { it },
    restore = { it },
)

/**
 * The screen a signed-in member sees: everyone on a map, and a row of buttons
 * to move the map onto one of them.
 *
 * The buttons are the point of the screen. Answering "where is X" should be a
 * tap rather than a search across a map of everybody, so two members sit in
 * the row itself and the rest are one tap further in.
 */
@Composable
fun MapScreen(
    positions: List<MemberPosition>,
    /** How many members' positions this device is being refused, if any. */
    outOfReach: Int,
    /** What each member calls themselves, by member id; absent until they sign in. */
    profiles: Map<String, MemberProfile>,
    /** Who else has the map open, as they themselves claim. */
    watchers: List<Watcher>,
    ownMemberId: String,
    /**
     * This device's own `{uid}.{deviceId}`: the administrator's panel rings
     * every device but this one.
     */
    ownPhoneId: String?,
    ownDisplayName: String?,
    ownPictureUrl: String?,
    /** The group's searches, by the device each is looking for. */
    searches: Map<String, Search>,
    /** Everything the settings screen shows, which the map only carries through. */
    settings: SettingsState,
    /** What the settings screen can ask for, likewise. */
    actions: SettingsActions,
    /** What this member calls the devices and the people, for themselves. */
    labels: Labels,
    /** Gives a device or a person a name for this member alone, or takes it away. */
    onLabel: (LabelKind, String, String?) -> Unit,
    /** The groups this device is in, for the chip that switches between them. */
    groups: GroupSwitch,
    modifier: Modifier = Modifier,
) {
    val now by currentTime()
    // The devices the group is looking for right now, worked out against the
    // same clock the ages tick by: nothing arrives from the database at the
    // moment a search runs out, so this is read afresh whenever [now] moves.
    val soughtPhones = remember(searches, now) { standingSearches(searches, now).keys }
    // Read here rather than only where the administrator's panel is opened,
    // because the card of a member being looked for offers "Found" on this
    // same condition, and a device that stops running the group should lose
    // the button on both at once.
    val runsTheGroup = standingIn(settings.roster, ownMemberId) == Standing.Administrator
    // The name being asked for, held by what it names rather than by the card:
    // the card may be closed and the dialog still up.
    var naming by remember { mutableStateOf<Pair<LabelKind, String>?>(null) }
    val members = orderedForNavigation(positions, ownMemberId)
    var focus by remember { mutableStateOf<MapFocus?>(null) }
    val follow = rememberFollowState()
    var mapHelpOpen by remember { mutableStateOf(false) }
    var groupsOpen by remember { mutableStateOf(false) }
    var leaveAsk by remember { mutableStateOf<LeaveAsk?>(null) }
    val switcher = switcherShown(groups.choices.size)
    val outside =
        standingIn(settings.roster, ownMemberId).let {
            it == Standing.Ungrouped || it == Standing.Outsider
        }
    val removed = removedGroup(groups, settings, outside)
    // A project the device reaches with no group of its own in it yet, told
    // only once the database has answered: before that the roster is null
    // for every member, and the card would flash on each start.
    val noGroup = removed == null && outside && settings.rosterAnswered
    // Kept across a recreated screen, as the follow mode is: turning the
    // device or a change of the system theme would otherwise close the card
    // somebody was reading.
    var showAll by rememberSaveable { mutableStateOf(false) }
    var openedPositionId by rememberSaveable { mutableStateOf<String?>(null) }

    // Kept across cards: a member who opened the details wants them on the
    // next card too, and one who closed them opened the card for the summary.
    var detailsOpen by rememberSaveable { mutableStateOf(false) }
    // The positions under one marker, held by id for the same reason a single
    // one is: the list keeps arriving, and the card has to follow it.
    var openedGroup by rememberSaveable(stateSaver = PositionIdsSaver) {
        mutableStateOf(emptyList())
    }
    // Where the buttons of the strip and the map came out across the screen:
    // a card stands over the button of the member it is about.
    val stripCentres = remember { mutableStateMapOf<String, Float>() }
    var mapLeft by remember { mutableFloatStateOf(0f) }
    // Where the row of buttons over the map ends, for following everybody
    // to keep the top pin below it.
    var topCover by remember { mutableStateOf(0) }
    val panels = rememberOpenPanels()
    UpdateEntry(panels)
    // Held by id rather than by value: the position keeps arriving, and a card
    // showing the one from when it was opened would quietly go stale.
    val opened = positions.firstOrNull { it.positionId == openedPositionId }
    val group =
        openedGroup.mapNotNull { positionId ->
            positions.firstOrNull { it.positionId == positionId }
        }
    val ownName = stringResource(R.string.map_member_you)
    val lookingNames = lookingNames(watchers, now, profiles, ownMemberId, ownName)
    // Read through the resources rather than through a composable, because the
    // map asks for this text while it is drawing, with a count it works out
    // itself from the members standing together.
    val resources = LocalContext.current.resources
    val groupLabel = { count: Int ->
        resources.getQuantityString(R.plurals.map_group_count, count, count)
    }
    // Loaded here rather than inside the map: the map draws bitmaps, and
    // fetching a picture is the screen's business, not the drawing's.
    val faces = rememberMemberFaces(profiles)
    val named = shownNames(positions, profiles, ownMemberId, ownName)
    val people = named.people
    val names = named.markers

    fun noteFollowTarget(positionId: String) {
        val memberId = positions.firstOrNull { it.positionId == positionId }?.memberId

        follow.note(positionId, names[positionId] ?: memberId?.let(::shortName) ?: positionId)
    }

    // A tap is a request to look at somebody, not a state: tapping the same
    // member twice has to bring the map back both times, and the counter is
    // what makes the second tap a change the map can see.
    fun lookAt(positionId: String) {
        focus = MapFocus(positionId = positionId, request = (focus?.request ?: 0) + 1)
        openedPositionId = positionId
        openedGroup = emptyList()
        noteFollowTarget(positionId)
    }

    fun pickFollow(picked: FollowMode) {
        follow.pick(
            picked,
            follow.target?.let { targetPosition(positions, it.positionId) != null } ?: false,
        )
    }

    // The way back out of whatever is open, in the order things were opened
    // in: the strip's own wait first, because it is the most recent thing a
    // press could have meant to cancel, then the card, then the settings
    // screen, and the application itself only when nothing of ours is left
    // on screen. Without these the gesture leaves the application from a
    // settings screen, which is not what anybody means by it.
    BackHandler(enabled = follow.awaitingTarget) { follow.awaitingTarget = false }
    SettingsBackHandlers(panels)
    BackHandler(enabled = !panels.settings && (opened != null || group.isNotEmpty())) {
        openedPositionId = null
        openedGroup = emptyList()
    }
    BackHandler(enabled = panels.members && !panels.settings) { panels.members = false }

    Column(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier =
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .onGloballyPositioned { mapLeft = it.positionInRoot().x },
        ) {
            // The member the card is about, or the first of a list in the
            // order the strip shows them.
            val about =
                openedPositionId
                    ?: members.firstOrNull { member ->
                        group.any {
                            it.positionId ==
                                member.positionId
                        }
                    }?.positionId
            val placed = cardPlace(about, stripCentres, mapLeft)

            MemberMap(
                positions = positions,
                names = names,
                faces = faces,
                ownMemberId = ownMemberId,
                now = now,
                soughtPhones = soughtPhones,
                // The tiles follow the screen they are looked at on: a white
                // map under a dark card at night is a lamp in the face, which
                // is the reason the page turns its own canvas over.
                dark = MaterialTheme.colorScheme.surface.luminance() < HALF_LIT,
                groupLabel = groupLabel,
                focus = focus,
                selected = openedPositionId,
                followMode = follow.mode,
                followTarget = follow.target?.positionId,
                onPausedChange = { follow.paused = it },
                topCover = topCover,
                onSelect = { positionId ->
                    openedPositionId = positionId
                    openedGroup = emptyList()
                    noteFollowTarget(positionId)
                },
                onSelectGroup = { positionIds ->
                    openedPositionId = null
                    openedGroup = positionIds
                },
                onDismiss = {
                    openedPositionId = null
                    openedGroup = emptyList()
                },
                modifier = Modifier.fillMaxSize(),
            )

            removed?.let { RemovedScreen(removed = it, switcher = switcher) }

            if (noGroup) {
                NoGroupScreen(
                    switcher = switcher,
                    onFoundGroup = actions.onFoundGroup,
                    onJoin = groups.onJoin,
                    readProjectMark = actions.onReadProjectMark,
                )
            }

            MapTopRow(
                settings = settings,
                actions = actions,
                follow =
                FollowControl(
                    mode = follow.mode,
                    paused = follow.paused,
                    targetName = follow.target?.name,
                    targetAvailable =
                    follow.target?.let { targetPosition(positions, it.positionId) != null } ?: true,
                    changeCount = follow.changes,
                    onPick = ::pickFollow,
                ),
                onHelp = { mapHelpOpen = true },
                // Only while there is a group to list: the device taken off
                // it, or never in one, has no strip either.
                onMembers = { panels.members = true }.takeIf { removed == null && !noGroup },
                onTopCover = { topCover = it },
            )

            if (mapHelpOpen) {
                MapHelpSheet(onClose = { mapHelpOpen = false })
            }

            MapGroupControls(
                switcher = switcher,
                groups = groups,
                groupsOpen = groupsOpen,
                onGroupsOpen = { groupsOpen = it },
                leaveAsk = leaveAsk,
                onLeaveAsk = { leaveAsk = it },
            )

            AccountButton(
                displayName = ownDisplayName,
                pictureUrl = ownPictureUrl,
                onClick = { panels.settings = true },
                updateAvailable = UpdateBoard.found != null,
                modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp),
            )

            MapBanners(
                searches = searches,
                now = now,
                ownMemberId = ownMemberId,
                ownPhoneId = ownPhoneId,
                ownName = ownName,
                names = names,
                profiles = profiles,
                actions = actions,
                switcher = switcher,
            )

            if (opened != null) {
                OpenedMemberCard(
                    opened = opened,
                    name = people[opened.positionId] ?: shortName(opened.memberId),
                    face = faces[opened.memberId],
                    positions = positions,
                    searches = searches,
                    now = now,
                    profiles = profiles,
                    own = OwnSide(ownMemberId, ownPhoneId, ownName, runsTheGroup),
                    settings = settings,
                    actions = actions,
                    detailsOpen = detailsOpen,
                    onDetailsOpen = { detailsOpen = it },
                    onClose = { openedPositionId = null },
                    onNaming = { kind, key -> naming = kind to key },
                    // Against the strip rather than floating over the map: the
                    // card is a sheet pulled up out of it, over the button of
                    // the member it is about.
                    modifier = placed,
                )
            } else if (group.size > 1) {
                GroupDetail(
                    members = group,
                    names = names,
                    faces = faces,
                    now = now,
                    searches = searches,
                    onSelect = { positionId ->
                        openedPositionId = positionId
                        openedGroup = emptyList()
                        noteFollowTarget(positionId)
                    },
                    onClose = { openedGroup = emptyList() },
                    modifier = placed.padding(12.dp),
                )
            }
        }

        // Nothing below the card that says the device was taken off, or that
        // it has no group yet: the strip would go on counting members of a
        // group it does not see.
        if (removed == null && !noGroup) {
            HorizontalDivider()

            BelowTheMap(
                lookingNames = lookingNames,
                members = members,
                names = names,
                faces = faces,
                // Whoever the open card is about: one member, or everybody under a
                // marker whose list is showing.
                openCards =
                openedPositionId?.let { setOf(it) } ?: group.map { it.positionId }.toSet(),
                now = now,
                searches = searches,
                showAll = showAll,
                awaitingTarget = follow.awaitingTarget,
                outOfReach = outOfReach,
                onSelect = ::lookAt,
                onShowAll = { showAll = it },
                onCentre = { positionId, x -> stripCentres[positionId] = x },
            )
        }
    }

    // Over the strip as well as the map: the strip's own few are in the list
    // too, and a list that left them showing below would read as two lists.
    if (panels.members && removed == null && !noGroup) {
        MembersScreen(
            members = members,
            positions = positions,
            ownMemberId = ownMemberId,
            names = names,
            faces = faces,
            now = now,
            searches = searches,
            onSelect = { positionId ->
                panels.members = false
                lookAt(positionId)
            },
            onClose = { panels.members = false },
        )
    }

    if (panels.settings) {
        SettingsLayers(
            panels = panels,
            settings = settings,
            actions = actions,
            profiles = profiles,
            ownMemberId = ownMemberId,
            ownPhoneId = ownPhoneId,
            labels = labels,
            now = now,
            searches = searches,
            positions = positions,
            groups = groups,
            switcher = switcher,
            runsTheGroup = runsTheGroup,
            removed = removed,
            onLeaveAsk = { leaveAsk = it },
        )
    }

    naming?.let { (kind, key) ->
        NamingDialog(kind, key, labels, onLabel, onDone = { naming = null })
    }
}
