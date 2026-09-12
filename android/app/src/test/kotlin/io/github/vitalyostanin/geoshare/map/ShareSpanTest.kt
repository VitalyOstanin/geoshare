package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/**
 * The two directions a permission is read in: a choice turned into a moment
 * to write, and a moment read back into the choice a screen shows.
 */
class ShareSpanTest {
    private val now: Instant = Instant.parse("2026-09-12T10:00:00Z")

    @Test
    fun `a span becomes the moment it runs out`() {
        assertEquals(now.plus(Duration.ofHours(1)), shareUntil(ShareSpan.Hour, now))
        assertEquals(now.plus(Duration.ofHours(8)), shareUntil(ShareSpan.EightHours, now))
        assertEquals(now.plus(Duration.ofDays(1)), shareUntil(ShareSpan.Day, now))
    }

    @Test
    fun `sharing with nobody has no moment to write`() {
        assertNull(shareUntil(ShareSpan.None, now))
    }

    // The rules compare a timestamp, so "without an end" is a date far enough
    // away that nothing this application outlives reaches it.
    @Test
    fun `a permission without an end is a date beyond any use`() {
        val until = shareUntil(ShareSpan.Forever, now)

        assertEquals(FOREVER, until)
        assertTrue(until!!.isAfter(now.plus(Duration.ofDays(365 * 100))))
    }

    @Test
    fun `a permission without an end reads back as one`() {
        assertEquals(ShareSpan.Forever, spanOf(FOREVER, now))
    }

    // The same thing to the reader either way: the marker is gone, and the
    // screen must not offer to end what has already ended.
    @Test
    fun `a permission that has run out reads as none`() {
        assertEquals(ShareSpan.None, spanOf(now.minus(Duration.ofMinutes(1)), now))
        assertEquals(ShareSpan.None, spanOf(null, now))
    }

    // Given hours ago and counting down since: no choice describes it, and the
    // screen says the time it ends rather than naming a span.
    @Test
    fun `a permission part way through answers to no choice`() {
        assertNull(spanOf(now.plus(Duration.ofMinutes(20)), now))
    }
}
