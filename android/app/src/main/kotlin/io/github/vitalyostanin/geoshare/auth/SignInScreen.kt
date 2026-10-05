package io.github.vitalyostanin.geoshare.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.service.STAND_IN_ACCOUNTS
import io.github.vitalyostanin.geoshare.service.StandInServices

/**
 * The way in, and nothing else.
 *
 * What a signed-in member sees is the map; this screen is what stands in its
 * place until there is an account behind it.
 */
@Composable
fun SignInScreen(
    state: AuthState,
    onSignIn: () -> Unit,
    onSignInAsStandIn: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** The device was signed out because another device of the member removed it. */
    removedElsewhere: Boolean = false,
) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            // Scrolled as well as centred: sideways, on a split screen or at a
            // large system font, the column stands taller than the window, and
            // without this the button at the bottom of it cannot be reached.
            .verticalScroll(rememberScrollState())
            // Clear of the clock and of the gesture bar: from this target
            // version the window is drawn under both.
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (state) {
            AuthState.Starting -> CircularProgressIndicator()

            AuthState.SigningIn -> {
                CircularProgressIndicator()
                Text(text = stringResource(R.string.sign_in_running))
            }

            is AuthState.SignedOut ->
                SignedOut(
                    failure = state.failure,
                    removedElsewhere = removedElsewhere,
                    onSignIn = onSignIn,
                    onSignInAsStandIn = onSignInAsStandIn,
                )

            // The caller shows the map instead of this screen; the branch is
            // here because the state is a closed set, not because it happens.
            is AuthState.SignedIn -> Unit
        }
    }
}

@Composable
private fun SignedOut(
    failure: SignInFailure?,
    removedElsewhere: Boolean,
    onSignIn: () -> Unit,
    onSignInAsStandIn: (String) -> Unit,
) {
    Text(
        text = stringResource(R.string.sign_in_explanation),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )

    Button(onClick = onSignIn) {
        Text(text = stringResource(R.string.sign_in_action))
    }

    // Only while this build talks to the emulators, where there is no Google
    // account to choose and the stand-in household is what there is.
    if (StandInServices.inUse) {
        for (account in STAND_IN_ACCOUNTS) {
            Button(onClick = { onSignInAsStandIn(account) }) {
                Text(text = account.substringBefore('@'))
            }
        }
    }

    when (failure) {
        // Said quietly, like an unfinished sign-in: nothing went wrong on
        // this device, and signing in again is all it takes.
        null ->
            if (removedElsewhere) {
                Text(
                    text = stringResource(R.string.sign_in_removed_elsewhere),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

        // A page closed without an answer is as often the browser losing the
        // page's own state as the person saying "not now", and from here the
        // two look the same: said quietly, with what helps when it was the
        // browser, rather than as an error.
        SignInFailure.Cancelled, SignInFailure.Unknown ->
            Text(
                text = stringResource(R.string.sign_in_unfinished),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

        SignInFailure.Offline, SignInFailure.Misconfigured ->
            Text(
                text = stringResource(failureMessage(failure)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
    }
}

private fun failureMessage(failure: SignInFailure): Int = when (failure) {
    SignInFailure.Offline -> R.string.sign_in_failed_offline
    else -> R.string.sign_in_failed_misconfigured
}
