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

    // A dismissed chooser is the person saying "not now"; answering it with an
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

private fun failureMessage(failure: SignInFailure): Int = when (failure) {
    SignInFailure.Cancelled -> R.string.sign_in_failed_unknown
    SignInFailure.Offline -> R.string.sign_in_failed_offline
    SignInFailure.Misconfigured -> R.string.sign_in_failed_misconfigured
    SignInFailure.Unknown -> R.string.sign_in_failed_unknown
}
