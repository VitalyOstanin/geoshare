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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.vitalyostanin.geoshare.auth.AuthState
import io.github.vitalyostanin.geoshare.auth.SignInController
import io.github.vitalyostanin.geoshare.auth.SignInScreen
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.KeepWatching
import io.github.vitalyostanin.geoshare.map.LocalLink
import io.github.vitalyostanin.geoshare.map.MapScreen
import io.github.vitalyostanin.geoshare.map.PublishProfile
import io.github.vitalyostanin.geoshare.map.SettingsActions
import io.github.vitalyostanin.geoshare.map.SettingsState
import io.github.vitalyostanin.geoshare.map.UnansweredException
import io.github.vitalyostanin.geoshare.map.WriteOutcome
import io.github.vitalyostanin.geoshare.map.labelledPositions
import io.github.vitalyostanin.geoshare.map.labelledProfiles
import io.github.vitalyostanin.geoshare.map.publishSharing
import io.github.vitalyostanin.geoshare.map.rememberDatabaseLink
import io.github.vitalyostanin.geoshare.map.rememberGroupRoster
import io.github.vitalyostanin.geoshare.map.rememberMemberGrants
import io.github.vitalyostanin.geoshare.map.rememberMemberPositions
import io.github.vitalyostanin.geoshare.map.rememberMemberProfiles
import io.github.vitalyostanin.geoshare.map.rememberOwnLabels
import io.github.vitalyostanin.geoshare.map.rememberOwnSharedWith
import io.github.vitalyostanin.geoshare.map.rememberSearches
import io.github.vitalyostanin.geoshare.map.rememberWatchers
import io.github.vitalyostanin.geoshare.map.withShare
import io.github.vitalyostanin.geoshare.map.withinAnswerWait
import io.github.vitalyostanin.geoshare.map.writeLabel
import io.github.vitalyostanin.geoshare.map.writePhoneName
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
import io.github.vitalyostanin.geoshare.service.AidingDoubtSetting
import io.github.vitalyostanin.geoshare.service.BackgroundWorkSetting
import io.github.vitalyostanin.geoshare.service.DeviceIdentity
import io.github.vitalyostanin.geoshare.service.JumpFilterSetting
import io.github.vitalyostanin.geoshare.service.LanguageChoice
import io.github.vitalyostanin.geoshare.service.SharingService
import io.github.vitalyostanin.geoshare.service.SharingSetting
import io.github.vitalyostanin.geoshare.service.SourcesSetting
import io.github.vitalyostanin.geoshare.service.ThemeSetting
import io.github.vitalyostanin.geoshare.service.callSiren
import io.github.vitalyostanin.geoshare.service.darkWanted
import io.github.vitalyostanin.geoshare.service.dndAccessGranted
import io.github.vitalyostanin.geoshare.service.endSearch
import io.github.vitalyostanin.geoshare.service.forgetAidingData
import io.github.vitalyostanin.geoshare.service.forgetPhone
import io.github.vitalyostanin.geoshare.service.foundGroup
import io.github.vitalyostanin.geoshare.service.joinGroup
import io.github.vitalyostanin.geoshare.service.languageChosen
import io.github.vitalyostanin.geoshare.service.languageTag
import io.github.vitalyostanin.geoshare.service.leavePreviousId
import io.github.vitalyostanin.geoshare.service.makeInvitation
import io.github.vitalyostanin.geoshare.service.openSearch
import io.github.vitalyostanin.geoshare.service.phoneSiren
import io.github.vitalyostanin.geoshare.service.positionId
import io.github.vitalyostanin.geoshare.service.refusalToStart
import io.github.vitalyostanin.geoshare.service.removeFromGroup
import io.github.vitalyostanin.geoshare.service.renewSearch
import io.github.vitalyostanin.geoshare.service.setRunning
import io.github.vitalyostanin.geoshare.service.startConditions
import io.github.vitalyostanin.geoshare.service.stopSiren
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant

