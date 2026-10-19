package io.github.vitalyostanin.geoshare.map

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.membership.Standing
import io.github.vitalyostanin.geoshare.membership.standingIn
import io.github.vitalyostanin.geoshare.update.UpdateBoard
import java.time.Instant

/**
 * What the map screen has worked out from the positions, the profiles and the
 * clock, and what it was given, handed on whole to the parts it is drawn in
 * together with the state it keeps. The worked-out values are read afresh on
 * every pass, as the map screen reads them.
 */
internal class MapScene(
    val state: MapScreenState,
    val follow: FollowState,
    val panels: OpenPanels,
    val positions: List<MemberPosition>,
    val profiles: Map<String, MemberProfile>,
    val searches: Map<String, Search>,
    val now: Instant,
    val ownName: String,
    val faces: Map<String, Bitmap>,
    val soughtPhones: Set<String>,
    val lookingNames: List<String>,
    val ownMemberId: String,
    val ownPhoneId: String?,
    val ownDisplayName: String?,
    val ownPictureUrl: String?,
    /** Everything the settings screen shows, which the map only carries through. */
    val settings: SettingsState,
    val actions: SettingsActions,
    val groups: GroupSwitch,
    /** How many members' positions this device is being refused, if any. */
    val outOfReach: Int,
    /** What a test draws in place of the map; null for the map itself. */
    val mapStandIn: (@Composable (Modifier) -> Unit)? = null,
) {
    /** Everybody in the order the strip shows them. */
    val members = orderedForNavigation(positions, ownMemberId)

    private val named = shownNames(positions, profiles, ownMemberId, ownName)

    /** What each position is called on the map, by position id. */
    val names: Map<String, String> = named.markers

    /** What each position is called in a card or a list, by position id. */
    val people: Map<String, String> = named.people

    /** The card that is open, if it is about one member. */
    val opened = positions.firstOrNull { it.positionId == state.openedPositionId }

    /** The members under the marker whose list is open. */
    val group = state.openedGroup.mapNotNull { positionId ->
        positions.firstOrNull { it.positionId == positionId }
    }

    private val standing = standingIn(settings.roster, ownMemberId)

    val switcher = switcherShown(groups.choices.size)

    /** Whether this device is an administrator, which the card of a sought member reads too. */
    val runsTheGroup = standing == Standing.Administrator

    private val outside = standing == Standing.Ungrouped || standing == Standing.Outsider

    val removed = removedGroup(groups, settings, outside)

    // A project the device reaches with no group of its own in it yet, told
    // only once the database has answered: before that the roster is null
    // for every member, and the card would flash on each start.
    val noGroup = removed == null && outside && settings.rosterAnswered

    /** Whether the strip and the list of members have a group to show. */
    val showsMembers: Boolean get() = removed == null && !noGroup

    /** Opens the card of a member and notes them as the one to follow. */
    fun select(positionId: String) {
        state.openCard(positionId)
        noteFollowTarget(positionId)
    }

    /** Brings the map to a member, as a tap on their button does. */
    fun lookAt(positionId: String) {
        state.lookAt(positionId)
        noteFollowTarget(positionId)
    }

    private fun noteFollowTarget(positionId: String) {
        val memberId = positions.firstOrNull { it.positionId == positionId }?.memberId

        follow.note(positionId, names[positionId] ?: memberId?.let(::shortName) ?: positionId)
    }
}

/**
 * The way back out of whatever is open, in the order things were opened in:
 * the strip's own wait first, because it is the most recent thing a press
 * could have meant to cancel, then the card, then the settings screen, and the
 * application itself only when nothing of ours is left on screen. Without
 * these the gesture leaves the application from a settings screen, which is
 * not what anybody means by it.
 */
@Composable
internal fun MapBackHandlers(scene: MapScene) {
    val panels = scene.panels

    BackHandler(enabled = scene.follow.awaitingTarget) { scene.follow.awaitingTarget = false }
    SettingsBackHandlers(panels)
    BackHandler(
        enabled = !panels.settings && (scene.opened != null || scene.group.isNotEmpty()),
    ) { scene.state.closeCard() }
    BackHandler(enabled = panels.members && !panels.settings) { panels.members = false }
}

