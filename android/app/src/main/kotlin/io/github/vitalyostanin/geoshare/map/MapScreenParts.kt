package io.github.vitalyostanin.geoshare.map

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.collection.Watcher
import io.github.vitalyostanin.geoshare.collection.watchingMembers
import io.github.vitalyostanin.geoshare.membership.Leave
import io.github.vitalyostanin.geoshare.membership.howToLeave
import io.github.vitalyostanin.geoshare.update.UpdateBoard
import java.time.Instant
import kotlin.math.roundToInt

/**
 * The gap between the controls in the row over the map; wider than the count
 * on the sources button sticks out past its circle.
 */
private val TOP_BUTTONS_GAP = 12.dp

/**
 * The screens the map opens over itself, each kept across a recreated screen:
 * turning the device or a change of the system theme would otherwise drop the
 * member back on the map from whatever they were reading.
 */
internal class OpenPanels(
    settingsState: MutableState<Boolean>,
    adminState: MutableState<Boolean>,
    myPhonesState: MutableState<Boolean>,
    accuracyState: MutableState<Boolean>,
    licencesState: MutableState<Boolean>,
    reportState: MutableState<Boolean>,
    membersState: MutableState<Boolean>,
) {
    var settings by settingsState

    // Opened from within the settings and drawn over them, so that closing it
    // puts the member back where they were rather than on the map.
    var admin by adminState
    var myPhones by myPhonesState
    var accuracy by accuracyState
    var licences by licencesState
    var report by reportState

    /** The list of every member, over the map and the strip. */
    var members by membersState

    /** Whether one of the screens drawn over the settings is open. */
    val overSettings: Boolean get() = admin || myPhones || accuracy || licences || report
}

@Composable
internal fun rememberOpenPanels(): OpenPanels = OpenPanels(
    settingsState = rememberSaveable { mutableStateOf(false) },
    adminState = rememberSaveable { mutableStateOf(false) },
    myPhonesState = rememberSaveable { mutableStateOf(false) },
    accuracyState = rememberSaveable { mutableStateOf(false) },
    licencesState = rememberSaveable { mutableStateOf(false) },
    reportState = rememberSaveable { mutableStateOf(false) },
    membersState = rememberSaveable { mutableStateOf(false) },
)

/**
 * Opens the settings when the update notification was tapped
 * ([UpdateBoard.openRequested]), where the update block is. The request is
 * cleared once acted on, so that a recreated screen does not open them again.
 */
@Composable
internal fun UpdateEntry(panels: OpenPanels) {
    LaunchedEffect(UpdateBoard.openRequested) {
        if (UpdateBoard.openRequested) {
            panels.settings = true
            UpdateBoard.openRequested = false
        }
    }
}

/**
 * The back gesture over the settings: whatever is drawn over them closes
 * first, then the settings themselves.
 */
@Composable
internal fun SettingsBackHandlers(panels: OpenPanels) {
    BackHandler(enabled = panels.admin) { panels.admin = false }
    BackHandler(enabled = panels.myPhones) { panels.myPhones = false }
    BackHandler(enabled = panels.accuracy) { panels.accuracy = false }
    BackHandler(enabled = panels.licences) { panels.licences = false }
    BackHandler(enabled = panels.report) { panels.report = false }
    BackHandler(enabled = panels.settings && !panels.overSettings) { panels.settings = false }
}

/**
 * The group shown, when an administrator has taken this device off it, or
 * when the invitation it came with was refused and it never got in: the map
 * and the settings both say so, in the same words.
 */
internal fun removedGroup(
    groups: GroupSwitch,
    settings: SettingsState,
    outside: Boolean,
): RemovedGroup? = groups.choices
    .firstOrNull { shown ->
        shown.projectId == groups.selected &&
            (shown.removed || (outside && settings.refusedJoin?.projectId == shown.projectId))
    }
    ?.let { shown ->
        RemovedGroup(
            caption = shown.caption,
            onDrop = {
                groups.onLeave(LeaveAsk(shown.projectId, shown.caption, Leave.NOT_ON_ROSTER))
            },
            onJoin = groups.onJoin,
            // A refusal is the newer word: one removed and then turned
            // down by a spent link is told about the link.
            refused = settings.refusedJoin?.takeIf { it.projectId == shown.projectId },
        )
    }