/**
 * An AppCompat activity for one reason: the language a member picks inside the
 * application is applied through AppCompatDelegate, and on a phone before
 * Android 13 that only reaches the screens from an activity of this kind.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var signIn: SignInController
    private lateinit var sharing: SharingSetting

    /** What the member asked the application to be painted in. */
    private lateinit var appearance: ThemeSetting

    /** Which phone this is, so that a member carrying two is two markers rather than one. */
    private lateinit var device: DeviceIdentity

    /** Whether the member asked for the positions the phone cannot have reached to be left out. */
    private lateinit var jumps: JumpFilterSetting
    private lateinit var sources: SourcesSetting
    private lateinit var aidingDoubt: AidingDoubtSetting

    /** What the member has answered about letting the application keep running. */
    private lateinit var backgroundWork: BackgroundWorkSetting

    /** How the phone stands on restricting us, read again every time the screen comes up. */
    private var batteryFree by mutableStateOf(false)
    private var sirenLoud by mutableStateOf(false)

    /** Whether this phone has a startup list of its own, asked once at the start. */
    private var startupAvailable = false

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

        // Asked once: the answer is the set of applications installed by the
        // maker of the phone, which does not change while the screen is open,
        // and asking inside the drawing put five questions to the package
        // manager on every position that arrived.
        startupAvailable = startupScreen(this) != null

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
                    val state by signIn.state.collectAsStateWithLifecycle()
                    var sharingOn by remember { mutableStateOf(sharing.enabled) }
                    var startupSet by remember { mutableStateOf(backgroundWork.startupSet) }
                    var jumpsFiltered by remember { mutableStateOf(jumps.enabled) }
                    var sourcesLeftOut by remember { mutableStateOf(sources.leftOut) }
                    // Read once per screen rather than watched: the
                    // service writes it after half an hour of epochs, and
                    // a member who has just erased the aiding data is
                    // told by the row itself rather than by this.
                    val aidingDoubted = remember { aidingDoubt.doubted }

                    when (val current = state) {
                        // Signed in, the map is the application; the sign-in
                        // screen has nothing left to say.
                        is AuthState.SignedIn -> {
                            val profiles by rememberMemberProfiles()
                            val searches by rememberSearches()
                            val grants by rememberMemberGrants(profiles)
                            val ownDocument =
                                remember(current.memberId) {
                                    positionId(current.memberId, device.id)
                                }
                            val onTheMap by rememberMemberPositions(grants, ownDocument, searches)
                            val labels by rememberOwnLabels(current.memberId)
                            val sharedWith by rememberOwnSharedWith(current.memberId)
                            val roster by rememberGroupRoster()
                            val link by rememberDatabaseLink()
                            // Kept on the screen rather than in the database:
                            // the code is written where nobody may read it
                            // back, this phone included, so the only copy to
                            // show is the one just made.
                            var invitation by remember { mutableStateOf<String?>(null) }
                            // Whether the last write to the group was refused.
                            // Held beside the invitation and for the same
                            // reason: nothing in the database says it, and the
                            // person who pressed the button is owed an answer.
                            var groupTrouble by remember { mutableStateOf(false) }

                            // The service does the same; whichever starts
                            // first takes the old documents back.
                            LaunchedEffect(current.memberId) {
                                leavePreviousId(current.memberId, device)
                            }

                            // A service left without a session stops after a
                            // minute, and the account signed in after that —
                            // the phone handed to somebody else — would share
                            // nothing until the screen was next opened.
                            LaunchedEffect(current.memberId) {
                                continueSharingSetup()
                            }

                            KeepWatching(current.memberId, device.id)

                            // The other half of the same collection: this
                            // phone says it is looking, and reads who else is.
                            val watching by rememberWatchers(current.memberId, device.id)

                            PublishProfile(
                                memberId = current.memberId,
                                displayName = current.displayName,
                                pictureUrl = current.pictureUrl,
                                deviceId = device.id,
                            )

                            // Whether the database answers, told to every screen below the map
                            // rather than passed down through each of them.
                            CompositionLocalProvider(LocalLink provides link) {
                                MapScreen(
                                    // Called what this member called them, where
                                    // they did; the rest of the screen never
                                    // needs to know which name is whose.
                                    positions =
                                    labelledPositions(onTheMap.positions, labels, grants, searches),
                                    // Those whose position this phone is refused:
                                    // an empty map with a reason is not the same
                                    // thing as an empty map without one.
                                    outOfReach = onTheMap.outOfReach,
                                    profiles = labelledProfiles(profiles.orEmpty(), labels),
                                    watchers = watching,
                                    ownMemberId = current.memberId,
                                    ownPhoneId = positionId(current.memberId, device.id),
                                    ownDisplayName = current.displayName,
                                    ownPictureUrl = current.pictureUrl,
                                    searches = searches,
                                    settings =
                                    SettingsState(
                                        sharing = sharingOn,
                                        permissionNotice = permissionNotice,
                                        theme = theme,
                                        language = language,
                                        sharedWith = sharedWith,
                                        batteryUnrestricted = batteryFree,
                                        sirenThroughSilence = sirenLoud,
                                        startupScreen = startupAvailable,
                                        startupSet = startupSet,
                                        jumpsFiltered = jumpsFiltered,
                                        sourcesLeftOut = sourcesLeftOut,
                                        aidingDoubted = aidingDoubted,
                                        roster = roster,
                                        grants = grants,
                                        invitation = invitation,
                                        groupTrouble = groupTrouble,
                                        phoneName =
                                        grants[current.memberId]?.phones?.get(device.id)?.name,
                                        searches = searches,
                                    ),
                                    actions =
                                    SettingsActions(
                                        onSharingChange = { wanted ->
                                            sharingOn = wanted
                                            applySharing(wanted)
                                        },
                                        // Read by the service out of the same
                                        // store, which is where it looks on every
                                        // fix that arrives.
                                        onJumpsFilteredChange = { wanted ->
                                            jumpsFiltered = wanted
                                            jumps.enabled = wanted
                                        },
                                        // Left in the same store for the service
                                        // to pick up and spend: the request has
                                        // to outlive this screen, since the phone
                                        // may not have a fix to answer it with
                                        // until minutes from now.
                                        // The service reads the same store on
                                        // every answer it gets, so the choice
                                        // takes effect at the next fix rather
                                        // than at the next restart.
                                        onSourceUseChange = { source, counted ->
                                            sources.use(source, counted)
                                            sourcesLeftOut = sources.leftOut
                                        },
                                        onForgetPlace = { jumps.askToForget() },
                                        // Done here and not in the service:
                                        // the command is answered by the
                                        // receiver on the spot, and what it
                                        // answered is what the row shows.
                                        onForgetAiding = {
                                            forgetAidingData(this@MainActivity)
                                        },
                                        onThemeChange = { wanted ->
                                            theme = wanted
                                            appearance.choice = wanted
                                        },
                                        // Handed to the platform and not kept
                                        // here: it stores the answer, tells the
                                        // system settings about it and builds the
                                        // screens again in the new language.
                                        onLanguageChange = { wanted -> speak(wanted) },
                                        // Written into the grant, and left to be
                                        // read back: the phones of a member share
                                        // the same permissions, so the answer has
                                        // to come from where all of them look.
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
                                        // Read back through the phones of this
                                        // member, like every other phone reads it.
                                        onPhoneNameChange = { name ->
                                            lifecycleScope.launch {
                                                writePhoneName(current.memberId, device.id, name)
                                            }
                                        },
                                        onOpenSettings = ::openApplicationSettings,
                                        onAllowBattery = ::askBatteryExemption,
                                        // Read back in onStart, as the battery row is.
                                        onAllowSirenThroughSilence = ::askSirenThroughSilence,
                                        onOpenStartup = ::openStartupScreen,
                                        onStartupSetChange = { set ->
                                            startupSet = set
                                            backgroundWork.startupSet = set
                                        },
                                        // Refused when a group already exists,
                                        // which is the answer to the question the
                                        // screen cannot ask: a phone outside a
                                        // group cannot read the roster, so it
                                        // cannot tell an empty household from one
                                        // it is not in.
                                        onFoundGroup = {
                                            lifecycleScope.launch {
                                                groupTrouble =
                                                    !withGroup("found a group") {
                                                        foundGroup(
                                                            this@MainActivity,
                                                            current.memberId,
                                                        )
                                                    }
                                            }
                                        },
                                        onMakeInvitation = {
                                            lifecycleScope.launch {
                                                groupTrouble =
                                                    !withGroup("made an invitation") {
                                                        invitation =
                                                            makeInvitation(current.memberId)
                                                    }
                                            }
                                        },
                                        onJoinGroup = { invited ->
                                            lifecycleScope.launch {
                                                groupTrouble =
                                                    !withGroup("joined a group") {
                                                        joinGroup(
                                                            this@MainActivity,
                                                            current.memberId,
                                                            invited,
                                                        )
                                                    }
                                            }
                                        },
                                        onRemoveMember = { memberId ->
                                            lifecycleScope.launch {
                                                groupTrouble =
                                                    !withGroup("removed a member") {
                                                        roster?.let {
                                                            removeFromGroup(it, memberId)
                                                        }
                                                    }
                                            }
                                        },
                                        onSetRunning = { memberId, runs ->
                                            lifecycleScope.launch {
                                                groupTrouble =
                                                    !withGroup("changed who runs the group") {
                                                        setRunning(memberId, runs)
                                                    }
                                            }
                                        },
                                        onSignOut = {
                                            lifecycleScope.launch {
                                                forgetThisPhone(current.memberId)
                                                signIn.signOut()
                                            }
                                        },
                                        // A suspend function called from the
                                        // dialog's own coroutine rather than
                                        // wrapped in lifecycleScope.launch here:
                                        // the dialog has to stay open on a
                                        // refusal and say so, which nothing
                                        // fire-and-forget could tell it.
                                        onSearch = { phoneId, phoneName, renew, hours ->
                                            writeOutcome("a search could not be written") {
                                                if (renew) {
                                                    renewSearch(
                                                        phoneId,
                                                        phoneName,
                                                        current.memberId,
                                                        hours,
                                                    )
                                                } else {
                                                    openSearch(
                                                        phoneId,
                                                        phoneName,
                                                        current.memberId,
                                                        hours,
                                                    )
                                                }
                                            }
                                        },
                                        sirenOf = ::phoneSiren,
                                        onCallSiren = { phoneId ->
                                            writeOutcome("a siren could not be called") {
                                                callSiren(phoneId, current.memberId)
                                            }
                                        },
                                        onStopSiren = { phoneId ->
                                            try {
                                                withinAnswerWait {
                                                    stopSiren(phoneId, current.memberId)
                                                }
                                            } catch (cancellation: CancellationException) {
                                                throw cancellation
                                            } catch (refusal: Exception) {
                                                AppLog.w(
                                                    TAG,
                                                    "a siren could not be stopped",
                                                    refusal,
                                                )
                                            }
                                        },
                                        onForgetPhone = { phoneId ->
                                            try {
                                                forgetPhone(
                                                    current.memberId,
                                                    phoneId.substringAfter('.'),
                                                )
                                                true
                                            } catch (cancellation: CancellationException) {
                                                throw cancellation
                                            } catch (refusal: Exception) {
                                                AppLog.w(
                                                    TAG,
                                                    "a phone could not be removed",
                                                    refusal,
                                                )
                                                false
                                            }
                                        },
                                        onEndSearch = { phoneId ->
                                            try {
                                                withinAnswerWait { endSearch(phoneId) }
                                            } catch (cancellation: CancellationException) {
                                                throw cancellation
                                            } catch (refusal: Exception) {
                                                AppLog.w(
                                                    TAG,
                                                    "a search could not be ended",
                                                    refusal,
                                                )
                                            }
                                        },
                                    ),
                                    labels = labels,
                                    onLabel = { kind, key, name ->
                                        lifecycleScope.launch {
                                            writeLabel(current.memberId, kind, key, name)
                                        }
                                    },
                                )
                            }
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
    private fun speak(choice: LanguageChoice) {
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

    private fun askSirenThroughSilence() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
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
    private suspend fun withGroup(done: String, write: suspend () -> Unit): Boolean {
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
        sirenLoud = dndAccessGranted(this)
        continueSharingSetup()
    }

    override fun onStop() {
        signIn.stop()
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
    private suspend fun forgetThisPhone(memberId: String) {
        sharing.enabled = false
        stopService(Intent(this, SharingService::class.java))

        val forgotten =
            try {
                withTimeoutOrNull(SIGN_OUT_WAIT_MS) { forgetPhone(memberId, device.id) } != null
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
    private suspend fun writeOutcome(failure: String, write: suspend () -> Unit): WriteOutcome =
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

    private companion object {
        const val TAG = "geoshare.main"

        /** How long signing out waits for this phone to be taken out of the group. */
        const val SIGN_OUT_WAIT_MS = 5_000L
    }
}
