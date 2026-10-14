package io.github.vitalyostanin.geoshare.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** One fix judged, written and remembered before the next is judged. */
class TurnTest {
    @Test
    fun `a fix arriving during a write is judged against what that write left`() = runBlocking {
        val turn = Mutex()
        val firstWriteLands = CompletableDeferred<Unit>()
        var lastWritten = "home"
        val measuredFrom = mutableListOf<String>()

        val first =
            async {
                inTurn(
                    turn,
                    judge = { measuredFrom += lastWritten },
                    worthWriting = { true },
                    write = {
                        firstWriteLands.await()
                        lastWritten = "shop"
                        true
                    },
                )
            }

        // The first fix is now waiting for the database.
        repeat(TIMES) { yield() }

        val second =
            async {
                inTurn(
                    turn,
                    judge = { measuredFrom += lastWritten },
                    worthWriting = { true },
                    write = { true },
                )
            }

        repeat(TIMES) { yield() }

        assertEquals(listOf("home"), measuredFrom)

        firstWriteLands.complete(Unit)
        first.await()
        second.await()

        assertEquals(listOf("home", "shop"), measuredFrom)
    }

    @Test
    fun `a fix not worth writing is not written`() = runBlocking {
        var written = false

        val turn =
            inTurn(
                Mutex(),
                judge = { "jump" },
                worthWriting = { it != "jump" },
                write = {
                    written = true
                    true
                },
            )

        assertEquals("jump", turn.judged)
        assertFalse(turn.wrote)
        assertFalse(written)
    }

    @Test
    fun `a write the database refused is reported as not written`() = runBlocking {
        val turn = inTurn(Mutex(), judge = { 1 }, worthWriting = { true }, write = { false })

        assertFalse(turn.wrote)
    }

    @Test
    fun `the turn is given back after it is taken`() = runBlocking {
        val lock = Mutex()

        inTurn(lock, judge = { 1 }, worthWriting = { true }, write = { true })

        assertTrue(lock.tryLock())
    }

    private companion object {
        const val TIMES = 10
    }
}
