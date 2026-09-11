package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.permission.PermissionStep
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** How often the ages on screen are recounted. Half a minute keeps "5 minutes ago" honest. */
private const val AGE_TICK_MILLIS = 30_000L

/** As much of the screen as the full list may take before it starts scrolling instead. */
private val LIST_MAX_HEIGHT = 220.dp

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
    /** What each member calls themselves, by member id; absent until they sign in. */
    profiles: Map<String, MemberProfile>,
    ownMemberId: String,
    ownDisplayName: String?,
    ownPictureUrl: String?,
    sharing: Boolean,
    /** The permission the member has to grant on the settings screen, if any. */
    permissionNotice: PermissionStep?,
    onSharingChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now by currentTime()
    val members = orderedForNavigation(positions, ownMemberId)
    var focus by remember { mutableStateOf<MapFocus?>(null) }
    var showAll by remember { mutableStateOf(false) }
    var openedMemberId by remember { mutableStateOf<String?>(null) }
    // The members under one marker, held by id for the same reason a single
    // member is: the list keeps arriving, and the card has to follow it.
    var openedGroup by remember { mutableStateOf<List<String>>(emptyList()) }
    var settingsOpen by remember { mutableStateOf(false) }
    // Held by id rather than by value: the position keeps arriving, and a card
    // showing the one from when it was opened would quietly go stale.
    val opened = positions.firstOrNull { it.memberId == openedMemberId }
    val group =
        openedGroup.mapNotNull { memberId ->
            positions.firstOrNull { it.memberId == memberId }
        }
    val ownName = stringResource(R.string.map_member_you)
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
    val names =
        positions.associate { position ->
            position.memberId to
                memberName(
                    profile = profiles[position.memberId],
                    memberId = position.memberId,
                    own = position.memberId == ownMemberId,
                    ownName = ownName,
                )
        }

    // A tap is a request to look at somebody, not a state: tapping the same
    // member twice has to bring the map back both times, and the counter is
    // what makes the second tap a change the map can see.
    fun lookAt(memberId: String) {
        focus = MapFocus(memberId = memberId, request = (focus?.request ?: 0) + 1)
        openedMemberId = memberId
        openedGroup = emptyList()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            MemberMap(
                positions = positions,
                names = names,
                faces = faces,
                ownMemberId = ownMemberId,
                now = now,
                groupLabel = groupLabel,
                focus = focus,
                onSelect = { memberId ->
                    openedMemberId = memberId
                    openedGroup = emptyList()
                },
                onSelectGroup = { memberIds ->
                    openedMemberId = null
                    openedGroup = memberIds
                },
                onDismiss = {
                    openedMemberId = null
                    openedGroup = emptyList()
                },
                modifier = Modifier.fillMaxSize(),
            )

            AccountButton(
                displayName = ownDisplayName,
                pictureUrl = ownPictureUrl,
                onClick = { settingsOpen = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            )

            if (opened != null) {
                MemberDetail(
                    member = opened,
                    name = names[opened.memberId] ?: shortName(opened.memberId),
                    now = now,
                    onClose = { openedMemberId = null },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                )
            } else if (group.size > 1) {
                GroupDetail(
                    members = group,
                    names = names,
                    now = now,
                    onSelect = { memberId ->
                        openedMemberId = memberId
                        openedGroup = emptyList()
                    },
                    onClose = { openedGroup = emptyList() },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                )
            }
        }

        HorizontalDivider()

        if (members.isEmpty()) {
            Text(
                text = stringResource(R.string.map_nobody_yet),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            MemberButtons(
                members = members,
                names = names,
                now = now,
                showAll = showAll,
                onSelect = ::lookAt,
                onToggleAll = { showAll = !showAll },
            )
        }
    }

    if (settingsOpen) {
        SettingsScreen(
            sharing = sharing,
            permissionNotice = permissionNotice,
            onSharingChange = onSharingChange,
            onOpenSettings = onOpenSettings,
            onSignOut = onSignOut,
            onClose = { settingsOpen = false },
        )
    }
}

@Composable
private fun MemberButtons(
    members: List<MemberPosition>,
    names: Map<String, String>,
    now: Instant,
    showAll: Boolean,
    onSelect: (String) -> Unit,
    onToggleAll: () -> Unit,
) {
    val shown = members.take(MEMBERS_SHOWN)
    val rest = members.drop(MEMBERS_SHOWN)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        shown.forEach { member ->
            MemberButton(
                member = member,
                name = names[member.memberId] ?: shortName(member.memberId),
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
            items(rest, key = { it.memberId }) { member ->
                MemberRow(
                    member = member,
                    name = names[member.memberId] ?: shortName(member.memberId),
                    now = now,
                    onSelect = onSelect,
                )
            }
        }
    }
}

@Composable
private fun MemberButton(
    member: MemberPosition,
    name: String,
    now: Instant,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable { onSelect(member.memberId) },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MemberDot(memberId = member.memberId)

                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            AgeText(member = member, now = now)
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
    now: Instant,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable { onSelect(member.memberId) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MemberDot(memberId = member.memberId)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
            )

            AgeText(member = member, now = now)
        }

        member.batteryPercent?.let { percent ->
            Text(
                text = stringResource(R.string.map_battery, percent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MemberDot(memberId: String) {
    Text(
        text = "●",
        color = Color(memberColour(memberId)),
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun AgeText(member: MemberPosition, now: Instant) {
    Text(
        text = ageText(positionAge(member.knownAt, now)),
        style = MaterialTheme.typography.bodySmall,
        // Grey is the whole point of the stale mark: an old position still
        // shows where the member was, and should not read as where they are.
        color =
        if (isStale(member.knownAt, now)) {
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
                items(members, key = { it.memberId }) { member ->
                    MemberRow(
                        member = member,
                        name = names[member.memberId] ?: shortName(member.memberId),
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
    now: Instant,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MemberDot(memberId = member.memberId)

                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )

                // A word rather than an icon: the icon packs are a dependency
                // of their own, and this is the only one the screen needs.
                TextButton(onClick = onClose) {
                    Text(text = stringResource(R.string.map_detail_close))
                }
            }

            DetailRow(R.string.map_detail_last_seen, ageText(positionAge(member.knownAt, now)))
            DetailRow(R.string.map_detail_at, momentText(member.knownAt))
            DetailRow(
                R.string.map_detail_mode,
                member.mode ?: stringResource(R.string.map_detail_unknown),
            )
            DetailRow(
                R.string.map_detail_battery,
                member.batteryPercent
                    ?.let { stringResource(R.string.map_battery, it) }
                    ?: stringResource(R.string.map_detail_unknown),
            )
            DetailRow(
                R.string.map_detail_position,
                stringResource(R.string.map_detail_coordinates, member.latitude, member.longitude),
            )
        }
    }
}

@Composable
private fun DetailRow(name: Int, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(name),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(text = value, style = MaterialTheme.typography.bodyMedium)
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
private fun ageText(age: PositionAge): String = when (age) {
    PositionAge.JustNow -> stringResource(R.string.map_age_just_now)
    is PositionAge.Minutes -> pluralAge(R.plurals.map_age_minutes, age.count)
    is PositionAge.Hours -> pluralAge(R.plurals.map_age_hours, age.count)
    is PositionAge.Days -> pluralAge(R.plurals.map_age_days, age.count)
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
