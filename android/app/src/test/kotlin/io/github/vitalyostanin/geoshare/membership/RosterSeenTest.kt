package io.github.vitalyostanin.geoshare.membership

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RosterSeenTest {
    private val roster = Roster(members = listOf("a", "b"), admins = listOf("b"), founder = "b")

    private fun seen(vararg reads: RosterRead): List<RosterSeen> =
        runBlocking { rosterSeen(flowOf(*reads)).toList() }

    @Test
    fun `nothing is answered before the first read`() {
        assertEquals(listOf(RosterSeen(null, answered = false)), seen())
    }

    @Test
    fun `a read carries its roster and the answer together`() {
        assertEquals(
            listOf(RosterSeen(null, answered = false), RosterSeen(roster, answered = true)),
            seen(RosterRead.Read(roster)),
        )
    }

    @Test
    fun `silence keeps the last roster`() {
        assertEquals(
            listOf(RosterSeen(null, answered = false), RosterSeen(roster, answered = true)),
            seen(RosterRead.Read(roster), RosterRead.Unreachable),
        )
    }

    @Test
    fun `a refusal is an answer with no group`() {
        assertEquals(RosterSeen(null, answered = true), seen(RosterRead.Refused).last())
    }

    @Test
    fun `outside only once the database has answered`() {
        assertFalse(RosterSeen(null, answered = false).outside("a"))
        assertTrue(RosterSeen(null, answered = true).outside("a"))
        assertTrue(RosterSeen(roster, answered = true).outside("c"))
        assertFalse(RosterSeen(roster, answered = true).outside("a"))
    }
}
