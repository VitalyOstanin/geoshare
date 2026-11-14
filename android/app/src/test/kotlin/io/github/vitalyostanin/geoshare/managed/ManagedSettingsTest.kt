package io.github.vitalyostanin.geoshare.managed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// DEV-15: following the device policy controller of a managed device (ADR-0043).
class ManagedSettingsTest {
    private val digest = "ab".repeat(32)

    private fun entry(name: Any?, cert: Any?) = mapOf("package_name" to name, "cert_sha256" to cert)

    @Test
    fun `an empty configuration is NONE`() {
        assertEquals(ManagedSettings.NONE, ManagedSettings.parse(emptyMap()))
        assertNull(ManagedSettings.NONE.sharingEnabled)
        assertFalse(ManagedSettings.NONE.lockGroup)
    }

    @Test
    fun `a full configuration is read as given`() {
        val settings = ManagedSettings.parse(
            mapOf(
                "trusted_controllers" to listOf(entry("org.example.control", digest)),
                "connect_link" to "https://example.web.app/join",
                "expected_account" to "someone@example.org",
                "sharing_enabled" to true,
                "lock_sharing" to true,
                "lock_group" to true,
                "lock_account" to true,
            ),
        )
        assertEquals(
            ManagedSettings(
                trustedControllers = listOf(TrustedController("org.example.control", digest)),
                connectLink = "https://example.web.app/join",
                expectedAccount = "someone@example.org",
                sharingEnabled = true,
                lockSharing = true,
                lockGroup = true,
                lockAccount = true,
            ),
            settings,
        )
    }

    @Test
    fun `sharing switched off is not the same as sharing not set`() {
        assertEquals(false, ManagedSettings.parse(mapOf("sharing_enabled" to false)).sharingEnabled)
    }

    @Test
    fun `an entry with a malformed digest is dropped and the others stay`() {
        val settings = ManagedSettings.parse(
            mapOf(
                "trusted_controllers" to listOf(
                    entry("org.example.one", "not-a-digest"),
                    entry("org.example.two", "ab".repeat(31)),
                    entry("org.example.three", digest),
                ),
            ),
        )
        assertEquals(
            listOf(TrustedController("org.example.three", digest)),
            settings.trustedControllers,
        )
    }

    @Test
    fun `blank and invalid package names are dropped`() {
        val settings = ManagedSettings.parse(
            mapOf(
                "trusted_controllers" to listOf(
                    entry("  ", digest),
                    entry(null, digest),
                    entry("nodots", digest),
                    entry("org example.control", digest),
                    entry("org.example.control|x", digest),
                ),
            ),
        )
        assertTrue(settings.trustedControllers.isEmpty())
    }

    @Test
    fun `a digest written with colons and capitals is normalised`() {
        val written = "AB:".repeat(31) + "AB"
        val settings = ManagedSettings.parse(
            mapOf("trusted_controllers" to listOf(entry("org.example.control", written))),
        )
        assertEquals(
            listOf(TrustedController("org.example.control", digest)),
            settings.trustedControllers,
        )
    }

    @Test
    fun `a digest written with spaces is normalised`() {
        assertEquals(digest, normaliseCertSha256(" " + "ab ".repeat(31) + "ab "))
    }

    @Test
    fun `blank strings are not set`() {
        val settings = ManagedSettings.parse(
            mapOf("connect_link" to "   ", "expected_account" to ""),
        )
        assertNull(settings.connectLink)
        assertNull(settings.expectedAccount)
    }

    @Test
    fun `values of the wrong type are ignored`() {
        val settings = ManagedSettings.parse(
            mapOf("sharing_enabled" to "true", "lock_group" to 1, "trusted_controllers" to "x"),
        )
        assertEquals(ManagedSettings.NONE, settings)
    }

    @Test
    fun `a position interval inside one to twenty is kept and anything else is not set`() {
        fun read(value: Any?) = ManagedSettings.parse(
            mapOf("position_interval_minutes" to value),
        ).positionIntervalMinutes
        assertNull(ManagedSettings.parse(emptyMap()).positionIntervalMinutes)
        assertEquals(5, read(5))
        assertEquals(1, read(1))
        assertEquals(20, read(20))
        assertNull(read(0))
        assertNull(read(21))
        assertNull(read("5"))
    }
}
