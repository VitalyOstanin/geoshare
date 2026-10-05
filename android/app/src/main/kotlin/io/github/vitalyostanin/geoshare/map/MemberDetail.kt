package io.github.vitalyostanin.geoshare.map

import android.graphics.Bitmap
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.collection.foundByLabel
import io.github.vitalyostanin.geoshare.collection.modeLabel
import java.time.Instant

/**
 * Who is standing under a marker that stands for several members.
 *
 * A list rather than a stack of cards: which of the members was meant is a
 * question only the viewer can answer, and answering it is one more tap.
 */
@Composable
internal fun GroupDetail(
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
internal fun MemberDetail(
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
    /** Whether the card is pulled up far enough to show the rows under the summary. */
    detailsOpen: Boolean,
    onDetailsOpen: (Boolean) -> Unit,
    onClose: () -> Unit,
    onNamePhone: () -> Unit,
    onNamePerson: (() -> Unit)?,
    /** Shows them this member's place for a span; null where the card does not offer it. */
    onShareBack: ((ShareSpan) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val pull =
        rememberSheetPull(
            detailsOpen = detailsOpen,
            onDetailsOpen = onDetailsOpen,
            onClose = onClose,
        )

    Surface(
        modifier = modifier.fillMaxWidth().nestedScroll(pull.connection),
        shape = SHEET_SHAPE,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
    ) {
        Column {
            SheetHandle(
                detailsOpen = detailsOpen,
                onDrag = pull::drag,
                onDragStopped = pull::settle,
                onClick = { onDetailsOpen(!detailsOpen) },
            )

            DetailBody(
                member = member,
                name = name,
                face = face,
                now = now,
                away = away,
                search = search,
                openedByName = openedByName,
                runsTheGroup = runsTheGroup,
                onFound = onFound,
                detailsOpen = detailsOpen,
                onNamePhone = onNamePhone,
                onNamePerson = onNamePerson,
                onShareBack = onShareBack,
            )
        }
    }
}

/** Everything of the card under its handle, scrolled when it outgrows the map. */
@Composable
private fun DetailBody(
    member: MemberPosition,
    name: String,
    face: Bitmap?,
    now: Instant,
    away: Double?,
    search: Search?,
    openedByName: String?,
    runsTheGroup: Boolean,
    onFound: suspend () -> Unit,
    detailsOpen: Boolean,
    onNamePhone: () -> Unit,
    onNamePerson: (() -> Unit)?,
    onShareBack: ((ShareSpan) -> Unit)?,
) {
    Column(
        modifier =
        Modifier
            .animateContentSize()
            // Bounded by whoever placed it, so that a card is never taller
            // than the map it stands on; what does not fit is scrolled to
            // rather than cut off. A drag the scroll has no use for goes on to
            // the sheet, which is how a card that fits is pulled at all.
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 8.dp, bottom = 12.dp),
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
        }

        DetailSummary(member = member, away = away, now = now)

        // Under the summary, before the rows behind the handle: it is said
        // the moment the card is opened, the time it is noticed.
        if (onShareBack != null) {
            ShareBackLine(name = name, onShare = onShareBack)
        }

        if (search != null && openedByName != null) {
            DetailSought(
                search = search,
                openedByName = openedByName,
                showFound = runsTheGroup,
                onFound = onFound,
            )
        }

        DetailWarnings(member = member, now = now)

        // Everything else behind the pull of the handle: the summary above
        // is what a card is opened for, and these rows are what pushed it
        // past the height of a screen.
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

        knownNote()?.let { Fact(it) }
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
