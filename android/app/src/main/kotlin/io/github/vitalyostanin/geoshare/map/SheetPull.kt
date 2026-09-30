package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R

/** Rounded at the top only: the card rises from the strip the way a sheet does. */
internal val SHEET_SHAPE = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

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
internal fun SheetHandle(
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
 * already shut closes it: the card has no button to close it with, the pull
 * down is that button, as are the back gesture and a tap on the map beside
 * it. The pull arrives both from the handle and from the body of the card,
 * whatever part of a drag the body's own scroll leaves over.
 */
internal class SheetPull(
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
internal fun rememberSheetPull(
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
