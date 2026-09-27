package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Told to the member being looked for: the same day-and-time reading a permission ends with. */
private val SOUGHT_ENDS_AT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM HH:mm")

/**
 * Told to the member being looked for, over the map: their place is open to
 * the whole group until the search ends, and they may end it themselves.
 *
 * Drawn only while their own search stands, and nowhere else: this is a fact
 * about being looked for, not a setting to be found again once it is over.
 */
@Composable
fun SoughtBanner(
    search: Search,
    openedByName: String,
    onFound: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text =
                stringResource(
                    R.string.sought_banner,
                    SOUGHT_ENDS_AT.format(search.endsAt.atZone(ZoneId.systemDefault())),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )

            Text(
                text = stringResource(R.string.sought_banner_note, openedByName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )

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
                colors =
                ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Text(text = stringResource(R.string.sought_found))
            }
        }
    }
}
