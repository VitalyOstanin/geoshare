package io.github.vitalyostanin.geoshare.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.managed.ManagedConfiguration
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
    onSignInWithEmail: (email: String, password: String) -> Unit,
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
                    onSignInWithEmail = onSignInWithEmail,
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
    onSignInWithEmail: (email: String, password: String) -> Unit,
) {
    // Kept across a rotation and across a failed attempt: after a wrong
    // password the form stays where it was, with the address still in it.
    var emailForm by rememberSaveable { mutableStateOf(false) }

    // About the Google account; the form carries its own note instead.
    if (!emailForm) {
        Text(
            text = stringResource(R.string.sign_in_explanation),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }

    if (emailForm) {
        EmailForm(
            onSubmit = onSignInWithEmail,
            onBack = { emailForm = false },
        )
    } else {
        Button(onClick = onSignIn) {
            Text(text = stringResource(R.string.sign_in_action))
        }

        // Second to Google: for a group whose member has no Google account
        // to share under, such as a device set up and managed for somebody.
        OutlinedButton(onClick = { emailForm = true }) {
            Text(text = stringResource(R.string.sign_in_email_action))
        }
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

        SignInFailure.Offline,
        SignInFailure.Misconfigured,
        SignInFailure.WrongCredentials,
        SignInFailure.TooManyAttempts,
        ->
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
    SignInFailure.WrongCredentials -> R.string.sign_in_failed_wrong_credentials
    SignInFailure.TooManyAttempts -> R.string.sign_in_failed_too_many_attempts
    else -> R.string.sign_in_failed_misconfigured
}

/**
 * The address and password of an account made for the member by the project
 * owner. There is no way to make one here, nor to get a password back, and
 * the note under the form says whom to ask.
 */
@Composable
private fun EmailForm(onSubmit: (email: String, password: String) -> Unit, onBack: () -> Unit) {
    // The address is kept across a rotation and offered from the managed
    // configuration when there is one; the password is not kept anywhere.
    var email by rememberSaveable {
        mutableStateOf(ManagedConfiguration.current.expectedAccount.orEmpty())
    }
    var password by remember { mutableStateOf("") }
    val ready = email.isNotBlank() && password.isNotEmpty()
    val submit = { if (ready) onSubmit(email.trim(), password) }

    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text(text = stringResource(R.string.sign_in_email_address)) },
        singleLine = true,
        keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    )

    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text(text = stringResource(R.string.sign_in_email_password)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
    )

    Button(onClick = { submit() }, enabled = ready) {
        Text(text = stringResource(R.string.sign_in_email_submit))
    }

    OutlinedButton(onClick = onBack) {
        Text(text = stringResource(R.string.sign_in_email_back))
    }

    Text(
        text = stringResource(R.string.sign_in_email_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}
