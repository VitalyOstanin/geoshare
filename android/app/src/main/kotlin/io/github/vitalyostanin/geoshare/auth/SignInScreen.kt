package io.github.vitalyostanin.geoshare.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.permission.PermissionStep

/**
 * The whole application until the map arrives: either the way in, or who is
 * already in.
 */
@Composable
fun SignInScreen(
    state: AuthState,
    sharing: Boolean,
    /** The permission the member has to grant on the settings screen, if any. */
    permissionNotice: PermissionStep?,
    onSharingChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (state) {
            AuthState.Starting -> CircularProgressIndicator()

            AuthState.SigningIn -> {
                CircularProgressIndicator()
                Text(text = stringResource(R.string.sign_in_running))
            }

            is AuthState.SignedOut -> SignedOut(failure = state.failure, onSignIn = onSignIn)

            is AuthState.SignedIn ->
                SignedIn(
                    state = state,
                    sharing = sharing,
                    permissionNotice = permissionNotice,
                    onSharingChange = onSharingChange,
                    onOpenSettings = onOpenSettings,
                    onSignOut = onSignOut,
                )
        }
    }
}

@Composable
private fun SignedOut(failure: SignInFailure?, onSignIn: () -> Unit) {
    Text(
        text = stringResource(R.string.sign_in_explanation),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )

    Button(onClick = onSignIn) {
        Text(text = stringResource(R.string.sign_in_action))
    }

    // A dismissed chooser is the user saying "not now"; answering it with an
    // error message would be arguing.
    if (failure != null && failure != SignInFailure.Cancelled) {
        Text(
            text = stringResource(failureMessage(failure)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SignedIn(
    state: AuthState.SignedIn,
    sharing: Boolean,
    permissionNotice: PermissionStep?,
    onSharingChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onSignOut: () -> Unit,
) {
    Text(
        text = stringResource(
            R.string.signed_in_as,
            state.displayName ?: stringResource(R.string.signed_in_nameless),
        ),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )

    // The member id is what the position documents are named after, so it is
    // worth seeing until there is a map to look at instead.
    Text(
        text = stringResource(R.string.member_id, state.memberId),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.sharing_action))

        Switch(checked = sharing, onCheckedChange = onSharingChange)
    }

    // Shown once the system has stopped asking: from there on the answer is
    // changed on the settings screen, and saying so beats an inert switch.
    if (sharing && permissionNotice != null) {
        Text(
            text = stringResource(permissionMessage(permissionNotice)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )

        TextButton(onClick = onOpenSettings) {
            Text(text = stringResource(R.string.permission_open_settings))
        }
    }

    TextButton(onClick = onSignOut) {
        Text(text = stringResource(R.string.sign_out_action))
    }
}

private fun permissionMessage(step: PermissionStep): Int = when (step) {
    PermissionStep.Location -> R.string.permission_location_needed
    PermissionStep.Notifications -> R.string.permission_notifications_needed
    PermissionStep.BackgroundLocation -> R.string.permission_background_needed
}

private fun failureMessage(failure: SignInFailure): Int = when (failure) {
    SignInFailure.Cancelled -> R.string.sign_in_failed_unknown
    SignInFailure.NoAccount -> R.string.sign_in_failed_no_account
    SignInFailure.Offline -> R.string.sign_in_failed_offline
    SignInFailure.Misconfigured -> R.string.sign_in_failed_misconfigured
    SignInFailure.Unknown -> R.string.sign_in_failed_unknown
}
