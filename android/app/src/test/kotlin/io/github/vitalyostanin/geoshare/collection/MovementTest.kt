package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MovementTest {
    private val walkBegan = ActivityChange(travelling = true, begun = true)
    private val walkEnded = ActivityChange(travelling = true, begun = false)
    private val stillBegan = ActivityChange(travelling = false, begun = true)
    private val stillEnded = ActivityChange(travelling = false, begun = false)

    @Test
    fun `a device nobody has heard from is not moving`() {
        assertFalse(movingAfter(emptyList()))
    }

    @Test
    fun `setting off counts as moving`() {
        assertTrue(movingAfter(listOf(walkBegan)))
    }

    @Test
    fun `standing still ends it`() {
        assertFalse(movingAfter(listOf(walkBegan, walkEnded, stillBegan)))
    }

    @Test
    fun `being picked up off a table is not setting off`() {
        assertFalse(movingAfter(listOf(stillEnded), before = false))
    }

    @Test
    fun `the end of walking is enough to stop, without waiting to hear what follows`() {
        assertFalse(movingAfter(listOf(walkEnded), before = true))
    }

    @Test
    fun `the last change in a batch is the one that counts`() {
        val changes = listOf(stillEnded, walkBegan, walkEnded, stillBegan, stillEnded, walkBegan)

        assertTrue(movingAfter(changes))
    }

    @Test
    fun `a batch that says nothing new leaves the answer alone`() {
        assertTrue(movingAfter(listOf(stillEnded), before = true))
    }
}