/** What the follow button shows and how a pick from its menu is answered. */
internal class FollowControl(
    val mode: FollowMode,
    val paused: Boolean,
    val targetName: String?,
    val targetAvailable: Boolean,
    val changeCount: Int,
    val onPick: (FollowMode) -> Unit,
)

/**
 * The controls on one side of the top of the map, the way map applications
 * lay them out: a row keeps a second control from ever landing on top of the
 * first whatever either measures. [onMembers] is null while there is no group
 * to list.
 */
@Composable
internal fun BoxScope.MapTopRow(
    settings: SettingsState,
    actions: SettingsActions,
    follow: FollowControl,
    onHelp: () -> Unit,
    onMembers: (() -> Unit)?,
    onTopCover: (Int) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(TOP_BUTTONS_GAP),
        verticalAlignment = Alignment.CenterVertically,
        // The map is drawn to the edges of the window on purpose; what stands
        // over it is not, or the clock is what the member taps.
        modifier =
        Modifier
            .align(Alignment.TopStart)
            .onGloballyPositioned { onTopCover(it.boundsInParent().bottom.roundToInt()) }
            .statusBarsPadding()
            .padding(12.dp),
    ) {
        SourcePicker(
            leftOut = settings.sourcesLeftOut,
            onSourceUseChange = actions.onSourceUseChange,
        )

        FollowButton(
            mode = follow.mode,
            paused = follow.paused,
            targetName = follow.targetName,
            targetAvailable = follow.targetAvailable,
            changeCount = follow.changeCount,
            onPick = follow.onPick,
        )

        MapRoundButton(
            icon = R.drawable.ic_help,
            label = stringResource(R.string.help_open),
            onClick = onHelp,
        )

        onMembers?.let {
            MapRoundButton(
                icon = R.drawable.ic_members,
                label = stringResource(R.string.map_members),
                onClick = it,
            )
        }
    }
}

/**
 * The chip that names the group shown, the list it opens, and the question
 * asked before leaving one of them.
 */
@Composable
internal fun BoxScope.MapGroupControls(
    switcher: Boolean,
    groups: GroupSwitch,
    groupsOpen: Boolean,
    onGroupsOpen: (Boolean) -> Unit,
    leaveAsk: LeaveAsk?,
    onLeaveAsk: (LeaveAsk?) -> Unit,
) {
    if (switcher) {
        GroupChip(
            caption = shownCaption(groups),
            onClick = { onGroupsOpen(true) },
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding(),
        )
    }

    if (groupsOpen) {
        GroupSheet(
            choices = groups.choices,
            selected = groups.selected,
            onSelect = { projectId ->
                onGroupsOpen(false)
                groups.onSelect(projectId)
            },
            onJoin = {
                onGroupsOpen(false)
                groups.onJoin()
            },
            onLeave = { choice ->
                onGroupsOpen(false)
                onLeaveAsk(LeaveAsk(choice.projectId, choice.caption, choice.leave))
            },
            onClose = { onGroupsOpen(false) },
        )
    }

    leaveAsk?.let { ask ->
        LeaveGroupDialog(
            ask = ask,
            onLeave = { groups.onLeave(ask) },
            onDismiss = { onLeaveAsk(null) },
        )
    }
}

/** What the group shown is called, or its project id while it has no caption. */
internal fun shownCaption(groups: GroupSwitch): String =
    groups.choices.firstOrNull { it.projectId == groups.selected }?.caption ?: groups.selected

/**
 * The banners about this device's own searches and siren.
 *
 * Below the row of round buttons and the group chip rather than on top of
 * them: the two are read in different moments, and a banner covering the
 * buttons would be in the way of the one thing this screen is for.
 */
