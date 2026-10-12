package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.membership.Leave
import io.github.vitalyostanin.geoshare.membership.Presence
import io.github.vitalyostanin.geoshare.membership.Roster
import io.github.vitalyostanin.geoshare.membership.howToLeave
import io.github.vitalyostanin.geoshare.project.GroupConnection
import io.github.vitalyostanin.geoshare.project.GroupNames
import io.github.vitalyostanin.geoshare.service.groupRoster
import io.github.vitalyostanin.geoshare.service.groupSearches
import io.github.vitalyostanin.geoshare.service.phoneSiren
import io.github.vitalyostanin.geoshare.service.positionId
import io.github.vitalyostanin.geoshare.service.presenceIn
import io.github.vitalyostanin.geoshare.service.signedInTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold
import java.time.Instant

/** How tall the chip naming the group is. */
val SWITCHER_HEIGHT: Dp = 36.dp

/** The room between the row of round buttons and the chip, and between the chip and a banner. */
val SWITCHER_GAP: Dp = 8.dp

/** Where the banners start below the row of round buttons while there is no chip. */
private val BANNER_TOP: Dp = 64.dp

/** Where the chip starts: the row's own padding and its buttons' height, and the gap. */
private val SWITCHER_TOP: Dp = 12.dp + 40.dp + SWITCHER_GAP

/** As wide as the chip grows before a long name is cut short. */
private val SWITCHER_MAX_WIDTH: Dp = 220.dp

/**
 * How often the list is worked out again while nothing arrives: a search
 * and a siren both run out on their own, and nothing is written when they do.
 */
private const val CHOICES_TICK_MS = 10_000L

/**
 * One group as the list of groups shows it.
 *
 * [members] is null when the group's document could not be read — signed
 * out there, or no longer let in — rather than a count of nobody. [siren] is
 * a call waiting for this device or sounding on it in that group, and
 * [search] a search standing there: what a member reading the list of the
 * groups they are not looking at needs to know is going on in them.
 * [removed] says an administrator took this device off the group's roster.
 */
data class GroupChoice(
    val projectId: String,
    val caption: String,
    val members: Int?,
    val siren: Boolean,
    val search: Boolean,
    /** What leaving the group would do for the account this device is signed in as there. */
    val leave: Leave,
    val removed: Boolean = false,
)

/**
 * The groups the map can switch between, and what it asks for when the
 * member switches, wants to join one more or leaves one.
 */
data class GroupSwitch(
    val choices: List<GroupChoice>,
    /** The project of the group the screens show. */
    val selected: String,
    val onSelect: (String) -> Unit,
    val onJoin: () -> Unit,
    /** Leaves a group the way asked, true once it is gone from this device. */
    val onLeave: suspend (LeaveAsk) -> Boolean,
)

/**
 * Whether the chip is drawn at all: with one group there is nothing to
 * switch to, and the screen stays as it was before a device could be in
 * more than one.
 */
fun switcherShown(groups: Int): Boolean = groups > 1

/** How far below the top the banners start, below the chip while it is there. */
fun bannerOffset(switcher: Boolean): Dp =
    if (switcher) BANNER_TOP + SWITCHER_HEIGHT + SWITCHER_GAP else BANNER_TOP

/**
 * [projectId]'s group as the list shows it, out of what was read there.
 *
 * Called what the screens call it ([groupCaption]), or by its project when
 * its document could not be read: two such groups in one list are still told
 * apart.
 *
 * A group the device was taken off is still called by the last [roster] read
 * there, if any, but counts nobody and has nobody's place to leave.
 */
fun groupChoice(
    projectId: String,
    roster: Roster?,
    profiles: Map<String, MemberProfile>,
    unnamedPattern: String,
    searches: Map<String, Search>,
    ownSiren: Siren?,
    /** Who this device is signed in as in the group, or null when it is not. */
    uid: String?,
    now: Instant,
    presence: Presence = Presence.UNKNOWN,
    /** What the group was last known to be called, for when its roster is not read. */
    known: String? = null,
): GroupChoice {
    val removed = presence == Presence.REMOVED
    val sounding =
        ownSiren?.let {
            when (sirenState(it, now)) {
                SirenState.Waiting, is SirenState.Sounding -> true
                else -> false
            }
        } ?: false

    return GroupChoice(
        projectId = projectId,
        caption = roster?.let { groupCaption(it, profiles, unnamedPattern) } ?: known ?: projectId,
        members = roster?.members?.size?.takeUnless { removed },
        siren = sounding,
        search = standingSearches(searches, now).isNotEmpty(),
        leave =
        uid?.takeUnless { removed }?.let { howToLeave(roster, it) } ?: Leave.NOT_ON_ROSTER,
        removed = removed,
    )
}

