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
    fun `Done carrying an invitation leaves its group and code, without the captions`() {
        val done = ConnectState.Done(SETTINGS, INVITATION)

        assertEquals(
            PendingInvitation(groupId = "group-1", code = "ABCDEFGH"),
            pendingInvitationFor(done),
        )
    }

    @Test
    fun `an owner's own link leaves nothing pending`() {
        assertNull(pendingInvitationFor(ConnectState.Done(SETTINGS, LINK.invitation)))
    }
}
