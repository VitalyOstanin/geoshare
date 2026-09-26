package io.github.vitalyostanin.geoshare.map

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.collection.JumpReason
import io.github.vitalyostanin.geoshare.collection.Watcher
import io.github.vitalyostanin.geoshare.collection.foundByLabel
import io.github.vitalyostanin.geoshare.collection.leftOutReasonLabel
import io.github.vitalyostanin.geoshare.collection.modeLabel
import io.github.vitalyostanin.geoshare.collection.modesExplained
import io.github.vitalyostanin.geoshare.collection.reasonsExplained
import io.github.vitalyostanin.geoshare.collection.sourceChoiceLabel
import io.github.vitalyostanin.geoshare.collection.sourceLeftOut
import io.github.vitalyostanin.geoshare.collection.watchingMembers
import io.github.vitalyostanin.geoshare.membership.Roster
import io.github.vitalyostanin.geoshare.membership.Standing
import io.github.vitalyostanin.geoshare.membership.standingIn
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** How often the ages on screen are recounted. Half a minute keeps "5 minutes ago" honest. */
private const val AGE_TICK_MILLIS = 30_000L

/** Green for a charge that is growing, one shade for a pale surface and one for a dark one. */
private val CHARGING_DAY = Color(0xFF2E7D32)
private val CHARGING_NIGHT = Color(0xFF81C784)

/**
 * Ochre for an answer wider than a hundred metres, in the two shades again.
 *
 * Ochre rather than red: a wide answer is not a fault, it is a claim about a
 * block instead of about a doorway, and the colour is there to be noticed
 * rather than acted on.
 */
private val WIDE_DAY = Color(0xFFA85A17)
private val WIDE_NIGHT = Color(0xFFD99149)

/** Above this a surface counts as pale, below it as dark. */
private const val HALF_LIT = 0.5f

/** The circle a card carries: large enough for a face, small enough for a strip of cards. */
private val FACE_SIZE = 22.dp

/** What takes the colour out of the picture of a member whose position is old. */
private val GREYSCALE = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** As much of the screen as the full list may take before it starts scrolling instead. */
private val LIST_MAX_HEIGHT = 220.dp

/** The gap between the account button and the follow button stacked under it. */
private val TOP_BUTTONS_GAP = 8.dp

/**
 * Who the follow button's [FollowMode.ONE] centres on.
 *
 * The name is captured at the moment the member's card is opened rather than
 * looked up again from [MemberPosition] each time the button is drawn: once
 * the member's position drops out of the positions this phone is being shown,
 * there is nowhere left to look the name up from, and the button still has to
 * say whose card it was.
 */
