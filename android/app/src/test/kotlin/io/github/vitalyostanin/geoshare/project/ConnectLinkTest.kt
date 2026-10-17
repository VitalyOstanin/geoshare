package io.github.vitalyostanin.geoshare.project

import io.github.vitalyostanin.geoshare.cutToLimit
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URLEncoder
import java.util.Random

private const val SITE = "geoshare-example-abc123"

private const val EMOJI = "\uD83D\uDE00"

/**
 * The cases the join page's `invitationTyped` is held to as well
 * (`web/tests/join.test.mjs`). Relative to the module, which is where Gradle
 * runs these tests from.
 */
private const val SHARED_CASES = "../../web/tests/fixtures/connect-links.json"

private val PIECES = listOf(
    "a", "b", "Z", "7", "Я", "ж", "é", " ", "  ", "\t", "&", "#", "%", "=", "+", "?", "/", "…",
    "%41", EMOJI,
)

/** A caption from [PIECES]: sometimes short or empty, sometimes of blanks only. */
private fun randomCaption(random: Random): String = when (random.nextInt(8)) {
    0 -> ""

    1 -> " ".repeat(random.nextInt(5))

    else -> buildString {
        repeat(random.nextInt(12)) { append(PIECES[random.nextInt(PIECES.size)]) }
    }
}

/**
 * A caption of 59 to 61 UTF-16 units with an emoji put so that it covers, or
 * ends just short of, the 60-unit limit, and blanks around it at times.
 */
private fun captionAtTheLimit(random: Random): String {
    val before = 56 + random.nextInt(5)
    val lead = if (random.nextBoolean()) " ".repeat(random.nextInt(3)) else ""
    val tail = if (random.nextBoolean()) "я".repeat(random.nextInt(3)) else ""
    val blanks = if (random.nextBoolean()) " " else ""

    return lead + "a".repeat(before) + blanks + EMOJI + tail
}

/** An address whose shortened form reaches the limit, or stays short. */
private fun randomAddress(random: Random): String {
    val name =
        buildString { repeat(random.nextInt(9)) { append("ivanпетров&#%"[random.nextInt(13)]) } }
    val domain = when (random.nextInt(3)) {
        0 -> "example.com"
        1 -> "d".repeat(48 + random.nextInt(8)) + EMOJI + ".com"
        else -> "x".repeat(random.nextInt(70)) + ".org"
    }

    return if (random.nextInt(10) == 0) name else "$name@$domain"
}

/**
 * What [ConnectLink.parse] keeps of a caption it is given whole: trimmed, cut
 * to the limit without splitting a pair, trimmed again, and nothing if blank.
 */
private fun kept(caption: String?): String? =
    caption?.trim()?.let { cutToLimit(it, 60) }?.trim()?.takeIf { it.isNotEmpty() }

class ConnectLinkTest {
    @Test
    fun `the project owner's link without an invitation is read`() {
        assertEquals(
            ConnectLink(site = SITE, invitation = null),
            ConnectLink.parse("https://$SITE.web.app/join"),
        )
    }

    @Test
    fun `a cut that falls inside an emoji drops the whole emoji`() {
        val link = ConnectLink.invitationLink(
            site = SITE,
            groupId = "g",
            code = "c",
            inviter = "a".repeat(59) + "\uD83D\uDE00",
            groupName = null,
            email = "ivan.petrov@example.com",
        )

        assertEquals("a".repeat(59), ConnectLink.parse(link)?.invitation?.inviter)
        val typed = "a".repeat(59) + "%F0%9F%98%80"
        val read = ConnectLink.parse("https://$SITE.web.app/join#g=g&c=c&by=$typed")
        assertEquals("a".repeat(59), read?.invitation?.inviter)
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

    // The join page reads the fragment first; so does the application.
    @Test
    fun `where the query and the fragment disagree the fragment wins`() {
        assertEquals(
            ConnectLink.parse("https://$SITE.web.app/join#g=group-2&c=HGFEDCBA"),
            ConnectLink.parse(
                "https://$SITE.web.app/join?g=group-1&c=ABCDEFGH#g=group-2&c=HGFEDCBA",
            ),
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

    @Test
    fun `an emoji on the sixtieth position is dropped whole or kept whole`() {
        val atTheEdge = "a".repeat(59) + EMOJI
        val justInside = "a".repeat(58) + EMOJI
        val outside = "a".repeat(60) + EMOJI

        assertEquals("a".repeat(59), readBackInviter(atTheEdge))
        assertEquals(justInside, readBackInviter(justInside))
        assertEquals("a".repeat(60), readBackInviter(outside))
    }

    // ADR-0017: the link reads back as the captions it was made from, whatever
    // they hold. A fixed seed keeps a failure the same on every run.
    @Test
    fun `any captions and address read back as parse would keep them`() {
        val random = Random(560L)

        repeat(600) { case ->
            val atTheLimit = case % 2 == 0
            val inviter = if (random.nextInt(6) == 0) {
                null
            } else {
                if (atTheLimit) captionAtTheLimit(random) else randomCaption(random)
            }
            val groupName = if (random.nextInt(6) == 0) {
                null
            } else {
                if (atTheLimit) captionAtTheLimit(random) else randomCaption(random)
            }
            val email = randomAddress(random)
            val link = ConnectLink.invitationLink(
                SITE,
                "Kx9aQ2",
                "7FK3M9QA",
                inviter,
                groupName,
                email,
            )

            assertEquals(
                "case $case: inviter=$inviter groupName=$groupName email=$email",
                ConnectLink(
                    site = SITE,
                    invitation = Invitation(
                        groupId = "Kx9aQ2",
                        code = "7FK3M9QA",
                        inviter = kept(inviter),
                        groupName = kept(groupName),
                        invitedAddress = kept(ConnectLink.shortenedAddress(email)),
                    ),
                ),
                ConnectLink.parse(link),
            )
        }
    }

    @Test
    fun `every case the join page shares is read as the file says`() {
        val cases = JSONObject(File(SHARED_CASES).readText()).getJSONArray("cases")

        assertTrue("$SHARED_CASES holds no cases", cases.length() > 0)

        for (index in 0 until cases.length()) {
            val case = cases.getJSONObject(index)
            val expected = if (case.isNull(
                    "expected",
                )
            ) {
                null
            } else {
                linkOf(case.getJSONObject("expected"))
            }

            assertEquals(
                case.getString("name"),
                expected,
                ConnectLink.parse(case.getString("text")),
            )
        }
    }

    private fun linkOf(json: JSONObject): ConnectLink {
        val invitation = if (json.isNull("invitation")) null else json.getJSONObject("invitation")

        return ConnectLink(
            site = json.getString("site"),
            invitation = invitation?.let {
                Invitation(
                    groupId = it.getString("groupId"),
                    code = it.getString("code"),
                    inviter = it.optStringOrNull("inviter"),
                    groupName = it.optStringOrNull("groupName"),
                    invitedAddress = it.optStringOrNull("invitedAddress"),
                )
            },
        )
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else getString(key)

    private fun readBackInviter(inviter: String): String? = ConnectLink.parse(
        ConnectLink.invitationLink(SITE, "g", "c", inviter, null, "ivan.petrov@example.com"),
    )?.invitation?.inviter
}
