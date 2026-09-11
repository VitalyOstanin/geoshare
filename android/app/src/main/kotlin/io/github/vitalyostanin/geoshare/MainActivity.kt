package io.github.vitalyostanin.geoshare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.vitalyostanin.geoshare.auth.SignInController
import io.github.vitalyostanin.geoshare.auth.SignInScreen
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.service.SharingService
import io.github.vitalyostanin.geoshare.service.SharingSetting
import io.github.vitalyostanin.geoshare.service.refusalToStart
import io.github.vitalyostanin.geoshare.service.startConditions
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var signIn: SignInController
    private lateinit var sharing: SharingSetting

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        signIn =
            SignInController(
                // Written into the resources by the google-services plugin,
                // out of the same file that names the project.
                serverClientId = getString(R.string.default_web_client_id),
                credentials = CredentialManager.create(this),
            )

        sharing = SharingSetting(this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state by signIn.state.collectAsStateWithLifecycle()
                    var sharingOn by remember { mutableStateOf(sharing.enabled) }

                    SignInScreen(
                        state = state,
                        sharing = sharingOn,
                        onSharingChange = { wanted ->
                            sharingOn = wanted
                            applySharing(wanted)
                        },
                        onSignIn = { lifecycleScope.launch { signIn.signIn(this@MainActivity) } },
                        onSignOut = { lifecycleScope.launch { signIn.signOut() } },
                    )
                }
            }
        }
    }

    /**
     * The switch states what the member wants; whether the service can act on
     * it is a separate question, and one the platform answers.
     */
    private fun applySharing(wanted: Boolean) {
        sharing.enabled = wanted

        if (!wanted) {
            SharingService.stop(this)

            return
        }

        val refusal = refusalToStart(startConditions(this, fromBackground = false))

        if (refusal == null) {
            SharingService.start(this)
        } else {
            AppLog.i(TAG, "not starting the service: $refusal")
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

    private companion object {
        const val TAG = "geoshare.main"
    }
}