private data class FollowTarget(val positionId: String, val name: String)

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
    /** How many members' positions this phone is being refused, if any. */
    outOfReach: Int,
    /** What each member calls themselves, by member id; absent until they sign in. */
    profiles: Map<String, MemberProfile>,
    /** Who else has the map open, as they themselves claim. */
    watchers: List<Watcher>,
    ownMemberId: String,
    ownDisplayName: String?,
    ownPictureUrl: String?,
    /** Everything the settings screen shows, which the map only carries through. */
    settings: SettingsState,
    /** What the settings screen can ask for, likewise. */
    actions: SettingsActions,
    /** What this member calls the phones and the people, for themselves. */
    labels: Labels,
    /** Gives a phone or a person a name for this member alone, or takes it away. */
    onLabel: (LabelKind, String, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val now by currentTime()
    // The name being asked for, held by what it names rather than by the card:
    // the card may be closed and the dialog still up.
    var naming by remember { mutableStateOf<Pair<LabelKind, String>?>(null) }
    val members = orderedForNavigation(positions, ownMemberId)
    var focus by remember { mutableStateOf<MapFocus?>(null) }
    var followMode by remember { mutableStateOf(FollowMode.NONE) }
    var followTarget by remember { mutableStateOf<FollowTarget?>(null) }
    var showAll by remember { mutableStateOf(false) }
    var openedPositionId by remember { mutableStateOf<String?>(null) }

    // Kept across cards: a member who opened the details wants them on the
    // next card too, and one who closed them opened the card for the summary.
    var detailsOpen by rememberSaveable { mutableStateOf(false) }
    // The positions under one marker, held by id for the same reason a single
    // one is: the list keeps arriving, and the card has to follow it.
    var openedGroup by remember { mutableStateOf<List<String>>(emptyList()) }
    var settingsOpen by remember { mutableStateOf(false) }
    // Opened from within the settings and drawn over them, so that closing it
    // puts the member back where they were rather than on the map.
    var adminOpen by remember { mutableStateOf(false) }
    // Held by id rather than by value: the position keeps arriving, and a card
    // showing the one from when it was opened would quietly go stale.
    val opened = positions.firstOrNull { it.positionId == openedPositionId }
    val group =
        openedGroup.mapNotNull { positionId ->
            positions.firstOrNull { it.positionId == positionId }
        }
    val ownName = stringResource(R.string.map_member_you)
    // Who else has the map open, named rather than counted. The card used to
    // carry this as the reason for the mode it was in — "somebody is looking" —
    // which said that the household was being watched without saying by whom,
    // and said it on every card at once, because a single open map puts every
    // phone of the group into the same mode.
    val lookingNames =
        watchingMembers(watchers, now).map { member ->
            memberName(
                profile = profiles[member],
                memberId = member,
                own = member == ownMemberId,
                ownName = ownName,
            )
        }
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
    // Worked out here rather than inside the map: the fallbacks come from the
    // resources, and the map draws pictures, not strings from a resource file.
    // A member carrying two phones gets two markers, and a pair of markers with
    // the same name on them says nothing about which phone is where; the name
    // of the phone is added only then, because most members have one.
    val carried = positions.groupingBy { it.memberId }.eachCount()
    val people =
        positions.associate { position ->
            position.positionId to
                memberName(
                    profile = profiles[position.memberId],
                    memberId = position.memberId,
                    own = position.memberId == ownMemberId,
                    ownName = ownName,
                )
        }
    val names =
        positions.associate { position ->
            val name = people.getValue(position.positionId)
            val device = position.deviceName

            position.positionId to
                if (carried.getOrDefault(position.memberId, 1) > 1 && !device.isNullOrBlank()) {
                    "$name · $device"
                } else {
                    name
                }
        }

    // Named at the place a member's card is opened, from whichever of the
    // three ways it happened to open: a tap on the strip, a tap on the
    // member's own marker, or a pick out of a group's list. All three end up
    // here so that the follow button's ONE mode always has a name for
    // whoever the last card was about, not only for the strip's own taps.
    fun noteFollowTarget(positionId: String) {
        val memberId = positions.firstOrNull { it.positionId == positionId }?.memberId

        followTarget =
            FollowTarget(
                positionId = positionId,
                name = names[positionId] ?: memberId?.let(::shortName) ?: positionId,
            )
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

    // The way back out of whatever is open, in the order things were opened
    // in: the card first, the settings screen after it, and the application
    // itself only when nothing of ours is left on screen. Without these the
    // gesture leaves the application from a settings screen, which is not what
    // anybody means by it.
    BackHandler(enabled = adminOpen) { adminOpen = false }
    BackHandler(enabled = settingsOpen && !adminOpen) { settingsOpen = false }
    BackHandler(enabled = !settingsOpen && (opened != null || group.isNotEmpty())) {
        openedPositionId = null
        openedGroup = emptyList()
    }

    Column(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // Half the room above the strip: a card is a heading and five
            // rows, and in a sideways window it would otherwise stand taller
            // than the map it is a card about.
            val cardHeight = maxHeight / 2
            MemberMap(
                positions = positions,
                names = names,
                faces = faces,
                ownMemberId = ownMemberId,
                now = now,
                // The tiles follow the screen they are looked at on: a white
                // map under a dark card at night is a lamp in the face, which
                // is the reason the page turns its own canvas over.
                dark = MaterialTheme.colorScheme.surface.luminance() < HALF_LIT,
                groupLabel = groupLabel,
                focus = focus,
                selected = openedPositionId,
                followMode = followMode,
                followTarget = followTarget?.positionId,
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

            SourcePicker(
                leftOut = settings.sourcesLeftOut,
                onSourceUseChange = actions.onSourceUseChange,
                modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp),
            )

            // Stacked in one corner rather than each finding its own: the two
            // buttons are the only controls drawn over the map, and a column
            // keeps a second one from ever landing on top of the first
            // whatever either of them turns out to measure.
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(TOP_BUTTONS_GAP),
                // The map is drawn to the edges of the window on purpose; what
                // stands over it is not, or the clock is what the member taps.
                modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp),
            ) {
                AccountButton(
                    displayName = ownDisplayName,
                    pictureUrl = ownPictureUrl,
                    onClick = { settingsOpen = true },
                )

                FollowButton(
                    mode = followMode,
                    targetName = followTarget?.name,
                    targetAvailable =
                    followTarget?.let { targetPosition(positions, it.positionId) != null }
                        ?: true,
                    onClick = {
                        val hasTarget =
                            followTarget?.let { targetPosition(positions, it.positionId) != null }
                                ?: false

                        followMode = nextMode(followMode, hasTarget)
                    },
                )
            }

            if (opened != null) {
                MemberDetail(
                    member = opened,
                    name = people[opened.positionId] ?: shortName(opened.memberId),
                    face = faces[opened.memberId],
                    now = now,
                    away = distanceFromViewer(opened, positions, ownMemberId, now),
                    detailsOpen = detailsOpen,
                    onToggleDetails = { detailsOpen = !detailsOpen },
                    onClose = { openedPositionId = null },
                    onNamePhone = { naming = LabelKind.PHONE to opened.positionId },
                    // Oneself is named on the account, not here.
                    onNamePerson =
                    { naming = LabelKind.MEMBER to opened.memberId }
                        .takeIf { opened.memberId != ownMemberId },
                    modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .heightIn(max = cardHeight)
                        .padding(12.dp),
                )
            } else if (group.size > 1) {
                GroupDetail(
                    members = group,
                    names = names,
                    faces = faces,
                    now = now,
                    onSelect = { positionId ->
                        openedPositionId = positionId
                        openedGroup = emptyList()
                        noteFollowTarget(positionId)
                    },
                    onClose = { openedGroup = emptyList() },
                    modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .heightIn(max = cardHeight)
                        .padding(12.dp),
                )
            }
        }

        HorizontalDivider()

        // Above the buttons rather than on a card: an open map is a fact about
        // the household, not about the member whose card happens to be open,
        // and a member with no position of their own still watches.
        if (lookingNames.isNotEmpty()) {
            Text(
                text = stringResource(R.string.map_watching, lookingNames.joinToString(", ")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        if (members.isEmpty()) {
            Text(
                text =
                if (outOfReach > 0) {
                    pluralStringResource(R.plurals.map_out_of_reach, outOfReach, outOfReach)
                } else {
                    stringResource(R.string.map_nobody_yet)
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.navigationBarsPadding().padding(16.dp),
            )
        } else {
            MemberButtons(
                members = members,
                names = names,
                faces = faces,
                // Whoever the open card is about: one member, or everybody
                // under a marker whose list is showing.
                openCards =
                openedPositionId?.let { setOf(it) }
                    ?: group.map { it.positionId }.toSet(),
                now = now,
                showAll = showAll,
                onSelect = ::lookAt,
                onToggleAll = { showAll = !showAll },
                modifier = Modifier.navigationBarsPadding(),
            )

            // A member whose position is refused simply has no marker, which
            // reads as a member who is not sharing — and so does a phone that
            // cannot reach the group at all. One line is the difference.
            if (outOfReach > 0) {
                Text(
                    text = pluralStringResource(R.plurals.map_out_of_reach, outOfReach, outOfReach),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                    Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }

    if (settingsOpen) {
        SettingsScreen(
            settings = settings,
            actions = actions,
            profiles = profiles,
            ownMemberId = ownMemberId,
            now = now,
            onClose = { settingsOpen = false },
            onOpenAdmin = { adminOpen = true },
        )
    }

    // Over the settings rather than instead of them: the way back out is the
    // screen it was opened from, whichever of the two ways back is used.
    //
    // Weighed again on every draw rather than only when it was opened: an
    // administrator can stop being one while the panel is up — somebody else
    // takes the role away, or this phone is taken off the roster and the
    // refused subscription empties it — and a panel left standing would go on
    // offering writes that are now refused.
    val runsTheGroup = standingIn(settings.roster, ownMemberId) == Standing.Administrator

    if (settingsOpen && adminOpen && runsTheGroup) {
        AdminPanel(
            roster = settings.roster ?: Roster(),
            profiles = profiles,
            grants = settings.grants,
            ownMemberId = ownMemberId,
            labels = labels,
            now = now,
            invitation = settings.invitation,
            trouble = settings.groupTrouble,
            onMakeInvitation = actions.onMakeInvitation,
            onRemoveMember = actions.onRemoveMember,
            onClose = { adminOpen = false },
        )
    }

    naming?.let { (kind, key) ->
        val phone = kind == LabelKind.PHONE
        val shelf = if (phone) labels.phones else labels.members

        NameDialog(
            title = stringResource(if (phone) R.string.name_phone else R.string.name_person),
            note = stringResource(R.string.name_for_you),
            current = shelf[key],
            onSave = { name ->
                onLabel(kind, key, name)
                naming = null
            },
            onClose = { naming = null },
        )
    }
}

/**
 * The single button that walks the camera through [FollowMode].
 *
 * A press advances the ring; what it lands on next is [nextMode]'s decision,
 * and this composable only reports the mode already chosen. The label is the
 * state itself rather than a caption beside a fixed word, the way
 * [MoreButton] below reads "More" or "Hide" depending on whether the rest of
 * the strip is open.
 */
@Composable
private fun FollowButton(
    mode: FollowMode,
    /** Who [FollowMode.ONE] is centred on, named when their card was opened. */
    targetName: String?,
    /** False once the chosen member's position has dropped off the map entirely. */
    targetAvailable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when (mode) {
        FollowMode.NONE -> stringResource(R.string.map_follow_none)

        FollowMode.ALL -> stringResource(R.string.map_follow_all)

        // targetName is null here only if nextMode was never given the
        // chance to skip ONE for want of a target, which the ring's own
        // rules do not allow; the fallback exists for the type rather than
        // for a case this screen expects to reach.
        FollowMode.ONE ->
            targetName
                ?.let { stringResource(R.string.map_follow_one, it) }
                ?: stringResource(R.string.map_follow_one_unnamed)
    }

    Surface(
        modifier = modifier.clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = if (mode == FollowMode.NONE) 2.dp else 8.dp,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color =
            if (targetAvailable) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun MemberButtons(
    members: List<MemberPosition>,
    names: Map<String, String>,
    /** The account pictures, by member id; a member without one gets their initial. */
    faces: Map<String, Bitmap>,
    /** The positions whose card is open, marked in the strip. */
    openCards: Set<String>,
    now: Instant,
    showAll: Boolean,
    onSelect: (String) -> Unit,
    onToggleAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onStrip = membersOnTheStrip(members.size)
    val shown = members.take(onStrip)
    val rest = members.drop(onStrip)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            shown.forEach { member ->
                MemberButton(
                    member = member,
                    name = names[member.positionId] ?: shortName(member.memberId),
                    face = faces[member.memberId],
                    shown = member.positionId in openCards,
                    now = now,
                    onSelect = onSelect,
                    modifier = Modifier.weight(1f),
                )
            }

            if (rest.isNotEmpty()) {
                MoreButton(
                    count = rest.size,
                    open = showAll,
                    onToggle = onToggleAll,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (showAll && rest.isNotEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = LIST_MAX_HEIGHT)) {
                items(rest, key = { it.positionId }) { member ->
                    MemberRow(
                        member = member,
                        name = names[member.positionId] ?: shortName(member.memberId),
                        face = faces[member.memberId],
                        now = now,
                        onSelect = onSelect,
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberButton(
    member: MemberPosition,
    name: String,
    face: Bitmap?,
    /** Whether the card over the map is about this member. */
    shown: Boolean,
    now: Instant,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable { onSelect(member.positionId) },
        shape = MaterialTheme.shapes.medium,
        // Raised and outlined rather than merely tinted: the strip stands on a
        // surface of its own, and a tint alone was not the difference between
        // the member being looked at and the rest.
        tonalElevation = if (shown) 8.dp else 2.dp,
        border =
        if (shown) {
            BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MemberFace(
                    memberId = member.memberId,
                    name = name,
                    picture = face,
                    stale = isGrey(member, now),
                )

                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // The age on the left and the charge against the right edge: the
            // charges then line up down the strip and can be compared at a
            // glance, which is what they are there for.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AgeText(member = member, now = now, short = true)

                BatteryText(member = member)
            }
        }
    }
}

@Composable
private fun MoreButton(
    count: Int,
    open: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable { onToggle() },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                text = stringResource(if (open) R.string.map_more_hide else R.string.map_more),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )

            Text(
                text = pluralStringResource(R.plurals.map_more_count, count, count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MemberRow(
    member: MemberPosition,
    name: String,
    face: Bitmap?,
    now: Instant,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable { onSelect(member.positionId) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MemberFace(
            memberId = member.memberId,
            name = name,
            picture = face,
            stale = isGrey(member, now),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
            )

            AgeText(member = member, now = now)
        }

        BatteryText(member = member)
    }
}

/**
 * What is left in the phone, and whether it is filling or emptying.
 *
 * Shown on every card rather than only in the one that opens: a marker an hour
 * old is explained by a phone at five percent, and that is the question asked
 * while looking at the panel, not after tapping into it.
 */
@Composable
private fun BatteryText(member: MemberPosition) {
    val percent = member.batteryPercent ?: return

    Text(
        text = stringResource(R.string.map_battery, percent),
        style = MaterialTheme.typography.bodySmall,
        color = batteryColour(member.charging),
    )
}

@Composable
private fun widthText(width: PositionWidth): String = when (width) {
    is PositionWidth.Metres -> stringResource(R.string.map_width_metres, width.count)
    is PositionWidth.Kilometres -> stringResource(R.string.map_width_kilometres, width.count)
}

/** The muted colour of the other figures on the card, and ochre once it is wide. */
@Composable
private fun widthColour(wide: Boolean): Color = when {
    !wide -> MaterialTheme.colorScheme.onSurfaceVariant
    MaterialTheme.colorScheme.surface.luminance() < HALF_LIT -> WIDE_NIGHT
    else -> WIDE_DAY
}

/**
 * The colour the left-out rows are written in: the same ochre a wide answer
 * wears.
 *
 * Ochre rather than red for the same reason as there — what these rows report
 * is not a fault of the phone, and nothing about them is to be acted on in the
 * next minute. What they are for is to stop an old marker being read as a
 * phone that has gone quiet.
 */
@Composable
private fun leftOutColour(): Color = when {
    MaterialTheme.colorScheme.surface.luminance() < HALF_LIT -> WIDE_NIGHT
    else -> WIDE_DAY
}

/**
 * The colour a charge is written in: green while the phone is on a charger.
 *
 * The colour rather than a mark next to the number: a percentage that is
 * growing is the reassuring case, and saying so in the number itself costs the
 * card no room at all.
 *
 * Dark green on a pale surface, pale green on a dark one; which of the two is
 * in front is answered by the surface the theme paints.
 */
@Composable
private fun batteryColour(charging: Boolean): Color = when {
    !charging -> MaterialTheme.colorScheme.onSurfaceVariant
    MaterialTheme.colorScheme.surface.luminance() < HALF_LIT -> CHARGING_NIGHT
    else -> CHARGING_DAY
}

/**
 * Who a card is about: the picture their account carries, ringed in the colour
 * their marker wears, or the first letter of their name until the picture
 * arrives.
 *
 * The same drawing the pin on the map has, so that a card and a marker are
 * recognisably one member; the ring is what the coloured dot used to say.
 */
@Composable
private fun MemberFace(memberId: String, name: String, picture: Bitmap?, stale: Boolean) {
    // Grey the moment the position is old, picture and all: the marker on the
    // map fades the same way, and a card in full colour beside a faded pin
    // reads as a member who is there right now.
    val colour = if (stale) Color(STALE_COLOUR) else Color(memberColour(memberId))

    Box(
        modifier =
        Modifier
            .size(FACE_SIZE)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(width = 2.dp, color = colour, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (picture == null) {
            Text(
                text = memberInitial(name),
                color = colour,
                style = MaterialTheme.typography.labelLarge,
            )
        } else {
            Image(
                bitmap = picture.asImageBitmap(),
                // The name is written next to it, so the picture says nothing
                // a reader of the screen has not been told already.
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = if (stale) GREYSCALE else null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        }
    }
}

@Composable
private fun AgeText(member: MemberPosition, now: Instant, short: Boolean = false) {
    // How old the position is, not how long ago the phone spoke: a phone under
    // a transmitter goes on saying the same position, and an age counted from
    // the stamp would read as a member who is where the marker stands.
    val age = positionAge(member.positionAt, now)

    Text(
        text = if (short) shortAgeText(age) else ageText(age),
        style = MaterialTheme.typography.bodySmall,
        // Grey is the whole point of the stale mark: an old position still
        // shows where the member was, and should not read as where they are.
        color =
        if (isGrey(member, now)) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        maxLines = 1,
    )
}

/**
 * Who is standing under a marker that stands for several members.
 *
 * A list rather than a stack of cards: which of the members was meant is a
 * question only the viewer can answer, and answering it is one more tap.
 */
@Composable
private fun GroupDetail(
    members: List<MemberPosition>,
    names: Map<String, String>,
    faces: Map<String, Bitmap>,
    now: Instant,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text =
                    pluralStringResource(
                        R.plurals.map_group_count,
                        members.size,
                        members.size,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )

                TextButton(onClick = onClose) {
                    Text(text = stringResource(R.string.map_detail_close))
                }
            }

            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = LIST_MAX_HEIGHT)) {
                items(members, key = { it.positionId }) { member ->
                    MemberRow(
                        member = member,
                        name = names[member.positionId] ?: shortName(member.memberId),
                        face = faces[member.memberId],
                        now = now,
                        onSelect = onSelect,
                    )
                }
            }
        }
    }
}

/**
 * What the card says once a marker is tapped.
 *
 * Everything the position document carries beyond the two coordinates: when it
 * was taken, how the phone was collecting at the time and how much battery it
 * had left, which together answer "why is this old" without a second question.
 */
@Composable
private fun MemberDetail(
    member: MemberPosition,
    name: String,
    face: Bitmap?,
    now: Instant,
    away: Double?,
    detailsOpen: Boolean,
    onToggleDetails: () -> Unit,
    onClose: () -> Unit,
    onNamePhone: () -> Unit,
    onNamePerson: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier =
            Modifier
                // Bounded by whoever placed it, so that a card is never taller
                // than the map it stands on; what does not fit is scrolled to
                // rather than cut off.
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MemberFace(
                    memberId = member.memberId,
                    name = name,
                    picture = face,
                    stale = isGrey(member, now),
                )

                // The two names are what is pressed to rename them, underlined
                // the way the other words of the card that can be pressed are:
                // a row of its own for renaming took two lines of a card that
                // is short of them. Oneself is named on the account, not here.
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    NameText(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        onClick = onNamePerson,
                    )
                    NameText(
                        text =
                        member.deviceName?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.unnamed_device),
                        style = MaterialTheme.typography.bodyMedium,
                        onClick = onNamePhone,
                    )
                }

                // Beside the close rather than under the details: opened, the
                // details reach the bottom of the card, and a button below
                // them was cut off by its edge. Words rather than icons: the
                // icon packs are a dependency of their own.
                TextButton(onClick = onToggleDetails, contentPadding = HEADER_BUTTON_PADDING) {
                    Text(
                        text =
                        stringResource(
                            if (detailsOpen) {
                                R.string.map_detail_fewer
                            } else {
                                R.string.map_detail_more
                            },
                        ),
                    )
                }
                TextButton(onClick = onClose, contentPadding = HEADER_BUTTON_PADDING) {
                    Text(text = stringResource(R.string.map_detail_close))
                }
            }

            DetailSummary(member = member, away = away, now = now)
            DetailWarnings(member = member, now = now)

            // Everything else behind one button: the summary above is what a
            // card is opened for, and these rows are what pushed it past the
            // height of a screen.
            if (detailsOpen) {
                DetailRow(R.string.map_detail_at, momentText(member.positionAt))
                DetailRow(R.string.map_detail_online, momentText(member.knownAt))
                DetailRow(R.string.map_detail_mode) {
                    var helpOpen by remember { mutableStateOf(false) }

                    // A name alone answers nothing: "Standby" says what the
                    // phone was doing only to somebody who already knows the
                    // five, and a card is the one place a member meets a mode.
                    Text(
                        text = stringResource(modeLabel(member.mode)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { helpOpen = true },
                    )

                    if (helpOpen) {
                        ModeHelp(onClose = { helpOpen = false })
                    }
                }
                DetailRow(
                    R.string.map_detail_found_by,
                    stringResource(foundByLabel(member.foundBy)),
                )
                DetailRow(R.string.map_detail_width) {
                    val metres = member.accuracyMetres

                    Text(
                        text =
                        metres
                            ?.let { widthText(positionWidth(it)) }
                            ?: stringResource(R.string.map_detail_unknown),
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                        if (metres != null) {
                            widthColour(widthIsWide(metres))
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
                DetailRow(
                    R.string.map_detail_position,
                    stringResource(
                        R.string.map_detail_coordinates,
                        member.latitude,
                        member.longitude,
                    ),
                )
            }
        }
    }
}

/** Narrower than a text button's own, so that two of them fit beside the names. */
private val HEADER_BUTTON_PADDING = PaddingValues(horizontal = 8.dp, vertical = 4.dp)

/**
 * The line under the name: how far, how old the position, how long since the
 * phone was online, and the charge.
 *
 * The two ages side by side because under a spoof they are hours apart, and a
 * card naming only one of them is read as naming the other. The page draws
 * the same line.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailSummary(member: MemberPosition, away: Double?, now: Instant) {
    // Clipped at its own edge: the dot before the first fact of every line
    // falls outside it, so a line never starts with a separator.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(FACT_GAP),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(top = 6.dp).clipToBounds(),
    ) {
        away?.let {
            Fact(stringResource(R.string.map_detail_from_you, widthText(distanceUnits(it))))
        }
        Fact(
            stringResource(
                R.string.map_detail_position_ago,
                ageText(positionAge(member.positionAt, now)),
            ),
        )
        Fact(
            stringResource(
                R.string.map_detail_online_ago,
                ageText(positionAge(member.knownAt, now)),
            ),
        )

        val percent = member.batteryPercent

        Fact(
            text =
            percent
                ?.let { stringResource(R.string.map_battery, it) }
                ?: stringResource(R.string.map_detail_unknown),
            colour =
            if (percent != null && member.charging) {
                batteryColour(charging = true)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

/** The room between two facts of the summary, the dot between them in its middle. */
private val FACT_GAP = 14.dp

/**
 * One fact of the summary, kept whole on one line, with a dot drawn in the gap
 * before it.
 *
 * Drawn rather than written, so that TalkBack reads the facts one by one and
 * says nothing about the dots.
 */
@Composable
private fun Fact(text: String, colour: Color = MaterialTheme.colorScheme.onSurface) {
    val dot = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = colour,
        maxLines = 1,
        softWrap = false,
        modifier =
        Modifier.drawBehind {
            drawCircle(
                color = dot,
                radius = 1.5.dp.toPx(),
                center = Offset(-FACT_GAP.toPx() / 2, size.height / 2),
            )
        },
    )
}

/**
 * What is wrong with the position, as one paragraph, or nothing when nothing is.
 *
 * Each sentence only while it is true: a marker left behind by a phone that is
 * still online, answers being rejected, a receiver that has not settled. Put
 * together rather than as rows, because they are read as one account of why
 * the marker should not be trusted. The reason is underlined and pressed like
 * the mode: it names what the phone saw without saying what it means.
 */
@Composable
private fun DetailWarnings(member: MemberPosition, now: Instant) {
    val stuck = isStuck(member.positionAt, member.knownAt, now)
    val count = member.leftOutCount?.takeIf { it > 0 }

    if (!member.inFalsePlace && !stuck && count == null && !member.settling) {
        return
    }

    var helpOpen by remember { mutableStateOf(false) }

    val stuckSentence =
        stringResource(
            R.string.map_detail_stuck_sentence,
            ageText(positionAge(member.knownAt, now)),
        ).takeIf { stuck }
    val lead =
        count?.let {
            stringResource(
                R.string.map_detail_left_out_lead,
                pluralStringResource(R.plurals.map_detail_left_out_answers, it, it),
            )
        }
    // A source the member switched off is named: which of the five it was is
    // what they open the card to find out.
    val reason =
        sourceLeftOut(member.leftOutReason)
            ?.let {
                stringResource(
                    R.string.left_out_by_source_named,
                    stringResource(sourceChoiceLabel(it)),
                )
            }
            ?: stringResource(leftOutReasonLabel(member.leftOutReason))
    val settling = stringResource(R.string.map_detail_settling_sentence).takeIf { member.settling }
    // The answers refused for lying in the same place are that place again, so
    // they are counted into its sentence rather than given one of their own.
    val refusedHere = member.inFalsePlace && count != null &&
        member.leftOutReason == JumpReason.FalsePlace.name
    val falsePlaceLead =
        when {
            refusedHere -> stringResource(R.string.map_detail_false_place_and_after, count ?: 0)
            member.inFalsePlace -> stringResource(R.string.map_detail_false_place_lead)
            else -> null
        }
    val falsePlace =
        if (refusedHere) {
            stringResource(R.string.map_detail_false_place_where)
        } else {
            stringResource(leftOutReasonLabel(JumpReason.FalsePlace.name))
                .replaceFirstChar { it.lowercase(Locale.getDefault()) }
        }
    val reasonLink =
        LinkAnnotation.Clickable(
            tag = "reason",
            styles = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline)),
        ) { helpOpen = true }

    val paragraph =
        buildAnnotatedString {
            falsePlaceLead?.let {
                append(it)
                append(' ')
                withLink(reasonLink) { append(falsePlace) }
                append('.')
            }

            stuckSentence?.let {
                if (length > 0) append(' ')

                append(it)
            }

            if (lead != null && !refusedHere) {
                if (length > 0) append(' ')

                append(lead)
                append(' ')
                withLink(reasonLink) {
                    append(reason.replaceFirstChar { it.lowercase(Locale.getDefault()) })
                }
                append('.')
            }

            settling?.let {
                if (length > 0) append(' ')

                append(it)
            }
        }

    Text(
        text = paragraph,
        style = MaterialTheme.typography.bodyMedium,
        color = leftOutColour(),
        modifier = Modifier.padding(top = 6.dp),
    )

    if (helpOpen) {
        ReasonHelp(onClose = { helpOpen = false })
    }
}

/**
 * The five ways of collecting, read against each other.
 *
 * One place rather than a sentence at a time on the cards: what a mode means
 * is only clear beside the other four, and a member reads this once. The page
 * shows the same table.
 */
@Composable
private fun ModeHelp(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = onClose) {
                Text(text = stringResource(R.string.map_detail_close))
            }
        },
        title = { Text(text = stringResource(R.string.mode_help_title)) },
        text = {
            Column(
                // Five modes with two sentences each outgrow a short screen,
                // and a dialog clips what does not fit rather than folding it.
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.mode_help_intro),
                    style = MaterialTheme.typography.bodyMedium,
                )

                modesExplained().forEach { explained ->
                    Column {
                        Text(
                            text = stringResource(explained.name),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = stringResource(explained.whenItHappens),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(explained.whatItDoes),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
    )
}

/**
 * What each reason for rejecting a position means.
 *
 * One place rather than a sentence at a time on the cards, as with the modes:
 * the reasons are read against each other — several of them are the same
 * evidence held against different things — and a member reads this once. The
 * page shows the same table.
 */
@Composable
private fun ReasonHelp(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = onClose) {
                Text(text = stringResource(R.string.map_detail_close))
            }
        },
        title = { Text(text = stringResource(R.string.reason_help_title)) },
        text = {
            Column(
                // Fourteen reasons with two sentences each outgrow any screen,
                // and a dialog clips what does not fit rather than folding it.
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.reason_help_intro),
                    style = MaterialTheme.typography.bodyMedium,
                )

                reasonsExplained().forEach { explained ->
                    Column {
                        Text(
                            text = stringResource(explained.name),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = stringResource(explained.sign),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(explained.means),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
    )
}

/** A name at the head of the card, pressed to rename it where it can be. */
@Composable
private fun NameText(text: String, style: TextStyle, onClick: (() -> Unit)?) {
    Text(
        text = text,
        style = style,
        textDecoration = onClick?.let { TextDecoration.Underline },
        modifier = onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier,
    )
}

@Composable
private fun DetailRow(name: Int, value: String) {
    DetailRow(name) {
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** The same row when the value is more than one piece of text, as a charge is. */
@Composable
private fun DetailRow(name: Int, value: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(name),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        value()
    }
}

/** The moment itself, for the card a member opens rather than for the buttons. */
@Composable
private fun momentText(moment: Instant): String {
    val format =
        remember {
            DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withZone(ZoneId.systemDefault())
        }

    return format.format(moment)
}

@Composable
internal fun ageText(age: PositionAge): String = when (age) {
    PositionAge.JustNow -> stringResource(R.string.map_age_just_now)
    is PositionAge.Minutes -> pluralAge(R.plurals.map_age_minutes, age.count)
    is PositionAge.Hours -> pluralAge(R.plurals.map_age_hours, age.count)
    is PositionAge.Days -> pluralAge(R.plurals.map_age_days, age.count)
}

/**
 * The same age in as few characters as it can be said in.
 *
 * A card of the panel holds a face, a name and an age across a third of a
 * phone; written out in full the age lost its last word to the edge of the
 * card, which reads as a broken card rather than as a short one.
 */
@Composable
private fun shortAgeText(age: PositionAge): String = when (age) {
    PositionAge.JustNow -> stringResource(R.string.map_age_short_now)
    is PositionAge.Minutes -> stringResource(R.string.map_age_short_minutes, age.count)
    is PositionAge.Hours -> stringResource(R.string.map_age_short_hours, age.count)
    is PositionAge.Days -> stringResource(R.string.map_age_short_days, age.count)
}

@Composable
private fun pluralAge(plural: Int, count: Long): String =
    pluralStringResource(plural, count.toInt(), count.toInt())

/**
 * A clock that ticks slowly enough to be free and often enough to be right.
 *
 * Without it the ages would only change when a position does, and a member who
 * has gone quiet — the case worth noticing — is exactly the one whose entry
 * would never update.
 */
@Composable
private fun currentTime() = produceState(initialValue = Instant.now()) {
    while (true) {
        delay(AGE_TICK_MILLIS)
        value = Instant.now()
    }
}