/**
 * Every group of [connections] as the list shows it, kept up to date.
 *
 * Each group is read through its own connection, signed in as whoever that
 * project knows this device by: the same person has a different account id
 * in every project, and so does this device's siren.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun rememberGroupChoices(
    connections: List<GroupConnection>,
    deviceId: String,
): State<List<GroupChoice>> {
    val unnamed = stringResource(R.string.group_name_unnamed)
    val context = LocalContext.current.applicationContext
    val choices =
        remember(connections, deviceId, unnamed) {
            val perGroup =
                connections.map { connection ->
                    // Who the device is there, its siren there, which is
                    // addressed by that same account, and whether that
                    // account is still on the roster.
                    val session: Flow<Session> =
                        signedInTo(connection).flatMapLatest { uid ->
                            if (uid == null) {
                                flowOf(Session())
                            } else {
                                combine(
                                    phoneSiren(positionId(uid, deviceId), connection)
                                        .onStart { emit(null) },
                                    presenceIn(context, connection, uid)
                                        .onStart { emit(Presence.UNKNOWN) },
                                ) { siren, presence -> Session(uid, siren, presence) }
                            }
                        }
                    // The last roster read, kept past a refusal: a group the
                    // device was taken off is still called what it was called.
                    val named =
                        whileSignedIn(connection, none = null) { groupRoster(connection) }
                            .onStart { emit(null) }
                            .runningFold(Pair<Roster?, Roster?>(null, null)) { (_, last), now ->
                                now to (now ?: last)
                            }

                    combine(
                        named,
                        whileSignedIn(connection, none = emptyMap()) { memberProfiles(connection) }
                            .onStart { emit(emptyMap()) },
                        whileSignedIn(connection, none = emptyMap()) { groupSearches(connection) }
                            .onStart { emit(emptyMap()) },
                        session.onStart { emit(Session()) },
                        ticking(),
                    ) { (roster, last), profiles, searches, session, now ->
                        val removed = session.presence == Presence.REMOVED
                        val read = if (removed) last else roster
                        val choice =
                            groupChoice(
                                connection.projectId,
                                read,
                                profiles,
                                unnamed,
                                searches,
                                session.siren,
                                session.uid,
                                now,
                                session.presence,
                                GroupNames.of(context, connection.projectId),
                            )

                        // Kept for the next time the roster cannot be read:
                        // signed out there, the group cannot say its name.
                        if (read != null) {
                            GroupNames.note(context, connection.projectId, choice.caption)
                        }

                        choice
                    }
                }

            if (perGroup.isEmpty()) flowOf(emptyList()) else combine(perGroup) { it.toList() }
        }

    return choices.collectAsStateWithLifecycle(initialValue = emptyList())
}

/** Who the device is in one group, and what that account hears there. */
private data class Session(
    val uid: String? = null,
    val siren: Siren? = null,
    val presence: Presence = Presence.UNKNOWN,
)

/**
 * What [read] gives while this device has a session in [connection], and
 * [none] while it has not: a group signed out of refuses every read, and
 * asking it again and again would only fill the log until the next sign-in.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private fun <T> whileSignedIn(connection: GroupConnection, none: T, read: () -> Flow<T>): Flow<T> =
    signedInTo(connection).flatMapLatest { uid -> if (uid == null) flowOf(none) else read() }

private fun ticking(): Flow<Instant> = flow {
    while (true) {
        emit(Instant.now())
        delay(CHOICES_TICK_MS)
    }
}

/**
 * The chip naming the group the screen shows, under the row of round
 * buttons on the left; pressing it opens the list of groups.
 */
@Composable
fun GroupChip(
    caption: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** How far below the top it stands: under the round buttons on the map, flush where it has a label. */
    top: Dp = SWITCHER_TOP,
) {
    val described = stringResource(R.string.group_switcher_open, caption)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(SWITCHER_HEIGHT / 2),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
        modifier =
        modifier
            .padding(top = top, start = 12.dp)
            .heightIn(min = SWITCHER_HEIGHT)
            .widthIn(max = SWITCHER_MAX_WIDTH)
            .semantics { contentDescription = described },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, end = 12.dp).heightIn(min = SWITCHER_HEIGHT),
        ) {
            Text(
                text = caption,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )

            // The triangle that says the chip opens a list rather than
            // standing for the group on its own.
            ListTriangle(color = muted)
        }
    }
}

