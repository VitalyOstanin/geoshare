package io.github.vitalyostanin.geoshare.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetPullTest {
    private var open = false
    private var closed = false

    private val pull =
        SheetPull(
            threshold = THRESHOLD,
            detailsOpen = { open },
            onDetailsOpen = { open = it },
            onClose = { closed = true },
        )

    // A card that fits scrolls nothing of its own, so the whole drag is left
    // over for the sheet, the way it reaches it on the device.
    private fun pullBody(vararg deltas: Float) = runBlocking {
        deltas.forEach {
            pull.connection.onPostScroll(Offset.Zero, Offset(0f, it), NestedScrollSource.UserInput)
        }
        pull.connection.onPostFling(Velocity.Zero, Velocity.Zero)
    }

    @Test
    fun `pulled down from the summary the card closes`() {
        pullBody(30f, 30f)

        assertTrue(closed)
        assertFalse(open)
    }

    @Test
    fun `pulled down from the details the card goes back to the summary and stays`() {
        open = true

        pullBody(THRESHOLD + 1f)

        assertFalse(open)
        assertFalse(closed)
    }

    @Test
    fun `the second pull down after the details closes the card`() {
        open = true

        pullBody(THRESHOLD + 1f)
        pullBody(THRESHOLD + 1f)

        assertTrue(closed)
    }

    @Test
    fun `pulled up from the summary the details open`() {
        pullBody(-THRESHOLD - 1f)

        assertTrue(open)
        assertFalse(closed)
    }

    @Test
    fun `a pull shorter than the threshold changes nothing`() {
        pullBody(THRESHOLD - 1f)

        assertFalse(open)
        assertFalse(closed)
    }

    // Each drag is counted from its own start: two short pulls are not one long one.
    @Test
    fun `short pulls do not add up across drags`() {
        pullBody(THRESHOLD - 1f)
        pullBody(THRESHOLD - 1f)

        assertFalse(closed)
    }

    @Test
    fun `a fling that is not the finger does not pull the card`() {
        runBlocking {
            pull.connection.onPostScroll(
                Offset.Zero,
                Offset(0f, THRESHOLD * 2),
                NestedScrollSource.SideEffect,
            )
            pull.connection.onPostFling(Velocity.Zero, Velocity.Zero)
        }

        assertFalse(closed)
    }

    @Test
    fun `the handle pulls the card the same way`() {
        pull.drag(THRESHOLD + 1f)
        pull.settle()

        assertTrue(closed)
    }

    @Test
    fun `the sheet takes what the body left over`() {
        val taken =
            pull.connection.onPostScroll(Offset.Zero, Offset(0f, 12f), NestedScrollSource.UserInput)

        assertEquals(Offset(0f, 12f), taken)
    }

    private companion object {
        const val THRESHOLD = 48f
    }
}
