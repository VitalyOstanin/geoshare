package io.github.vitalyostanin.geoshare.map

import io.github.vitalyostanin.geoshare.membership.Invitation
import io.github.vitalyostanin.geoshare.project.ConnectLink
import org.junit.Assert.assertEquals
import org.junit.Test

class WayInPasteTest {
    private val link = ConnectLink.invitationLink(
        site = "geoshare-example-abc123",
        groupId = "alice-uid",
        code = "ABCD2345",
        inviter = "Ivan",
        groupName = "Neighbours",
        email = "ivan.petrov@example.com",
    )

    @Test
    fun `a pasted link is handed to the same path a messenger's link takes`() {
        assertEquals(WayInPaste.Link(link), wayInPaste(link))
    }

    @Test
    fun `a link carries whatever else it was pasted alongside`() {
        val message = "Here — $link — join us"

        assertEquals(WayInPaste.Link(message), wayInPaste(message))
    }

    @Test
    fun `a pasted group and code still reads as the older shape`() {
        assertEquals(
            WayInPaste.Code(Invitation("alice-uid", "ABCD2345")),
            wayInPaste("alice-uid.ABCD2345"),
        )
    }

    @Test
    fun `the owner's own link, with no invitation on it, names nothing to join here`() {
        assertEquals(
            WayInPaste.Neither,
            wayInPaste("https://geoshare-example-abc123.web.app/join"),
        )
    }

    @Test
    fun `neither shape is read from anything else`() {
        assertEquals(WayInPaste.Neither, wayInPaste(""))
        assertEquals(WayInPaste.Neither, wayInPaste("not an invitation"))
    }
}
