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

    // A group of twenty with one map open used to hold all twenty devices in
    // the fast mode; a device the viewer cannot see has nothing to be fast for.
    @Test
    fun `a viewer the owner shares nothing with does not count`() {
        val watchers = listOf(Watcher("boris", now.plusSeconds(300)))

        assertEquals(emptyList<Instant>(), watchedUntil(watchers, "anna", emptyMap()))
    }

    @Test
    fun `a viewer the owner shares with counts until the claim runs out`() {
        val claim = now.plusSeconds(300)
        val watchers = listOf(Watcher("boris", claim))
        val sharedWith = mapOf("boris" to now.plusSeconds(86_400))

        assertEquals(listOf(claim), watchedUntil(watchers, "anna", sharedWith))
    }

    @Test
    fun `a permission ending first ends the claim with it`() {
        val permission = now.plusSeconds(60)
        val watchers = listOf(Watcher("boris", now.plusSeconds(300)))

        assertEquals(
            listOf(permission),
            watchedUntil(watchers, "anna", mapOf("boris" to permission)),
        )
    }

    // The owner looking from another device of theirs names nobody in the
    // grant, and is somebody the device should be fresh for all the same.
    @Test
    fun `the owner's own other device always counts`() {
        val claim = now.plusSeconds(300)

        assertEquals(
            listOf(claim),
            watchedUntil(listOf(Watcher("anna", claim)), "anna", emptyMap()),
        )
    }

    @Test
    fun `before the grant is read every viewer counts`() {
        val watchers = listOf(Watcher("boris", now.plusSeconds(300)))

        assertEquals(listOf(now.plusSeconds(300)), watchedUntil(watchers, "anna", null))
    }

    // While the group is looking for this member, the rules open their
    // position to everybody in it -- a viewer the grant would otherwise leave
    // out is exactly the one the search is for.
    @Test
    fun `a search open for the owner counts every viewer, shared with or not`() {
        val claim = now.plusSeconds(300)
        val watchers = listOf(Watcher("boris", claim))

        assertEquals(
            listOf(claim),
            watchedUntil(watchers, "anna", emptyMap(), sought = true),
        )
    }

    @Test
    fun `the device is watched while anybody in any group watches it`() {
        val family = listOf(now.plusSeconds(600))
        val hike = listOf(now.plusSeconds(60))

        assertEquals(
            listOf(now.plusSeconds(60), now.plusSeconds(600)),
            watchedInAnyGroup(listOf(family, hike)),
        )
    }

    @Test
    fun `no group watching is no watching`() {
        assertEquals(emptyList<Instant>(), watchedInAnyGroup(listOf(emptyList(), emptyList())))
    }

    @Test
    fun `no groups at all is no watching`() {
        assertEquals(emptyList<Instant>(), watchedInAnyGroup(emptyList()))
    }
}
