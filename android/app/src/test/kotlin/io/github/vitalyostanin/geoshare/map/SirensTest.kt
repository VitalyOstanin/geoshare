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
 * The same expectations the page's own `sirens.test.mjs` checks: the phone and
 * the caller's screen read the same document, and have to agree on whether a
 * call is still worth answering.
 */
class SirensTest {
    private val now: Instant = Instant.parse("2026-09-27T19:40:00Z")

    private fun called(
        calledSecondsAgo: Long,
        heardAt: Instant? = null,
        stoppedBy: String? = null,
        stoppedAt: Instant? = null,
    ): Siren = sirenOf(
        phoneId = "grandma.tablet",
        calledBy = "anna",
        calledAt = now.minusSeconds(calledSecondsAgo),
        heardAt = heardAt,
        stoppedBy = stoppedBy,
        stoppedAt = stoppedAt,
    )!!

    @Test
    fun `a document that is not a siren is none`() {
        assertNull(sirenOf("grandma.tablet", null, now, null, null, null))
        assertNull(sirenOf("grandma.tablet", "anna", null, null, null, null))
        assertNull(sirenOf("grandma", "anna", now, null, null, null))
    }

    @Test
    fun `the member is the phone id up to its dot`() {
        assertEquals("grandma", called(0).memberId)
    }

    @Test
    fun `a stale call is not sounded`() {
        assertFalse(shouldSound(called(61), lastAnswered = null, now = now))
    }

    @Test
    fun `a call already answered is not sounded again`() {
        val siren = called(10)

        assertFalse(shouldSound(siren, lastAnswered = siren.calledAt, now = now))
        assertFalse(shouldSound(siren, lastAnswered = now, now = now))
    }

    @Test
    fun `a stopped call is not sounded`() {
        val siren = called(10, stoppedBy = "boris", stoppedAt = now)

        assertFalse(shouldSound(siren, lastAnswered = null, now = now))
    }

    @Test
    fun `a fresh unanswered call is sounded`() {
        val siren = called(10)

        assertTrue(shouldSound(siren, lastAnswered = now.minusSeconds(20), now = now))
        assertTrue(shouldSound(siren, lastAnswered = null, now = now))
    }

    @Test
    fun `called and still within reach is waiting`() {
        assertEquals(SirenState.Waiting, sirenState(called(59), now))
    }

    @Test
    fun `heard and within its seconds is sounding`() {
        val siren = called(30, heardAt = now.minusSeconds(5))

        assertEquals(
            SirenState.Sounding(siren.heardAt!!.plus(Duration.ofSeconds(SIREN_SECONDS))),
            sirenState(siren, now),
        )
    }

    @Test
    fun `heard and past its seconds is over`() {
        val siren = called(30, heardAt = now.minusSeconds(SIREN_SECONDS + 1))

        assertEquals(SirenState.Over, sirenState(siren, now))
    }

    @Test
    fun `unheard and past the fresh window is no answer`() {
        assertEquals(SirenState.NoAnswer, sirenState(called(61), now))
    }

    @Test
    fun `stopped wins over every other state`() {
        val siren = called(61, heardAt = now.minusSeconds(30), stoppedBy = "boris", stoppedAt = now)

        assertEquals(SirenState.Stopped("boris"), sirenState(siren, now))
    }

    @Test
    fun `a countdown moves only while some call is waiting or sounding`() {
        val over = called(60, heardAt = now.minusSeconds(40))
        val stopped = called(5, stoppedBy = "anna", stoppedAt = now)

        assertFalse(anyLive(emptyList(), now))
        assertFalse(anyLive(listOf(over, stopped), now))
        assertTrue(anyLive(listOf(over, called(5)), now))
        assertTrue(anyLive(listOf(stopped, called(10, heardAt = now.minusSeconds(3))), now))
    }

    @Test
    fun `every phone offered is followed, and a phone never called is left out`() {
        val rung = called(5)
        val sirens =
            runBlocking {
                sirensOf(setOf("grandma.tablet", "anna.phone")) { phoneId ->
                    flowOf(rung.takeIf { phoneId == "grandma.tablet" })
                }.first()
            }

        assertEquals(mapOf("grandma.tablet" to rung), sirens)
    }

    @Test
    fun `a call over for more than five minutes is no longer told of`() {
        val minute = 60L

        assertFalse(
            sirenTold(
                called(10 * minute, stoppedBy = "anna", stoppedAt = now.minusSeconds(6 * minute)),
                now,
            ),
        )
        assertTrue(
            sirenTold(
                called(10 * minute, stoppedBy = "anna", stoppedAt = now.minusSeconds(4 * minute)),
                now,
            ),
        )
        // Unanswered: over a minute after the call, told for five more.
        assertTrue(sirenTold(called(5 * minute), now))
        assertFalse(sirenTold(called(6 * minute + 1), now))
        // Heard: over when its seconds ran out.
        assertTrue(sirenTold(called(10 * minute, heardAt = now.minusSeconds(5 * minute)), now))
        assertFalse(sirenTold(called(10 * minute, heardAt = now.minusSeconds(6 * minute)), now))
        // Still being called is always told.
        assertTrue(sirenTold(called(1), now))
    }
}
