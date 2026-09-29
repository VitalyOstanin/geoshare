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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.collection.Watcher
import io.github.vitalyostanin.geoshare.collection.watchingMembers
import io.github.vitalyostanin.geoshare.membership.Roster
import io.github.vitalyostanin.geoshare.membership.Standing
import io.github.vitalyostanin.geoshare.membership.standingIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.Instant

/** How often the siren banner decides whether the phone is still sounding. */
private const val SIREN_BANNER_TICK_MS = 1_000L

/** Above this a surface counts as pale, below it as dark. */
internal const val HALF_LIT = 0.5f

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

/** As much of the screen as the full list may take before it starts scrolling instead. */
internal val LIST_MAX_HEIGHT = 220.dp

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

            val link = LocalLink.current

            if (ownSearches.isNotEmpty() || link.lost) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 64.dp, start = 12.dp, end = 12.dp),
                ) {
                    if (link.lost) {
                        LinkBanner(link)
                    }

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
                    onDetailsOpen = { detailsOpen = it },
                    onClose = { openedPositionId = null },
                    onNamePhone = { naming = LabelKind.PHONE to opened.positionId },
                    // Oneself is named on the account, not here.
                    onNamePerson =
                    { naming = LabelKind.MEMBER to opened.memberId }
                        .takeIf { opened.memberId != ownMemberId },
                    // Against the strip rather than floating over the map: the
                    // card is a sheet pulled up out of it.
                    modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .heightIn(max = cardHeight),
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
                onShowAll = { showAll = it },
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
