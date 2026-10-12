package io.github.vitalyostanin.geoshare.service

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class UnknownAfterTest {
    // The grant read from the cache arrives in hundredths of a second: the
    // first decision is taken on it, not on a guess made a moment before.
    @Test
    fun `an answer in time is the first thing said`() = runBlocking {
        val grant =
            flow<Map<String, Int>?> {
                delay(20)
                emit(mapOf("alice" to 1))
            }

        val first = withTimeout(5_000) { grant.unknownAfter(Duration.ofSeconds(2)).first() }

        assertEquals(mapOf("alice" to 1), first)
    }

    // A grant that never comes leaves the device counting every claim, as it
    // did before the wait.
    @Test
    fun `silence past the wait is said as not known`() = runBlocking {
        val grant = flow<Map<String, Int>?> { awaitCancellation() }

        val first = withTimeout(5_000) { grant.unknownAfter(Duration.ofMillis(20)).first() }

        assertEquals(null, first)
    }

    @Test
    fun `a late answer follows the not known`() = runBlocking {
        val grant =
            flow<Map<String, Int>?> {
                delay(300)
                emit(mapOf("alice" to 1))
            }

        val said = withTimeout(5_000) { grant.unknownAfter(Duration.ofMillis(20)).take(2).toList() }

        assertEquals(listOf(null, mapOf("alice" to 1)), said)
    }
}
