package io.github.vitalyostanin.geoshare.map

import android.graphics.Bitmap
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import java.time.Instant
import java.time.ZoneId

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

/** The circle a card carries: large enough for a face, small enough for a strip of cards. */
private val FACE_SIZE = 22.dp

/** What takes the colour out of the picture of a member whose position is old. */
private val GREYSCALE = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
internal fun MemberButtons(
    members: List<MemberPosition>,
    names: Map<String, String>,
    /** The account pictures, by member id; a member without one gets their initial. */
    faces: Map<String, Bitmap>,
    /** The positions whose card is open, marked in the strip. */
    openCards: Set<String>,
    now: Instant,
    /** The group's searches, so a phone being looked for is marked in the strip too. */
    searches: Map<String, Search>,
    /** Whether the sheet listing the members the strip has no room for is open. */
    showAll: Boolean,
    /** Whether ONE is waiting for a member to be chosen off this very strip. */
    awaitingTarget: Boolean,
    onSelect: (String) -> Unit,
    onShowAll: (Boolean) -> Unit,
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
                    onOpen = { onShowAll(true) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showAll && rest.isNotEmpty()) {
        MoreSheet(
            members = rest,
            names = names,
            faces = faces,
            now = now,
            searches = searches,
            onSelect = { positionId ->
                onShowAll(false)
                onSelect(positionId)
            },
            onClose = { onShowAll(false) },
        )
    }
}

/**
 * The members the strip has no room for, in a sheet over the map.
 *
 * A sheet rather than a list unfolded under the strip: the list pushed the
 * strip up the screen and took the map's height with it, and closing it wanted
 * a button of its own. The sheet is swiped away or dismissed by a tap beside
 * it, the way the help is, and choosing a member closes it too, since the card
 * that opens is what the member was chosen for.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreSheet(
    members: List<MemberPosition>,
    names: Map<String, String>,
    faces: Map<String, Bitmap>,
    now: Instant,
    searches: Map<String, Search>,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onClose) {
        Text(
            text = stringResource(R.string.map_more_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)) {
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
private fun MoreButton(count: Int, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable { onOpen() },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                text = stringResource(R.string.map_more),
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
internal fun MemberRow(
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
internal fun widthText(width: PositionWidth): String = when (width) {
    is PositionWidth.Metres -> stringResource(R.string.map_width_metres, width.count)
    is PositionWidth.Kilometres -> stringResource(R.string.map_width_kilometres, width.count)
}

/** The muted colour of the other figures on the card, and ochre once it is wide. */
@Composable
internal fun widthColour(wide: Boolean): Color = when {
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
internal fun leftOutColour(): Color = when {
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
internal fun batteryColour(charging: Boolean): Color = when {
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
internal fun MemberFace(memberId: String, name: String, picture: Bitmap?, stale: Boolean) {
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
