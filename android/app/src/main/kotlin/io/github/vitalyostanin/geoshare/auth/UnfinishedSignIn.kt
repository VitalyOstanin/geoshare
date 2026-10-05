package io.github.vitalyostanin.geoshare.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.R

/**
 * Says over the map that signing in to another group did not finish.
 *
 * A sign-in to a further group that ends without an answer takes the device
 * back to a group it is signed in to, and the sign-in screen with its own
 * explanation is gone before anybody reads it. Said once, for a while, with a
 * way to try that group again.
 */
@Composable
fun UnfinishedSignIn(
    /** What the group whose sign-in did not finish is called. */
    caption: String,
    onSignIn: () -> Unit,
    /** Called once the message is gone, by itself or by its button. */
    onShown: () -> Unit,
) {
    val host = remember { SnackbarHostState() }
    val message = stringResource(R.string.sign_in_unfinished_group, caption)
    val action = stringResource(R.string.sign_in_unfinished_go)

    LaunchedEffect(message) {
        val result =
            host.showSnackbar(
                message = message,
                actionLabel = action,
                duration = SnackbarDuration.Long,
            )

        onShown()

        if (result == SnackbarResult.ActionPerformed) {
            onSignIn()
        }
    }

    // Clear of the strip of members along the bottom of the map.
    Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)) {
        SnackbarHost(
            hostState = host,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp),
        )
    }
}
