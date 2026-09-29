package io.github.vitalyostanin.geoshare.map

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** The same expectations the page's own `link.test.mjs` checks. */
class DatabaseLinkTest {
    private val now: Instant = Instant.parse("2026-09-29T22:24:00Z")

    @Test
    fun aDatabaseThatNeverAnswersIsToldOfAfterTheGrace() {
        val link = DatabaseLink.from(now)

        assertFalse(link.lost(now + LINK_GRACE - Duration.ofMillis(1)))
        assertTrue(link.lost(now + LINK_GRACE))
        assertNull(link.confirmedAt)
    }

    @Test
    fun theCacheCountsFromTheFirstCachedSnapshotAndKeepsTheLastConfirmation() {
        val again =
            DatabaseLink
                .from(now)
                .next(fromCache = false, now = now.plusSeconds(1))
                .next(fromCache = true, now = now.plusSeconds(5))
                .next(fromCache = true, now = now.plusSeconds(9))

        assertEquals(
            DatabaseLink(confirmedAt = now.plusSeconds(1), cachedSince = now.plusSeconds(5)),
            again,
        )
        assertEquals(now.plusSeconds(5) + LINK_GRACE, again.lostAt())
    }

    @Test
    fun aConfirmationEndsTheSilenceAtOnce() {
        val back = DatabaseLink.from(
            now,
        ).next(fromCache = true, now = now).next(fromCache = false, now = now.plusSeconds(60))

        assertFalse(back.lost(now.plusSeconds(120)))
        assertNull(back.lostAt())
    }

    @Test
    fun aWriteTheServerAnswersInTimeIsItsAnswer() = runBlocking {
        assertEquals("done", withinAnswerWait(Duration.ofSeconds(1)) { "done" })
    }

    @Test(expected = UnansweredException::class)
    fun aWriteLeftUnansweredIsRefusedAsUnanswered() = runBlocking {
        withinAnswerWait(Duration.ofMillis(10)) { delay(1_000) }
    }

    @Test(expected = IllegalStateException::class)
    fun aWriteRefusedByTheDatabaseKeepsItsOwnRefusal() = runBlocking {
        withinAnswerWait<Unit>(Duration.ofSeconds(1)) { error("no") }
    }
}
