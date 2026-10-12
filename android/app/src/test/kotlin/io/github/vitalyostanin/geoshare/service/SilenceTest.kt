package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.IDLE_REPEAT
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** When the quiet device says its position again, and when it asks another way. */
class SilenceTest {
    private val silence = Silence()

    @Test
    fun `a write older than the repeat is said again at once`() = runBlocking {
        silence.wroteAt = Instant.now() - IDLE_REPEAT - Duration.ofMinutes(1)
        var said = 0

        stopped {
            silence.keepSaying {
                said += 1
                throw Stop()
            }
        }

        assertEquals(1, said)
    }

    @Test
    fun `a recent write is not said again before the repeat`() = runBlocking {
        silence.wroteAt = Instant.now()
        var said = false

        withTimeoutOrNull(SHORT) {
            silence.keepSaying {
                said = true
                true
            }
        }

        assertFalse(said)
    }

    @Test
    fun `past the silence the device asks once and writes what it hears`() = runBlocking {
        val start = Instant.now()
        silence.heardAt = start - Duration.ofMinutes(1)
        var written: Fix? = null

        stopped {
            silence.rescue(
                Duration.ofSeconds(30),
                MODE,
                ask = { FIX },
                write = { fix ->
                    written = fix
                    throw Stop()
                },
                foundNothing = { },
            )
        }

        assertSame(FIX, written)
        // Counted from the asking, so a rescue waits its turn like the rest.
        assertFalse(silence.heardAt.isBefore(start))
    }

    @Test
    fun `a rescue that hears nothing says what else it knows`() = runBlocking {
        silence.heardAt = Instant.now() - Duration.ofMinutes(1)
        var noted = false

        stopped {
            silence.rescue(
                Duration.ofSeconds(30),
                MODE,
                ask = { null },
                write = { },
                foundNothing = {
                    noted = true
                    throw Stop()
                },
            )
        }

        assertTrue(noted)
    }

    @Test
    fun `while the provider answers nobody is asked another way`() = runBlocking {
        silence.heardAt = Instant.now()
        var asked = false

        withTimeoutOrNull(SHORT) {
            silence.rescue(
                Duration.ofMinutes(1),
                MODE,
                ask = {
                    asked = true
                    null
                },
                write = { },
                foundNothing = { },
            )
        }

        assertFalse(asked)
    }

    /** Ends a loop that never ends by itself, at the point the test has seen enough. */
    private class Stop : Exception()

    private suspend fun stopped(loop: suspend () -> Unit) {
        try {
            loop()
        } catch (_: Stop) {
        }
    }

    private companion object {
        const val SHORT = 200L
        val MODE = CollectionMode.entries.first()
        val FIX = Fix(61.03268, 4.40300, Instant.parse("2026-09-13T12:00:00Z"))
    }
}