@Composable
internal fun BoxScope.MapBanners(
    searches: Map<String, Search>,
    now: Instant,
    ownMemberId: String,
    ownPhoneId: String?,
    ownName: String,
    names: Map<String, String>,
    profiles: Map<String, MemberProfile>,
    actions: SettingsActions,
    switcher: Boolean,
) {
    val placed =
        Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = bannerOffset(switcher), start = 12.dp, end = 12.dp)

    OwnBanners(
        searches = searches,
        now = now,
        ownMemberId = ownMemberId,
        names = names,
        profiles = profiles,
        ownName = ownName,
        onFound = actions.onEndSearch,
        modifier = placed,
    )

    OwnSirenBanner(
        ownPhoneId = ownPhoneId,
        actions = actions,
        profiles = profiles,
        ownMemberId = ownMemberId,
        ownName = ownName,
        modifier = placed,
    )
}

/**
 * The settings screen and whatever is opened over it.
 *
 * With one group there is no list to leave it from, so the settings offer
 * it; with more, the list does, for any of them.
 */
@Composable
internal fun SettingsLayers(
    panels: OpenPanels,
    settings: SettingsState,
    actions: SettingsActions,
    profiles: Map<String, MemberProfile>,
    ownMemberId: String,
    ownPhoneId: String?,
    labels: Labels,
    now: Instant,
    searches: Map<String, Search>,
    positions: List<MemberPosition>,
    groups: GroupSwitch,
    switcher: Boolean,
    runsTheGroup: Boolean,
    removed: RemovedGroup?,
    onLeaveAsk: (LeaveAsk) -> Unit,
) {
    SettingsScreen(
        settings = settings,
        actions = actions,
        profiles = profiles,
        ownMemberId = ownMemberId,
        now = now,
        onClose = { panels.settings = false },
        onOpenAdmin = { panels.admin = true },
        onOpenMyPhones = { panels.myPhones = true },
        onOpenAccuracy = { panels.accuracy = true },
        onOpenLicences = { panels.licences = true },
        onOpenReport = { panels.report = true },
        onLeaveGroup =
        {
            onLeaveAsk(
                LeaveAsk(
                    projectId = groups.selected,
                    caption = groups.choices.firstOrNull()?.caption ?: groups.selected,
                    how = howToLeave(settings.roster, ownMemberId),
                ),
            )
        }.takeUnless { switcher },
        groupCaption = shownCaption(groups),
        severalGroups = switcher,
        removed = removed,
    )

    OverTheSettings(
        myPhonesOpen = panels.myPhones,
        accuracyOpen = panels.accuracy,
        licencesOpen = panels.licences,
        reportOpen = panels.report,
        adminOpen = panels.admin,
        runsTheGroup = runsTheGroup,
        settings = settings,
        actions = actions,
        profiles = profiles,
        ownMemberId = ownMemberId,
        ownPhoneId = ownPhoneId,
        labels = labels,
        now = now,
        searches = searches,
        seenAt = positions.associate { it.positionId to it.knownAt },
        groups = groups,
        onCloseMyPhones = { panels.myPhones = false },
        onCloseAccuracy = { panels.accuracy = false },
        onCloseLicences = { panels.licences = false },
        onCloseReport = { panels.report = false },
        onCloseAdmin = { panels.admin = false },
    )
}

/** The dialog that gives a device or a person a name for this member alone. */
@Composable
internal fun NamingDialog(
    kind: LabelKind,
    key: String,
    labels: Labels,
    onLabel: (LabelKind, String, String?) -> Unit,
    onDone: () -> Unit,
) {
    val phone = kind == LabelKind.PHONE
    val shelf = if (phone) labels.phones else labels.members

    NameDialog(
        title = stringResource(if (phone) R.string.name_phone else R.string.name_person),
        note = stringResource(R.string.name_for_you),
        current = shelf[key],
        onSave = { name ->
            onLabel(kind, key, name)
            onDone()
        },
        onClose = onDone,
    )
}

/**
 * The card about one member's device, with what this device may do about it:
 * reach it, say it is found, name it, share back.
 */
