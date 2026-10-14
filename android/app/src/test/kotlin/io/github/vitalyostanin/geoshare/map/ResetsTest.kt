package io.github.vitalyostanin.geoshare.map

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/**
 * The device and the asking screen read the same document: the device has to
 * know whether a request is still worth carrying out, and both screens, the
 * page's `resets.test.mjs` among them, whether it is waiting, done or
 * unanswered.
 */
class ResetsTest {
    private val now: Instant = Instant.parse("2026-10-02T09:00:00Z")

    private fun asked(askedAgo: Duration, doneAt: Instant? = null): Reset = resetOf(
        phoneId = "grandma.tablet",
        askedBy = "anna",
        askedAt = now.minus(askedAgo),
        doneAt = doneAt,
    )!!

    @Test
    fun `a document that is not a request is none`() {
        assertNull(resetOf("grandma.tablet", null, now, null))
        assertNull(resetOf("grandma.tablet", "anna", null, null))
        assertNull(resetOf("grandma", "anna", now, null))
    }

    @Test
    fun `a fresh request is carried out`() {
        assertTrue(shouldForget(asked(Duration.ofHours(3)), lastDone = null, now = now))
    }

    @Test
    fun `a request older than a day is left alone`() {
        assertFalse(
            shouldForget(asked(Duration.ofDays(1).plusMinutes(1)), lastDone = null, now = now),
        )
    }

    @Test
    fun `a request already carried out is not carried out again`() {
        val reset = asked(Duration.ofMinutes(10))

        assertFalse(shouldForget(reset, lastDone = reset.askedAt, now = now))
        assertTrue(shouldForget(reset, lastDone = reset.askedAt.minusSeconds(1), now = now))
    }

    @Test
    fun `the asking screen tells waiting, done and unanswered apart`() {
        assertEquals(ResetState.Waiting, resetState(asked(Duration.ofHours(2)), now))

        val doneAt = now.minusSeconds(30)

        assertEquals(ResetState.Done(doneAt), resetState(asked(Duration.ofMinutes(1), doneAt), now))
        assertEquals(ResetState.NoAnswer, resetState(asked(Duration.ofDays(2)), now))
    }

    @Test
    fun `a row tells of a request for a while after it ends`() {
        assertTrue(resetTold(asked(Duration.ofHours(5)), now))
        assertTrue(
            resetTold(
                asked(Duration.ofMinutes(10), doneAt = now.minus(RESET_TOLD).plusSeconds(1)),
                now,
            ),
        )
        assertFalse(resetTold(asked(Duration.ofMinutes(10), doneAt = now.minus(RESET_TOLD)), now))
        assertFalse(resetTold(asked(RESET_FRESH.plus(RESET_TOLD)), now))
    }

    @Test
    fun `the requests of several devices are gathered by device, unasked ones left out`() =
        runBlocking {
            val reset = asked(Duration.ofMinutes(1))
            val gathered =
                resetsOf(setOf("grandma.tablet", "anna.phone")) { phoneId ->
                    flowOf(if (phoneId == "grandma.tablet") reset else null)
                }.first()

            assertEquals(mapOf("grandma.tablet" to reset), gathered)
            assertEquals(emptyMap<String, Reset>(), resetsOf(emptySet()) { flowOf(null) }.first())
        }
}
