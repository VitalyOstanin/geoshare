package io.github.vitalyostanin.geoshare.map

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/** As big as the account picture it stands in a row with. */
private val ROUND_BUTTON_SIZE = 40.dp

/**
 * One of the controls drawn over the map: a circle with a mark in it.
 *
 * Every control over the map is this one shape, so that the row of them reads
 * as a row rather than as a pill, a box and a circle that happen to be near
 * each other. What a control does is said in [label], which is what TalkBack
 * reads; the mark is for the eye of a member who has used it before.
 */
@Composable
fun MapRoundButton(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** A mode that is doing something, set apart from the one that leaves the map alone. */
    active: Boolean = false,
    /** A mode whose subject is not on the map just now, drawn in the quieter ink. */
    dimmed: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(ROUND_BUTTON_SIZE),
        shape = CircleShape,
        // Over a map rather than over a page: without a ground of its own the
        // mark sits on whatever the tiles happen to be.
        color =
        if (active) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                tint =
                if (dimmed) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}
