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

        assertTrue(mayBeRemoved(roster, bob, by = alice))
        assertFalse(mayBeRemoved(roster, alice, by = alice))
        assertFalse(mayBeRemoved(roster, stranger, by = alice))
    }

    @Test
    fun `only somebody who runs the group takes others off it`() {
        val carol = "carol-uid"
        val roster = Roster(members = listOf(alice, bob, carol), admins = listOf(alice))

        assertFalse(mayBeRemoved(roster, carol, by = bob))
        assertTrue(mayBeRemoved(roster, carol, by = alice))
        assertFalse(mayBeRemoved(roster, carol, by = stranger))
    }

    // The household made in the console names no administrators, and its
    // owner runs it all the same once the document says who that is.
    @Test
    fun `the founder runs the group without being named among its administrators`() {
        val roster = Roster(members = listOf(alice, bob), founder = alice)

        assertEquals(Standing.Administrator, standingIn(roster, alice))
        assertEquals(Standing.Member, standingIn(roster, bob))
    }

    @Test
    fun `a founder who has left the roster runs nothing`() {
        val roster = Roster(members = listOf(bob), founder = alice)

        assertFalse(runsTheGroup(roster, alice))
        assertEquals(Standing.Unrun, standingIn(roster, bob))
    }

    @Test
    fun `the founder is taken off by nobody but themselves`() {
        val both = listOf(alice, bob)
        val roster = Roster(members = both, admins = both, founder = alice)

        assertFalse(mayBeRemoved(roster, alice, by = bob))
        assertTrue(mayBeRemoved(roster, bob, by = alice))
    }

    @Test
    fun `an administrator is taken off only by the founder`() {
        val carol = "carol-uid"
        val everybody = listOf(alice, bob, carol)
        val roster = Roster(members = everybody, admins = everybody, founder = alice)

        assertFalse(mayBeRemoved(roster, carol, by = bob))
        assertTrue(mayBeRemoved(roster, carol, by = alice))
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
    fun `an address the rules would accept is accepted before the write is tried`() {
        assertTrue(looksLikeAnAddress("ivan.petrov@example.com"))
        assertTrue(looksLikeAnAddress("  ivan.petrov@example.com  "))
        assertFalse(looksLikeAnAddress(""))
        assertFalse(looksLikeAnAddress("ivan.petrov"))
        assertFalse(looksLikeAnAddress("ivan.petrov@example"))
        assertFalse(looksLikeAnAddress("ivan petrov@example.com"))
        assertFalse(looksLikeAnAddress("@example.com"))

        val long = "a".repeat(249) + "@a.co"

        assertEquals(254, long.length)
        assertTrue(looksLikeAnAddress(long))
        assertFalse(looksLikeAnAddress("a$long"))
    }

    @Test
    fun `a group is named the way an account is`() {
        assertTrue(looksLikeAGroupName(alice))
        assertTrue(looksLikeAGroupName("family"))
        assertFalse(looksLikeAGroupName(""))
        assertFalse(looksLikeAGroupName("a".repeat(129)))
    }

    private val carol = "carol-uid"

    @Test
    fun `whoever runs the group hands it to a member who does not run it yet`() {
        val roster = Roster(members = listOf(alice, bob, carol), admins = listOf(alice, bob))

        assertTrue(mayBeMadeAdmin(roster, carol, alice))
        assertFalse(mayBeMadeAdmin(roster, bob, alice))
        assertFalse(mayBeMadeAdmin(roster, stranger, alice))
        assertFalse(mayBeMadeAdmin(roster, alice, carol))
    }

    @Test
    fun `the founder is handed nothing, running the group unnamed`() {
        val roster = Roster(members = listOf(alice, bob), founder = alice)

        assertFalse(mayBeMadeAdmin(roster, alice, alice))
        assertTrue(mayBeMadeAdmin(roster, bob, alice))
    }

    // The rules let an administrator who is not the founder relieve another,
    // and themselves: only the founder's place is out of their reach.
    @Test
    fun `an administrator relieves another, and may give the role up`() {
        val roster = Roster(members = listOf(alice, bob, carol), admins = listOf(alice, bob))

        assertTrue(mayStopRunning(roster, bob, alice))
        assertTrue(mayStopRunning(roster, alice, alice))
        assertFalse(mayStopRunning(roster, carol, alice))
        assertFalse(mayStopRunning(roster, alice, carol))
    }

    @Test
    fun `the last administrator keeps the role while no founder runs the group`() {
        val roster = Roster(members = listOf(alice, bob), admins = listOf(alice))

        assertFalse(mayStopRunning(roster, alice, alice))
    }

    @Test
    fun `with the founder there, the last named administrator may go`() {
        val roster = Roster(members = listOf(alice, bob), admins = listOf(bob), founder = alice)

        assertTrue(mayStopRunning(roster, bob, alice))
        assertTrue(mayStopRunning(roster, bob, bob))
    }

    @Test
    fun `the founder is not offered to be relieved even when named`() {
        val roster =
            Roster(members = listOf(alice, bob), admins = listOf(alice, bob), founder = alice)

        assertFalse(mayStopRunning(roster, alice, bob))
        assertFalse(mayStopRunning(roster, alice, alice))
    }
}