/** The map with the strip of buttons under it, one above the other. */
@Composable
internal fun MapColumn(scene: MapScene, modifier: Modifier = Modifier) {
    val state = scene.state

    Column(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onGloballyPositioned { state.mapLeft = it.positionInRoot().x },
        ) {
            MapLayers(scene)
        }

        // Nothing below the card that says the device was taken off, or that
        // it has no group yet: the strip would go on counting members of a
        // group it does not see.
        if (scene.showsMembers) {
            HorizontalDivider()

            BelowTheMap(
                lookingNames = scene.lookingNames,
                members = scene.members,
                names = scene.names,
                faces = scene.faces,
                // Whoever the open card is about: one member, or everybody under a
                // marker whose list is showing.
                openCards = state.openedPositionId?.let { setOf(it) }
                    ?: scene.group.map { it.positionId }.toSet(),
                now = scene.now,
                searches = scene.searches,
                showAll = state.showAll,
                awaitingTarget = scene.follow.awaitingTarget,
                outOfReach = scene.outOfReach,
                onSelect = scene::lookAt,
                onShowAll = { state.showAll = it },
                onCentre = { positionId, x -> state.stripCentres[positionId] = x },
            )
        }
    }
}

/** What stands over the map, bottom to top, in the order it is drawn. */
@Composable
private fun BoxWithConstraintsScope.MapLayers(scene: MapScene) {
    // The member the card is about, or the first of a list in the order the
    // strip shows them.
    val about = scene.state.openedPositionId
        ?: scene.members.firstOrNull { member ->
            scene.group.any { it.positionId == member.positionId }
        }?.positionId

    MapSurface(scene)
    MapControls(scene)

    MapBanners(
        searches = scene.searches,
        now = scene.now,
        ownMemberId = scene.ownMemberId,
        ownPhoneId = scene.ownPhoneId,
        ownName = scene.ownName,
        names = scene.names,
        profiles = scene.profiles,
        actions = scene.actions,
        switcher = scene.switcher,
    )

    MapCards(scene, cardPlace(about, scene.state.stripCentres, scene.state.mapLeft))
}

/** The map itself, and what takes its place when there is no group to show. */
@Composable
private fun BoxScope.MapSurface(scene: MapScene) {
    val state = scene.state
    val follow = scene.follow
    // Read through the resources rather than through a composable, because the
    // map asks for this text while it is drawing, with a count it works out
    // itself from the members standing together.
    val resources = LocalContext.current.resources
    val standIn = scene.mapStandIn

    if (standIn != null) {
        standIn(Modifier.fillMaxSize())
    } else {
        MemberMap(
            positions = scene.positions,
            names = scene.names,
            faces = scene.faces,
            ownMemberId = scene.ownMemberId,
            now = scene.now,
            soughtPhones = scene.soughtPhones,
            // The tiles follow the screen they are looked at on: a white
            // map under a dark card at night is a lamp in the face, which
            // is the reason the page turns its own canvas over.
            dark = MaterialTheme.colorScheme.surface.luminance() < HALF_LIT,
            groupLabel = { count: Int ->
                resources.getQuantityString(R.plurals.map_group_count, count, count)
            },
            focus = state.focus,
            selected = state.openedPositionId,
            followMode = follow.mode,
            followTarget = follow.target?.positionId,
            onPausedChange = { follow.paused = it },
            topCover = state.topCover,
            onSelect = scene::select,
            onSelectGroup = state::openGroup,
            onDismiss = state::closeCard,
            modifier = Modifier.fillMaxSize(),
        )
    }

    scene.removed?.let { RemovedScreen(removed = it, switcher = scene.switcher) }

    if (scene.noGroup) {
        NoGroupScreen(
            switcher = scene.switcher,
            onFoundGroup = scene.actions.onFoundGroup,
            onJoin = scene.groups.onJoin,
            readProjectMark = scene.actions.onReadProjectMark,
            trouble = scene.settings.groupTrouble,
            foundRefused = scene.settings.foundRefused,
        )
    }
}

