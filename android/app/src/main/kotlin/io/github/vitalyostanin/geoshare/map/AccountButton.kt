package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.vitalyostanin.geoshare.R

private val BUTTON_SIZE = 40.dp
private val DOT_SIZE = 12.dp

/**
 * The way into everything that is not the map.
 *
 * The account picture rather than a gear: it says both who is signed in and
 * where to change it, which is the arrangement every map application has
 * settled on, and it costs one circle in the corner. [updateAvailable] adds a
 * small dot to its corner: a new version is waiting in the settings, and the
 * member whose notifications are off would otherwise never hear of it.
 */
@Composable
fun AccountButton(
    displayName: String?,
    pictureUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    updateAvailable: Boolean = false,
) {
    // The dot sits on a box around the button rather than on the button,
    // whose clip to a circle would cut it off at the corner.
    Box(modifier = modifier.size(BUTTON_SIZE)) {
        AccountCircle(displayName, pictureUrl, updateAvailable, onClick)

        if (updateAvailable) {
            Box(
                modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .size(DOT_SIZE)
                    .background(MaterialTheme.colorScheme.error, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
            )
        }
    }
}

@Composable
private fun AccountCircle(
    displayName: String?,
    pictureUrl: String?,
    updateAvailable: Boolean,
    onClick: () -> Unit,
) {
    // The dot is only drawn, so a screen reader would never hear of the update
    // unless the button's own description says so. The description sits on the
    // button rather than on the picture, because the letter variant has no
    // picture to carry it.
    val description =
        stringResource(
            if (updateAvailable) R.string.map_open_settings_update else R.string.map_open_settings,
        )
    Surface(
        modifier =
        Modifier
            .size(BUTTON_SIZE)
            .clip(CircleShape)
            .clickable { onClick() }
            .semantics { contentDescription = description },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 4.dp,
        shadowElevation = 2.dp,
    ) {
        if (pictureUrl == null) {
            // An account without a picture still has a name, and its first
            // letter is enough to tell one account from another.
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text =
                    displayName?.take(1)?.uppercase()
                        ?: stringResource(R.string.map_account_nameless),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    // The button's description already stands for the letter.
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        } else {
            AsyncImage(
                model = pictureUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(BUTTON_SIZE).clip(CircleShape),
            )
        }
    }
}
