package io.github.vitalyostanin.geoshare.project

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.vitalyostanin.geoshare.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val SITE = "family-ab12"

private val SETTINGS = ProjectSettings(
    version = 1,
    projectId = "family-ab12",
    applicationId = "1:123456789012:android:0123456789abcdef",
    apiKey = "AIzaExampleKeyNotReal",
    gcmSenderId = "123456789012",
    site = SITE,
)

/**
 * The screen a connection link is accepted on. Its captions come from the
 * link itself and anybody can write them; the site is where the settings were
 * actually read, and the one line on it worth checking.
 */
@RunWith(AndroidJUnit4::class)
class ConnectScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun show(invitation: Invitation?) {
        composeRule.setContent {
            ConnectScreen(
                state = ConnectState.Confirm(ConnectLink(SITE, invitation), SETTINGS),
                onPasteLink = {},
                onCancel = {},
                onRetry = {},
                onConnect = {},
            )
        }
    }

    // MEM-01: the link shows the group and who invited, each marked as from the link.
    @Test
    fun anInvitationShowsTheSiteAndMarksWhatTheLinkSays() {
        show(
            Invitation(
                groupId = "founder",
                code = "abcdefgh",
                inviter = "Mum",
                groupName = "Family",
                invitedAddress = "anna@example.com",
            ),
        )

        composeRule.onNodeWithText("$SITE.web.app").assertExists()
        val marks = composeRule
            .onAllNodesWithText(context.getString(R.string.connect_from_link))
            .fetchSemanticsNodes()
        assertEquals("one mark for each caption the link carries", 3, marks.size)
    }

    // MEM-01: a link without an invitation shows the site and marks nothing.
    @Test
    fun anOwnLinkShowsTheSiteWithNothingMarked() {
        show(invitation = null)

        composeRule.onNodeWithText("$SITE.web.app").assertExists()
        composeRule
            .onNodeWithText(context.getString(R.string.connect_from_link))
            .assertDoesNotExist()
    }

    private fun showFirstScreen() {
        composeRule.setContent {
            ConnectScreen(
                state = ConnectState.NotConnected,
                onPasteLink = {},
                onCancel = {},
                onRetry = {},
                onConnect = {},
            )
        }
    }

    // A set-aside list is the one thing the first screen has to explain.
    @Test
    fun aDamagedListIsSaidSoInsteadOfGreetingAsIfNew() {
        val files = Connections.files(context)
        // A mark left by an earlier run that joined would hide the notice.
        files
            .edit()
            .putString(KEY_UNREADABLE_LIST, "{broken")
            .remove(KEY_JOINED_AFTER_UNREADABLE)
            .commit()

        try {
            showFirstScreen()

            composeRule
                .onNodeWithText(context.getString(R.string.connect_unreadable_title))
                .assertExists()
            composeRule
                .onNodeWithText(context.getString(R.string.connect_unreadable_notice))
                .assertExists()
            composeRule
                .onNodeWithText(context.getString(R.string.connect_not_connected_title))
                .assertDoesNotExist()
            composeRule
                .onNodeWithText(context.getString(R.string.connect_not_connected_hint))
                .assertDoesNotExist()
            composeRule
                .onNodeWithText(context.getString(R.string.connect_scan_qr))
                .assertExists()
        } finally {
            files.edit().remove(KEY_UNREADABLE_LIST).commit()
        }
    }

    @Test
    fun withoutASetAsideListTheFirstScreenIsTheUsualOne() {
        Connections.files(context).edit().remove(KEY_UNREADABLE_LIST).commit()

        showFirstScreen()

        composeRule
            .onNodeWithText(context.getString(R.string.connect_not_connected_title))
            .assertExists()
        composeRule
            .onNodeWithText(context.getString(R.string.connect_unreadable_title))
            .assertDoesNotExist()
    }
}