@Composable
internal fun OpenedMemberCard(
    opened: MemberPosition,
    name: String,
    face: Bitmap?,
    positions: List<MemberPosition>,
    searches: Map<String, Search>,
    now: Instant,
    profiles: Map<String, MemberProfile>,
    own: OwnSide,
    settings: SettingsState,
    actions: SettingsActions,
    detailsOpen: Boolean,
    onDetailsOpen: (Boolean) -> Unit,
    onClose: () -> Unit,
    onNaming: (LabelKind, String) -> Unit,
    modifier: Modifier,
) {
    val search = searches[opened.positionId]?.takeIf { it.stands(now) }

    MemberDetail(
        member = opened,
        name = name,
        face = face,
        now = now,
        away = distanceFromViewer(opened, positions, own.memberId, now),
        search = search,
        openedByName =
        search?.let {
            memberName(
                profile = profiles[it.openedBy],
                memberId = it.openedBy,
                own = it.openedBy == own.memberId,
                ownName = own.name,
                unnamed = unnamedMember(it.openedBy),
            )
        },
        runsTheGroup = own.runsTheGroup,
        onFound = { actions.onEndSearch(opened.positionId) },
        detailsOpen = detailsOpen,
        onDetailsOpen = onDetailsOpen,
        onClose = onClose,
        onNamePhone = { onNaming(LabelKind.PHONE, opened.positionId) },
        // Oneself is named on the account, not here.
        onNamePerson =
        { onNaming(LabelKind.MEMBER, opened.memberId) }
            .takeIf { opened.memberId != own.memberId },
        onShareBack =
        { span: ShareSpan -> actions.onShare(opened.memberId, span) }
            .takeIf { offersShareBack(opened.memberId, own.memberId, settings.sharedWith, now) },
        reach = openedReach(opened, name, own, actions),
        modifier = modifier,
    )
}

/** Who is looking at the map: the member, this device, and whether it runs the group. */
internal class OwnSide(
    val memberId: String,
    val phoneId: String?,
    val name: String,
    val runsTheGroup: Boolean,
)

/** Resetting the device's place and sounding its siren, where the card offers them. */
@Composable
private fun openedReach(
    opened: MemberPosition,
    memberName: String,
    own: OwnSide,
    actions: SettingsActions,
): DeviceReach? {
    if (!offersReach(
            opened.positionId,
            opened.memberId,
            own.phoneId,
            own.memberId,
            own.runsTheGroup,
        )
    ) {
        return null
    }

    val unnamedDevice = stringResource(R.string.unnamed_device)

    return rememberDeviceReach(
        phone =
        PhoneLine(
            phoneId = opened.positionId,
            name = opened.deviceName?.takeIf { it.isNotBlank() } ?: unnamedDevice,
            onlineAt = null,
            batteryPercent = null,
            charging = false,
        ),
        memberName = memberName,
        actions = actions,
    )
}

/**
 * Where a card stands: against the strip, over the button of [about], or over
 * the button for everybody else when [about] is off the strip.
 *
 * Half the room above the strip at most: a card is a heading and five rows,
 * and in a sideways window it would otherwise stand taller than the map it is
 * a card about.
 */
internal fun BoxWithConstraintsScope.cardPlace(
    about: String?,
    stripCentres: Map<String, Float>,
    mapLeft: Float,
): Modifier {
    val cardWidth = minOf(CARD_MAX_WIDTH, maxWidth - CARD_MARGIN * 2)
    val room = constraints.maxWidth.toFloat()
    val centre = about?.let { stripCentres[it] } ?: stripCentres[MORE_ON_STRIP]

    return Modifier
        .align(Alignment.BottomStart)
        .width(cardWidth)
        .offset {
            val left =
                cardLeft(
                    centre = centre?.minus(mapLeft) ?: (room / 2),
                    cardWidth = cardWidth.toPx(),
                    room = room,
                    margin = CARD_MARGIN.toPx(),
                )

            IntOffset(left.roundToInt(), 0)
        }
        .heightIn(max = maxHeight / 2)
}

/**
 * Who else has the map open, named rather than counted. The card used to
 * carry this as the reason for the mode it was in — "somebody is looking" —
 * which said that the household was being watched without saying by whom,
 * and said it on every card at once, because a single open map puts every
 * device of the group into the same mode.
 */
@Composable
internal fun lookingNames(
    watchers: List<Watcher>,
    now: Instant,
    profiles: Map<String, MemberProfile>,
    ownMemberId: String,
    ownName: String,
): List<String> = watchingMembers(watchers, now).map { member ->
    memberName(
        profile = profiles[member],
        memberId = member,
        own = member == ownMemberId,
        ownName = ownName,
        unnamed = unnamedMember(member),
    )
}
