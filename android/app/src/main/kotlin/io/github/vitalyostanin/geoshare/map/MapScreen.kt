package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.permission.PermissionStep
import kotlinx.coroutines.delay
import java.time.Instant

/** How often the ages on screen are recounted. Half a minute keeps "5 minutes ago" honest. */
private const val AGE_TICK_MILLIS = 30_000L

/** Enough characters of an id to tell two members apart at a glance. */
private const val SHORT_ID_LENGTH = 6

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
    ownMemberId: String,
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

    // A tap is a request to look at somebody, not a state: tapping the same
    // member twice has to bring the map back both times, and the counter is
    // what makes the second tap a change the map can see.
    fun lookAt(memberId: String) {
        focus = MapFocus(memberId = memberId, request = (focus?.request ?: 0) + 1)
    }

    Column(modifier = modifier.fillMaxSize()) {
        MemberMap(
            positions = positions,
            ownMemberId = ownMemberId,
            now = now,
            focus = focus,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )

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
                ownMemberId = ownMemberId,
                now = now,
                showAll = showAll,
                onSelect = ::lookAt,
                onToggleAll = { showAll = !showAll },
            )
        }

        HorizontalDivider()

        Controls(
            sharing = sharing,
            permissionNotice = permissionNotice,
            onSharingChange = onSharingChange,
            onOpenSettings = onOpenSettings,
            onSignOut = onSignOut,
        )
    }
}

@Composable
private fun MemberButtons(
    members: List<MemberPosition>,
    ownMemberId: String,
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
                own = member.memberId == ownMemberId,
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
                    own = member.memberId == ownMemberId,
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
    own: Boolean,
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
                    text = memberName(member.memberId, own),
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
    own: Boolean,
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
                text = memberName(member.memberId, own),
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

@Composable
private fun Controls(
    sharing: Boolean,
    permissionNotice: PermissionStep?,
    onSharingChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onSignOut: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.sharing_action), modifier = Modifier.weight(1f))

        Switch(checked = sharing, onCheckedChange = onSharingChange)

        TextButton(onClick = onSignOut) {
            Text(text = stringResource(R.string.sign_out_action))
        }
    }

    // Shown once the system has stopped asking: from there on the answer is
    // changed on the settings screen, and saying so beats an inert switch.
    if (sharing && permissionNotice != null) {
        Text(
            text = stringResource(permissionMessage(permissionNotice)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        TextButton(onClick = onOpenSettings, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(text = stringResource(R.string.permission_open_settings))
        }
    }
}

/** Until members carry names of their own, the first characters of the id tell them apart. */
@Composable
private fun memberName(memberId: String, own: Boolean): String =
    if (own) stringResource(R.string.map_member_you) else memberId.take(SHORT_ID_LENGTH)

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

private fun permissionMessage(step: PermissionStep): Int = when (step) {
    PermissionStep.Location -> R.string.permission_location_needed
    PermissionStep.Notifications -> R.string.permission_notifications_needed
    PermissionStep.BackgroundLocation -> R.string.permission_background_needed
}
