package io.github.vitalyostanin.geoshare.collection

import io.github.vitalyostanin.geoshare.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * That the help covers every reason a device can actually write.
 *
 * A reason missing from the help is a member reading a name on a card,
 * pressing it and not finding the one line they opened it for — and nothing
 * else goes wrong to say so. The list is checked against [JumpReason] itself
 * rather than against a list kept beside it, because the entries are what the
 * device writes into the document.
 */
class ReasonExplainedTest {
    @Test
    fun `the help explains every reason a phone writes`() {
        val explained = reasonsExplained().map { it.reason }.toSet()

        assertEquals(JumpReason.entries.map { it.name }.toSet(), explained - LEFT_OUT_BY_SOURCE)
    }

    // Not an entry of the list above, because nothing is wrong with such an
    // answer: the member told the device not to count that source. It is in
    // the help all the same, since it is what a card shows.
    @Test
    fun `a source the member switched off is explained as well`() {
        assertEquals(1, reasonsExplained().count { it.reason == LEFT_OUT_BY_SOURCE })
    }

    @Test
    fun `no reason is explained twice`() {
        val explained = reasonsExplained()

        assertEquals(explained.size, explained.map { it.reason }.distinct().size)
    }

    // The name on the card and the name in the help are one string, so that a
    // member who has just read "Two positions at one time" finds that same
    // line rather than a second wording of it.
    @Test
    fun `a reason is named on the card and in the help by the same words`() {
        reasonsExplained().forEach { explained ->
            assertEquals(leftOutReasonLabel(explained.reason), explained.name)
        }
    }

    @Test
    fun `every reason says both what was seen and what it means`() {
        reasonsExplained().forEach { explained ->
            assertNotEquals(
                "${explained.reason} explains itself with one line twice",
                explained.sign,
                explained.means,
            )
            assertNotEquals("${explained.reason} has no sign", 0, explained.sign)
            assertNotEquals("${explained.reason} has no meaning", 0, explained.means)
        }
    }

    // The order is the axis the help is read along: what the rule had to look
    // at. The width is read off one answer, and the switched-off source is not
    // a fault at all.
    @Test
    fun `the reason read off one answer comes first and the member's own choice last`() {
        val order = reasonsExplained().map { it.reason }

        assertEquals(JumpReason.Width.name, order.first())
        assertEquals(LEFT_OUT_BY_SOURCE, order.last())
    }

    @Test
    fun `no two reasons share an explanation`() {
        val explained = reasonsExplained()

        assertEquals(explained.size, explained.map { it.means }.distinct().size)
        assertEquals(explained.size, explained.map { it.sign }.distinct().size)
    }

    @Test
    fun `the help has a title and an opening of its own`() {
        assertNotEquals(R.string.reason_help_title, R.string.reason_help_intro)
    }
}
