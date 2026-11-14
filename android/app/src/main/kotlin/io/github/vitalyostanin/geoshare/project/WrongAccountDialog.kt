package io.github.vitalyostanin.geoshare.project

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R

/**
 * Screen Е of the invitation mockup (ADR-0019): the invitation carried across
 * the restart names another account than the one signed in. Both are shown
 * shortened, as the link carries the invited one. Switching accounts keeps the
 * invitation for the next sign-in; putting it off drops it, and the link can be
 * opened again.
 *
 * The same dialog points out an account other than the one the managed
 * configuration expects; there [expectedAddress] is set, the wording is about
 * the device rather than an invitation, and there is nothing kept to speak of.
 *
 * With [switchAllowed] off, as under the managed configuration's account lock,
 * the switching button stays but is disabled, with the reason under the text.
 */
@Composable
fun WrongAccountDialog(
    invitedAddress: String,
    signedInAddress: String,
    onLater: () -> Unit,
    onSwitchAccount: () -> Unit,
    expectedAddress: String? = null,
    switchAllowed: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = {
            Text(
                text = if (expectedAddress == null) {
                    stringResource(R.string.connect_wrong_account_title, invitedAddress)
                } else {
                    stringResource(R.string.expected_account_title, expectedAddress)
                },
                color = MaterialTheme.colorScheme.error,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(
                        if (expectedAddress == null) {
                            R.string.connect_wrong_account_text
                        } else {
                            R.string.expected_account_text
                        },
                        signedInAddress,
                    ),
                )

                if (!switchAllowed) {
                    Text(
                        text = stringResource(R.string.managed_by_controller),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSwitchAccount, enabled = switchAllowed) {
                Text(text = stringResource(R.string.connect_wrong_account_switch))
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) {
                Text(text = stringResource(R.string.connect_wrong_account_later))
            }
        },
    )
}
