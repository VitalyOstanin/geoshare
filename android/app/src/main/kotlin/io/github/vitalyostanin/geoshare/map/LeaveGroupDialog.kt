package io.github.vitalyostanin.geoshare.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import io.github.vitalyostanin.geoshare.membership.Leave
import kotlinx.coroutines.launch

/** A group the member asked to leave, with what leaving it would do for them. */
data class LeaveAsk(val projectId: String, val caption: String, val how: Leave)

/**
 * Asks before the member leaves [ask]'s group, saying what leaving does for
 * them: off the roster for good, or — for somebody the group cannot be left
 * by, or who is not on it — off this device only.
 *
 * [onLeave] is waited for here rather than fired and forgotten: a refusal
 * keeps the dialog open to say so, and the member tries again or gives up.
 */
@Composable
fun LeaveGroupDialog(ask: LeaveAsk, onLeave: suspend () -> Boolean, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var trouble by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.group_leave_title, ask.caption)) },
        text = { LeaveText(how = ask.how, trouble = trouble) },
        confirmButton = {
            TextButton(
                onClick = {
                    busy = true
                    scope.launch {
                        val left = onLeave()

                        busy = false
                        trouble = !left

                        if (left) {
                            onDismiss()
                        }
                    }
                },
                enabled = !busy,
            ) {
                Text(
                    text =
                    stringResource(
                        if (ask.how == Leave.ROSTER) {
                            R.string.group_leave_go
                        } else {
                            R.string.group_leave_device_go
                        },
                    ),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.search_cancel))
            }
        },
    )
}

/** What leaving does for the member, plus the refusal notice once [trouble] is set. */
@Composable
private fun LeaveText(how: Leave, trouble: Boolean) {
    Column {
        Text(
            text =
            stringResource(
                when (how) {
                    Leave.ROSTER -> R.string.group_leave_text
                    Leave.DEVICE_ONLY -> R.string.group_leave_runs_it_text
                    Leave.NOT_ON_ROSTER -> R.string.group_leave_not_on_roster_text
                },
            ),
        )

        if (trouble) {
            Text(
                text = stringResource(R.string.group_leave_refused),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