/** The row of buttons over the map, the group chip and the account button. */
@Composable
private fun BoxScope.MapControls(scene: MapScene) {
    val state = scene.state
    val follow = scene.follow
    val panels = scene.panels

    MapTopRow(
        settings = scene.settings,
        actions = scene.actions,
        follow = FollowControl(
            mode = follow.mode,
            paused = follow.paused,
            targetName = follow.target?.name,
            targetAvailable = follow.target?.let { targetAvailable(scene, it) } ?: true,
            changeCount = follow.changes,
            onPick = { picked ->
                follow.pick(picked, follow.target?.let { targetAvailable(scene, it) } ?: false)
            },
        ),
        onHelp = { state.mapHelpOpen = true },
        // Only while there is a group to list: the device taken off
        // it, or never in one, has no strip either.
        onMembers = { panels.members = true }.takeIf { scene.showsMembers },
        onTopCover = { state.topCover = it },
    )

    if (state.mapHelpOpen) {
        MapHelpSheet(onClose = { state.mapHelpOpen = false })
    }

    MapGroupControls(
        switcher = scene.switcher,
        groups = scene.groups,
        groupsOpen = state.groupsOpen,
        onGroupsOpen = { state.groupsOpen = it },
        leaveAsk = state.leaveAsk,
        onLeaveAsk = { state.leaveAsk = it },
    )

    AccountButton(
        displayName = scene.ownDisplayName,
        pictureUrl = scene.ownPictureUrl,
        onClick = { panels.settings = true },
        updateAvailable = UpdateBoard.found != null,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(12.dp),
    )
}

/** The card of the member opened, or the list of those under one marker. */
@Composable
private fun BoxScope.MapCards(scene: MapScene, placed: Modifier) {
    val state = scene.state
    val opened = scene.opened

    if (opened != null) {
        OpenedMemberCard(
            opened = opened,
            name = scene.people[opened.positionId] ?: shortName(opened.memberId),
            face = scene.faces[opened.memberId],
            positions = scene.positions,
            searches = scene.searches,
            now = scene.now,
            profiles = scene.profiles,
            own = OwnSide(scene.ownMemberId, scene.ownPhoneId, scene.ownName, scene.runsTheGroup),
            settings = scene.settings,
            actions = scene.actions,
            detailsOpen = state.detailsOpen,
            onDetailsOpen = { state.detailsOpen = it },
            onClose = { state.openedPositionId = null },
            onNaming = { kind, key -> state.naming = kind to key },
            // Against the strip rather than floating over the map: the
            // card is a sheet pulled up out of it, over the button of
            // the member it is about.
            modifier = placed,
        )
    } else if (scene.group.size > 1) {
        GroupDetail(
            members = scene.group,
            names = scene.names,
            faces = scene.faces,
            now = scene.now,
            searches = scene.searches,
            onSelect = scene::select,
            onClose = { state.openedGroup = emptyList() },
            modifier = placed.padding(12.dp),
        )
    }
}

/** Whether the position being followed is still among those the device can see. */
private fun targetAvailable(scene: MapScene, target: FollowTarget): Boolean =
    targetPosition(scene.positions, target.positionId) != null

/** What opens over the map and the strip: the list of members, the settings, the naming. */
@Composable
internal fun MapOverlayScreens(
    scene: MapScene,
    labels: Labels,
    onLabel: (LabelKind, String, String?) -> Unit,
) {
    val state = scene.state
    val panels = scene.panels

    // Over the strip as well as the map: the strip's own few are in the list
    // too, and a list that left them showing below would read as two lists.
    if (panels.members && scene.showsMembers) {
        MembersScreen(
            members = scene.members,
            positions = scene.positions,
            ownMemberId = scene.ownMemberId,
            names = scene.names,
            faces = scene.faces,
            now = scene.now,
            searches = scene.searches,
            onSelect = { positionId ->
                panels.members = false
                scene.lookAt(positionId)
            },
            onClose = { panels.members = false },
        )
    }

    if (panels.settings) {
        SettingsLayers(
            panels = panels,
            settings = scene.settings,
            actions = scene.actions,
            profiles = scene.profiles,
            ownMemberId = scene.ownMemberId,
            ownPhoneId = scene.ownPhoneId,
            labels = labels,
            now = scene.now,
            searches = scene.searches,
            positions = scene.positions,
            groups = scene.groups,
            switcher = scene.switcher,
            runsTheGroup = scene.runsTheGroup,
            removed = scene.removed,
            onLeaveAsk = { state.leaveAsk = it },
        )
    }

    state.naming?.let { (kind, key) ->
        NamingDialog(kind, key, labels, onLabel, onDone = { state.naming = null })
    }
}
