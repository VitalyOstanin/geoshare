package io.github.vitalyostanin.geoshare

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
import io.github.vitalyostanin.geoshare.auth.AuthState
import io.github.vitalyostanin.geoshare.auth.SignInController
import io.github.vitalyostanin.geoshare.auth.SignInScreen
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.UnansweredException
import io.github.vitalyostanin.geoshare.map.WriteOutcome
import io.github.vitalyostanin.geoshare.map.withinAnswerWait
import io.github.vitalyostanin.geoshare.permission.PermissionStep
import io.github.vitalyostanin.geoshare.permission.batteryExemptionRequest
import io.github.vitalyostanin.geoshare.permission.batteryExemptionWanted
import io.github.vitalyostanin.geoshare.permission.batteryUnrestricted
import io.github.vitalyostanin.geoshare.permission.devicePermissions
import io.github.vitalyostanin.geoshare.permission.movementGranted
import io.github.vitalyostanin.geoshare.permission.movementPermissionWanted
import io.github.vitalyostanin.geoshare.permission.nextPermissionStep
import io.github.vitalyostanin.geoshare.permission.permissionOf
import io.github.vitalyostanin.geoshare.permission.startupScreen
import io.github.vitalyostanin.geoshare.project.ConnectFlow
import io.github.vitalyostanin.geoshare.project.ConnectScreen
import io.github.vitalyostanin.geoshare.project.ConnectState
import io.github.vitalyostanin.geoshare.project.Connections
import io.github.vitalyostanin.geoshare.project.GroupConnection
import io.github.vitalyostanin.geoshare.project.INHERITED
import io.github.vitalyostanin.geoshare.project.PendingInvitationStore
import io.github.vitalyostanin.geoshare.project.fetchSettings
import io.github.vitalyostanin.geoshare.project.pendingInvitationFor
import io.github.vitalyostanin.geoshare.service.AidingDoubtSetting
import io.github.vitalyostanin.geoshare.service.BackgroundWorkSetting
import io.github.vitalyostanin.geoshare.service.DeviceIdentity
import io.github.vitalyostanin.geoshare.service.JumpFilterSetting
import io.github.vitalyostanin.geoshare.service.LanguageChoice
import io.github.vitalyostanin.geoshare.service.SharingService
import io.github.vitalyostanin.geoshare.service.SharingSetting
import io.github.vitalyostanin.geoshare.service.SourcesSetting
import io.github.vitalyostanin.geoshare.service.ThemeSetting
import io.github.vitalyostanin.geoshare.service.darkWanted
import io.github.vitalyostanin.geoshare.service.dndAccessGranted
import io.github.vitalyostanin.geoshare.service.forgetPhone
import io.github.vitalyostanin.geoshare.service.languageChosen
import io.github.vitalyostanin.geoshare.service.languageTag
import io.github.vitalyostanin.geoshare.service.refusalToStart
import io.github.vitalyostanin.geoshare.service.startConditions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * An AppCompat activity for one reason: the language a member picks inside the
 * application is applied through AppCompatDelegate, and on a phone before
 * Android 13 that only reaches the screens from an activity of this kind.
 */
class MainActivity : AppCompatActivity() {
    /**
     * One sign-in per group's project (ADR-0002), by the name of the Firebase
     * instance it signs in to: each project holds its own session, and the
     * screen of a group shows that group's. Made the first time a group is
     * shown, and started then if the screen is up.
     */
    private val signIns = mutableMapOf<String, Pair<FirebaseApp, SignInController>>()
    private var watchingSessions = false

    /** The sign-in of the group the screens show. */
    internal val signIn: SignInController
        get() = signInOf(checkNotNull(Connections.selected.value) { "no group is shown" })

    private lateinit var sharing: SharingSetting

    /**
     * What a connection link is doing, whether or not this phone already
     * has a project — a link can arrive either way. Created before the
     * placeholder below is decided, since it is what would move this phone
     * past it. [ConnectState.Done] is acted on by [acceptConnection], which
     * is set watching for it as soon as this is created.
     */
    internal lateinit var connectFlow: ConnectFlow

