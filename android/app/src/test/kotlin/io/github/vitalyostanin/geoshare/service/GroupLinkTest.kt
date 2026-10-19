package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.collection.Watcher
import io.github.vitalyostanin.geoshare.map.Search
import io.github.vitalyostanin.geoshare.project.GroupConnection
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** The moments a group's watching of this device runs out, from the claims, the grant and a search. */
class GroupLinkTest {
    private val owner = Watcher(MEMBER, Instant.parse("2026-10-13T10:00:00Z"))
    private val stranger = Watcher("member-2", Instant.parse("2026-10-13T10:05:00Z"))

    @Before
    fun listenToNothingReal() {
        mockkStatic(WATCH_SOURCE, PERMISSIONS, SEARCH_SOURCE)
        every { watchers(any(), any()) } returns flowOf(listOf(owner, stranger))
        every { ownSearch(any(), any()) } returns flowOf(null)
    }

    @After
    fun forgetTheSources() {
        unmockkStatic(WATCH_SOURCE, PERMISSIONS, SEARCH_SOURCE)
    }

    @Test
    fun `a viewer the grant leaves out is not counted`() = runBlocking {
        every { ownSharedWith(any(), any()) } returns flowOf(emptyMap())

        assertEquals(listOf(owner.until), link().watching().toList().last())
    }

    @Test
    fun `a viewer the grant names is counted no longer than the grant lasts`() = runBlocking {
        val grantEnds = Instant.parse("2026-10-13T10:02:00Z")
        every { ownSharedWith(any(), any()) } returns flowOf(mapOf(stranger.memberId to grantEnds))

        assertEquals(listOf(owner.until, grantEnds), link().watching().toList().last())
    }

    // A device that does not know yet whom it shares with answers everybody
    // rather than nobody for the second the subscription takes.
    @Test
    fun `until the grant arrives every viewer counts`() = runBlocking {
        every { ownSharedWith(any(), any()) } returns flow { awaitCancellation() }

        val watchEnds = withTimeout(WAIT) { link().watching().first { it.size == 2 } }

        assertEquals(listOf(owner.until, stranger.until), watchEnds)
    }

    // Nothing arrives from the database at the moment a search runs out, so
    // the link says so itself.
    @Test
    fun `a search counts every viewer until it runs out`() = runBlocking {
        every { ownSharedWith(any(), any()) } returns flowOf(emptyMap())
        every { ownSearch(any(), any()) } returns flowOf(searchEndingIn(Duration.ofMillis(300)))

        val said = withTimeout(WAIT) { link().watching().toList() }

        assertTrue(said.contains(listOf(owner.until, stranger.until)))
        assertEquals(listOf(owner.until), said.last())
    }

    private fun link(): GroupLink {
        val device = mockk<DeviceIdentity> { every { id } returns "device-1" }

        return GroupLink(mockk<GroupConnection>(relaxed = true), MEMBER, device)
    }

    private fun searchEndingIn(left: Duration): Search {
        val now = Instant.now()

        return Search(
            phoneId = "$MEMBER.device-1",
            memberId = MEMBER,
            phoneName = null,
            openedBy = "member-2",
            renewedBy = "member-2",
            startedAt = now,
            endsAt = now + left,
        )
    }

    private companion object {
        const val MEMBER = "member-1"
        const val WAIT = 5_000L
        const val WATCH_SOURCE = "io.github.vitalyostanin.geoshare.service.WatchSourceKt"
        const val PERMISSIONS = "io.github.vitalyostanin.geoshare.service.PermissionsKt"
        const val SEARCH_SOURCE = "io.github.vitalyostanin.geoshare.service.SearchSourceKt"
    }
}
