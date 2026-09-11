package io.github.vitalyostanin.geoshare.map

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
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.vitalyostanin.geoshare.R

private val BUTTON_SIZE = 40.dp

/**
 * The way into everything that is not the map.
 *
 * The account picture rather than a gear: it says both who is signed in and
 * where to change it, which is the arrangement every map application has
 * settled on, and it costs one circle in the corner.
 */
@Composable
fun AccountButton(
    displayName: String?,
    pictureUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(BUTTON_SIZE).clip(CircleShape).clickable { onClick() },
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
                )
            }
        } else {
            AsyncImage(
                model = pictureUrl,
                contentDescription = stringResource(R.string.map_open_settings),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(BUTTON_SIZE).clip(CircleShape),
            )
        }
    }
}
