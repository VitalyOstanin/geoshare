package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Test

class ModeExplainedTest {
    @Test
    fun `the help describes every mode a phone can be in`() {
        val described = modesExplained().map { it.mode }.toSet()

        assertEquals(CollectionMode.entries.toSet(), described)
    }

    @Test
    fun `no mode is explained twice`() {
        val explained = modesExplained()

        assertEquals(explained.size, explained.map { it.mode }.distinct().size)
    }

    @Test
    fun `the hardest working mode is read first and the quietest last`() {
        val order = modesExplained().map { it.mode }

        assertEquals(CollectionMode.Viewing, order.first())
        assertEquals(CollectionMode.Idle, order.last())
    }

    @Test
    fun `a mode is named on the card and in the help by the same words`() {
        modesExplained().forEach { explained ->
            assertEquals(modeLabel(explained.mode.name), explained.name)
        }
    }
}
