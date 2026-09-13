package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class WatchingTest {
    private val now: Instant = Instant.parse("2026-09-13T12:00:00Z")

    @Test
    fun `a member watching from two places is named once`() {
        val watchers =
            listOf(
                Watcher("anna", now.plusSeconds(60)),
                Watcher("anna", now.plusSeconds(120)),
            )

        assertEquals(listOf("anna"), watchingMembers(watchers, now))
    }

    @Test
    fun `a claim that has run out names nobody`() {
        val watchers = listOf(Watcher("anna", now.minusSeconds(1)))

        assertEquals(emptyList<String>(), watchingMembers(watchers, now))
    }

    @Test
    fun `the ones still looking are kept and the stale ones dropped`() {
        val watchers =
            listOf(
                Watcher("boris", now.plusSeconds(30)),
                Watcher("anna", now.minusSeconds(30)),
                Watcher("clara", now.plusSeconds(30)),
            )

        assertEquals(listOf("boris", "clara"), watchingMembers(watchers, now))
    }

    @Test
    fun `the order does not follow the order the claims arrived in`() {
        val watchers =
            listOf(
                Watcher("clara", now.plusSeconds(30)),
                Watcher("anna", now.plusSeconds(30)),
            )

        assertEquals(listOf("anna", "clara"), watchingMembers(watchers, now))
    }
}
