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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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
import io.github.vitalyostanin.geoshare.auth.SignInFailure
import io.github.vitalyostanin.geoshare.auth.SignInScreen
import io.github.vitalyostanin.geoshare.auth.UnfinishedSignIn
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.GroupSwitch
import io.github.vitalyostanin.geoshare.map.GroupSwitcher
import io.github.vitalyostanin.geoshare.map.UnansweredException
import io.github.vitalyostanin.geoshare.map.WriteOutcome
import io.github.vitalyostanin.geoshare.map.logFailure
import io.github.vitalyostanin.geoshare.map.rememberGroupChoices
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
import io.github.vitalyostanin.geoshare.project.GroupNames
import io.github.vitalyostanin.geoshare.project.INHERITED
import io.github.vitalyostanin.geoshare.project.PendingInvitationStore
import io.github.vitalyostanin.geoshare.project.RemovedHere
import io.github.vitalyostanin.geoshare.project.SignInStarted
import io.github.vitalyostanin.geoshare.project.fetchSettings
import io.github.vitalyostanin.geoshare.project.pendingInvitationFor
import io.github.vitalyostanin.geoshare.service.AidingDoubtSetting
import io.github.vitalyostanin.geoshare.service.BackgroundWorkSetting
import io.github.vitalyostanin.geoshare.service.DeviceIdentity
import io.github.vitalyostanin.geoshare.service.JumpFilterSetting
import io.github.vitalyostanin.geoshare.service.LanguageChoice
import io.github.vitalyostanin.geoshare.service.LanguageSetting
import io.github.vitalyostanin.geoshare.service.SharingService
import io.github.vitalyostanin.geoshare.service.SharingSetting
import io.github.vitalyostanin.geoshare.service.SourcesSetting
import io.github.vitalyostanin.geoshare.service.ThemeSetting
import io.github.vitalyostanin.geoshare.service.darkWanted
import io.github.vitalyostanin.geoshare.service.dndAccessGranted
import io.github.vitalyostanin.geoshare.service.languageChosen
import io.github.vitalyostanin.geoshare.service.languageTag
import io.github.vitalyostanin.geoshare.service.refusalToStart
import io.github.vitalyostanin.geoshare.service.startConditions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * An AppCompat activity for one reason: the language a member picks inside the
 * application is applied through AppCompatDelegate, and on a device before
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
     * What a connection link is doing, whether or not this device already
     * has a project — a link can arrive either way. Created before the
     * placeholder below is decided, since it is what would move this device
     * past it. [ConnectState.Done] is acted on by [acceptConnection], which
     * is set watching for it as soon as this is created.
     */
    internal lateinit var connectFlow: ConnectFlow

    /** What the member asked the application to be painted in. */
    internal lateinit var appearance: ThemeSetting

    /** Which device this is, so that a member carrying two is two markers rather than one. */
    internal lateinit var device: DeviceIdentity

    /** Whether the member asked for the positions the device cannot have reached to be left out. */
    internal lateinit var jumps: JumpFilterSetting
    internal lateinit var sources: SourcesSetting
    private lateinit var aidingDoubt: AidingDoubtSetting

    /** What the member has answered about letting the application keep running. */
    internal lateinit var backgroundWork: BackgroundWorkSetting

    /** How the device stands on restricting us, read again every time the screen comes up. */
    internal var batteryFree by mutableStateOf(false)
    internal var sirenLoud by mutableStateOf(false)

    /** Whether this device has a startup list of its own, asked once at the start. */
    internal var startupAvailable = false

    /** The step the member has to finish on the settings screen, if any. */
    internal var permissionNotice by mutableStateOf<PermissionStep?>(null)

    /**
     * What the sharing switch shows, kept here rather than in the drawing:
     * signing out turns sharing off from outside the screen, and a switch
     * that remembered its own value went on reading "on" over a setting
     * that was off, with nothing started when the member signed in again.
     */
    private var sharingOn by mutableStateOf(false)

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

        listenForLinks(savedInstanceState)

        // Nothing below may touch Firebase without a family's project behind
        // it: SignInController alone would reach for FirebaseAuth.getInstance
        // as a default argument, and it would throw rather than answer. A
        // link is what moves the device past this, so the screens that paint
        // ConnectFlow's states are all there is to build here.
        if (!Connections.connected) {
            setContent { ConnectOnly() }

            return
        }

        readSettings()

        // Before anything is drawn: the window is what shows while the screen
        // is still being built, and a white flash on the way into a dark map
        // is what the setting exists to avoid.
        paintWindow(darkWanted(appearance.choice, systemIsDark()))

        setContent { GroupsScreen() }
    }

    /** Follows the links this activity is handed, the first one included. */
    private fun listenForLinks(savedInstanceState: Bundle?) {
        // Independent of the project check in onCreate: a link is worth
        // acting on whether or not this device already has one, and singleTask
        // means this is the only place onCreate ever sees one — a later
        // arrival comes through onNewIntent instead.
        connectFlow = ConnectFlow(
            scope = lifecycleScope,
            fetch = ::fetchSettings,
            connected = { Connections.all.value.map { it.settings } },
        )
        // A recreated activity (a rotation) still carries the link it was
        // opened with, which has already been acted on.
        if (savedInstanceState == null) {
            connectFlow.linkArrived(intent.dataString)
        }

        // Reacts to Done wherever it is reached — the placeholder screen for
        // a device with no project yet, or the dialog offered over the map
        // for one switching to another — since accepting a link always ends
        // the same way.
        lifecycleScope.launch {
            connectFlow.state.collect { state ->
                if (state is ConnectState.Done) {
                    acceptConnection(state)
                }
            }
        }
    }

    /** All there is to show before the device is in any group: connecting to one. */
    @Composable
    private fun ConnectOnly() {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                val connectState by connectFlow.state.collectAsStateWithLifecycle()

                ConnectFlowScreen(connectFlow, connectState)
            }
        }
    }

    /** The settings the screens read, each from where it is kept. */
    private fun readSettings() {
        // Asked once: the answer is the set of applications installed by the
        // maker of the device, which does not change while the screen is open,
        // and asking inside the drawing put five questions to the package
        // manager on every position that arrived.
        startupAvailable = startupScreen(this) != null

        sharing = SharingSetting(this)
        sharingOn = sharing.enabled
        device = DeviceIdentity(this)
        appearance = ThemeSetting(this)
        backgroundWork = BackgroundWorkSetting(this)
        jumps = JumpFilterSetting(this)
        sources = SourcesSetting(this)
        aidingDoubt = AidingDoubtSetting(this)
        batteryFree = batteryUnrestricted(this)
        sirenLoud = dndAccessGranted(this)
    }

    /** The groups: the map of the one shown, or signing in to it. */
    @Composable
    private fun GroupsScreen() {
        var theme by remember { mutableStateOf(appearance.choice) }
        // Read from the platform rather than kept here: the same answer is
        // given in the device's own settings from Android 13, and whichever
        // place it was given in, this is where it is read back from.
        val language = languageChosen(spokenLanguages().toLanguageTags())
        val dark = darkWanted(theme, isSystemInDarkTheme())

        // The window stays behind the screen for the whole session: it is
        // what the system animates to and from, and what shows around a
        // screen that does not fill it.
        LaunchedEffect(dark) { paintWindow(dark) }

        MaterialTheme(colorScheme = appColours(dark)) {
            Surface(modifier = Modifier.fillMaxSize()) {
                val shown by Connections.selected.collectAsStateWithLifecycle()
                // The group whose sign-in did not finish, said over the map of
                // the group the device went back to.
                var unfinished by remember { mutableStateOf<String?>(null) }
                var startupSet by remember { mutableStateOf(backgroundWork.startupSet) }
                var jumpsFiltered by remember { mutableStateOf(jumps.enabled) }
                var sourcesLeftOut by remember { mutableStateOf(sources.leftOut) }
                // Read once per screen rather than watched: the service writes
                // it after half an hour of epochs, and a member who has just
                // erased the aiding data is told by the row itself rather than
                // by this.
                val aidingDoubted = remember { aidingDoubt.doubted }

                // A link can still arrive on a device that already has a
                // project, to offer an invitation or to name a different one;
                // ConnectState.NotConnected and Done are left out since
                // neither has anything of its own to show above the map.
                val connectState by connectFlow.state.collectAsStateWithLifecycle()

                ConnectFlowDialog(connectState)

                val connection = shown

                // The last group left or removed while the screen is up:
                // what is left to do is connect to another.
                if (connection == null) {
                    ConnectFlowScreen(connectFlow, connectState)

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
                            kept =
                            KeptChoices(
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
                            ),
                        )

                    else ->
                        SignInGate(connection, current, signIn) { projectId ->
                            unfinished = projectId
                        }
                }

                unfinished?.let { projectId ->
                    if (state is AuthState.SignedIn) {
                        UnfinishedSignIn(
                            caption = GroupNames.of(this, projectId) ?: projectId,
                            onSignIn = { signInAgain(projectId) },
                            onShown = { unfinished = null },
                        )
                    }
                }
            }
        }
    }

    /**
     * The link that arrived on a device already in a group, offering an
     * invitation or naming another project, over the map. NotConnected and
     * Done have nothing of their own to show there.
     */
    @Composable
    private fun ConnectFlowDialog(connectState: ConnectState) {
        if (connectState !is ConnectState.NotConnected &&
            connectState !is ConnectState.Done
        ) {
            Dialog(
                onDismissRequest = connectFlow::cancel,
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ConnectFlowScreen(connectFlow, connectState)
                }
            }
        }
    }

    /** Signing in to the group shown, and the ways back to another group. */
    @Composable
    private fun SignInGate(
        connection: GroupConnection,
        current: AuthState,
        signIn: SignInController,
        /** Told which group's sign-in did not finish when the device goes back to another. */
        onUnfinished: (String) -> Unit,
    ) {
        // A sign-in started here and cancelled leaves the group listed but
        // not shown: the device goes back to a group it is signed in to. Only
        // one started while this screen was up counts, or a group whose last
        // attempt was cancelled could never be shown again.
        var started by remember(connection) { mutableStateOf(false) }

        LaunchedEffect(current, connection) {
            if (current is AuthState.SigningIn) {
                started = true
            }

            val cancelled = (current as? AuthState.SignedOut)?.failure == SignInFailure.Cancelled

            if (started && cancelled) {
                Connections.all.value
                    .firstOrNull {
                        it !== connection && it.auth.currentUser != null
                    }
                    ?.let {
                        Connections.select(this@MainActivity, it.projectId)
                        onUnfinished(connection.projectId)
                    }
            }
        }

        val everyGroup by Connections.all.collectAsStateWithLifecycle()
        val choices by rememberGroupChoices(everyGroup, device.id)

        // A column rather than the chip laid over the screen: sideways the
        // centred text reached up to the chip. Below it the sign-in screen
        // keeps its own margin, and the status bar is taken here once.
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            // The way back to another group, the same chip and list as on the
            // map: a group picked before the device signed in to it is not a
            // place to be kept in.
            GroupSwitcher(
                groups =
                GroupSwitch(
                    choices = choices,
                    selected = connection.projectId,
                    onSelect = { projectId -> Connections.select(this@MainActivity, projectId) },
                    onJoin = connectFlow::askForLink,
                    onLeave = { ask ->
                        val leaving = Connections.of(ask.projectId)

                        // In the activity's scope, waited on from the leave
                        // dialog's: leaving the group shown takes this screen
                        // and the dialog away, and the dialog's scope with
                        // them, which would cancel the sign-out halfway rather
                        // than only the wait for it.
                        leaving == null ||
                            lifecycleScope.async { leave(leaving, ask.how) }.await()
                    },
                ),
            )

            // Read again whenever the session changes: the service signs out
            // of a group it was removed from while this screen may be up.
            val removedElsewhere =
                remember(current, connection) {
                    RemovedHere.has(this@MainActivity, connection.projectId)
                }

            SignInScreen(
                state = current,
                removedElsewhere = removedElsewhere,
                onSignIn = {
                    RemovedHere.forget(this@MainActivity, connection.projectId)
                    lifecycleScope.launch { signIn.signIn(this@MainActivity) }
                },
                onSignInAsStandIn = { account ->
                    lifecycleScope.launch { signIn.signInAsStandIn(account) }
                },
                modifier = Modifier.weight(1f),
            )
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

        PendingInvitationStore.save(this, projectId, pendingInvitationFor(done))
        GroupNames.note(this, projectId, done.invitation?.groupName)

        if (Connections.of(projectId) == null) {
            // The invitation's group from the start: the inherited one is not
            // there in a project of its own, and the service would write into
            // it, refused, until the invitation is spent.
            Connections.connect(this, done.settings, done.invitation?.groupId ?: INHERITED)
        } else {
            Connections.select(this, projectId)
        }

        connectFlow.cancel()

        if (first) {
            recreate()
        }
    }

    /** Shows [projectId]'s group and opens its sign-in page once more. */
    private fun signInAgain(projectId: String) {
        val connection = Connections.of(projectId) ?: return

        Connections.select(this, projectId)
        lifecycleScope.launch { signInOf(connection).signIn(this@MainActivity) }
    }

    /**
     * The sign-in of [connection]'s project, made the first time it is asked
     * for. A group removed and joined again in the same run comes back on the
     * same instance, and so to the same sign-in.
     */
    internal fun signInOf(connection: GroupConnection): SignInController {
        val kept = signIns[connection.app.name]

        if (kept != null && kept.first === connection.app) {
            return kept.second
        }

        kept?.second?.stop()

        val made =
            SignInController(
                connection.auth,
                SignInStarted.mark(this, connection.projectId),
            )

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
        val scheme = appColours(dark)

        window.setBackgroundDrawable(ColorDrawable(scheme.background.toArgb()))

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }

    /** The languages the application was told to speak, empty while it follows the device. */
    private fun spokenLanguages(): LocaleListCompat = AppCompatDelegate.getApplicationLocales()

    /**
     * Says which language to read the application in.
     *
     * An empty list is what "follow the device" is made of, and handing it over
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

        LanguageSetting(this).tag = tag
        AppCompatDelegate.setApplicationLocales(locales)
    }

    /** What the system is set to, read outside Compose. */
    private fun systemIsDark(): Boolean =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES

    /**
     * Asks once for the device to say when its owner is on the move.
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
     * Asks once, and only once, for the device to stop restricting us.
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
     * the absent case is a device whose screen went away between the two, and
     * the application settings are the nearest thing left.
     */
    // A write to the group, and what happens when the rules refuse it. They
    // refuse for reasons the screen could not have ruled out: founding a group
    // that already exists, a code that has run out, a member somebody else
    // removed a moment ago. None of them is a fault of this device, so the
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
            logFailure(TAG, "the group was left as it was: $done", refusal)

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
        signIns.values.forEach { (_, controller) ->
            controller.start()
            lifecycleScope.launch { controller.cameBack(this@MainActivity) }
        }
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
     * Stops the service and turns the switch off, ahead of this device
     * leaving the last group it is in or signing out: no position written
     * afterwards puts the device back, and whoever signs in next decides for
     * themselves whether to share.
     */
    internal fun stopSharing() {
        sharing.enabled = false
        sharingOn = false
        stopService(Intent(this, SharingService::class.java))
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
            logFailure(TAG, failure, refusal)

            WriteOutcome.REFUSED
        }

    internal companion object {
        const val TAG = "geoshare.main"

        /** How long signing out or leaving waits for each group to answer. */
        const val SIGN_OUT_WAIT_MS = 5_000L
    }
}

/** The connecting screen, driven by [flow]: offered on its own and over the map alike. */
@Composable
private fun ConnectFlowScreen(flow: ConnectFlow, state: ConnectState) {
    ConnectScreen(
        state = state,
        onPasteLink = flow::pasteLink,
        onCancel = flow::cancel,
        onRetry = flow::retry,
        onConnect = flow::connect,
    )
}
