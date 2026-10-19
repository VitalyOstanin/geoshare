package io.github.vitalyostanin.geoshare.map

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.membership.Roster
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

/**
 * The administrator's panel drawn on a device, with no database behind it.
 *
 * What is checked is what a person sees and what a press hands to the callbacks:
 * who gets which buttons, how a device that is in touch reads against one that
 * is not yet, and that each dialog opens, can be backed out of, and passes the
 * right arguments on when it is confirmed. Texts are looked up in the resources
 * so that the tests hold in either language.
 */
@RunWith(AndroidJUnit4::class)
class AdminPanelTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val now = Instant.now()

    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    private fun describedAs(id: Int): String = context.getString(id)

    /** What the callbacks of the panel were called with, in order. */
    private class Calls {
        val removed = mutableListOf<String>()
        val running = mutableListOf<Pair<String, Boolean>>()
        val invited = mutableListOf<String>()
        val renamed = mutableListOf<String>()
        val searched = mutableListOf<List<Any?>>()
        val searchesEnded = mutableListOf<String>()
        val rung = mutableListOf<String>()
        val sirensStopped = mutableListOf<String>()
        val resets = mutableListOf<String>()
        var closed = 0
    }

    private fun show(
        calls: Calls = Calls(),
        roster: Roster = ROSTER,
        viewer: String = ADMIN,
        ownPhoneId: String? = ADMIN_OWN_PHONE,
        searches: Map<String, Search> = emptyMap(),
        sirens: Map<String, Siren> = emptyMap(),
        renameOutcome: WriteOutcome = WriteOutcome.DONE,
        writeOutcome: WriteOutcome = WriteOutcome.DONE,
    ) {
        composeRule.setContent {
            AdminPanel(
                roster = roster,
                profiles = PROFILES,
                grants = grantsAt(now),
                ownMemberId = viewer,
                labels = Labels(),
                now = now,
                invitation = null,
                site = null,
                trouble = false,
                searches = searches,
                seenAt = emptyMap(),
                onMakeInvitation = { calls.invited.add(it) },
                onRemoveMember = { calls.removed.add(it) },
                onSetRunning = { member, on -> calls.running.add(member to on) },
                onRenameGroup = {
                    calls.renamed.add(it)
                    renameOutcome
                },
                onSearch = { phoneId, phoneName, renew, hours ->
                    calls.searched.add(listOf(phoneId, phoneName, renew, hours))
                    writeOutcome
                },
                onEndSearch = { calls.searchesEnded.add(it) },
                ownPhoneId = ownPhoneId,
                sirenOf = { phoneId -> flowOf(sirens[phoneId]) },
                onCallSiren = {
                    calls.rung.add(it)
                    writeOutcome
                },
                onStopSiren = { calls.sirensStopped.add(it) },
                resetOf = { flowOf(null) },
                onAskReset = {
                    calls.resets.add(it)
                    writeOutcome
                },
                invitationLive = { flowOf(null) },
                onClose = { calls.closed++ },
            )
        }
    }

    /** A button on the panel's page, which is a scrolling column and may hold it off screen. */
    private fun SemanticsNodeInteraction.tap() {
        performScrollTo()
        performClick()
    }

    private fun tapText(id: Int, index: Int = 0) {
        composeRule.onAllNodesWithText(text(id))[index].tap()
    }

    private fun tapIcon(id: Int, index: Int = 0) {
        composeRule.onAllNodesWithContentDescription(describedAs(id))[index].tap()
    }

    /** A button in a dialog, which stands over the page and does not scroll. */
    private fun press(id: Int) {
        composeRule.onNodeWithText(text(id)).performClick()
    }

    // ADM-06: the panel lists every member, and an administrator gets the controls over the others.
    @Test
    fun administratorSeesEveryMemberAndTheControlsOverTheOthers() {
        show()

        composeRule.onNodeWithText("Anna").assertExists()
        composeRule.onNodeWithText("Boris").assertExists()
        composeRule.onNodeWithText("Clara").assertExists()
        composeRule.onNodeWithText(text(R.string.group_name_rename)).assertExists()
        // Boris and Clara may be removed or promoted; Anna, who is looking, may not.
        composeRule.onAllNodesWithText(text(R.string.group_remove)).assertCountEquals(2)
        composeRule.onAllNodesWithText(text(R.string.group_make_admin)).assertCountEquals(2)
    }

    // ADM-05: an ordinary member is offered no removal, promotion or renaming of the group.
    @Test
    fun ordinaryMemberSeesNoRenameRemovalOrPromotion() {
        show(viewer = MATE, ownPhoneId = "$MATE.dev-1")

        composeRule.onNodeWithText("Boris").assertExists()
        composeRule.onNodeWithText(text(R.string.group_name_rename)).assertDoesNotExist()
        composeRule.onAllNodesWithText(text(R.string.group_remove)).assertCountEquals(0)
        composeRule.onAllNodesWithText(text(R.string.group_make_admin)).assertCountEquals(0)
        composeRule.onAllNodesWithText(text(R.string.group_stop_running)).assertCountEquals(0)
    }

    // ADM-06: a device is described by its last write, or as not in touch yet.
    @Test
    fun aDeviceInTouchReadsDifferentlyFromOneNotInTouchYet() {
        show()

        // The age is worded by the resources; the name and the lead-in are what tell them apart.
        val inTouch = text(R.string.admin_phone_online, "Boris phone", "").trim()

        composeRule.onNode(hasText(inTouch, substring = true)).assertExists()
        composeRule
            .onNodeWithText(text(R.string.admin_phone_silent, "Boris tablet"))
            .assertExists()
        composeRule
            .onNodeWithText(text(R.string.admin_phone_silent, "Boris phone"))
            .assertDoesNotExist()
    }

    // ADM-06: the device the panel is open on is marked as this one and has no siren.
    @Test
    fun theDeviceThePanelIsOpenOnIsMarkedAndNeverRung() {
        show()

        composeRule.onNodeWithText(text(R.string.admin_phone_this, "Anna phone")).assertExists()
        // Only Boris's two devices carry a siren button.
        composeRule
            .onAllNodesWithContentDescription(describedAs(R.string.siren_hint))
            .assertCountEquals(2)
    }

    @Test
    fun closeButtonClosesThePanel() {
        val calls = Calls()

        show(calls)
        press(R.string.settings_close)

        assertEquals(1, calls.closed)
    }

    // ADM-02: the typed group name is handed on, and the dialog closes once it is written.
    @Test
    fun renameDialogPassesTheTypedNameOnAndClosesOnceWritten() {
        val calls = Calls()

        show(calls)
        tapText(R.string.group_name_rename)
        composeRule.onNodeWithText(text(R.string.group_name_label)).assertExists()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("Home Sweet")
        press(R.string.group_name_save)

        assertEquals(listOf("Home Sweet"), calls.renamed)
        composeRule.onNodeWithText(text(R.string.group_name_label)).assertDoesNotExist()
    }

    // ADM-02: a refused rename keeps the dialog open and says so.
    @Test
    fun renameDialogStaysOpenAndSaysSoWhenTheWriteIsRefused() {
        val calls = Calls()

        show(calls, renameOutcome = WriteOutcome.REFUSED)
        tapText(R.string.group_name_rename)
        composeRule.onNode(hasSetTextAction()).performTextReplacement("Home Sweet")
        press(R.string.group_name_save)

        assertEquals(listOf("Home Sweet"), calls.renamed)
        composeRule.onNodeWithText(text(R.string.group_name_label)).assertExists()
        composeRule.onNodeWithText(text(R.string.group_refused)).assertExists()
    }

    // ADM-02: cancelling the rename writes nothing.
    @Test
    fun renameDialogCanBeCancelledWithoutWriting() {
        val calls = Calls()

        show(calls)
        tapText(R.string.group_name_rename)
        press(R.string.search_cancel)

        assertTrue(calls.renamed.isEmpty())
        composeRule.onNodeWithText(text(R.string.group_name_label)).assertDoesNotExist()
    }

    // ADM-04: taking a member off asks by name and removes the one asked about.
    @Test
    fun removalAsksByNameAndRemovesTheMemberThatWasAskedAbout() {
        val calls = Calls()

        show(calls)
        // Rows are in name order after the viewer's own: Boris, then Clara.
        tapText(R.string.group_remove, index = 1)
        composeRule.onNodeWithText(text(R.string.group_remove_title, "Clara")).assertExists()
        press(R.string.group_remove_go)

        assertEquals(listOf(KID), calls.removed)
        composeRule
            .onNodeWithText(text(R.string.group_remove_title, "Clara"))
            .assertDoesNotExist()
    }

    // ADM-04: declining the question removes nobody.
    @Test
    fun removalCanBeCancelledAndRemovesNobody() {
        val calls = Calls()

        show(calls)
        tapText(R.string.group_remove)
        press(R.string.search_cancel)

        assertTrue(calls.removed.isEmpty())
        composeRule
            .onNodeWithText(text(R.string.group_remove_title, "Boris"))
            .assertDoesNotExist()
    }

    // ADM-05: making a member an administrator is done at once, without a question.
    @Test
    fun makingAMemberAdministratorNeedsNoQuestion() {
        val calls = Calls()

        show(calls)
        tapText(R.string.group_make_admin)

        assertEquals(listOf(MATE to true), calls.running)
    }

    // ADM-05: stepping down asks first and is done on confirmation.
    @Test
    fun givingUpOneSOwnRoleAsksFirstAndThenDoesIt() {
        val calls = Calls()

        // Boris runs the group but did not found it, so he may step down.
        show(
            calls,
            roster = Roster(listOf(ADMIN, MATE, KID), listOf(ADMIN, MATE), ADMIN, "Home"),
            viewer = MATE,
            ownPhoneId = "$MATE.dev-1",
        )
        tapText(R.string.group_stop_running)
        composeRule.onNodeWithText(text(R.string.group_give_up_title)).assertExists()
        assertTrue("nothing is done before the answer", calls.running.isEmpty())
        press(R.string.group_give_up)

        assertEquals(listOf(MATE to false), calls.running)
    }

    // ADM-05: stepping down can be declined, and the role stays.
    @Test
    fun givingUpOneSOwnRoleCanBeDeclined() {
        val calls = Calls()

        show(
            calls,
            roster = Roster(listOf(ADMIN, MATE, KID), listOf(ADMIN, MATE), ADMIN, "Home"),
            viewer = MATE,
            ownPhoneId = "$MATE.dev-1",
        )
        tapText(R.string.group_stop_running)
        press(R.string.group_keep_running)

        assertTrue(calls.running.isEmpty())
        composeRule.onNodeWithText(text(R.string.group_give_up_title)).assertDoesNotExist()
    }

    // ADM-08: the siren dialog names the device and rings it on confirmation.
    @Test
    fun sirenDialogNamesTheDeviceAndRingsItOnConfirmation() {
        val calls = Calls()

        show(calls)
        tapIcon(R.string.siren_hint)
        composeRule
            .onNodeWithText(text(R.string.siren_title, "Boris", "Boris phone"))
            .assertExists()
        press(R.string.siren_go)

        assertEquals(listOf("$MATE.dev-1"), calls.rung)
        composeRule
            .onNodeWithText(text(R.string.siren_title, "Boris", "Boris phone"))
            .assertDoesNotExist()
    }

    // ADM-08: cancelling the siren dialog rings nothing.
    @Test
    fun sirenDialogCanBeCancelledAndRingsNothing() {
        val calls = Calls()

        show(calls)
        tapIcon(R.string.siren_hint)
        press(R.string.search_cancel)

        assertTrue(calls.rung.isEmpty())
        composeRule.onNodeWithText(text(R.string.siren_note)).assertDoesNotExist()
    }

    // ADM-08: a siren write that goes unanswered keeps the dialog open.
    @Test
    fun sirenDialogStaysOpenWhenTheWriteGoesUnanswered() {
        val calls = Calls()

        show(calls, writeOutcome = WriteOutcome.UNANSWERED)
        tapIcon(R.string.siren_hint)
        press(R.string.siren_go)

        assertEquals(listOf("$MATE.dev-1"), calls.rung)
        composeRule.onNodeWithText(text(R.string.siren_note)).assertExists()
        composeRule.onNodeWithText(text(R.string.write_unanswered)).assertExists()
    }

    // ADM-08: the caller sees on the device's row that the call was stopped.
    @Test
    fun aCallThatWasStoppedIsToldOnTheDeviceSRow() {
        show(
            sirens =
            mapOf(
                "$MATE.dev-1" to
                    Siren(
                        phoneId = "$MATE.dev-1",
                        calledBy = ADMIN,
                        calledAt = now.minusSeconds(30),
                        heardAt = null,
                        stoppedBy = ADMIN,
                        stoppedAt = now.minusSeconds(10),
                    ),
            ),
        )

        composeRule
            .onNodeWithText(text(R.string.siren_status_stopped, "Boris phone"))
            .assertExists()
    }

    // ADM-09: the reset dialog asks about the chosen device and hands it on.
    @Test
    fun resetDialogAsksForTheChosenDeviceAndPassesItOn() {
        val calls = Calls()

        show(calls)
        tapIcon(R.string.reset_hint)
        composeRule.onNodeWithText(text(R.string.reset_confirm_title, "Boris phone")).assertExists()
        press(R.string.reset_go)

        assertEquals(listOf("$MATE.dev-1"), calls.resets)
    }

    // ADM-09: cancelling the reset dialog resets nothing.
    @Test
    fun resetDialogCanBeCancelled() {
        val calls = Calls()

        show(calls)
        tapIcon(R.string.reset_hint)
        press(R.string.search_cancel)

        assertTrue(calls.resets.isEmpty())
        composeRule.onNodeWithText(text(R.string.reset_confirm_note)).assertDoesNotExist()
    }

    // ADM-07: the search dialog opens on the default length and hands it on.
    @Test
    fun searchDialogOpensWithTheDefaultLengthAndPassesItOn() {
        val calls = Calls()

        show(calls)
        tapIcon(R.string.search_start)
        composeRule
            .onNodeWithText(text(R.string.search_open_title, "Boris phone", "Boris"))
            .assertExists()
        press(R.string.search_go)

        assertEquals(
            listOf(listOf<Any?>("$MATE.dev-1", "Boris phone", false, DEFAULT_SEARCH_HOURS)),
            calls.searched,
        )
    }

    // ADM-07: the length picked for a search is the one handed on.
    @Test
    fun searchDialogPassesOnTheLengthThatWasPicked() {
        val calls = Calls()

        show(calls)
        tapIcon(R.string.search_start)
        press(R.string.search_hours_6)
        press(R.string.search_go)

        assertEquals(
            listOf(listOf<Any?>("$MATE.dev-1", "Boris phone", false, 6)),
            calls.searched,
        )
    }

    // ADM-07: cancelling the search dialog opens no search.
    @Test
    fun searchDialogCanBeCancelledAndOpensNothing() {
        val calls = Calls()

        show(calls)
        tapIcon(R.string.search_start)
        press(R.string.search_cancel)

        assertTrue(calls.searched.isEmpty())
        composeRule.onNodeWithText(text(R.string.search_note)).assertDoesNotExist()
    }

    // ADM-07: a search is not offered on the viewer's own devices.
    @Test
    fun nobodyOffersASearchOnTheViewerSOwnDevices() {
        show()

        // Boris's two devices only: Anna, who is looking, is not offered.
        composeRule
            .onAllNodesWithContentDescription(describedAs(R.string.search_start))
            .assertCountEquals(2)
    }

    // ADM-07: a standing search is summarised and can be ended as found.
    @Test
    fun aStandingSearchIsSummarisedAndCanBeEndedAsFound() {
        val calls = Calls()

        show(calls, searches = mapOf("$MATE.dev-1" to searchOnBoris()))

        composeRule.onNodeWithText(text(R.string.search_opened_by_you)).assertExists()
        press(R.string.search_found)

        assertEquals(listOf("$MATE.dev-1"), calls.searchesEnded)
    }

    // ADM-07: a standing search is renewed before it ends, through its own dialog.
    @Test
    fun aStandingSearchCanBeRenewedThroughItsOwnDialog() {
        val calls = Calls()

        show(calls, searches = mapOf("$MATE.dev-1" to searchOnBoris()))
        tapText(R.string.search_renew)
        composeRule
            .onNodeWithText(text(R.string.search_renew_title, "Boris phone", "Boris"))
            .assertExists()
        press(R.string.search_cancel)

        assertTrue(calls.searched.isEmpty())
    }

    // ADM-03: an invitation is made out to an address, and only with one.
    @Test
    fun invitationDialogIsConfirmedOnlyWithAnAddressAndHandsItOn() {
        val calls = Calls()

        show(calls)
        tapText(R.string.group_make_invitation)
        composeRule.onNodeWithText(text(R.string.group_invite_title, "Home")).assertExists()
        composeRule.onNodeWithText(text(R.string.group_invite_create)).assertIsNotEnabled()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("guest@example.org")
        press(R.string.group_invite_create)

        assertEquals(listOf("guest@example.org"), calls.invited)
        composeRule
            .onNodeWithText(text(R.string.group_invite_title, "Home"))
            .assertDoesNotExist()
    }

    // ADM-03: cancelling the invitation dialog makes no invitation.
    @Test
    fun invitationDialogCanBeCancelled() {
        val calls = Calls()

        show(calls)
        tapText(R.string.group_make_invitation)
        press(R.string.search_cancel)

        assertTrue(calls.invited.isEmpty())
        composeRule.onNodeWithText(text(R.string.group_invite_note)).assertDoesNotExist()
    }

    @Test
    fun helpButtonOpensTheHelpSheet() {
        show()

        composeRule.onNodeWithContentDescription(describedAs(R.string.help_open)).performClick()

        composeRule.onNodeWithText(text(R.string.help_title_screen)).assertExists()
    }

    private fun searchOnBoris() = Search(
        phoneId = "$MATE.dev-1",
        memberId = MATE,
        phoneName = "Boris phone",
        openedBy = ADMIN,
        renewedBy = ADMIN,
        startedAt = now.minus(Duration.ofHours(1)),
        endsAt = now.plus(Duration.ofHours(5)),
    )

    private companion object {
        const val ADMIN = "member-a"
        const val MATE = "member-b"
        const val KID = "member-c"
        const val ADMIN_OWN_PHONE = "$ADMIN.own"

        val ROSTER = Roster(listOf(ADMIN, MATE, KID), listOf(ADMIN), ADMIN, "Home")

        val PROFILES =
            mapOf(
                ADMIN to MemberProfile(ADMIN, "Anna", null),
                MATE to MemberProfile(MATE, "Boris", null),
                KID to MemberProfile(KID, "Clara", null),
            )

        /** Anna's one device is in touch now, Boris's first five minutes ago and his second never. */
        fun grantsAt(now: Instant) = mapOf(
            ADMIN to
                MemberGrant(
                    memberId = ADMIN,
                    devices = listOf("own"),
                    phones = mapOf("own" to PhoneStatus(name = "Anna phone", onlineAt = now)),
                ),
            MATE to
                MemberGrant(
                    memberId = MATE,
                    devices = listOf("dev-1", "dev-2"),
                    phones =
                    mapOf(
                        "dev-1" to
                            PhoneStatus(
                                name = "Boris phone",
                                onlineAt = now.minus(Duration.ofMinutes(5)),
                            ),
                        "dev-2" to PhoneStatus(name = "Boris tablet", onlineAt = null),
                    ),
                ),
        )
    }
}
