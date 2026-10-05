package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class ShareBackTest {
    private val now = Instant.parse("2026-10-05T08:00:00Z")

    @Test
    fun `a member given nothing is offered it`() {
        assertTrue(offersShareBack("oona", "me", emptyMap(), now))
    }

    @Test
    fun `a member whose permission has run out is offered it again`() {
        val ended = mapOf("oona" to now.minus(Duration.ofMinutes(1)))

        assertTrue(offersShareBack("oona", "me", ended, now))
    }

    @Test
    fun `a member with a permission standing is not offered it`() {
        val standing = mapOf("oona" to now.plus(Duration.ofHours(1)))

        assertFalse(offersShareBack("oona", "me", standing, now))
    }

    @Test
    fun `nobody is offered to show themselves their own place`() {
        assertFalse(offersShareBack("me", "me", emptyMap(), now))
    }
}
