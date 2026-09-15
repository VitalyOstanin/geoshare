package io.github.vitalyostanin.geoshare.collection

import io.github.vitalyostanin.geoshare.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * That every reason a phone can write has a name on a card.
 *
 * A reason left out of the words is shown as unknown, and nothing else goes
 * wrong to say so: the build compiles, the tests pass and the card quietly
 * says "неизвестно" about the one thing a member opened it for. The entries
 * are what the phone writes into the document, so the check is over the whole
 * of [JumpReason] rather than over a list kept beside it.
 */
class LeftOutWordsTest {
    @Test
    fun `every reason a phone writes has a name`() {
        for (reason in JumpReason.entries) {
            assertNotEquals(
                "${reason.name} has no name of its own",
                R.string.map_detail_unknown,
                leftOutReasonLabel(reason.name),
            )
        }
    }

    // Not an entry of the list above, because nothing is wrong with such an
    // answer: the member told the phone not to count that source.
    @Test
    fun `a source the member switched off is named as well`() {
        assertEquals(R.string.left_out_by_source, leftOutReasonLabel(LEFT_OUT_BY_SOURCE))
    }

    // Which of the five was switched off is what a member opens the card for,
    // so the phone writes the switch beside the fact.
    @Test
    fun `a reason names the switch that was turned off`() {
        assertEquals(SourceChoice.Satellites, sourceLeftOut("Source:Satellites"))
        assertEquals(SourceChoice.OtherRadios, sourceLeftOut("Source:OtherRadios"))
        assertEquals(R.string.left_out_by_source, leftOutReasonLabel("Source:Satellites"))
    }

    // What the builds before this wrote, and what a build newer than this one
    // may write: both say a source was switched off without this one being
    // able to name it, which is true and better than a foreign word.
    @Test
    fun `a switch this build cannot name still says a source was switched off`() {
        assertEquals(null, sourceLeftOut(LEFT_OUT_BY_SOURCE))
        assertEquals(null, sourceLeftOut("Source:Lighthouse"))
        assertEquals(R.string.left_out_by_source, leftOutReasonLabel("Source:Lighthouse"))
    }

    @Test
    fun `a position with nothing left out names no reason`() {
        assertEquals(R.string.map_detail_unknown, leftOutReasonLabel(null))
    }

    // A word from a build newer than this one: shown as unknown rather than as
    // it came, for the same reason a source is.
    @Test
    fun `a reason this build has never heard of is shown as unknown`() {
        assertEquals(R.string.map_detail_unknown, leftOutReasonLabel("Sunspots"))
    }
}
