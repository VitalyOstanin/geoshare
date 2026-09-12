package io.github.vitalyostanin.geoshare

import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.vitalyostanin.geoshare.auth.AuthState
import io.github.vitalyostanin.geoshare.auth.SignInController
import io.github.vitalyostanin.geoshare.auth.SignInScreen
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.KeepWatching
import io.github.vitalyostanin.geoshare.map.MapScreen
import io.github.vitalyostanin.geoshare.map.PublishProfile
import io.github.vitalyostanin.geoshare.map.publishSharing
import io.github.vitalyostanin.geoshare.map.rememberMemberPositions
import io.github.vitalyostanin.geoshare.map.rememberMemberProfiles
import io.github.vitalyostanin.geoshare.map.rememberOwnSharedWith
import io.github.vitalyostanin.geoshare.map.withShare
import io.github.vitalyostanin.geoshare.permission.PermissionStep
import io.github.vitalyostanin.geoshare.permission.batteryExemptionRequest
import io.github.vitalyostanin.geoshare.permission.batteryExemptionWanted
import io.github.vitalyostanin.geoshare.permission.batteryUnrestricted
import io.github.vitalyostanin.geoshare.permission.devicePermissions
import io.github.vitalyostanin.geoshare.permission.nextPermissionStep
import io.github.vitalyostanin.geoshare.permission.permissionOf
import io.github.vitalyostanin.geoshare.permission.startupScreen
import io.github.vitalyostanin.geoshare.service.BackgroundWorkSetting
import io.github.vitalyostanin.geoshare.service.DeviceIdentity
import io.github.vitalyostanin.geoshare.service.SharingService
import io.github.vitalyostanin.geoshare.service.SharingSetting
import io.github.vitalyostanin.geoshare.service.ThemeSetting
import io.github.vitalyostanin.geoshare.service.darkWanted
import io.github.vitalyostanin.geoshare.service.refusalToStart
import io.github.vitalyostanin.geoshare.service.startConditions
import kotlinx.coroutines.launch
import java.time.Instant

class MainActivity : ComponentActivity() {
    private lateinit var signIn: SignInController
    private lateinit var sharing: SharingSetting

    /** What the member asked the application to be painted in. */
    private lateinit var appearance: ThemeSetting

    /** Which phone this is, so that a member carrying two is two markers rather than one. */
    private lateinit var device: DeviceIdentity

    /** What the member has answered about letting the application keep running. */
    private lateinit var backgroundWork: BackgroundWorkSetting

    /** How the phone stands on restricting us, read again every time the screen comes up. */
    private var batteryFree by mutableStateOf(false)

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

    // The answer arrives as a closed dialog rather than as a result: the
    // system tells nobody what was pressed, and the state is read back.
    private val askBattery =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            batteryFree = batteryUnrestricted(this)
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
        device = DeviceIdentity(this)
        appearance = ThemeSetting(this)
        backgroundWork = BackgroundWorkSetting(this)
        batteryFree = batteryUnrestricted(this)

        // Before anything is drawn: the window is what shows while the screen
        // is still being built, and a white flash on the way into a dark map
        // is what the setting exists to avoid.
        paintWindow(darkWanted(appearance.choice, systemIsDark()))

