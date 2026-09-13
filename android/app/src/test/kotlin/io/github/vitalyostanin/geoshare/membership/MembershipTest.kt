package io.github.vitalyostanin.geoshare.membership

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MembershipTest {
    private val alice = "alice-uid"
    private val bob = "bob-uid"
    private val stranger = "stranger-uid"

    @Test
    fun `an account with no group at all can found one`() {
        assertEquals(Standing.Ungrouped, standingIn(null, alice))
    }

    @Test
    fun `an account off the roster is an outsider`() {
        val roster = Roster(members = listOf(alice), admins = listOf(alice))

        assertEquals(Standing.Outsider, standingIn(roster, stranger))
    }

    @Test
    fun `the roster tells a member from the one running the group`() {
        val roster = Roster(members = listOf(alice, bob), admins = listOf(alice))

        assertEquals(Standing.Administrator, standingIn(roster, alice))
        assertEquals(Standing.Member, standingIn(roster, bob))
    }

    // The household that predates invitations: the rules let nobody change
    // such a roster, so the screen has to say so rather than offer controls
    // whose every write would be refused.
    @Test
    fun `a group nobody runs is its own standing`() {
        val roster = Roster(members = listOf(alice, bob))

        assertEquals(Standing.Unrun, standingIn(roster, alice))
    }

    @Test
    fun `a founder is alone on both lists`() {
        assertEquals(Roster(listOf(alice), listOf(alice)), rosterFounded(alice))
    }

    // Leaving the administrators alone would leave somebody outside the group
    // still able to say who is in it, and the rules refuse that write whole.
    @Test
    fun `removing a member takes the running of the group with them`() {
        val roster = Roster(members = listOf(alice, bob), admins = listOf(alice, bob))

        assertEquals(Roster(listOf(alice), listOf(alice)), rosterWithout(roster, bob))
    }

    @Test
    fun `the last administrator cannot be removed`() {
        val roster = Roster(members = listOf(alice, bob), admins = listOf(alice))

        assertTrue(mayBeRemoved(roster, bob))
        assertFalse(mayBeRemoved(roster, alice))
        assertFalse(mayBeRemoved(roster, stranger))
    }

    @Test
    fun `a code carries neither the letters nor the digits read for each other`() {
        val codes = (1..200).map { newInvitationCode() }

        assertTrue(codes.all { it.length == CODE_LENGTH })
        val misread = setOf('I', 'O', '0', '1')

        assertTrue(codes.none { code -> code.any { it in misread } })
        assertTrue(codes.all { looksLikeACode(it) })
        assertTrue(codes.toSet().size > 190)
    }

    @Test
    fun `a code typed as it was read is tidied into the one that was sent`() {
        assertEquals("ABCD2345", tidyCode("abcd-2345"))
        assertEquals("ABCD2345", tidyCode("ABCD 2345"))
        assertEquals("ABCD2345", tidyCode(" abcd 2345 "))
    }

    @Test
    fun `what the rules would refuse is refused before it is written`() {
        assertFalse(looksLikeACode("SHORT"))
        assertFalse(looksLikeACode("ABCD234"))
        assertFalse(looksLikeACode("ABCD2340"))
        assertFalse(looksLikeACode("ABCD/345"))
        assertTrue(looksLikeACode("ABCD2345"))
    }
}
