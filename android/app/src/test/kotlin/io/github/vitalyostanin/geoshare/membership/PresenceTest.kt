package io.github.vitalyostanin.geoshare.membership

import org.junit.Assert.assertEquals
import org.junit.Test

class PresenceTest {
    private val roster = Roster(members = listOf("a", "b"), admins = listOf("b"), founder = "b")

    @Test
    fun `on the roster is in the group`() {
        assertEquals(Presence.IN, presenceOf(RosterRead.Read(roster), "a", wasIn = false))
    }

    @Test
    fun `missing from a roster once stood on is removed`() {
        val without = roster.copy(members = listOf("b"))

        assertEquals(Presence.REMOVED, presenceOf(RosterRead.Read(without), "a", wasIn = true))
    }

    @Test
    fun `a refused read after being in is removed`() {
        assertEquals(Presence.REMOVED, presenceOf(RosterRead.Refused, "a", wasIn = true))
    }

    // The rules refuse the group document to whoever is not on its roster, so
    // a device that has not joined yet hears the same refusal as one taken
    // off: only having been in tells the two apart.
    @Test
    fun `a refused read before ever being in is outside, not removed`() {
        assertEquals(Presence.OUTSIDE, presenceOf(RosterRead.Refused, "a", wasIn = false))
    }

    @Test
    fun `a group not made yet is not one the device was taken off`() {
        assertEquals(Presence.OUTSIDE, presenceOf(RosterRead.Read(null), "a", wasIn = false))
    }

    @Test
    fun `no network says nothing about the roster`() {
        assertEquals(Presence.UNKNOWN, presenceOf(RosterRead.Unreachable, "a", wasIn = true))
    }

    @Test
    fun `a group once stood in counts as unknown before the answer`() {
        assertEquals(Presence.UNKNOWN, presenceBeforeAnswer(wasIn = true))
    }

    @Test
    fun `a group never stood in waits for the answer`() {
        assertEquals(null, presenceBeforeAnswer(wasIn = false))
    }
}
