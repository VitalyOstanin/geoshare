package io.github.vitalyostanin.geoshare.control

import io.github.vitalyostanin.geoshare.managed.ManagedConfiguration
import io.github.vitalyostanin.geoshare.managed.ManagedSettings
import io.github.vitalyostanin.geoshare.managed.TrustedController
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.vitalyostanin.geoshare.control.SharingControlProtocol as Protocol

// DEV-15: following the device policy controller of a managed device (ADR-0043).
class ControlTrustTest {
    private val digest = "ab".repeat(32)
    private val other = "cd".repeat(32)
    private val controller = mapOf("org.example.control" to listOf(digest))
    private val configured = ManagedSettings(
        trustedControllers = listOf(TrustedController("org.example.control", digest)),
    )

    @After
    fun forget() {
        ManagedConfiguration.refreshWith { ManagedSettings.NONE }
    }

    @Test
    fun `a configuration set after the process started trusts the very next request`() {
        // The process started on a device not yet managed, and no broadcast followed.
        ManagedConfiguration.refreshWith { ManagedSettings.NONE }
        var onTheDevice = ManagedSettings.NONE

        assertEquals(
            Protocol.Trust.NoControllers,
            Protocol.trustOf(ManagedConfiguration.refreshWith { onTheDevice }, controller),
        )

        onTheDevice = configured

        assertEquals(
            Protocol.Trust.Trusted,
            Protocol.trustOf(ManagedConfiguration.refreshWith { onTheDevice }, controller),
        )
        assertEquals(configured, ManagedConfiguration.current)
    }

    @Test
    fun `a configuration that cannot be read refuses even when an older copy trusted the caller`() {
        ManagedConfiguration.refreshWith { configured }

        val read = ManagedConfiguration.refreshWith { throw IllegalStateException("gone") }

        assertNull(read)
        assertEquals(Protocol.Trust.Unreadable, Protocol.trustOf(read, controller))
        assertEquals(configured, ManagedConfiguration.current)
    }

    @Test
    fun `a refusal names why`() {
        assertEquals(
            Protocol.Trust.NoControllers,
            Protocol.trustOf(ManagedSettings.NONE, controller),
        )
        assertEquals(
            Protocol.Trust.PackageNotListed,
            Protocol.trustOf(configured, mapOf("org.example.other" to listOf(digest))),
        )
        assertEquals(
            Protocol.Trust.CertificateMismatch,
            Protocol.trustOf(configured, mapOf("org.example.control" to listOf(other))),
        )
    }

    @Test
    fun `the same refusal is written once a minute`() {
        var clock = 0L
        val log = RefusalLog(intervalMillis = 60_000, now = { clock })

        assertTrue(log.shouldWrite("10123 NoControllers"))
        clock = 59_999
        assertFalse(log.shouldWrite("10123 NoControllers"))
        assertTrue(log.shouldWrite("10123 CertificateMismatch"))
        clock = 60_000
        assertTrue(log.shouldWrite("10123 NoControllers"))
    }
}
