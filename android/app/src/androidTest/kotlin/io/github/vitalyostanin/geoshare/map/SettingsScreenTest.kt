package io.github.vitalyostanin.geoshare.map

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.vitalyostanin.geoshare.BuildConfig
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.download.DownloadState
import io.github.vitalyostanin.geoshare.service.AidingErasure
import io.github.vitalyostanin.geoshare.service.LanguageChoice
import io.github.vitalyostanin.geoshare.service.ThemeChoice
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * The settings screen drawn on a device, with nothing behind it.
 *
 * The screen is one long scrolling column, so what sits at its foot is
 * reached only by scrolling, which a test on the JVM does not do. Both rows
 * checked here are the last ones: the way to the licences, and the version a
 * member reporting a fault is asked for.
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun show(onOpenLicences: () -> Unit = {}) {
        composeRule.setContent {
            SettingsScreen(
                settings = quietSettings(),
                actions = quietActions(),
                profiles = emptyMap(),
                ownMemberId = "member-a",
                now = Instant.now(),
                onClose = {},
                onOpenAdmin = {},
                onOpenMyPhones = {},
                onOpenLicences = onOpenLicences,
                onLeaveGroup = null,
                groupCaption = "",
                severalGroups = false,
                removed = null,
            )
        }
    }

    @Test
    fun licencesRowOpensTheLicences() {
        var opened = 0
        show(onOpenLicences = { opened++ })

        composeRule
            .onNodeWithText(context.getString(R.string.licences_open))
            .performScrollTo()
            .performClick()

        assertTrue("the licences were not opened", opened == 1)
    }

    @Test
    fun versionRowShowsTheBuiltVersion() {
        show()

        composeRule
            .onNodeWithText(context.getString(R.string.app_version, BuildConfig.VERSION_NAME))
            .performScrollTo()
            .assertExists()
    }
}

/** A device that shares nothing, hears nothing and sits in no group. */
private fun quietSettings() = SettingsState(
    sharing = false,
    permissionNotice = null,
    theme = ThemeChoice.System,
    language = LanguageChoice.System,
    sharedWith = emptyMap(),
    jumpsFiltered = false,
    sourcesLeftOut = emptySet(),
    aidingDoubted = emptySet(),
    downloads = DownloadState(allowed = emptySet(), waitingInRoaming = false),
    batteryUnrestricted = false,
    sirenThroughSilence = false,
    startupScreen = false,
    startupSet = false,
    roster = null,
    rosterAnswered = true,
    grants = emptyMap(),
    invitation = null,
    site = null,
    groupTrouble = false,
    phoneName = null,
)

/** Actions that do nothing and answer the way a database that is not there would. */
private fun quietActions() = SettingsActions(
    onSharingChange = {},
    onJumpsFilteredChange = {},
    onSourceUseChange = { _, _ -> },
    onDownloadNetworkChange = { _, _ -> },
    onForgetPlace = {},
    onForgetAiding = { AidingErasure.Untouched },
    onThemeChange = {},
    onLanguageChange = {},
    onShare = { _, _ -> },
    onOpenSettings = {},
    onAllowBattery = {},
    onAllowSirenThroughSilence = {},
    onOpenStartup = {},
    onStartupSetChange = {},
    onFoundGroup = {},
    onMakeInvitation = {},
    onJoinGroup = {},
    onPasteLink = {},
    onRemoveMember = {},
    onSetRunning = { _, _ -> },
    onRenameGroup = { WriteOutcome.UNANSWERED },
    onPhoneNameChange = {},
    onSignOut = {},
    onSearch = { _, _, _, _ -> WriteOutcome.UNANSWERED },
    onEndSearch = {},
    sirenOf = { flowOf(null) },
    onCallSiren = { WriteOutcome.UNANSWERED },
    onStopSiren = {},
    resetOf = { flowOf(null) },
    onAskReset = { WriteOutcome.UNANSWERED },
    onForgetPhone = { false },
    invitationStanding = { flowOf(null) },
)
