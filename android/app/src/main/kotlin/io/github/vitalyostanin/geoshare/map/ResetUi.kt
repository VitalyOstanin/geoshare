package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The round button that asks a device to forget its place. Plain rather than
 * red: unlike the siren, nothing on the device sounds, and the member holding
 * it is only told once it is done.
 */
@Composable
fun ResetButton(onClick: () -> Unit) {
    OutlinedIconButton(
        onClick = onClick,
        enabled = !LocalLink.current.lost,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_place_reset),
            contentDescription = stringResource(R.string.reset_hint),
        )
    }
}

/** What the row says of a request: waiting for the device, done, or never answered; nothing once it is old. */
@Composable
fun resetText(reset: Reset, now: Instant): String? {
    if (!resetTold(reset, now)) {
        return null
    }

    return when (val state = resetState(reset, now)) {
        ResetState.Waiting -> stringResource(
            R.string.reset_waiting,
            RESET_TIME.format(reset.askedAt.local()),
        )

        is ResetState.Done -> stringResource(
            R.string.reset_done,
            RESET_TIME.format(state.at.local()),
        )

        ResetState.NoAnswer -> stringResource(R.string.reset_no_answer)
    }
}

/** Confirming a reset: what it does, and how the last attempt to write it came out. */
@Composable
fun ResetDialog(
    phoneName: String,
    /** How the last attempt came out; null before any. */
    trouble: WriteOutcome?,
    busy: Boolean,
    onGo: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = stringResource(R.string.reset_confirm_title, phoneName)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.reset_confirm_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                WriteTrouble(trouble)
            }
        },
        confirmButton = {
            TextButton(onClick = onGo, enabled = writable(busy, trouble)) {
                Text(text = stringResource(R.string.reset_go))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(text = stringResource(R.string.search_cancel)) }
        },
    )
}

private fun Instant.local() = atZone(ZoneId.systemDefault())

/** When a request was made or carried out: the time alone, since a row tells of one only for a day. */
private val RESET_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
