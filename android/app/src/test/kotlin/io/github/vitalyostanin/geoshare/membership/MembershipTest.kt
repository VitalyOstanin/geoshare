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

    @Test
    fun `an invitation says which group it lets somebody into`() {
        assertEquals("$alice.ABCD2345", invitationText(alice, "ABCD2345"))
        assertEquals(Invitation(alice, "ABCD2345"), invitationFrom("$alice.ABCD2345"))
    }

    // A message carrying one is broken across lines by whatever it travelled
    // in, and the code half is read aloud as often as it is pasted.
    @Test
    fun `an invitation arrives as it was sent however it travelled`() {
        assertEquals(Invitation(alice, "ABCD2345"), invitationFrom(" $alice.abcd-2345 "))
        assertEquals(Invitation(alice, "ABCD2345"), invitationFrom("$alice.\nABCD 2345"))
    }

    // The case of an account id is the difference between two accounts, so the
    // half that names the group is passed on exactly as it came.
    @Test
    fun `the group half of an invitation keeps its case`() {
        val mixed = "Ab3xYz"

        assertEquals(Invitation(mixed, "ABCD2345"), invitationFrom("$mixed.abcd2345"))
    }

    // The group becomes part of a document path, and the code is written into
    // a document whose shape the rules hold to exactly eight characters.
    @Test
    fun `an invitation that would not reach a document is refused`() {
        assertEquals(null, invitationFrom("ABCD2345"))
        assertEquals(null, invitationFrom(".ABCD2345"))
        assertEquals(null, invitationFrom("$alice."))
        assertEquals(null, invitationFrom("$alice.ABCD234"))
        assertEquals(null, invitationFrom("../other.ABCD2345"))
        assertEquals(null, invitationFrom("groups/other.ABCD2345"))
    }

    @Test
    fun `a group is named the way an account is`() {
        assertTrue(looksLikeAGroupName(alice))
        assertTrue(looksLikeAGroupName("family"))
        assertFalse(looksLikeAGroupName(""))
        assertFalse(looksLikeAGroupName("a".repeat(129)))
    }
}
