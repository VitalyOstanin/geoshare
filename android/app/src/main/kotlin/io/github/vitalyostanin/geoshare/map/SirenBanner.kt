package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

/**
 * Told to everyone in the group, over the map: a caller has just switched on
 * their phone's siren, and it is theirs to switch off again before the
 * twenty-second window the server tracks runs out.
 */
@Composable
fun SirenBanner(
    callerName: String,
    /** When the twenty seconds run out, by the server's clock. */
    until: Instant,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now by produceState(Instant.now(), until) {
        while (true) {
            value = Instant.now()
            delay(1_000)
        }
    }
    val secondsLeft = maxOf(0L, Duration.between(now, until).seconds)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.siren_notification_title, callerName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )

            Text(
                text =
                stringResource(R.string.siren_notification_text) + " " +
                    stringResource(R.string.siren_seconds_left, secondsLeft),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )

            Button(
                onClick = onStop,
                colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(painterResource(R.drawable.ic_siren_off), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(text = stringResource(R.string.siren_off))
            }
        }
    }
}
