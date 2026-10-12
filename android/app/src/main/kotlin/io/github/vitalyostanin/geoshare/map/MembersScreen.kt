package io.github.vitalyostanin.geoshare.map

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import java.time.Instant

/**
 * Every member of the group in one list, opened from the round button over
 * the map.
 *
 * The strip under the map holds two members and a button for the rest, and
 * the sheet behind that button covers only part of the screen; with a larger
 * group most of the choosing happened in a short list near the bottom edge.
 * This one takes the whole screen below the clock — the clock itself stays
 * over the map, the way the panels leave it — and each row says how far away
 * the member is, so that the list answers "where is everybody" before any
 * card is opened.
 *
 * Choosing a member closes the list and opens their card on the map, as the
 * sheet does; the back gesture closes it without a choice.
 */
@Composable
internal fun MembersScreen(
    /** The positions in the order the strip offers them. */
    members: List<MemberPosition>,
    /** Every position on the map, for measuring the distances from the viewer's own. */
    positions: List<MemberPosition>,
    ownMemberId: String,
    names: Map<String, String>,
    faces: Map<String, Bitmap>,
    now: Instant,
    searches: Map<String, Search>,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Clear of the clock above and of the gesture bar and the cut-outs around;
    // the surface itself still runs down to the bottom edge.
    val clear =
        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
    val count = members.size

    Surface(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        Column(modifier = Modifier.windowInsetsPadding(clear)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = stringResource(R.string.map_members_close),
                    )
                }

                Text(
                    text = stringResource(R.string.map_members),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )

                Text(
                    text = pluralStringResource(R.plurals.map_more_count, count, count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider()

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                memberRows(members, names, faces, searches, now, onSelect) { member ->
                    distanceFromViewer(member, positions, ownMemberId, now)
                }
            }
        }
    }
}
