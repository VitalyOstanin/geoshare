package io.github.vitalyostanin.geoshare.map

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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

/** How faded the follow button is while a hand holds the following still, as on the page. */
private const val PAUSED_ALPHA = 0.75f

/** As tall as MapRoundButton's own circle, so the row does not jump when this one widens. */
private val FOLLOW_BUTTON_HEIGHT = 40.dp

/**
 * How long the follow button spells its new mode out in words before it
 * shrinks back down to the mark alone.
 *
 * Long enough to be read once, short enough that the row of controls over the
 * map is not left wide for good: the button says what changed and then goes
 * back to being one more circle among the others.
 */
private const val FOLLOW_LABEL_MILLIS = 2_500L

/**
 * How long [FollowMode.ONE] waits, once picked with nobody yet chosen to sit
 * on, before giving up and letting the strip stop asking.
 *
 * Long enough that turning to tap a card is not a race against the menu
 * closing on its own; short enough that a member who opened the menu by
 * mistake is not left with a strip ringed in a colour they never meant to
 * see.
 */
private const val AWAITING_TARGET_MILLIS = 15_000L

/** As wide as the menu's own rows, so the hint under them wraps the same way. */
private val FOLLOW_MENU_WIDTH = 260.dp

/** The circle a card carries: large enough for a face, small enough for a strip of cards. */
private val FACE_SIZE = 22.dp

/** What takes the colour out of the picture of a member whose position is old. */
private val GREYSCALE = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** As much of the screen as the full list may take before it starts scrolling instead. */
private val LIST_MAX_HEIGHT = 220.dp

/**
 * The gap between the controls in the row over the map; wider than the count
 * on the sources button sticks out past its circle.
 */
