package io.github.vitalyostanin.geoshare.map

import android.graphics.Bitmap
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.collection.JumpReason
import io.github.vitalyostanin.geoshare.collection.foundByLabel
import io.github.vitalyostanin.geoshare.collection.leftOutReasonLabel
import io.github.vitalyostanin.geoshare.collection.modeLabel
import io.github.vitalyostanin.geoshare.collection.sourceChoiceLabel
import io.github.vitalyostanin.geoshare.collection.sourceLeftOut
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
                onClose = onClose,
                onNamePhone = onNamePhone,
                onNamePerson = onNamePerson,
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
    onClose: () -> Unit,
    onNamePhone: () -> Unit,
    onNamePerson: (() -> Unit)?,
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

/** Narrower than a text button's own, so that the close keeps the names their width. */
private val HEADER_BUTTON_PADDING = PaddingValues(horizontal = 8.dp, vertical = 4.dp)

/** Rounded at the top only: the card rises from the strip the way a sheet does. */
private val SHEET_SHAPE = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/** How far the card is pulled before letting go counts as pulling it open or shut. */
private val PULL_THRESHOLD = 48.dp

/**
 * The strip across the top of the card that says it is pulled rather than
 * pressed, pulled itself or tapped for the same result.
 *
 * The tap is there for TalkBack, which has no drag to offer, and for whoever
 * reads the strip as a button.
 */
@Composable
private fun SheetHandle(
    detailsOpen: Boolean,
    onDrag: (Float) -> Unit,
    onDragStopped: () -> Unit,
    onClick: () -> Unit,
) {
    val label =
        stringResource(
            if (detailsOpen) R.string.map_detail_collapse else R.string.map_detail_expand,
        )

    Box(
        contentAlignment = Alignment.Center,
        modifier =
        Modifier
            .fillMaxWidth()
            .height(HANDLE_AREA)
            .draggable(
                state = rememberDraggableState(onDrag),
                orientation = Orientation.Vertical,
                onDragStopped = { onDragStopped() },
            )
            .clickable(onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        Box(
            modifier =
            Modifier
                .size(width = 32.dp, height = 4.dp)
                .background(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    RoundedCornerShape(2.dp),
                ),
        )
    }
}

/** Tall enough to be caught by a thumb, the height the handle of a sheet has. */
private val HANDLE_AREA = 24.dp

/**
 * How far the card has been pulled since the finger went down, and what
 * letting go of it means.
 *
 * Up opens the rows under the summary; down shuts them, and down from a card
 * already shut closes it. The pull arrives both from the handle and from the
 * body of the card, whatever part of a drag the body's own scroll leaves over.
 */
private class SheetPull(
    private val threshold: Float,
    private val detailsOpen: () -> Boolean,
    private val onDetailsOpen: (Boolean) -> Unit,
    private val onClose: () -> Unit,
) {
    private var pulled = 0f

    fun drag(delta: Float) {
        pulled += delta
    }

    fun settle() {
        val open = detailsOpen()

        when {
            pulled < -threshold && !open -> onDetailsOpen(true)
            pulled > threshold && open -> onDetailsOpen(false)
            pulled > threshold -> onClose()
        }
        pulled = 0f
    }

    val connection =
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero

                drag(available.y)

                return Offset(0f, available.y)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                settle()

                return Velocity.Zero
            }
        }
}

@Composable
private fun rememberSheetPull(
    detailsOpen: Boolean,
    onDetailsOpen: (Boolean) -> Unit,
    onClose: () -> Unit,
): SheetPull {
    val threshold = with(LocalDensity.current) { PULL_THRESHOLD.toPx() }
    val open by rememberUpdatedState(detailsOpen)
    val setOpen by rememberUpdatedState(onDetailsOpen)
    val close by rememberUpdatedState(onClose)

    return remember(threshold) {
        SheetPull(
            threshold = threshold,
            detailsOpen = { open },
            onDetailsOpen = { setOpen(it) },
            onClose = { close() },
        )
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
internal val SOUGHT_ENDS_AT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM HH:mm")

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
