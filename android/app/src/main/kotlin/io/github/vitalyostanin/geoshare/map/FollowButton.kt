package io.github.vitalyostanin.geoshare.map

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import kotlinx.coroutines.delay

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
private const val FOLLOW_LABEL_MS = 2_500L

/**
 * How wide a row of the map's drop-down menus is: the follow menu's and the
 * sources'. The hint under the follow menu is as wide, so it wraps the same way.
 */
internal val MENU_ROW_WIDTH = 260.dp

/** How faded the follow button is while a hand holds the following still, as on the page. */
private const val PAUSED_ALPHA = 0.75f

/** A target that has gone from the map: the button half there, as the page draws it. */
private const val LOST_ALPHA = 0.5f

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
internal fun FollowButton(
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
    val label = followLabel(mode, targetName)

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
        delay(FOLLOW_LABEL_MS)
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
                .alpha(
                    when {
                        !targetAvailable -> LOST_ALPHA
                        paused -> PAUSED_ALPHA
                        else -> 1f
                    },
                ),
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
            FollowFace(icon = icon, label = label, showLabel = showLabel)
        }

        FollowMenu(
            open = open,
            mode = mode,
            targetName = targetName,
            onDismiss = { open = false },
            onPick = { picked ->
                open = false
                onPick(picked)
            },
        )
    }
}

/** What the button says it is doing, out loud and to TalkBack alike. */
@Composable
private fun followLabel(mode: FollowMode, targetName: String?): String = when (mode) {
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

/** The inside of the button: the mode's mark, and its name while [showLabel] holds. */
@Composable
private fun FollowFace(icon: Int, label: String, showLabel: Boolean) {
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
                tint = MaterialTheme.colorScheme.onSurface,
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

/** The three modes to choose from, and what the second of them needs explained. */
@Composable
private fun FollowMenu(
    open: Boolean,
    mode: FollowMode,
    targetName: String?,
    onDismiss: () -> Unit,
    onPick: (FollowMode) -> Unit,
) {
    DropdownMenu(expanded = open, onDismissRequest = onDismiss) {
        FollowModeItem(
            icon = R.drawable.ic_follow_none,
            title = stringResource(R.string.map_follow_menu_none_title),
            subtitle = stringResource(R.string.map_follow_menu_none_subtitle),
            current = mode == FollowMode.NONE,
            onClick = { onPick(FollowMode.NONE) },
        )
        FollowModeItem(
            icon = R.drawable.ic_follow_one,
            title =
            targetName
                ?.let { stringResource(R.string.map_follow_menu_one_title_named, it) }
                ?: stringResource(R.string.map_follow_menu_one_title),
            subtitle = stringResource(R.string.map_follow_menu_one_subtitle),
            current = mode == FollowMode.ONE,
            onClick = { onPick(FollowMode.ONE) },
        )
        FollowModeItem(
            icon = R.drawable.ic_follow_all,
            title = stringResource(R.string.map_follow_menu_all_title),
            subtitle = stringResource(R.string.map_follow_menu_all_subtitle),
            current = mode == FollowMode.ALL,
            onClick = { onPick(FollowMode.ALL) },
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
                .width(MENU_ROW_WIDTH)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
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
            Column(modifier = Modifier.width(MENU_ROW_WIDTH)) {
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
