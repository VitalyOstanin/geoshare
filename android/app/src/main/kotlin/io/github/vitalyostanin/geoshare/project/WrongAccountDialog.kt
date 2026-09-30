package io.github.vitalyostanin.geoshare.project

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.vitalyostanin.geoshare.R

/**
 * Screen Е of the invitation mockup (ADR-0019): the invitation carried across
 * the restart names another account than the one signed in. Both are shown
 * shortened, as the link carries the invited one. Switching accounts keeps the
 * invitation for the next sign-in; putting it off drops it, and the link can be
 * opened again.
 */
@Composable
fun WrongAccountDialog(
    invitedAddress: String,
    signedInAddress: String,
    onLater: () -> Unit,
    onSwitchAccount: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = {
            Text(
                text = stringResource(R.string.connect_wrong_account_title, invitedAddress),
                color = MaterialTheme.colorScheme.error,
            )
        },
        text = {
            Text(text = stringResource(R.string.connect_wrong_account_text, signedInAddress))
        },
        confirmButton = {
            TextButton(onClick = onSwitchAccount) {
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
