package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * What the erasing row says, which is the whole of what can be checked off a
 * phone: the command itself is answered by the receiver, and no receiver runs
 * in these tests.
 */
class AidingDataTest {
    // A member who has not touched the button is told nothing: a row that
    // said "erased" or "refused" before anything was asked would be a claim
    // about a receiver nobody has spoken to.
    @Test
    fun `nothing is said before the receiver has been asked`() {
        assertNull(aidingErasureNote(AidingErasure.Untouched))
    }

    @Test
    fun `a receiver that took the command says so`() {
        assertEquals(R.string.aiding_erased, aidingErasureNote(AidingErasure.Erased))
    }

    // Refusal is the answer on a phone whose receiver does not know the
    // command at all, and it has to read as a refusal rather than as a
    // silence that looks exactly like success.
    @Test
    fun `a receiver that would not is not shown as a receiver that did`() {
        assertEquals(R.string.aiding_refused, aidingErasureNote(AidingErasure.Refused))
        assertNotEquals(
            aidingErasureNote(AidingErasure.Erased),
            aidingErasureNote(AidingErasure.Refused),
        )
    }

    @Test
    fun `every way the asking can end has a word of its own`() {
        val said = AidingErasure.entries.map { state -> aidingErasureNote(state) }

        assertEquals(AidingErasure.entries.size, said.distinct().size)
    }
}
