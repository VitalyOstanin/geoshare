package io.github.vitalyostanin.geoshare.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

/**
 * The way in, and nothing else.
 *
 * What a signed-in member sees is the map; this screen is what stands in its
 * place until there is an account behind it.
 */
@Composable
fun SignInScreen(state: AuthState, onSignIn: () -> Unit, modifier: Modifier = Modifier) {
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

            // The caller shows the map instead of this screen; the branch is
            // here because the state is a closed set, not because it happens.
            is AuthState.SignedIn -> Unit
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

private fun failureMessage(failure: SignInFailure): Int = when (failure) {
    SignInFailure.Cancelled -> R.string.sign_in_failed_unknown
    SignInFailure.NoAccount -> R.string.sign_in_failed_no_account
    SignInFailure.Offline -> R.string.sign_in_failed_offline
    SignInFailure.Misconfigured -> R.string.sign_in_failed_misconfigured
    SignInFailure.Unknown -> R.string.sign_in_failed_unknown
}