        setContent {
            var theme by remember { mutableStateOf(appearance.choice) }
            val dark = darkWanted(theme, isSystemInDarkTheme())

            // The window stays behind the screen for the whole session: it is
            // what the system animates to and from, and what shows around a
            // screen that does not fill it.
            LaunchedEffect(dark) { paintWindow(dark) }

            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state by signIn.state.collectAsStateWithLifecycle()
                    var sharingOn by remember { mutableStateOf(sharing.enabled) }
                    var startupSet by remember { mutableStateOf(backgroundWork.startupSet) }

                    when (val current = state) {
                        // Signed in, the map is the application; the sign-in
                        // screen has nothing left to say.
                        is AuthState.SignedIn -> {
                            val profiles by rememberMemberProfiles()
                            val positions by rememberMemberPositions(profiles)
                            val sharedWith by rememberOwnSharedWith(current.memberId)

                            KeepWatching(current.memberId, device.id)

                            PublishProfile(
                                memberId = current.memberId,
                                displayName = current.displayName,
                                pictureUrl = current.pictureUrl,
                                deviceId = device.id,
                            )

                            MapScreen(
                                positions = positions,
                                profiles = profiles.orEmpty(),
                                ownMemberId = current.memberId,
                                ownDisplayName = current.displayName,
                                ownPictureUrl = current.pictureUrl,
                                sharing = sharingOn,
                                permissionNotice = permissionNotice,
                                theme = theme,
                                sharedWith = sharedWith,
                                batteryUnrestricted = batteryFree,
                                startupScreen = startupScreen(this@MainActivity) != null,
                                startupSet = startupSet,
                                onSharingChange = { wanted ->
                                    sharingOn = wanted
                                    applySharing(wanted)
                                },
                                onThemeChange = { wanted ->
                                    theme = wanted
                                    appearance.choice = wanted
                                },
                                // Written into the profile, and left to be
                                // read back: the phones of a member share the
                                // same permissions, so the answer has to come
                                // from where all of them look for it.
                                onShare = { readerId, span ->
                                    lifecycleScope.launch {
                                        publishSharing(
                                            memberId = current.memberId,
                                            sharedWith =
                                            withShare(
                                                sharedWith = sharedWith,
                                                readerId = readerId,
                                                span = span,
                                                now = Instant.now(),
                                            ),
                                        )
                                    }
                                },
                                onOpenSettings = ::openApplicationSettings,
                                onAllowBattery = ::askBatteryExemption,
                                onOpenStartup = ::openStartupScreen,
                                onStartupSetChange = { set ->
                                    startupSet = set
                                    backgroundWork.startupSet = set
                                },
                                onSignOut = { lifecycleScope.launch { signIn.signOut() } },
                            )
                        }

                        else ->
                            SignInScreen(
                                state = current,
                                onSignIn = {
                                    lifecycleScope.launch { signIn.signIn(this@MainActivity) }
                                },
                                onSignInAsStandIn = { account ->
                                    lifecycleScope.launch { signIn.signInAsStandIn(account) }
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
            askBatteryExemptionOnce()

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

    /**
     * Paints the window, and tells the system bars what they are standing on.
     *
     * The bars draw over the screen rather than beside it, so their icons are
     * dark on a light application and light on a dark one; nothing else knows
     * which of the two this is.
     */
    private fun paintWindow(dark: Boolean) {
        val scheme = if (dark) darkColorScheme() else lightColorScheme()

        window.setBackgroundDrawable(ColorDrawable(scheme.background.toArgb()))

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }

    /** What the system is set to, read outside Compose. */
    private fun systemIsDark(): Boolean =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES

    /**
     * Asks once, and only once, for the phone to stop restricting us.
     *
     * Made where the permissions end rather than at startup: a member who has
     * just allowed the position is being asked about the same thing, and a
     * member who shares nothing has nothing to be interrupted.
     */
    private fun askBatteryExemptionOnce() {
        val wanted =
            batteryExemptionWanted(
                sharing = sharing.enabled,
                unrestricted = batteryFree,
                askedBefore = backgroundWork.batteryAsked,
            )

        if (!wanted) {
            return
        }

        backgroundWork.batteryAsked = true
        askBatteryExemption()
    }

    private fun askBatteryExemption() {
        askBattery.launch(batteryExemptionRequest(this))
    }

    /**
     * Opens the manufacturer's own startup list.
     *
     * The row that calls this is shown only when such a screen was found, so
     * the absent case is a phone whose screen went away between the two, and
     * the application settings are the nearest thing left.
     */
    private fun openStartupScreen() {
        val screen = startupScreen(this)

        if (screen == null) {
            openApplicationSettings()

            return
        }

        startActivity(screen)
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
        batteryFree = batteryUnrestricted(this)
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
