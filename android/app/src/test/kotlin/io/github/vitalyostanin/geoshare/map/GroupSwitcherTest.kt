package io.github.vitalyostanin.geoshare.map

import androidx.compose.ui.unit.dp
import io.github.vitalyostanin.geoshare.membership.Leave
import io.github.vitalyostanin.geoshare.membership.Presence
import io.github.vitalyostanin.geoshare.membership.Roster
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class GroupSwitcherTest {
    private val now: Instant = Instant.parse("2026-10-03T12:30:00Z")

    private val roster =
        Roster(
            members = listOf("anna", "boris", "vera"),
            admins = listOf("anna"),
            founder = "anna",
            name = "Hike",
        )

    private fun search(endsInMinutes: Long): Search = Search(
        phoneId = "boris.phone",
        memberId = "boris",
        phoneName = null,
        openedBy = "anna",
        renewedBy = "anna",
        startedAt = now.minusSeconds(600),
        endsAt = now.plusSeconds(endsInMinutes * 60),
    )

    private fun siren(calledSecondsAgo: Long, heard: Boolean = false): Siren = sirenOf(
        phoneId = "vera.phone",
        calledBy = "anna",
        calledAt = now.minusSeconds(calledSecondsAgo),
        heardAt = if (heard) now.minusSeconds(calledSecondsAgo - 1) else null,
        stoppedBy = null,
        stoppedAt = null,
    )!!

    private fun choice(
        roster: Roster? = this.roster,
        searches: Map<String, Search> = emptyMap(),
        ownSiren: Siren? = null,
        uid: String? = "boris",
        presence: Presence = Presence.UNKNOWN,
    ) = groupChoice(
        projectId = "geoshare-hike",
        roster = roster,
        profiles = emptyMap(),
        unnamedPattern = "%1\$s's group",
        searches = searches,
        ownSiren = ownSiren,
        uid = uid,
        now = now,
        presence = presence,
    )

    @Test
    fun `one group needs no switcher`() {
        assertFalse(switcherShown(groups = 1))
    }

    @Test
    fun `two groups show it`() {
        assertTrue(switcherShown(groups = 2))
    }

    @Test
    fun `the banners move below the switcher only while it is shown`() {
        assertEquals(64.dp, bannerOffset(switcher = false))
        assertEquals(64.dp + SWITCHER_HEIGHT + SWITCHER_GAP, bannerOffset(switcher = true))
    }

    @Test
    fun `a group is called what its roster calls it, with its members counted`() {
        val choice = choice()

        assertEquals("geoshare-hike", choice.projectId)
        assertEquals("Hike", choice.caption)
        assertEquals(3, choice.members)
    }

    // A group whose document cannot be read — signed out there, or no longer
    // let in — still has to be told apart from the others in the list.
    @Test
    fun `a group with no roster read is called by its project and counts nobody`() {
        val choice = choice(roster = null)

        assertEquals("geoshare-hike", choice.caption)
        assertNull(choice.members)
    }

    @Test
    fun `a search standing in the group is told`() {
        assertTrue(choice(searches = mapOf("boris.phone" to search(endsInMinutes = 30))).search)
    }

    @Test
    fun `a search that has run out is not`() {
        assertFalse(choice(searches = mapOf("boris.phone" to search(endsInMinutes = -1))).search)
    }

    @Test
    fun `a siren still waiting for this device or sounding on it is told`() {
        assertTrue(choice(ownSiren = siren(calledSecondsAgo = 5)).siren)
        assertTrue(choice(ownSiren = siren(calledSecondsAgo = 5, heard = true)).siren)
    }

    @Test
    fun `a siren long over is not`() {
        assertFalse(choice(ownSiren = siren(calledSecondsAgo = 600)).siren)
        assertFalse(choice().siren)
    }

    @Test
    fun `a member signed in there may leave the roster`() {
        assertEquals(Leave.ROSTER, choice().leave)
    }

    @Test
    fun `the only administrator only takes the group off this device`() {
        assertEquals(Leave.DEVICE_ONLY, choice(uid = "anna").leave)
    }

    @Test
    fun `a group this device is not signed in to has no roster place to leave`() {
        assertEquals(Leave.NOT_ON_ROSTER, choice(uid = null).leave)
    }

    @Test
    fun `a group still read is not one the device was taken off`() {
        assertFalse(choice(presence = Presence.IN).removed)
        assertFalse(choice().removed)
    }

    // Called by the last roster read there, so the member knows which group
    // it was; but nobody in it is theirs to count, and there is no place on
    // the roster left to give up.
    @Test
    fun `a group the device was taken off keeps its name and nothing else`() {
        val choice = choice(presence = Presence.REMOVED)

        assertTrue(choice.removed)
        assertEquals("Hike", choice.caption)
        assertNull(choice.members)
        assertEquals(Leave.NOT_ON_ROSTER, choice.leave)
    }
}
