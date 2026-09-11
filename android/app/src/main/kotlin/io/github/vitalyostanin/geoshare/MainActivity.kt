package io.github.vitalyostanin.geoshare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.vitalyostanin.geoshare.auth.SignInController
import io.github.vitalyostanin.geoshare.auth.SignInScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var signIn: SignInController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        signIn =
            SignInController(
                // Written into the resources by the google-services plugin,
                // out of the same file that names the project.
                serverClientId = getString(R.string.default_web_client_id),
                credentials = CredentialManager.create(this),
            )

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state by signIn.state.collectAsStateWithLifecycle()

                    SignInScreen(
                        state = state,
                        onSignIn = { lifecycleScope.launch { signIn.signIn(this@MainActivity) } },
                        onSignOut = { lifecycleScope.launch { signIn.signOut() } },
                    )
                }
            }
        }
    }

    // The session is watched only while the screen is up: nothing else in the
    // application reads it yet, and the service will keep its own watch.
    override fun onStart() {
        super.onStart()
        signIn.start()
    }

    override fun onStop() {
        signIn.stop()
        super.onStop()
    }
}
