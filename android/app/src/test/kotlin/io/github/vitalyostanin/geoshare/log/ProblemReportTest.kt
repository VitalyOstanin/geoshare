package io.github.vitalyostanin.geoshare.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder

/** The address of the issue a problem report opens: its length, what is cut and how it is written. */
class ProblemReportTest {
    private val log =
        (1..2000).joinToString("\n") { "12:00:%02d I/Map: line number $it".format(it % 60) }

    private fun bodyOf(url: String): String =
        URLDecoder.decode(url.substringAfter("&body="), "UTF-8")

    @Test
    fun `a short log goes in whole and nothing is said to be left out`() {
        val draft = buildIssue("it froze", "1.2.3", 34, "first\nsecond")

        assertFalse(draft.leftOut)
        assertEquals("first\nsecond", draft.tail)
        assertTrue(bodyOf(draft.url).contains("```\nfirst\nsecond\n```"))
    }

    @Test
    fun `the address never passes the limit however long the log`() {
        val draft = buildIssue("it froze", "1.2.3", 34, log)

        assertTrue(draft.url.length <= ISSUE_URL_LIMIT)
        assertTrue(draft.leftOut)
    }

    @Test
    fun `the log is cut from the start by whole lines and the last line is kept`() {
        val draft = buildIssue("it froze", "1.2.3", 34, log)
        val lines = draft.tail.lines()

        assertTrue(lines.first().startsWith("… earlier lines left out"))
        assertEquals(log.lines().last(), lines.last())
        assertTrue(lines.drop(1).all { it in log.lines() })
    }

    @Test
    fun `as many lines are kept as the limit allows`() {
        val draft = buildIssue("it froze", "1.2.3", 34, log)
        val longer = buildIssue("it froze", "1.2.3", 34, log, limit = ISSUE_URL_LIMIT + 200)

        assertTrue(longer.tail.lines().size > draft.tail.lines().size)
        assertTrue(draft.url.length > ISSUE_URL_LIMIT - 200)
    }

    @Test
    fun `text is encoded and a space is not a plus`() {
        val draft = buildIssue("a&b c=d\nsecond line", "1.2.3", 34, "x y")

        val page = "https://github.com/VitalyOstanin/geoshare/issues/new"

        assertTrue(draft.url.startsWith("$page?title=a%26b%20c%3Dd&body="))
        assertFalse(draft.url.substringAfter("?").contains("+"))
        assertTrue(
            bodyOf(
                draft.url,
            ).startsWith("a&b c=d\nsecond line\n\nApp version: 1.2.3\nAndroid SDK: 34"),
        )
    }

    @Test
    fun `the title is the first line of the description or a stock one`() {
        assertEquals("Problem report", issueTitle("  \n "))
        assertEquals("it froze", issueTitle("\n it froze \nthen more"))
        assertEquals(100, issueTitle("x".repeat(300)).length)
    }

    @Test
    fun `a long description in a wide alphabet still keeps the address within the limit`() {
        val draft = buildIssue("ж".repeat(5000), "1.2.3", 34, log)

        assertTrue(draft.url.length <= ISSUE_URL_LIMIT)
    }
}