private val TOP_BUTTONS_GAP = 12.dp

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
    /** This phone's own `{uid}.{deviceId}`: the admin panel rings every phone but this one. */
    ownPhoneId: String?,
    ownDisplayName: String?,
    ownPictureUrl: String?,
    /** The group's searches, by the phone each is looking for. */
    searches: Map<String, Search>,
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
    // The phones the group is looking for right now, worked out against the
    // same clock the ages tick by: nothing arrives from the database at the
    // moment a search runs out, so this is read afresh whenever [now] moves.
    val soughtPhones = remember(searches, now) { standingSearches(searches, now).keys }
    // Read here rather than only where the admin panel is opened, because the
    // card of a member being looked for offers "Found" on this same
    // condition, and a phone that stops running the group should lose the
    // button on both at once.
    val runsTheGroup = standingIn(settings.roster, ownMemberId) == Standing.Administrator
    // The name being asked for, held by what it names rather than by the card:
    // the card may be closed and the dialog still up.
    var naming by remember { mutableStateOf<Pair<LabelKind, String>?>(null) }
    val members = orderedForNavigation(positions, ownMemberId)
    var focus by remember { mutableStateOf<MapFocus?>(null) }
    var followMode by remember { mutableStateOf(FollowMode.NONE) }
    var mapHelpOpen by remember { mutableStateOf(false) }
    // Whether a hand is holding the following still; the button says so.
    var followPaused by remember { mutableStateOf(false) }
    var followTarget by remember { mutableStateOf<FollowTarget?>(null) }
    // Bumped on every real change of the follow mode — from the menu, or from
    // a target finally being chosen for [awaitingTarget] — so the button's
    // label can unroll again each time rather than only on the first change:
    // a key that stayed the same (say, ALL picked while ALL already ran)
    // would leave [LaunchedEffect] with nothing to restart.
    var followModeChanges by remember { mutableStateOf(0) }
    // Set once ONE is picked with nobody yet chosen to sit on, and cleared the
    // moment a target is noted — by any of the three ways a card opens, by
    // picking a different mode from the menu, by the back gesture, or on its
    // own once nobody has answered it for a while.
    var awaitingTarget by remember { mutableStateOf(false) }

    // Nobody answering the strip for that long is a menu opened by mistake
    // rather than a member still deciding, and the ring left running on the
    // strip forever would read as broken rather than as waiting.
    LaunchedEffect(awaitingTarget) {
        if (awaitingTarget) {
            delay(AWAITING_TARGET_MILLIS)
            awaitingTarget = false
        }
    }

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
    var myPhonesOpen by remember { mutableStateOf(false) }
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
    //
    // Also where ONE's own wait ends: the same three ways of opening a card
    // are the three ways the strip's ring is answered, and a name arriving
    // now is exactly the one [awaitingTarget] was left waiting for.
    fun noteFollowTarget(positionId: String) {
        val memberId = positions.firstOrNull { it.positionId == positionId }?.memberId

        followTarget =
            FollowTarget(
                positionId = positionId,
                name = names[positionId] ?: memberId?.let(::shortName) ?: positionId,
            )

        if (awaitingTarget) {
            followMode = FollowMode.ONE
            awaitingTarget = false
            followModeChanges++
        }
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
    // in: the strip's own wait first, because it is the most recent thing a
    // press could have meant to cancel, then the card, then the settings
    // screen, and the application itself only when nothing of ours is left
    // on screen. Without these the gesture leaves the application from a
    // settings screen, which is not what anybody means by it.
    BackHandler(enabled = awaitingTarget) { awaitingTarget = false }
    BackHandler(enabled = adminOpen) { adminOpen = false }
    BackHandler(enabled = myPhonesOpen) { myPhonesOpen = false }
    BackHandler(enabled = settingsOpen && !adminOpen && !myPhonesOpen) { settingsOpen = false }
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
                soughtPhones = soughtPhones,
                // The tiles follow the screen they are looked at on: a white
                // map under a dark card at night is a lamp in the face, which
                // is the reason the page turns its own canvas over.
                dark = MaterialTheme.colorScheme.surface.luminance() < HALF_LIT,
                groupLabel = groupLabel,
                focus = focus,
                selected = openedPositionId,
                followMode = followMode,
                followTarget = followTarget?.positionId,
                onPausedChange = { followPaused = it },
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

            // The controls on one side and the account on the other, the way
            // map applications lay them out: a row keeps a second control from
            // ever landing on top of the first whatever either measures.
            Row(
                horizontalArrangement = Arrangement.spacedBy(TOP_BUTTONS_GAP),
                verticalAlignment = Alignment.CenterVertically,
                // The map is drawn to the edges of the window on purpose; what
                // stands over it is not, or the clock is what the member taps.
                modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp),
            ) {
                SourcePicker(
                    leftOut = settings.sourcesLeftOut,
                    onSourceUseChange = actions.onSourceUseChange,
                )

                FollowButton(
                    mode = followMode,
                    paused = followPaused,
                    targetName = followTarget?.name,
                    targetAvailable =
                    followTarget?.let { targetPosition(positions, it.positionId) != null }
                        ?: true,
                    changeCount = followModeChanges,
                    onPick = { picked ->
                        val hasTarget =
                            followTarget?.let { targetPosition(positions, it.positionId) != null }
                                ?: false

                        // ONE picked with nobody to sit on does not become the
                        // mode yet: the strip is asked instead, and it is
                        // noteFollowTarget, once somebody answers, that turns
                        // the wait into the mode itself.
                        if (picked == FollowMode.ONE && !hasTarget) {
                            awaitingTarget = true
                        } else {
                            followMode = picked
                            awaitingTarget = false
                            followModeChanges++
                        }
                    },
                )

                MapRoundButton(
                    icon = R.drawable.ic_help,
                    label = stringResource(R.string.help_open),
                    onClick = { mapHelpOpen = true },
                )
            }

            if (mapHelpOpen) {
                HelpSheet(
                    title = stringResource(R.string.help_title_map),
                    entries =
                    listOf(
                        HelpEntry(
                            R.drawable.ic_follow_none,
                            HelpLook.Plain,
                            stringResource(R.string.help_follow_none),
                        ),
                        HelpEntry(
                            R.drawable.ic_follow_one,
                            HelpLook.Plain,
                            stringResource(R.string.help_follow_one),
                        ),
                        HelpEntry(
                            R.drawable.ic_follow_all,
                            HelpLook.Plain,
                            stringResource(R.string.help_follow_all),
                        ),
                        HelpEntry(
                            R.drawable.ic_sources,
                            HelpLook.Plain,
                            stringResource(R.string.help_sources),
                        ),
                    ),
                    onClose = { mapHelpOpen = false },
                )
            }

            AccountButton(
                displayName = ownDisplayName,
                pictureUrl = ownPictureUrl,
                onClick = { settingsOpen = true },
                modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp),
            )

            // Told to the member over the map whenever one of their own
            // phones is being looked for, and below the row of round buttons
            // rather than on top of it: the two are read in different
            // moments, and a banner covering the buttons would be in the way
            // of the one thing this screen is for. A member carrying two
            // phones may have both looked for at once, so this is a list
            // rather than one banner.
            val ownSearches =
                remember(searches, now, ownMemberId) {
                    standingSearches(searches, now).values.filter { it.memberId == ownMemberId }
                }

            if (ownSearches.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 64.dp, start = 12.dp, end = 12.dp),
                ) {
                    ownSearches.forEach { search ->
                        SoughtBanner(
                            search = search,
                            phoneName =
                            names[search.phoneId] ?: search.phoneName ?: shortName(search.phoneId),
                            openedByName =
                            memberName(
                                profile = profiles[search.openedBy],
                                memberId = search.openedBy,
                                own = search.openedBy == ownMemberId,
                                ownName = ownName,
                            ),
                            onFound = { actions.onEndSearch(search.phoneId) },
                        )
                    }
                }
            }

            // The siren sounding on this very phone, with the one button that
            // matters while it does; the notification carries the same one
            // for a phone that is locked or showing something else.
            val ownSirenFlow =
                remember(ownPhoneId) {
                    ownPhoneId?.let(actions.sirenOf) ?: flowOf(null)
                }
            val ownSiren by ownSirenFlow.collectAsState(initial = null)
            val sirenNow by produceState(Instant.now(), ownSiren) {
                while (ownSiren != null) {
                    value = Instant.now()
                    delay(SIREN_BANNER_TICK_MS)
                }
            }
            val sirenScope = rememberCoroutineScope()

            ownSiren?.let { siren ->
                val state = sirenState(siren, sirenNow)

                if (state is SirenState.Sounding) {
                    SirenBanner(
                        callerName =
                        memberName(
                            profile = profiles[siren.calledBy],
                            memberId = siren.calledBy,
                            own = siren.calledBy == ownMemberId,
                            ownName = ownName,
                        ),
                        until = state.until,
                        onStop = { sirenScope.launch { actions.onStopSiren(siren.phoneId) } },
                        modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 64.dp, start = 12.dp, end = 12.dp),
                    )
                }
            }

            if (opened != null) {
                val openedSearch = searches[opened.positionId]?.takeIf { it.stands(now) }

                MemberDetail(
                    member = opened,
                    name = people[opened.positionId] ?: shortName(opened.memberId),
                    face = faces[opened.memberId],
                    now = now,
                    away = distanceFromViewer(opened, positions, ownMemberId, now),
                    search = openedSearch,
                    openedByName =
                    openedSearch?.let {
                        memberName(
                            profile = profiles[it.openedBy],
                            memberId = it.openedBy,
                            own = it.openedBy == ownMemberId,
                            ownName = ownName,
                        )
                    },
                    runsTheGroup = runsTheGroup,
                    onFound = { actions.onEndSearch(opened.positionId) },
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
                    searches = searches,
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
                searches = searches,
                showAll = showAll,
                awaitingTarget = awaitingTarget,
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
            onOpenMyPhones = { myPhonesOpen = true },
        )
    }

    // Over the settings, as the admin panel is. The phones are read from this
    // member's own grant, which the member always may read.
    if (settingsOpen && myPhonesOpen) {
        MyPhonesScreen(
            phones =
            phoneLines(
                ownMemberId,
                settings.grants[ownMemberId]?.phones.orEmpty(),
                labels,
            ),
            ownPhoneId = ownPhoneId,
            now = now,
            sirenOf = actions.sirenOf,
            onCallSiren = actions.onCallSiren,
            onStopSiren = actions.onStopSiren,
            onForgetPhone = actions.onForgetPhone,
            searches = searches,
            mayLookFor = runsTheGroup,
            onSearch = actions.onSearch,
            onEndSearch = actions.onEndSearch,
            onClose = { myPhonesOpen = false },
        )
    }

    // Over the settings rather than instead of them: the way back out is the
    // screen it was opened from, whichever of the two ways back is used.
    //
    // [runsTheGroup] is weighed again on every draw rather than only when the
    // panel was opened: an administrator can stop being one while it is up —
    // somebody else takes the role away, or this phone is taken off the
    // roster and the refused subscription empties it — and a panel left
    // standing would go on offering writes that are now refused.
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
            searches = searches,
            onMakeInvitation = actions.onMakeInvitation,
            onRemoveMember = actions.onRemoveMember,
            onSetRunning = actions.onSetRunning,
            onSearch = actions.onSearch,
            onEndSearch = actions.onEndSearch,
            ownPhoneId = ownPhoneId,
            sirenOf = actions.sirenOf,
            onCallSiren = actions.onCallSiren,
            onStopSiren = actions.onStopSiren,
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
 * The button that opens a choice of [FollowMode] and, once it is closed
 * again, says out loud what was just chosen.
 *
 * A press used to walk a ring of three modes; it now opens a menu naming all
 * three at once, the way [SourcePicker] already does for its own five
 * choices, because a ring hides two thirds of what pressing it could mean
 * until it is pressed. Picking ONE with nobody chosen yet does not close the
 * loop here — [onPick] reports the pick and the caller decides whether there
 * is anybody to sit on.
 */
