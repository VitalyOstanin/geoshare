package io.github.vitalyostanin.geoshare.map

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R

/** Half of one blink of a standing search: dimming, then the same back. */
private const val BLINK_HALF_MS = 600

/** How far a blink dims the button. */
private const val BLINK_DIMMEST = 0.25f

/**
 * The round button that opens a search for a member, and ends it once they
 * are found.
 *
 * Outlined in red while nobody is looking, like the siren beside it; filled
 * and blinking while a search stands, so that a row being looked for is seen
 * before it is read. The words it used to carry are its description, for the
 * screen reader; the help on the screen says the same to everybody else.
 */
@Composable
fun SearchButton(
    standing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Off while the database is silent, for a standing search: ending it writes at once. */
    enabled: Boolean = true,
) {
    val icon = painterResource(R.drawable.ic_search_alarm)

    if (standing) {
        val blinking = rememberInfiniteTransition(label = "search")
        val shown by blinking.animateFloat(
            initialValue = 1f,
            targetValue = BLINK_DIMMEST,
            animationSpec =
            infiniteRepeatable(
                animation = tween(BLINK_HALF_MS, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "blink",
        )

        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = Color(SOUGHT_COLOUR),
                contentColor = Color.White,
            ),
            modifier = modifier.alpha(shown),
        ) {
            Icon(painter = icon, contentDescription = stringResource(R.string.search_found))
        }
    } else {
        OutlinedIconButton(
            onClick = onClick,
            border = BorderStroke(1.dp, Color(SOUGHT_COLOUR)),
            modifier = modifier,
        ) {
            Icon(
                painter = icon,
                contentDescription = stringResource(R.string.search_start),
                tint = Color(SOUGHT_COLOUR),
            )
        }
    }
}
