package io.github.vitalyostanin.geoshare.map

import io.github.vitalyostanin.geoshare.project.ConnectLink
import io.github.vitalyostanin.geoshare.service.MadeInvitation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class AdminInvitationTest {
    private val made = MadeInvitation(
        groupId = "group-1",
        code = "ABCDEFGH",
        expiresAt = Instant.parse("2026-10-01T12:40:00Z"),
        email = "ivan.petrov@example.com",
    )

    @Test
    fun `a link is built from what making an invitation returned`() {
        val expected = ConnectLink.invitationLink(
            site = "geoshare-example-abc123",
            groupId = made.groupId,
            code = made.code,
            inviter = "Ivan",
            groupName = "Neighbours",
            email = made.email,
        )

        assertEquals(
            expected,
            invitationLinkText(
                made = made,
                site = "geoshare-example-abc123",
                inviter = "Ivan",
                groupName = "Neighbours",
            ),
        )
    }

    @Test
    fun `no link is built before this phone's own site is known`() {
        assertNull(invitationLinkText(made, site = null, inviter = null, groupName = null))
    }
}