@Composable
private fun FollowButton(
    mode: FollowMode,
    /** Who [FollowMode.ONE] is centred on, named when their card was opened. */
    targetName: String?,
    /** False once the chosen member's position has dropped off the map entirely. */
    targetAvailable: Boolean,
    /** Bumped by the caller on every real change of mode, to unroll the label again. */
    changeCount: Int,
    onPick: (FollowMode) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Whether a hand is holding the following still. The button is faded
     * rather than changed, so that a map that has stopped following reads as
     * deliberately left alone rather than as stuck; the page does the same.
     */
    paused: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    val label = when (mode) {
        FollowMode.NONE -> stringResource(R.string.map_follow_none)

        FollowMode.ALL -> stringResource(R.string.map_follow_all)

        // targetName is null here only where nobody has ever been chosen, or
        // the menu's own ONE was picked and is still waiting for somebody —
        // a case the button reports through targetAvailable, not through
        // this fallback.
        FollowMode.ONE ->
            targetName
                ?.let { stringResource(R.string.map_follow_one, it) }
                ?: stringResource(R.string.map_follow_one_unnamed)
    }

    val icon = when (mode) {
        FollowMode.NONE -> R.drawable.ic_follow_none
        FollowMode.ALL -> R.drawable.ic_follow_all
        FollowMode.ONE -> R.drawable.ic_follow_one
    }

    // Shown for a few seconds after every real change of mode, then folded
    // away again: [changeCount] rather than [mode] itself as the key, because
    // a pick that lands on the mode already running — ALL chosen again while
    // ALL was already the mode — is still worth saying out loud, and [mode]
    // alone would not have changed to say it.
    var showLabel by remember { mutableStateOf(false) }

    LaunchedEffect(changeCount) {
        if (changeCount == 0) return@LaunchedEffect

        showLabel = true
        delay(FOLLOW_LABEL_MILLIS)
        showLabel = false
    }

    // The menu opens where the button is, which is the button's own corner,
    // the same as SourcePicker's.
    Box(modifier = modifier) {
        Surface(
            onClick = { open = true },
            modifier =
            Modifier
                .height(FOLLOW_BUTTON_HEIGHT)
                .alpha(if (paused) PAUSED_ALPHA else 1f),
            shape = CircleShape,
            color =
            if (mode != FollowMode.NONE) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            tonalElevation = 3.dp,
            shadowElevation = 3.dp,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(FOLLOW_BUTTON_HEIGHT),
                    contentAlignment = Alignment.Center,
                ) {
                    // The description TalkBack reads for the whole button,
                    // named on the icon the same way MapRoundButton names it:
                    // the label beside it, once it is showing, would only
                    // repeat the same words a second time.
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = label,
                        tint =
                        if (!targetAvailable) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }

                AnimatedVisibility(
                    visible = showLabel,
                    enter = expandHorizontally(),
                    exit = shrinkHorizontally(),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(end = 14.dp).clearAndSetSemantics {},
                    )
                }
            }
        }

        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            FollowModeItem(
                icon = R.drawable.ic_follow_none,
                title = stringResource(R.string.map_follow_menu_none_title),
                subtitle = stringResource(R.string.map_follow_menu_none_subtitle),
                current = mode == FollowMode.NONE,
                onClick = {
                    open = false
                    onPick(FollowMode.NONE)
                },
            )
            FollowModeItem(
                icon = R.drawable.ic_follow_one,
                title =
                targetName
                    ?.let { stringResource(R.string.map_follow_menu_one_title_named, it) }
                    ?: stringResource(R.string.map_follow_menu_one_title),
                subtitle = stringResource(R.string.map_follow_menu_one_subtitle),
                current = mode == FollowMode.ONE,
                onClick = {
                    open = false
                    onPick(FollowMode.ONE)
                },
            )
            FollowModeItem(
                icon = R.drawable.ic_follow_all,
                title = stringResource(R.string.map_follow_menu_all_title),
                subtitle = stringResource(R.string.map_follow_menu_all_subtitle),
                current = mode == FollowMode.ALL,
                onClick = {
                    open = false
                    onPick(FollowMode.ALL)
                },
            )

            HorizontalDivider()

            // Unselectable on purpose: this is not a fourth choice, it is what
            // the second choice above needs explained, and a menu of three
            // rows and a caption reads as three rows all the same.
            Text(
                text = stringResource(R.string.map_follow_menu_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                Modifier
                    .width(FOLLOW_MENU_WIDTH)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

/** One row of the follow menu: a mode's mark, its name and what it does. */
@Composable
private fun FollowModeItem(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    /** Whether this is the mode running right now, marked by the tint behind the row. */
    current: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        modifier =
        if (current) {
            Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
        } else {
            Modifier
        },
        leadingIcon = {
            // Decorative here: the row's own text already says what the
            // icon marks, and the icon on the button itself is what carries
            // the description once the menu is closed.
            Icon(painter = painterResource(icon), contentDescription = null)
        },
        text = {
            Column(modifier = Modifier.width(FOLLOW_MENU_WIDTH)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        onClick = onClick,
    )
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
    /** The group's searches, so a phone being looked for is marked in the strip too. */
    searches: Map<String, Search>,
    showAll: Boolean,
    /** Whether ONE is waiting for a member to be chosen off this very strip. */
    awaitingTarget: Boolean,
    onSelect: (String) -> Unit,
    onToggleAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onStrip = membersOnTheStrip(members.size)
    val shown = members.take(onStrip)
    val rest = members.drop(onStrip)

    // How bright the ring around every card is while the strip is waiting to
    // be answered. It fades in and out on its own rather than sitting at one
    // strength, because a border that simply appeared and stayed would be
    // missed by anybody not looking at the strip at that exact moment — the
    // whole reason it is a card being chosen from rather than a mode already
    // running.
    val ringAlpha =
        if (awaitingTarget) {
            val blink = rememberInfiniteTransition(label = "followTargetRing")

            blink.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 700),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "followTargetRingAlpha",
            ).value
        } else {
            null
        }

    Column(modifier = modifier) {
        if (awaitingTarget) {
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.map_follow_awaiting_banner),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

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
                    ringAlpha = ringAlpha,
                    now = now,
                    sought = searches[member.positionId]?.takeIf { it.stands(now) },
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
                        sought = searches[member.positionId]?.takeIf { it.stands(now) },
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
    /** How bright the ring waiting for ONE's target is, or null while nobody is waiting. */
    ringAlpha: Float?,
    now: Instant,
    /** The search standing for this phone, or null while nobody is looking for it. */
    sought: Search?,
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
        // The tint the search puts on this card is the whole surface, the
        // same way the page reads it: a card being looked for is red before
        // anything on it is read.
        color =
        if (sought != null) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border =
        when {
            // A search takes over from every other border a card can wear:
            // it is a fact about the member, not about what this screen is
            // waiting on right now.
            sought != null -> BorderStroke(width = 2.dp, color = Color(SOUGHT_COLOUR))

            // The ring waiting to be answered takes over from the plainer
            // one below: a card cannot at once be the one already open and
            // the one ONE is waiting to be given, since opening it is what
            // answers the wait.
            ringAlpha != null ->
                BorderStroke(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = ringAlpha),
                )

            shown -> BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.primary)

            else -> null
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

            if (sought != null) {
                Text(
                    text =
                    stringResource(
                        R.string.sought_until,
                        SOUGHT_ENDS_AT.format(sought.endsAt.atZone(ZoneId.systemDefault())),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(SOUGHT_COLOUR),
                    maxLines = 1,
                )
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
    /** The search standing for this phone, or null while nobody is looking for it. */
    sought: Search?,
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

            if (sought != null) {
                Text(
                    text =
                    stringResource(
                        R.string.sought_until,
                        SOUGHT_ENDS_AT.format(sought.endsAt.atZone(ZoneId.systemDefault())),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(SOUGHT_COLOUR),
                )
            }
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
    searches: Map<String, Search>,
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
                        sought = searches[member.positionId]?.takeIf { it.stands(now) },
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
    /** The search standing for this phone, or null while nobody is looking for it. */
    search: Search?,
    /** Who opened [search], said the way the card says who is looking at the map. */
    openedByName: String?,
    /** Whether this viewer runs the group, and so may say the member has been found. */
    runsTheGroup: Boolean,
    onFound: suspend () -> Unit,
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

            if (search != null && openedByName != null) {
                DetailSought(
                    search = search,
                    openedByName = openedByName,
                    showFound = runsTheGroup,
                    onFound = onFound,
                )
            }

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
        // This phone's own position before it has left: there has been no
        // moment of being online to count from, and what the reader needs to
        // know is that nobody else sees it yet.
        Fact(
            if (member.unsent) {
                stringResource(R.string.map_detail_unsent)
            } else {
                stringResource(
                    R.string.map_detail_online_ago,
                    ageText(positionAge(member.knownAt, now)),
                )
            },
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

/** When a search ends: the day as well as the time, the same reading a permission's end has. */
private val SOUGHT_ENDS_AT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM HH:mm")

/**
 * Told on the card of a member the group is looking for: until when, who
 * opened the search, and, to whoever runs the group, the button that ends it.
 *
 * A row of its own rather than one more fact in [DetailSummary]: this is not a
 * fact about the position, it is a fact about what reading it means, and it
 * carries a colour of its own for the same reason a warning does.
 */
@Composable
private fun DetailSought(
    search: Search,
    openedByName: String,
    showFound: Boolean,
    onFound: suspend () -> Unit,
) {
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp),
    ) {
        // One line rather than two: "ищем до … · Открыл(а): …" is read as one
        // fact about the position, the way the summary above it is.
        Text(
            text =
            stringResource(
                R.string.sought_until,
                SOUGHT_ENDS_AT.format(search.endsAt.atZone(ZoneId.systemDefault())),
            ) + " · " + stringResource(R.string.search_opened_by, openedByName),
            style = MaterialTheme.typography.bodySmall,
            color = Color(SOUGHT_COLOUR),
            modifier = Modifier.weight(1f),
        )

        if (showFound) {
            TextButton(
                onClick = {
                    busy = true
                    scope.launch {
                        try {
                            onFound()
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
            ) {
                Text(text = stringResource(R.string.search_found), color = Color(SOUGHT_COLOUR))
            }
        }
    }
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
    // The language the card is written in, read so that a change of it
    // redraws the card: the default locale changes under Compose unseen.
    val locale = LocalConfiguration.current.locales[0]

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
                .replaceFirstChar { it.lowercase(locale) }
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
                    append(reason.replaceFirstChar { it.lowercase(locale) })
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

/** How often the siren banner decides whether the phone is still sounding. */
private const val SIREN_BANNER_TICK_MS = 1_000L
