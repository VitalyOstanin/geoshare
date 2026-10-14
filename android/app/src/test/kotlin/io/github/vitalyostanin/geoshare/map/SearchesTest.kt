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

    private fun written(
        phoneId: String = "grandma.oldphone",
        renewedHoursAgo: Long,
        hours: Int?,
        phoneName: String? = null,
    ): Search? = searchOf(
        phoneId = phoneId,
        phoneName = phoneName,
        openedBy = "anna",
        renewedBy = "boris",
        startedAt = now.minus(Duration.ofHours(30)),
        renewedAt = now.minus(Duration.ofHours(renewedHoursAgo)),
        hours = hours,
    )

    @Test
    fun `a search ends its hours after the last renewal`() {
        val search = written(renewedHoursAgo = 2, hours = 24)!!

        assertEquals(now.plus(Duration.ofHours(22)), search.endsAt)
        assertEquals("grandma.oldphone", search.phoneId)
        assertEquals("grandma", search.memberId)
        assertEquals("anna", search.openedBy)
        assertEquals("boris", search.renewedBy)
        assertEquals(now.minus(Duration.ofHours(30)), search.startedAt)
    }

    @Test
    fun `the device's name travels along with the search, when there is one`() {
        val named = written(renewedHoursAgo = 2, hours = 24, phoneName = "Old phone")!!
        val unnamed = written(renewedHoursAgo = 2, hours = 24)!!

        assertEquals("Old phone", named.phoneName)
        assertNull(unnamed.phoneName)
    }

    @Test
    fun `a document that is not a search is none`() {
        assertNull(searchOf("grandma.oldphone", null, null, "boris", now, now, 6))
        assertNull(written(renewedHoursAgo = 1, hours = null))
        assertNull(written(renewedHoursAgo = 1, hours = 0))
    }

    @Test
    fun `an id with no dot names no device, and is dropped like any other bad document`() {
        assertNull(
            searchOf(
                phoneId = "grandma",
                phoneName = null,
                openedBy = "anna",
                renewedBy = "anna",
                startedAt = now,
                renewedAt = now,
                hours = 6,
            ),
        )
    }

    @Test
    fun `a search written before its start came back starts at its renewal`() {
        val search =
            searchOf(
                phoneId = "grandma.oldphone",
                phoneName = null,
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
        val searches =
            mapOf(
                "grandma.oldphone" to written(renewedHoursAgo = 2, hours = 24)!!,
                "uncle.phone" to written("uncle.phone", renewedHoursAgo = 7, hours = 6)!!,
            )

        assertEquals(setOf("grandma.oldphone"), standingSearches(searches, now).keys)
    }

    @Test
    fun `two devices of one member may stand at once`() {
        val searches =
            mapOf(
                "grandma.oldphone" to
                    written("grandma.oldphone", renewedHoursAgo = 2, hours = 24)!!,
                "grandma.newphone" to
                    written("grandma.newphone", renewedHoursAgo = 1, hours = 6)!!,
            )

        assertEquals(
            setOf("grandma.oldphone", "grandma.newphone"),
            soughtPhones(searches, now),
        )
    }

    @Test
    fun `a search that has run out marks no device`() {
        val searches = mapOf("grandma.oldphone" to written(renewedHoursAgo = 7, hours = 6)!!)

        assertEquals(emptySet<String>(), soughtPhones(searches, now))
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
