package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/**
 * The same expectations the page's own `searches.test.mjs` checks: the two
 * clients read the same documents, and have to agree on when a search stands.
 */
class SearchesTest {
    private val now: Instant = Instant.parse("2026-09-27T19:40:00Z")

    private fun written(renewedHoursAgo: Long, hours: Int?): Search? = searchOf(
        memberId = "grandma",
        openedBy = "anna",
        renewedBy = "boris",
        startedAt = now.minus(Duration.ofHours(30)),
        renewedAt = now.minus(Duration.ofHours(renewedHoursAgo)),
        hours = hours,
    )

    @Test
    fun `a search ends its hours after the last renewal`() {
        val search = written(2, 24)!!

        assertEquals(now.plus(Duration.ofHours(22)), search.endsAt)
        assertEquals("anna", search.openedBy)
        assertEquals("boris", search.renewedBy)
        assertEquals(now.minus(Duration.ofHours(30)), search.startedAt)
    }

    @Test
    fun `a document that is not a search is none`() {
        assertNull(searchOf("grandma", null, "boris", now, now, 6))
        assertNull(written(1, null))
        assertNull(written(1, 0))
    }

    @Test
    fun `a search written before its start came back starts at its renewal`() {
        val search =
            searchOf(
                memberId = "grandma",
                openedBy = "anna",
                renewedBy = "anna",
                startedAt = null,
                renewedAt = now,
                hours = 6,
            )!!

        assertEquals(now, search.startedAt)
    }

    @Test
    fun `only the searches still standing are kept`() {
        val searches = mapOf("grandma" to written(2, 24)!!, "uncle" to written(7, 6)!!)

        assertEquals(setOf("grandma"), standingSearches(searches, now).keys)
    }

    @Test
    fun `a search that has run out marks nobody`() {
        val searches = mapOf("grandma" to written(7, 6)!!)

        assertEquals(emptySet<String>(), soughtMembers(searches, now))
    }

    @Test
    fun `the next moment to look again is the nearest end`() {
        val searches =
            mapOf(
                "grandma" to written(2, 24)!!,
                "uncle" to written(1, 6)!!,
                "gone" to written(9, 6)!!,
            )

        assertEquals(now.plus(Duration.ofHours(5)), nextSearchEnd(searches, now))
        assertNull(nextSearchEnd(emptyMap(), now))
    }

    @Test
    fun `a choice of hours is an end counted from now`() {
        assertEquals(now.plus(Duration.ofHours(72)), endIfChosen(72, now))
    }

    @Test
    fun `the default and the choices match what the page offers`() {
        assertEquals(listOf(6, 24, 72), SEARCH_HOURS)
        assertEquals(24, DEFAULT_SEARCH_HOURS)
        assertTrue(DEFAULT_SEARCH_HOURS in SEARCH_HOURS)
    }
}
