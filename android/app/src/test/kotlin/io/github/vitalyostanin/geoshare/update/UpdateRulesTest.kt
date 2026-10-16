package io.github.vitalyostanin.geoshare.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

private val NOW = Instant.parse("2026-10-17T12:00:00Z")

class UpdateRulesTest {
    @Test
    fun `versions are compared by number, not as text`() {
        assertTrue(isNewerVersion(found = "0.1.10", current = "0.1.9"))
        assertTrue(isNewerVersion(found = "1.0.0", current = "0.9.9"))
        assertFalse(isNewerVersion(found = "0.1.9", current = "0.1.10"))
    }

    @Test
    fun `the same version is not newer`() {
        assertFalse(isNewerVersion(found = "0.1.1", current = "0.1.1"))
    }

    @Test
    fun `a build suffix on the current version is ignored`() {
        assertTrue(isNewerVersion(found = "0.1.2", current = "0.1.1-debug"))
        assertFalse(isNewerVersion(found = "0.1.1", current = "0.1.1-debug"))
    }

    @Test
    fun `a version that does not parse is never newer`() {
        assertFalse(isNewerVersion(found = "latest", current = "0.1.1"))
        assertFalse(isNewerVersion(found = "0.2.0", current = "dev"))
        assertFalse(isNewerVersion(found = "1.2", current = "1.1.0"))
    }

    @Test
    fun `a found version is announced once`() {
        assertTrue(shouldNotify(lastNotified = null, found = "0.1.2", current = "0.1.1"))
        assertFalse(shouldNotify(lastNotified = "0.1.2", found = "0.1.2", current = "0.1.1"))
        assertTrue(shouldNotify(lastNotified = "0.1.2", found = "0.1.3", current = "0.1.1"))
    }

    @Test
    fun `nothing is announced when the found version is not newer`() {
        assertFalse(shouldNotify(lastNotified = null, found = "0.1.1", current = "0.1.1"))
        assertFalse(shouldNotify(lastNotified = null, found = "0.1.0", current = "0.1.1"))
    }

    @Test
    fun `a check is due when none was made or a day has passed`() {
        assertTrue(isCheckDue(lastCheck = null, now = NOW))
        assertTrue(isCheckDue(lastCheck = NOW.minus(Duration.ofHours(24)), now = NOW))
        assertFalse(isCheckDue(lastCheck = NOW.minus(Duration.ofHours(23)), now = NOW))
    }

    @Test
    fun `a check stamped in the future is due, not blocked until the clock catches up`() {
        assertTrue(isCheckDue(lastCheck = NOW.plus(Duration.ofDays(3)), now = NOW))
        assertEquals(Duration.ofHours(24), CHECK_INTERVAL)
    }
}
