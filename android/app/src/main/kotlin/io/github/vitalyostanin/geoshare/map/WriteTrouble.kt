package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R

/**
 * What a window offering a call says under its choices: that the database is
 * silent and the call would not get through, that the last attempt was
 * refused, or that it went unanswered and will go out later. Nothing
 * otherwise.
 */
@Composable
fun WriteTrouble(trouble: WriteOutcome?) {
    val words =
        when {
            LocalLink.current.lost -> R.string.link_lost_action
            trouble == WriteOutcome.REFUSED -> R.string.group_refused
            trouble == WriteOutcome.UNANSWERED -> R.string.write_unanswered
            else -> return
        }

    Text(
        text = stringResource(words),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/**
 * Whether a window's go button may be pressed: not while a write is on its
 * way, not while the database is silent — a call is not left queued to ring
 * an hour later — and not after one went unanswered, since Firestore still
 * holds that one and sends it once the database is back.
 */
@Composable
fun writable(busy: Boolean, trouble: WriteOutcome?): Boolean =
    !busy && !LocalLink.current.lost && trouble != WriteOutcome.UNANSWERED
