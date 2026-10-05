package io.github.vitalyostanin.geoshare.map

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class RefusalsTest {
    @Test
    fun `a refusal is asked again once the timer runs out`() = runBlocking {
        val asked = mutableListOf<String>()
        val refusals = Refusals(this, Duration.ofMillis(50))

        refusals.retryLater("oona") { asked += "oona" }
        delay(200)

        assertEquals(listOf("oona"), asked)
    }

    @Test
    fun `asking now asks every refusal at once and only once`() = runBlocking {
        val asked = mutableListOf<String>()
        val refusals = Refusals(this, Duration.ofMillis(100))

        refusals.retryLater("oona") { asked += "oona" }
        refusals.retryLater("ivan") { asked += "ivan" }
        refusals.retryNow()

        assertEquals(listOf("oona", "ivan"), asked)

        delay(300)

        assertEquals(listOf("oona", "ivan"), asked)
    }

    @Test
    fun `a second refusal of the same document replaces the first`() = runBlocking {
        var asked = 0
        val refusals = Refusals(this, Duration.ofMillis(50))

        refusals.retryLater("oona") { asked++ }
        refusals.retryLater("oona") { asked++ }
        delay(200)

        assertEquals(1, asked)
    }

    @Test
    fun `the last change is the latest moment among the profiles`() {
        val early = Instant.parse("2026-10-05T08:00:00Z")
        val late = Instant.parse("2026-10-05T08:30:00Z")
        val profiles =
            mapOf(
                "oona" to MemberProfile("oona", "Oona", null, early),
                "ivan" to MemberProfile("ivan", "Ivan", null, late),
                "petya" to MemberProfile("petya", "Petya", null),
            )

        assertEquals(late, lastProfileChange(profiles))
    }

    @Test
    fun `no change is known before the profiles arrive or carry one`() {
        assertNull(lastProfileChange(null))
        assertNull(lastProfileChange(mapOf("oona" to MemberProfile("oona", "Oona", null))))
    }
}
