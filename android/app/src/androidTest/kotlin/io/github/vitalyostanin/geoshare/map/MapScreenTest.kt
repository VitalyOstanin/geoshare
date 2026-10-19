package io.github.vitalyostanin.geoshare.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.vitalyostanin.geoshare.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

/**
 * The parts of the map screen a person presses, drawn on a device with no
 * database and no map behind them.
 *
 * The screen as a whole is not drawn here: its middle is a MapLibre view that
 * fetches tiles over the network, and the screen reads the group out of the
 * database itself. What sits around the map is drawn out of plain values,
 * though, and that is what these tests hold: the strip of members under the
 * map, the follow button over it and the card a member's position opens. The
 * positions are made up, in the same invented area the unit tests use. Texts
 * are looked up in the resources so that the tests hold in either language.
 */
@RunWith(AndroidJUnit4::class)
class MapScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val now = Instant.now()

    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    /** Draws the strip with [members] on it and records what it is asked to do. */
    private fun showStrip(
        members: List<MemberPosition>,
        selected: MutableList<String> = mutableListOf(),
        searches: Map<String, Search> = emptyMap(),
        awaitingTarget: Boolean = false,
        sheetOpen: (Boolean) -> Unit = {},
    ) {
        composeRule.setContent {
            var showAll by remember { mutableStateOf(false) }

            MemberButtons(
                members = members,
                names = NAMES,
                faces = emptyMap(),
                openCards = emptySet(),
                now = now,
                searches = searches,
                showAll = showAll,
                awaitingTarget = awaitingTarget,
                onSelect = { selected.add(it) },
                onShowAll = {
                    showAll = it
                    sheetOpen(it)
                },
            )
        }
    }

    // MEM-14: the strip names two members and gathers the rest under one button.
    @Test
    fun stripShowsTwoMembersAndGathersTheRestUnderMore() {
        showStrip(listOf(at(ANNA_PHONE), at(BORIS_PHONE), at(CLARA_PHONE), at(DAN_PHONE)))

        composeRule.onNodeWithText("Anna", substring = true).assertExists()
        composeRule.onNodeWithText("Boris", substring = true).assertExists()
        composeRule.onNodeWithText("Clara", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.map_more)).assertExists()
        composeRule
            .onNodeWithText(context.resources.getQuantityString(R.plurals.map_more_count, 2, 2))
            .assertExists()
    }

    // MEM-14: a third member takes the place a "more" button would have, rather than hiding
    // behind it.
    @Test
    fun threeMembersAllStandOnTheStrip() {
        showStrip(listOf(at(ANNA_PHONE), at(BORIS_PHONE), at(CLARA_PHONE)))

        composeRule.onNodeWithText("Clara", substring = true).assertExists()
        composeRule.onNodeWithText(text(R.string.map_more)).assertDoesNotExist()
    }

    // MEM-14: a card on the strip hands over the position it stands for, not the member.
    @Test
    fun tappingACardSelectsItsPosition() {
        val selected = mutableListOf<String>()
        showStrip(listOf(at(ANNA_PHONE), at(BORIS_PHONE)), selected = selected)

        composeRule.onNodeWithText("Boris", substring = true).performClick()

        assertEquals(listOf(BORIS_PHONE), selected)
    }

    // MEM-14: the members off the strip are chosen from a sheet, which closes on the choice.
    @Test
    fun choosingFromTheSheetSelectsAndClosesIt() {
        val selected = mutableListOf<String>()
        val sheet = mutableListOf<Boolean>()
        showStrip(
            listOf(at(ANNA_PHONE), at(BORIS_PHONE), at(CLARA_PHONE), at(DAN_PHONE)),
            selected = selected,
            sheetOpen = { sheet.add(it) },
        )

        composeRule.onNodeWithText(text(R.string.map_more)).performClick()
        composeRule.onNodeWithText(text(R.string.map_more_title)).assertExists()
        composeRule.onNodeWithText("Clara", substring = true).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(CLARA_PHONE), selected)
        assertEquals(listOf(true, false), sheet)
        composeRule.onNodeWithText(text(R.string.map_more_title)).assertDoesNotExist()
    }

    // MEM-15: while ONE waits for a member, the strip says it is the place to choose one.
    @Test
    fun stripAsksForAMemberOnlyWhileOneIsWaiting() {
        showStrip(listOf(at(ANNA_PHONE), at(BORIS_PHONE)), awaitingTarget = true)

        composeRule.onNodeWithText(text(R.string.map_follow_awaiting_banner)).assertExists()
    }

    // MEM-15: nothing over the strip while no follow mode is waiting on it.
    @Test
    fun stripCarriesNoBannerWhileNothingWaits() {
        showStrip(listOf(at(ANNA_PHONE), at(BORIS_PHONE)))

        composeRule.onNodeWithText(text(R.string.map_follow_awaiting_banner)).assertDoesNotExist()
    }

    // ADM-07: the card of a device being looked for says until when, and only that card.
    @Test
    fun aSoughtDeviceCardSaysUntilWhen() {
        showStrip(
            listOf(at(ANNA_PHONE), at(BORIS_PHONE)),
            searches = mapOf(BORIS_PHONE to searchOn(BORIS_PHONE)),
        )

        val lead = text(R.string.sought_until_short, "").trim()

        composeRule.onAllNodes(hasText(lead, substring = true)).assertCountEquals(1)
        composeRule
            .onNode(hasText("Boris", substring = true) and hasText(lead, substring = true))
            .assertExists()
    }

    /** Draws the follow button in [mode] and records the modes picked from its menu. */
    private fun showFollow(
        mode: FollowMode,
        targetName: String? = null,
        picked: MutableList<FollowMode> = mutableListOf(),
    ) {
        composeRule.setContent {
            FollowButton(
                mode = mode,
                targetName = targetName,
                targetAvailable = true,
                changeCount = 0,
                onPick = { picked.add(it) },
            )
        }
    }

    // MEM-15: the button names the mode it is in, which is what TalkBack reads out.
    @Test
    fun followButtonNamesTheModeItIsIn() {
        showFollow(FollowMode.ONE, targetName = "Boris")

        composeRule
            .onNodeWithContentDescription(text(R.string.map_follow_one, "Boris"))
            .assertExists()
    }

    // MEM-15: a press opens all three modes at once, and a pick is handed on.
    @Test
    fun followButtonOffersTheThreeModesAndReportsThePick() {
        val picked = mutableListOf<FollowMode>()
        showFollow(FollowMode.NONE, targetName = "Boris", picked = picked)

        composeRule
            .onNodeWithContentDescription(text(R.string.map_follow_none))
            .performClick()

        composeRule.onNodeWithText(text(R.string.map_follow_menu_none_title)).assertExists()
        composeRule
            .onNodeWithText(text(R.string.map_follow_menu_one_title_named, "Boris"))
            .assertExists()
        composeRule.onNodeWithText(text(R.string.map_follow_menu_all_title)).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(FollowMode.ALL), picked)
        composeRule.onNodeWithText(text(R.string.map_follow_menu_hint)).assertDoesNotExist()
    }

    /** What the card's callbacks were called with. */
    private class CardCalls {
        var phoneNamed = 0
        var personNamed = 0
        var found = 0
    }

    private fun showCard(
        member: MemberPosition,
        calls: CardCalls = CardCalls(),
        detailsOpen: Boolean = false,
        search: Search? = null,
        runsTheGroup: Boolean = false,
    ) {
        composeRule.setContent {
            MemberDetail(
                member = member,
                name = NAMES.getValue(member.positionId),
                face = null,
                now = now,
                away = null,
                search = search,
                openedByName = search?.let { "Anna" },
                runsTheGroup = runsTheGroup,
                onFound = { calls.found++ },
                detailsOpen = detailsOpen,
                onDetailsOpen = {},
                onClose = {},
                onNamePhone = { calls.phoneNamed++ },
                onNamePerson = { calls.personNamed++ },
            )
        }
    }

    // MEM-14: the card names the person and the device, and keeps its rows behind the handle.
    @Test
    fun cardNamesPersonAndDeviceAndKeepsTheRowsClosed() {
        showCard(at(BORIS_PHONE, deviceName = "Boris phone"))

        composeRule.onNodeWithText("Boris").assertExists()
        composeRule.onNodeWithText("Boris phone").assertExists()
        composeRule.onNodeWithText(text(R.string.map_detail_at)).assertDoesNotExist()
    }

    // MEM-14: pulled open, the card shows the moments and the coordinates themselves.
    @Test
    fun openedCardShowsTheRowsAndTheCoordinates() {
        val member = at(BORIS_PHONE)
        showCard(member, detailsOpen = true)

        composeRule.onNodeWithText(text(R.string.map_detail_at)).assertExists()
        composeRule
            .onNodeWithText(coordinatesText(member.latitude, member.longitude))
            .assertExists()
    }

    // MEM-14: a device nobody has named is called a device rather than left blank.
    @Test
    fun anUnnamedDeviceIsCalledADevice() {
        showCard(at(BORIS_PHONE, deviceName = null))

        composeRule.onNodeWithText(text(R.string.unnamed_device)).assertExists()
    }

    // MEM-14: this device's own position that has not left yet says it waits for the network.
    @Test
    fun anUnsentPositionSaysItWaitsForTheNetwork() {
        showCard(at(BORIS_PHONE).copy(unsent = true))

        composeRule.onNodeWithText(text(R.string.map_detail_unsent)).assertExists()
    }

    // MEM-16: each of the two names on the card is what is pressed to rename it.
    @Test
    fun pressingANameAsksToRenameIt() {
        val calls = CardCalls()
        showCard(at(BORIS_PHONE, deviceName = "Boris phone"), calls = calls)

        composeRule.onNodeWithText("Boris phone").performClick()
        composeRule.onNodeWithText("Boris").performClick()

        assertEquals(1, calls.phoneNamed)
        assertEquals(1, calls.personNamed)
    }

    // ADM-07: the card of a sought device says who opened the search, and an administrator
    // ends it there.
    @Test
    fun administratorEndsASearchFromTheCard() {
        val calls = CardCalls()
        showCard(
            at(BORIS_PHONE),
            calls = calls,
            search = searchOn(BORIS_PHONE),
            runsTheGroup = true,
        )

        composeRule
            .onNode(hasText(text(R.string.search_opened_by, "Anna"), substring = true))
            .assertExists()
        composeRule.onNodeWithText(text(R.string.search_found)).performClick()
        composeRule.waitForIdle()

        assertEquals(1, calls.found)
    }

    // ADM-07: a member who does not run the group reads about the search but cannot end it.
    @Test
    fun memberSeesTheSearchButCannotEndIt() {
        showCard(at(BORIS_PHONE), search = searchOn(BORIS_PHONE))

        composeRule
            .onNode(hasText(text(R.string.search_opened_by, "Anna"), substring = true))
            .assertExists()
        composeRule.onAllNodesWithText(text(R.string.search_found)).assertCountEquals(0)
    }

    /** A fresh position of [positionId], in the invented area the unit tests use. */
    private fun at(positionId: String, deviceName: String? = null) = MemberPosition(
        positionId = positionId,
        memberId = positionId.substringBefore('.'),
        deviceName = deviceName,
        latitude = 60.9,
        longitude = 4.3,
        takenAt = now.minus(Duration.ofMinutes(2)),
        writtenAt = now.minus(Duration.ofMinutes(2)),
        mode = null,
        batteryPercent = 80,
    )

    private fun searchOn(phoneId: String) = Search(
        phoneId = phoneId,
        memberId = phoneId.substringBefore('.'),
        phoneName = null,
        openedBy = ANNA,
        renewedBy = ANNA,
        startedAt = now.minus(Duration.ofHours(1)),
        endsAt = now.plus(Duration.ofHours(5)),
    )

    private companion object {
        const val ANNA = "member-a"
        const val ANNA_PHONE = "member-a.dev-1"
        const val BORIS_PHONE = "member-b.dev-1"
        const val CLARA_PHONE = "member-c.dev-1"
        const val DAN_PHONE = "member-d.dev-1"

        /** What the strip and the card call each position, as the screen works it out. */
        val NAMES =
            mapOf(
                ANNA_PHONE to "Anna",
                BORIS_PHONE to "Boris",
                CLARA_PHONE to "Clara",
                DAN_PHONE to "Dan",
            )
    }
}