    /** What the member asked the application to be painted in. */
    internal lateinit var appearance: ThemeSetting

    /** Which phone this is, so that a member carrying two is two markers rather than one. */
    internal lateinit var device: DeviceIdentity

    /** Whether the member asked for the positions the phone cannot have reached to be left out. */
    internal lateinit var jumps: JumpFilterSetting
    internal lateinit var sources: SourcesSetting
    private lateinit var aidingDoubt: AidingDoubtSetting

    /** What the member has answered about letting the application keep running. */
    internal lateinit var backgroundWork: BackgroundWorkSetting

    /** How the phone stands on restricting us, read again every time the screen comes up. */
    internal var batteryFree by mutableStateOf(false)
    internal var sirenLoud by mutableStateOf(false)

    /** Whether this phone has a startup list of its own, asked once at the start. */
    internal var startupAvailable = false

    /** The step the member has to finish on the settings screen, if any. */
    internal var permissionNotice by mutableStateOf<PermissionStep?>(null)

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

    /**
     * The one request the service can do without.
     *
     * Its own launcher rather than the one above, because the answer leads
     * somewhere else: the three the service needs are asked again until they
     * are granted, while this one is asked once and then left alone.
     */
    private val askMovement =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            askBatteryExemptionOnce()
        }

    // The answer arrives as a closed dialog rather than as a result: the
    // system tells nobody what was pressed, and the state is read back.
    private val askBattery =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            batteryFree = batteryUnrestricted(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Said out loud rather than left to the platform, which from this
        // target version draws the window under the bars whatever anybody
        // says. Saying it here is what makes the padding below a decision
        // instead of a repair.
        enableEdgeToEdge()

        // Independent of the project check below: a link is worth acting on
        // whether or not this phone already has one, and singleTask means
        // this is the only place onCreate ever sees one — a later arrival
        // comes through onNewIntent instead.
        connectFlow = ConnectFlow(
            scope = lifecycleScope,
            fetch = ::fetchSettings,
            connected = { Connections.selected.value?.settings },
        )
        // A recreated activity (a rotation) still carries the link it was
        // opened with, which has already been acted on.
        if (savedInstanceState == null) {
            connectFlow.linkArrived(intent.dataString)
        }

        // Reacts to Done wherever it is reached — the placeholder screen for
        // a phone with no project yet, or the dialog offered over the map
        // for one switching to another — since accepting a link always ends
        // the same way.
        lifecycleScope.launch {
            connectFlow.state.collect { state ->
                if (state is ConnectState.Done) {
                    acceptConnection(state)
                }
            }
        }

        // Nothing below may touch Firebase without a family's project behind
        // it: SignInController alone would reach for FirebaseAuth.getInstance
        // as a default argument, and it would throw rather than answer. A
        // link is what moves the phone past this, so the screens that paint
        // ConnectFlow's states are all there is to build here.
        if (!Connections.connected) {
            setContent {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val connectState by connectFlow.state.collectAsStateWithLifecycle()

                        ConnectScreen(
                            state = connectState,
                            onPasteLink = connectFlow::pasteLink,
                            onCancel = connectFlow::cancel,
                            onRetry = connectFlow::retry,
                            onConnect = connectFlow::connect,
                        )
                    }
                }
            }

            return
        }

        // Asked once: the answer is the set of applications installed by the
        // maker of the phone, which does not change while the screen is open,
        // and asking inside the drawing put five questions to the package
        // manager on every position that arrived.
        startupAvailable = startupScreen(this) != null

        sharing = SharingSetting(this)
        device = DeviceIdentity(this)
        appearance = ThemeSetting(this)
        backgroundWork = BackgroundWorkSetting(this)
        jumps = JumpFilterSetting(this)
        sources = SourcesSetting(this)
        aidingDoubt = AidingDoubtSetting(this)
        batteryFree = batteryUnrestricted(this)
        sirenLoud = dndAccessGranted(this)

        // Before anything is drawn: the window is what shows while the screen
        // is still being built, and a white flash on the way into a dark map
        // is what the setting exists to avoid.
        paintWindow(darkWanted(appearance.choice, systemIsDark()))

        setContent {
            var theme by remember { mutableStateOf(appearance.choice) }
            // Read from the platform rather than kept here: the same answer is
            // given in the phone's own settings from Android 13, and whichever
            // place it was given in, this is where it is read back from.
            val language = languageChosen(spokenLanguages().toLanguageTags())
            val dark = darkWanted(theme, isSystemInDarkTheme())

            // The window stays behind the screen for the whole session: it is
            // what the system animates to and from, and what shows around a
            // screen that does not fill it.
            LaunchedEffect(dark) { paintWindow(dark) }

            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val shown by Connections.selected.collectAsStateWithLifecycle()
                    var sharingOn by remember { mutableStateOf(sharing.enabled) }
                    var startupSet by remember { mutableStateOf(backgroundWork.startupSet) }
                    var jumpsFiltered by remember { mutableStateOf(jumps.enabled) }
                    var sourcesLeftOut by remember { mutableStateOf(sources.leftOut) }
                    // Read once per screen rather than watched: the
                    // service writes it after half an hour of epochs, and
                    // a member who has just erased the aiding data is
                    // told by the row itself rather than by this.
                    val aidingDoubted = remember { aidingDoubt.doubted }

                    // A link can still arrive on a phone that already has a
                    // project, to offer an invitation or to name a different
                    // one; ConnectState.NotConnected and Done are left out
                    // since neither has anything of its own to show above
                    // the map.
                    val connectState by connectFlow.state.collectAsStateWithLifecycle()

                    if (connectState !is ConnectState.NotConnected &&
                        connectState !is ConnectState.Done
                    ) {
                        Dialog(
                            onDismissRequest = connectFlow::cancel,
                            properties = DialogProperties(usePlatformDefaultWidth = false),
                        ) {
                            Surface(modifier = Modifier.fillMaxSize()) {
                                ConnectScreen(
                                    state = connectState,
                                    onPasteLink = connectFlow::pasteLink,
                                    onCancel = connectFlow::cancel,
                                    onRetry = connectFlow::retry,
                                    onConnect = connectFlow::connect,
                                )
                            }
                        }
                    }

                    val connection = shown

                    // The last group left or removed while the screen is up:
                    // what is left to do is connect to another.
                    if (connection == null) {
                        ConnectScreen(
                            state = connectState,
                            onPasteLink = connectFlow::pasteLink,
                            onCancel = connectFlow::cancel,
                            onRetry = connectFlow::retry,
                            onConnect = connectFlow::connect,
                        )

                        return@Surface
                    }

                    val signIn = remember(connection.app) { signInOf(connection) }
                    val state by signIn.state.collectAsStateWithLifecycle()

                    when (val current = state) {
                        // Signed in, the map is the application; the sign-in
                        // screen has nothing left to say.
                        is AuthState.SignedIn ->
                            SignedInScreen(
                                activity = this,
                                connection = connection,
                                current = current,
                                theme = theme,
                                onTheme = { wanted -> theme = wanted },
                                language = language,
                                sharingOn = sharingOn,
                                onSharingOn = { wanted -> sharingOn = wanted },
                                startupSet = startupSet,
                                onStartupSet = { set -> startupSet = set },
                                jumpsFiltered = jumpsFiltered,
                                onJumpsFiltered = { wanted -> jumpsFiltered = wanted },
                                sourcesLeftOut = sourcesLeftOut,
                                onSourcesLeftOut = { left -> sourcesLeftOut = left },
                                aidingDoubted = aidingDoubted,
                            )

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
     * What [ConnectState.Done] leads to: the group's project joins the list
     * and is shown, and a group invitation carried alongside it is kept for
     * the sign-in that follows to spend.
     *
     * Nothing is restarted and nothing signed out: every group has a Firebase
     * instance of its own (ADR-0026), so the groups this device was already
     * in go on as they were. A link back to a project already listed shows
     * that group rather than starting it again, and its session stays — the
     * account is the same, and the invitation is spent straight away.
     *
     * The first group a device connects to is the one exception to staying on
     * the screen: the activity was built without a group to show, so it is
     * built again.
     */
    private fun acceptConnection(done: ConnectState.Done) {
        val first = !Connections.connected
        val projectId = done.settings.projectId

        PendingInvitationStore.save(this, pendingInvitationFor(done))

        if (Connections.of(projectId) == null) {
            Connections.connect(this, done.settings, INHERITED)
        } else {
            Connections.select(this, projectId)
        }

        connectFlow.cancel()

        if (first) {
            recreate()
        }
    }

    /**
     * The sign-in of [connection]'s project, made the first time it is asked
     * for. A group removed and joined again gets a new instance, and so a new
     * sign-in in place of the one made for the instance that is gone.
     */
    internal fun signInOf(connection: GroupConnection): SignInController {
        val kept = signIns[connection.app.name]

        if (kept != null && kept.first === connection.app) {
            return kept.second
        }

        kept?.second?.stop()

        val made = SignInController(connection.auth)

        signIns[connection.app.name] = connection.app to made

        if (watchingSessions) {
            made.start()
        }

        return made
    }

    /**
     * The switch states what the member wants; whether the service can act on
     * it is a separate question, and one the platform answers.
     */
    internal fun applySharing(wanted: Boolean) {
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
    internal fun continueSharingSetup() {
        if (!sharing.enabled) {
            return
        }

        val step = nextPermissionStep(devicePermissions(this))

        if (step == null) {
            permissionNotice = null
            startSharingService()

            // Movement first and the battery after it, one dialog at a time:
            // the second is launched from the answer to the first.
            askMovementOnce()

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

    /** The languages the application was told to speak, empty while it follows the phone. */
    private fun spokenLanguages(): LocaleListCompat = AppCompatDelegate.getApplicationLocales()

    /**
     * Says which language to read the application in.
     *
     * An empty list is what "follow the phone" is made of, and handing it over
     * is what takes a member back to the language of their system.
     */
    internal fun speak(choice: LanguageChoice) {
        val tag = languageTag(choice)
        val locales =
            if (tag == null) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(tag)
            }

        AppCompatDelegate.setApplicationLocales(locales)
    }

    /** What the system is set to, read outside Compose. */
    private fun systemIsDark(): Boolean =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES

    /**
     * Asks once for the phone to say when its owner is on the move.
     *
     * Refusing costs the Moving mode and nothing else, so a member who says no
     * is not asked again and is not told anything is wrong. When there is
     * nothing to ask, the battery request follows straight away — otherwise it
     * follows the answer.
     */
    private fun askMovementOnce() {
        val wanted =
            movementPermissionWanted(
                sharing = sharing.enabled,
                granted = movementGranted(this),
                askedBefore = backgroundWork.movementAsked,
            )

        if (!wanted) {
            askBatteryExemptionOnce()

            return
        }

        backgroundWork.movementAsked = true
        askMovement.launch(Manifest.permission.ACTIVITY_RECOGNITION)
    }

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

    internal fun askSirenThroughSilence() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    internal fun askBatteryExemption() {
        askBattery.launch(batteryExemptionRequest(this))
    }

    /**
     * Opens the manufacturer's own startup list.
     *
     * The row that calls this is shown only when such a screen was found, so
     * the absent case is a phone whose screen went away between the two, and
     * the application settings are the nearest thing left.
     */
    // A write to the group, and what happens when the rules refuse it. They
    // refuse for reasons the screen could not have ruled out: founding a group
    // that already exists, a code that has run out, a member somebody else
    // removed a moment ago. None of them is a fault of this phone, so the
    // refusal is written down and the screen goes on showing what the roster
    // actually says.
    //
    // Whether it was taken is answered rather than kept here: the button was
    // pressed by somebody who is waiting to see something happen, and a
    // refusal that only reaches the log looks exactly like a write that did
    // nothing.
    internal suspend fun withGroup(done: String, write: suspend () -> Unit): Boolean {
        try {
            write()

            AppLog.i(TAG, done)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (refusal: Exception) {
            AppLog.w(TAG, "the group was left as it was: $done", refusal)

            return false
        }

        return true
    }

    internal fun openStartupScreen() {
        val screen = startupScreen(this)

        if (screen == null) {
            openApplicationSettings()

            return
        }

        startActivity(screen)
    }

    internal fun openApplicationSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null),
            ),
        )
    }

    // singleTask (AndroidManifest.xml) routes a second connection link here
    // instead of into a second instance of this activity, whether the first
    // one is in the foreground or was merely still on the back stack.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        connectFlow.linkArrived(intent.dataString)
    }

    // The session is watched only while the screen is up: nothing else in the
    // application reads it yet, and the service will keep its own watch.
    override fun onStart() {
        super.onStart()

        // Nothing to sign in to before a group is connected.
        if (!Connections.connected) {
            return
        }

        watchingSessions = true
        signIns.values.forEach { (_, controller) -> controller.start() }
        // Coming back from the settings screen is the usual way a permission
        // changes, and nothing tells the application about it.
        batteryFree = batteryUnrestricted(this)
        sirenLoud = dndAccessGranted(this)
        continueSharingSetup()
    }

    override fun onStop() {
        if (watchingSessions) {
            watchingSessions = false
            signIns.values.forEach { (_, controller) -> controller.stop() }
        }

        super.onStop()
    }

    /**
     * Takes this phone out of the group before its account signs out.
     *
     * A phone signed out is usually a phone changing hands, and what it wrote
     * would otherwise stay behind for good: a marker greying on everybody's
     * map, a line in the lists naming a phone the member no longer has. Only
     * the member may remove them, so it has to happen while they are still
     * signed in.
     *
     * The service is stopped first, so that no position written after the
     * removal puts the phone back, and the switch is turned off: whoever signs
     * in next decides for themselves whether to share. The wait is short, and a phone without
     * a network signs out all the same; its line is then removed from another
     * of the member's phones or from the page.
     */
    internal suspend fun forgetThisPhone(memberId: String, connection: GroupConnection) {
        sharing.enabled = false
        stopService(Intent(this, SharingService::class.java))

        val forgotten =
            try {
                withTimeoutOrNull(SIGN_OUT_WAIT_MS) {
                    forgetPhone(memberId, device.id, connection)
                } !=
                    null
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                AppLog.w(TAG, "could not take this phone out of the group", error)
                false
            }

        if (!forgotten) {
            AppLog.w(TAG, "signing out with this phone still listed")
        }
    }

    /**
     * How a write asked for from a window came out: answered within
     * [io.github.vitalyostanin.geoshare.map.ANSWER_WAIT], refused, or left
     * unanswered — in which case Firestore still holds it and sends it later.
     */
    internal suspend fun writeOutcome(failure: String, write: suspend () -> Unit): WriteOutcome =
        try {
            withinAnswerWait { write() }
            WriteOutcome.DONE
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (late: UnansweredException) {
            AppLog.w(TAG, "$failure: the database did not answer", late)
            WriteOutcome.UNANSWERED
        } catch (refusal: Exception) {
            AppLog.w(TAG, failure, refusal)
            WriteOutcome.REFUSED
        }

    internal companion object {
        const val TAG = "geoshare.main"

        /** How long signing out waits for this phone to be taken out of the group. */
        const val SIGN_OUT_WAIT_MS = 5_000L
    }
}