/**
 * The groups this device is in, from the bottom of the screen: the one shown
 * is marked, and under each name its members and what stands in it. Pressing
 * a group shows it, and its «⋮» offers to leave it without showing it first;
 * the last line opens the paste screen to join another.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSheet(
    choices: List<GroupChoice>,
    selected: String,
    onSelect: (String) -> Unit,
    onJoin: () -> Unit,
    onLeave: (GroupChoice) -> Unit,
    onClose: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Text(
                text = stringResource(R.string.group_switcher_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
            )

            for (choice in choices) {
                GroupRow(
                    choice = choice,
                    current = choice.projectId == selected,
                    onClick = { onSelect(choice.projectId) },
                    onLeave = { onLeave(choice) },
                )
            }

            Text(
                text = stringResource(R.string.group_switcher_join),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onJoin)
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun GroupRow(
    choice: GroupChoice,
    current: Boolean,
    onClick: () -> Unit,
    onLeave: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    val details =
        buildList {
            if (choice.removed) add(stringResource(R.string.group_switcher_removed))
            choice.members?.let {
                add(pluralStringResource(R.plurals.map_group_count, it, it))
            }
            // What stands in the group shown is on the map already; only the
            // others need it said here.
            if (!current && choice.siren) add(stringResource(R.string.group_switcher_siren))
            if (!current && choice.search) add(stringResource(R.string.group_switcher_search))
        }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
        Modifier
            .fillMaxWidth()
            .then(
                if (current) {
                    Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = choice.caption,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (current) {
            val shown = stringResource(R.string.group_switcher_current)

            Text(
                text = "✓",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { contentDescription = shown },
            )
        }

        Box {
            val more = stringResource(R.string.group_switcher_more, choice.caption)

            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.semantics { contentDescription = more },
            ) {
                Text(text = "⋮", style = MaterialTheme.typography.titleMedium)
            }

            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.group_leave)) },
                    onClick = {
                        menuOpen = false
                        onLeave()
                    },
                )
            }
        }
    }
}

/**
 * The chip, the list it opens and the question before leaving a group, for a
 * screen that has nothing else to coordinate them with.
 *
 * The sign-in screen of a group is such a screen: without it, a group picked
 * in the list before the device signed in to it could not be left for another
 * until a sign-in was started and cancelled. Drawn only with more than one
 * group, like the map's own chip.
 */
@Composable
fun GroupSwitcher(groups: GroupSwitch, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    var leaveAsk by remember { mutableStateOf<LeaveAsk?>(null) }

    if (!switcherShown(groups.choices.size)) {
        return
    }

    // Labelled here and not on the map: on the map the chip sits among the
    // group's own buttons, while here it is the only thing at the top of a
    // screen about something else, and reads as a stray name without a word
    // saying what it is.
    Column(modifier = modifier.padding(top = 12.dp)) {
        Text(
            text = stringResource(R.string.group_switcher_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 14.dp, bottom = 4.dp),
        )

        GroupChip(
            caption =
            groups.choices.firstOrNull { it.projectId == groups.selected }?.caption
                ?: groups.selected,
            onClick = { open = true },
            top = 0.dp,
        )
    }

    if (open) {
        GroupSheet(
            choices = groups.choices,
            selected = groups.selected,
            onSelect = { projectId ->
                open = false
                groups.onSelect(projectId)
            },
            onJoin = {
                open = false
                groups.onJoin()
            },
            onLeave = { choice ->
                open = false
                leaveAsk = LeaveAsk(choice.projectId, choice.caption, choice.leave)
            },
            onClose = { open = false },
        )
    }

    leaveAsk?.let { ask ->
        LeaveGroupDialog(
            ask = ask,
            onLeave = { groups.onLeave(ask) },
            onDismiss = { leaveAsk = null },
        )
    }
}

/** The small triangle pointing down that marks a value as one of a list. */
@Composable
internal fun ListTriangle(color: Color) {
    Canvas(modifier = Modifier.size(width = 10.dp, height = 5.dp)) {
        val triangle =
            Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2, size.height)
                close()
            }

        drawPath(triangle, color)
    }
}
