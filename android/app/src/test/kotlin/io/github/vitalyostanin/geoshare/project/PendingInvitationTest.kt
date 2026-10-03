package io.github.vitalyostanin.geoshare.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val SITE = "geoshare-example-abc123"
private val SETTINGS = ProjectSettings(
    version = 1,
    projectId = "geoshare-example",
    applicationId = "1:123456789012:android:0123456789abcdef",
    apiKey = "AIzaExampleKeyNotReal",
    gcmSenderId = "123456789012",
    site = SITE,
)
private val LINK = ConnectLink(site = SITE, invitation = null)
private val INVITATION = Invitation(
    groupId = "group-1",
    code = "ABCDEFGH",
    inviter = "Anya",
    groupName = "Sunday hikers",
    invitedAddress = "ivan…@example.com",
)

class PendingInvitationTest {
    @Test
    fun `a pending invitation round-trips through its json`() {
        val invitation = PendingInvitation(groupId = "group-1", code = "ABCDEFGH")

        assertEquals(invitation, PendingInvitation.parse(invitation.toJson()))
    }

    @Test
    fun `text that is not json is not one`() {
        assertNull(PendingInvitation.parse("thanks for the recipe, see you Sunday"))
    }

    @Test
    fun `json missing either field is not one`() {
        assertNull(PendingInvitation.parse("""{"groupId": "group-1"}"""))
        assertNull(PendingInvitation.parse("""{"code": "ABCDEFGH"}"""))
    }

    @Test
    fun `Done carrying an invitation leaves its group, code and invited address`() {
        val done = ConnectState.Done(SETTINGS, INVITATION)

        assertEquals(
            PendingInvitation(
                groupId = "group-1",
                code = "ABCDEFGH",
                invitedAddress = "ivan…@example.com",
                projectId = "geoshare-example",
            ),
            pendingInvitationFor(done),
        )
    }

    @Test
    fun `the invited address survives the json`() {
        val invitation = PendingInvitation("group-1", "ABCDEFGH", "ivan…@example.com")

        assertEquals(invitation, PendingInvitation.parse(invitation.toJson()))
    }

    @Test
    fun `an account whose shortened address differs is another account`() {
        val invitation = PendingInvitation("group-1", "ABCDEFGH", "ivan…@example.com")

        assertEquals(true, invitation.meantForAnotherAccount("petr.sidorov@example.com"))
        assertEquals(false, invitation.meantForAnotherAccount("ivan.petrov@example.com"))
        assertEquals(false, invitation.meantForAnotherAccount("Ivan.Petrov@Example.com"))
    }

    // Without either side there is nothing to compare, and the rules decide.
    @Test
    fun `nothing to compare is no verdict`() {
        assertEquals(false, PendingInvitation("g", "ABCDEFGH").meantForAnotherAccount("a@b.c"))
        assertEquals(
            false,
            PendingInvitation("g", "ABCDEFGH", "ivan…@example.com").meantForAnotherAccount(null),
        )
    }

    @Test
    fun `an owner's own link leaves nothing pending`() {
        assertNull(pendingInvitationFor(ConnectState.Done(SETTINGS, LINK.invitation)))
    }

    @Test
    fun `the project an invitation is for survives the json`() {
        val invitation = PendingInvitation("group-1", "ABCDEFGH", projectId = "geoshare-hike")

        assertEquals(invitation, PendingInvitation.parse(invitation.toJson()))
    }

    // A device in two groups signs in to each on its own; an invitation to
    // one is not to be spent on the sign-in to the other.
    @Test
    fun `an invitation is spent only on a sign-in to its own project`() {
        val invitation = PendingInvitation("group-1", "ABCDEFGH", projectId = "geoshare-hike")

        assertEquals(true, invitation.isFor("geoshare-hike"))
        assertEquals(false, invitation.isFor("example-family"))
    }

    // Kept by a build that knew a single project: it can only have been for
    // the project the device was in.
    @Test
    fun `an invitation kept without a project is for any`() {
        assertEquals(true, PendingInvitation("group-1", "ABCDEFGH").isFor("example-family"))
    }
}
