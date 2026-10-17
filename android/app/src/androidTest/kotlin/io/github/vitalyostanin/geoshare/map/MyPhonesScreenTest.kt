package io.github.vitalyostanin.geoshare.map

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.vitalyostanin.geoshare.R
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

/**
 * A member's own devices drawn on a device, with no database behind them.
 *
 * Checked here is what the list says of each device, which buttons a row has
 * and which it lacks, and that each question asked before a write can be
 * backed out of and hands the right device on when it is confirmed.
 */
@RunWith(AndroidJUnit4::class)
class MyPhonesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val now = Instant.now()

    private val own = PhoneLine("$MEMBER.own", "Own phone", now, 90, false)
    private val silent = PhoneLine("$MEMBER.dev-2", "Old tablet", null, null, false)
    private val inTouch =
        PhoneLine("$MEMBER.dev-3", "Spare phone", now.minus(Duration.ofMinutes(10)), 40, true)

    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    private fun describedAs(id: Int): String = context.getString(id)

    /** What the callbacks of the screen were called with, in order. */
    private class Calls {
        val rung = mutableListOf<String>()
        val stopped = mutableListOf<String>()
        val resets = mutableListOf<String>()
        val forgotten = mutableListOf<String>()
        val searched = mutableListOf<List<Any?>>()
        val searchesEnded = mutableListOf<String>()
        var closed = 0
    }

    private fun show(
        calls: Calls = Calls(),
        phones: List<PhoneLine> = listOf(own, silent, inTouch),
        mayLookFor: Boolean = false,
        groupNames: List<String> = emptyList(),
        groupCaption: String? = null,
        searches: Map<String, Search> = emptyMap(),
        writeOutcome: WriteOutcome = WriteOutcome.DONE,
        forgets: Boolean = true,
    ) {
        composeRule.setContent {
            MyPhonesScreen(
                phones = phones,
                ownPhoneId = own.phoneId,
                now = now,
                sirenOf = { flowOf(null) },
                onCallSiren = {
                    calls.rung.add(it)
                    writeOutcome
                },
                onStopSiren = { calls.stopped.add(it) },
                resetOf = { flowOf(null) },
                onAskReset = {
                    calls.resets.add(it)
                    writeOutcome
                },
                onForgetPhone = {
                    calls.forgotten.add(it)
                    forgets
                },
                groupNames = groupNames,
                groupCaption = groupCaption,
                searches = searches,
                mayLookFor = mayLookFor,
                onSearch = { phoneId, phoneName, renew, hours ->
                    calls.searched.add(listOf(phoneId, phoneName, renew, hours))
                    writeOutcome
                },
                onEndSearch = { calls.searchesEnded.add(it) },
                onClose = { calls.closed++ },
            )
        }
    }

    /** A button on the list, which is a scrolling column and may hold it off screen. */
    private fun SemanticsNodeInteraction.tap() {
        performScrollTo()
        performClick()
    }

    private fun tapIcon(id: Int, index: Int = 0) {
        composeRule.onAllNodesWithContentDescription(describedAs(id))[index].tap()
    }

    /** A button in a dialog, which stands over the list and does not scroll. */
    private fun press(id: Int) {
        composeRule.onNodeWithText(text(id)).performClick()
    }

    @Test
    fun everyDeviceIsListedUnderItsName() {
        show()

        composeRule.onNodeWithText("Own phone").assertExists()
        composeRule.onNodeWithText("Old tablet").assertExists()
        composeRule.onNodeWithText("Spare phone").assertExists()
        composeRule.onNodeWithText(text(R.string.my_phones_title)).assertExists()
    }

    @Test
    fun noDeviceIsSaidSoInPlaceOfAList() {
        show(phones = emptyList())

        composeRule.onNodeWithText(text(R.string.my_phones_none)).assertExists()
    }

    @Test
    fun rowsTellThisDeviceOneInTouchAndOneNotInTouchYet() {
        show()

        composeRule.onNodeWithText(text(R.string.my_phones_this)).assertExists()
        composeRule.onNodeWithText(text(R.string.phone_silent)).assertExists()
        // The age is worded by the resources; the lead-in tells the row apart.
        val inTouchWords = text(R.string.phone_online, "").trim()

        composeRule.onNode(hasText(inTouchWords, substring = true)).assertExists()
    }

    @Test
    fun theGroupIsNamedUnderTheTitleOnlyWhenOneIsGiven() {
        show(groupCaption = "Home")

        composeRule.onNodeWithText(text(R.string.my_phones_group, "Home")).assertExists()
    }

    @Test
    fun noGroupLineWithoutACaption() {
        show()

        composeRule.onNodeWithText(text(R.string.my_phones_group, "Home")).assertDoesNotExist()
    }

    @Test
    fun thisDeviceHasNeitherASirenNorABinAndTheOthersHaveBoth() {
        show()

        composeRule
            .onAllNodesWithContentDescription(describedAs(R.string.siren_hint))
            .assertCountEquals(2)
        composeRule
            .onAllNodesWithContentDescription(describedAs(R.string.reset_hint))
            .assertCountEquals(2)
        composeRule
            .onAllNodesWithContentDescription(describedAs(R.string.phone_remove))
            .assertCountEquals(2)
    }

    @Test
    fun searchButtonsAreOfferedOnlyToTheMemberWhoMayLook() {
        show(mayLookFor = false)

        composeRule
            .onAllNodesWithContentDescription(describedAs(R.string.search_start))
            .assertCountEquals(0)
    }

    @Test
    fun aMemberWhoMayLookIsOfferedASearchOnEveryDeviceThisOneToo() {
        show(mayLookFor = true)

        composeRule
            .onAllNodesWithContentDescription(describedAs(R.string.search_start))
            .assertCountEquals(3)
    }

    @Test
    fun closeButtonClosesTheScreen() {
        val calls = Calls()

        show(calls)
        press(R.string.settings_close)

        assertEquals(1, calls.closed)
    }

    @Test
    fun helpButtonOpensTheHelpSheet() {
        show()

        composeRule.onNodeWithContentDescription(describedAs(R.string.help_open)).performClick()

        composeRule.onNodeWithText(text(R.string.help_title_screen)).assertExists()
    }

    @Test
    fun sirenDialogNamesTheDeviceAndRingsItOnConfirmation() {
        val calls = Calls()

        show(calls)
        // Rows after this one: the old tablet, then the spare phone.
        tapIcon(R.string.siren_hint)
        composeRule
            .onNodeWithText(text(R.string.siren_title, text(R.string.map_member_you), "Old tablet"))
            .assertExists()
        press(R.string.siren_go)

        assertEquals(listOf(silent.phoneId), calls.rung)
        composeRule.onNodeWithText(text(R.string.siren_note)).assertDoesNotExist()
    }

    @Test
    fun sirenDialogRingsTheDeviceOfTheRowThatWasPressed() {
        val calls = Calls()

        show(calls)
        tapIcon(R.string.siren_hint, index = 1)
        press(R.string.siren_go)

        assertEquals(listOf(inTouch.phoneId), calls.rung)
    }

    @Test
    fun sirenDialogCanBeCancelledAndRingsNothing() {
        val calls = Calls()

        show(calls)
        tapIcon(R.string.siren_hint)
        press(R.string.search_cancel)

        assertTrue(calls.rung.isEmpty())
        composeRule.onNodeWithText(text(R.string.siren_note)).assertDoesNotExist()
    }

    @Test
    fun sirenDialogStaysOpenWhenTheWriteIsRefused() {
        val calls = Calls()

        show(calls, writeOutcome = WriteOutcome.REFUSED)
        tapIcon(R.string.siren_hint)
        press(R.string.siren_go)

        assertEquals(listOf(silent.phoneId), calls.rung)
        composeRule.onNodeWithText(text(R.string.siren_note)).assertExists()
        composeRule.onNodeWithText(text(R.string.group_refused)).assertExists()
    }

    @Test
    fun resetDialogAsksAboutTheDeviceAndPassesItOn() {
        val calls = Calls()

        show(calls)
        tapIcon(R.string.reset_hint)
        composeRule.onNodeWithText(text(R.string.reset_confirm_title, "Old tablet")).assertExists()
        press(R.string.reset_go)

        assertEquals(listOf(silent.phoneId), calls.resets)
        composeRule.onNodeWithText(text(R.string.reset_confirm_note)).assertDoesNotExist()
    }

    @Test
    fun resetDialogCanBeCancelled() {
        val calls = Calls()

        show(calls)
        tapIcon(R.string.reset_hint)
        press(R.string.search_cancel)

        assertTrue(calls.resets.isEmpty())
        composeRule.onNodeWithText(text(R.string.reset_confirm_note)).assertDoesNotExist()
    }

    @Test
    fun removalAsksByNameAndForgetsTheDeviceOnConfirmation() {
        val calls = Calls()

        show(calls)
        tapIcon(R.string.phone_remove)
        composeRule.onNodeWithText(text(R.string.phone_remove_title, "Old tablet")).assertExists()
        press(R.string.phone_remove_go)

        assertEquals(listOf(silent.phoneId), calls.forgotten)
        composeRule
            .onNodeWithText(text(R.string.phone_remove_title, "Old tablet"))
            .assertDoesNotExist()
    }

    @Test
    fun removalCanBeCancelledAndForgetsNothing() {
        val calls = Calls()

        show(calls)
        tapIcon(R.string.phone_remove)
        press(R.string.search_cancel)

        assertTrue(calls.forgotten.isEmpty())
        composeRule
            .onNodeWithText(text(R.string.phone_remove_title, "Old tablet"))
            .assertDoesNotExist()
    }

    @Test
    fun removalStaysOpenAndSaysSoWhenTheDatabaseRefuses() {
        val calls = Calls()

        show(calls, forgets = false)
        tapIcon(R.string.phone_remove)
        press(R.string.phone_remove_go)

        assertEquals(listOf(silent.phoneId), calls.forgotten)
        composeRule.onNodeWithText(text(R.string.phone_remove_title, "Old tablet")).assertExists()
        composeRule.onNodeWithText(text(R.string.phone_remove_refused)).assertExists()
    }

    @Test
    fun removalNamesTheGroupsOnlyWhenThereAreSeveral() {
        show(groupNames = listOf("Home", "Work"))

        tapIcon(R.string.phone_remove)

        composeRule
            .onNodeWithText(text(R.string.phone_remove_groups, "Home, Work"))
            .assertExists()
    }

    @Test
    fun removalSaysNothingOfGroupsWhenThereIsOne() {
        show(groupNames = listOf("Home"))

        tapIcon(R.string.phone_remove)

        composeRule
            .onNodeWithText(text(R.string.phone_remove_groups, "Home"))
            .assertDoesNotExist()
    }

    @Test
    fun searchDialogOpensOnTheChosenDeviceWithTheDefaultLength() {
        val calls = Calls()

        show(calls, mayLookFor = true)
        // The first search button is on this device's own row.
        tapIcon(R.string.search_start, index = 1)
        composeRule
            .onNodeWithText(
                text(R.string.search_open_title, "Old tablet", text(R.string.map_member_you)),
            ).assertExists()
        press(R.string.search_go)

        assertEquals(
            listOf(listOf<Any?>(silent.phoneId, "Old tablet", false, DEFAULT_SEARCH_HOURS)),
            calls.searched,
        )
    }

    @Test
    fun searchDialogPassesOnTheLengthThatWasPicked() {
        val calls = Calls()

        show(calls, mayLookFor = true)
        tapIcon(R.string.search_start, index = 1)
        press(R.string.search_hours_72)
        press(R.string.search_go)

        assertEquals(
            listOf(listOf<Any?>(silent.phoneId, "Old tablet", false, 72)),
            calls.searched,
        )
    }

    @Test
    fun searchDialogCanBeCancelledAndOpensNothing() {
        val calls = Calls()

        show(calls, mayLookFor = true)
        tapIcon(R.string.search_start)
        press(R.string.search_cancel)

        assertTrue(calls.searched.isEmpty())
        composeRule.onNodeWithText(text(R.string.search_note)).assertDoesNotExist()
    }

    @Test
    fun aStandingSearchIsToldOnItsRowAndEndedFromTheSameButton() {
        val calls = Calls()

        show(
            calls,
            mayLookFor = true,
            searches =
            mapOf(
                silent.phoneId to
                    Search(
                        phoneId = silent.phoneId,
                        memberId = MEMBER,
                        phoneName = silent.name,
                        openedBy = MEMBER,
                        renewedBy = MEMBER,
                        startedAt = now.minus(Duration.ofHours(1)),
                        endsAt = now.plus(Duration.ofHours(5)),
                    ),
            ),
        )

        val soughtWords = text(R.string.sought_until, "").trim()

        composeRule.onNode(hasText(soughtWords, substring = true)).assertExists()
        tapIcon(R.string.search_found)

        assertEquals(listOf(silent.phoneId), calls.searchesEnded)
        assertTrue(calls.searched.isEmpty())
    }

    private companion object {
        const val MEMBER = "member-a"
    }
}
