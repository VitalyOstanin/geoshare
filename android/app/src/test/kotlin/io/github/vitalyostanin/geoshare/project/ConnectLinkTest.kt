package io.github.vitalyostanin.geoshare.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.URLEncoder

private const val SITE = "geoshare-example-abc123"

class ConnectLinkTest {
    @Test
    fun `the project owner's link without an invitation is read`() {
        assertEquals(
            ConnectLink(site = SITE, invitation = null),
            ConnectLink.parse("https://$SITE.web.app/join"),
        )
    }

    @Test
    fun `a trailing slash after join is accepted`() {
        assertEquals(
            ConnectLink(site = SITE, invitation = null),
            ConnectLink.parse("https://$SITE.web.app/join/"),
        )
    }

    @Test
    fun `an invitation carries the group, the code and the captions, Cyrillic and all`() {
        val inviter = URLEncoder.encode("Иван Петров", "UTF-8")
        val groupName = URLEncoder.encode("Соседи & друзья", "UTF-8")
        val address = URLEncoder.encode("ivan…@example.com", "UTF-8")
        val text = "https://$SITE.web.app/join" +
            "#g=group-1&c=ABCDEFGH&by=$inviter&n=$groupName&to=$address"

        assertEquals(
            ConnectLink(
                site = SITE,
                invitation = Invitation(
                    groupId = "group-1",
                    code = "ABCDEFGH",
                    inviter = "Иван Петров",
                    groupName = "Соседи & друзья",
                    invitedAddress = "ivan…@example.com",
                ),
            ),
            ConnectLink.parse(text),
        )
    }

    @Test
    fun `an invitation with no captions still has the group and the code`() {
        val link = ConnectLink.parse("https://$SITE.web.app/join#g=group-1&c=ABCDEFGH")

        assertEquals(
            Invitation(
                groupId = "group-1",
                code = "ABCDEFGH",
                inviter = null,
                groupName = null,
                invitedAddress = null,
            ),
            link?.invitation,
        )
    }

    @Test
    fun `a link pasted into a longer message is still found`() {
        val text = "Join us: https://$SITE.web.app/join#g=group-1&c=ABCDEFGH see you there"

        assertEquals(
            ConnectLink(
                site = SITE,
                invitation = Invitation("group-1", "ABCDEFGH", null, null, null),
            ),
            ConnectLink.parse(text),
        )
    }

    @Test
    fun `a firebaseapp-dot-com link is read the same, and settingsUrl still names web-app`() {
        val link = ConnectLink.parse("https://$SITE.firebaseapp.com/join")

        assertEquals(SITE, link?.site)
        assertEquals("https://$SITE.web.app/geoshare.json", link?.settingsUrl)
    }

    @Test
    fun `a fragment with neither a group nor a code is the owner's link`() {
        assertEquals(
            ConnectLink(site = SITE, invitation = null),
            ConnectLink.parse("https://$SITE.web.app/join#utm_source=chat"),
        )
    }

    @Test
    fun `a plain http link is refused`() {
        assertNull(ConnectLink.parse("http://$SITE.web.app/join"))
    }

    @Test
    fun `a link to another domain is refused`() {
        assertNull(ConnectLink.parse("https://$SITE.example.com/join"))
    }

    @Test
    fun `a path other than join is refused`() {
        assertNull(ConnectLink.parse("https://$SITE.web.app/leave"))
    }

    @Test
    fun `a site id starting with a digit is refused`() {
        assertNull(ConnectLink.parse("https://1geoshare.web.app/join"))
    }

    @Test
    fun `an invitation fragment without a code is refused rather than dropped`() {
        assertNull(ConnectLink.parse("https://$SITE.web.app/join#g=group-1"))
    }

    @Test
    fun `an overlong caption is cut down to size`() {
        val overlong = "a".repeat(80)
        val text = "https://$SITE.web.app/join#g=group-1&c=ABCDEFGH&by=$overlong"

        assertEquals(60, ConnectLink.parse(text)?.invitation?.inviter?.length)
    }
}
