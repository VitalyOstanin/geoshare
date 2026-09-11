package io.github.vitalyostanin.geoshare

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import io.github.vitalyostanin.geoshare.auth.AuthState
import io.github.vitalyostanin.geoshare.auth.SignInController
import io.github.vitalyostanin.geoshare.auth.SignInScreen
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.KeepWatching
import io.github.vitalyostanin.geoshare.map.MapScreen
import io.github.vitalyostanin.geoshare.map.rememberMemberPositions
import io.github.vitalyostanin.geoshare.permission.PermissionStep
import io.github.vitalyostanin.geoshare.permission.devicePermissions
import io.github.vitalyostanin.geoshare.permission.nextPermissionStep
import io.github.vitalyostanin.geoshare.permission.permissionOf
import io.github.vitalyostanin.geoshare.service.SharingService
import io.github.vitalyostanin.geoshare.service.SharingSetting
import io.github.vitalyostanin.geoshare.service.refusalToStart
import io.github.vitalyostanin.geoshare.service.startConditions
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var signIn: SignInController
    private lateinit var sharing: SharingSetting

    /** The step the member has to finish on the settings screen, if any. */
    private var permissionNotice by mutableStateOf<PermissionStep?>(null)

    /**
     * The steps the system has already been asked about. A second request for
     * the same one is answered without showing anything, so asking twice would
     * look to the member like a button that does nothing.
     */
    private val asked = mutableSetOf<PermissionStep>()

    private val askPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            continueSharingSetup()
        }

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

                    when (val current = state) {
                        // Signed in, the map is the application; the sign-in
                        // screen has nothing left to say.
                        is AuthState.SignedIn -> {
                            val positions by rememberMemberPositions()

                            KeepWatching(current.memberId)

                            MapScreen(
                                positions = positions,
                                ownMemberId = current.memberId,
                                sharing = sharingOn,
                                permissionNotice = permissionNotice,
                                onSharingChange = { wanted ->
                                    sharingOn = wanted
                                    applySharing(wanted)
                                },
                                onOpenSettings = ::openApplicationSettings,
                                onSignOut = { lifecycleScope.launch { signIn.signOut() } },
                            )
                        }

                        else ->
                            SignInScreen(
                                state = current,
                                onSignIn = {
                                    lifecycleScope.launch { signIn.signIn(this@MainActivity) }
                                },
                            )
                    }
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
            permissionNotice = null
            SharingService.stop(this)

            return
        }

        continueSharingSetup()
    }

    /**
     * Asks for the next permission, or starts the service when none is left.
     *
     * Called again after every answer, so the three requests arrive one after
     * another rather than all at once.
     */
    private fun continueSharingSetup() {
        if (!sharing.enabled) {
            return
        }

        val step = nextPermissionStep(devicePermissions(this))

        if (step == null) {
            permissionNotice = null
            startSharingService()

            return
        }

        if (step in asked) {
            AppLog.i(TAG, "waiting for $step to be granted in the settings")
            permissionNotice = step

            return
        }

        asked += step
        askPermission.launch(arrayOf(permissionOf(step)))
    }

    private fun startSharingService() {
        val refusal = refusalToStart(startConditions(this, fromBackground = false))

        if (refusal == null) {
            SharingService.start(this)
        } else {
            AppLog.i(TAG, "not starting the service: $refusal")
        }
    }

    private fun openApplicationSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null),
            ),
        )
    }

    // The session is watched only while the screen is up: nothing else in the
    // application reads it yet, and the service will keep its own watch.
    override fun onStart() {
        super.onStart()
        signIn.start()
        // Coming back from the settings screen is the usual way a permission
        // changes, and nothing tells the application about it.
        continueSharingSetup()
    }

    override fun onStop() {
        signIn.stop()
        super.onStop()
    }

    private companion object {
        const val TAG = "geoshare.main"
    }
}
