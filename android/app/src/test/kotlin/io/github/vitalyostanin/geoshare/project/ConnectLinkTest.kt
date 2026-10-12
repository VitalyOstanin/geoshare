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
    fun `punctuation after a link in a message is not read as part of its last field`() {
        val link = "https://$SITE.web.app/join#g=group-1&c=ABCDEFGH&to=v%E2%80%A6%40gmail.com"

        for (mark in listOf(".", ",", ")", ").", "!?")) {
            assertEquals(
                "after '$mark'",
                "v…@gmail.com",
                ConnectLink.parse("Вступай: $link$mark")?.invitation?.invitedAddress,
            )
        }
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
    fun `an invitation handed over in the query is read like one after the hash`() {
        assertEquals(
            ConnectLink.parse("https://$SITE.web.app/join#g=group-1&c=ABCDEFGH&by=Anna"),
            ConnectLink.parse("https://$SITE.web.app/join?g=group-1&c=ABCDEFGH&by=Anna"),
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

    @Test
    fun `an invitation link reads back as the invitation it was made from`() {
        val link = ConnectLink.invitationLink(
            site = SITE,
            groupId = "Kx9aQ2",
            code = "7FK3M9QA",
            inviter = "Анна Петрова",
            groupName = "Походы & горы",
            email = "ivan.petrov@example.com",
        )

        assertEquals(
            ConnectLink(
                site = SITE,
                invitation = Invitation(
                    groupId = "Kx9aQ2",
                    code = "7FK3M9QA",
                    inviter = "Анна Петрова",
                    groupName = "Походы & горы",
                    invitedAddress = "ivan…@example.com",
                ),
            ),
            ConnectLink.parse(link),
        )
    }

    // The full address is for the rules alone (ADR-0018).
    @Test
    fun `an invitation link carries the address only shortened`() {
        val link = ConnectLink.invitationLink(
            SITE,
            "g",
            "7FK3M9QA",
            null,
            null,
            "ivan.petrov@example.com",
        )

        assertEquals(false, link.contains("petrov"))
    }

    @Test
    fun `captions that are missing are left out of the link`() {
        assertEquals(
            "https://$SITE.web.app/join#g=g&c=7FK3M9QA&to=ivan%E2%80%A6%40example.com",
            ConnectLink.invitationLink(SITE, "g", "7FK3M9QA", null, " ", "ivan.petrov@example.com"),
        )
    }

    @Test
    fun `a short name keeps only its first character`() {
        assertEquals("i…@example.com", ConnectLink.shortenedAddress("ivan@example.com"))
        assertEquals("ivan…@example.com", ConnectLink.shortenedAddress("ivan.petrov@example.com"))
    }
}
