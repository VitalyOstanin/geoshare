package io.github.vitalyostanin.geoshare.managed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// DEV-15: following the device policy controller of a managed device (ADR-0043).
class TrustedControllersTest {
    private val digest = "ab".repeat(32)
    private val list = listOf(TrustedController("org.example.control", digest))

    @Test
    fun `the listed package with its certificate is trusted`() {
        assertTrue(trusts(list, "org.example.control", digest))
    }

    @Test
    fun `the certificate may be given in the colon form`() {
        assertTrue(trusts(list, "org.example.control", "AB:".repeat(31) + "AB"))
    }

    @Test
    fun `another package with the same certificate is not trusted`() {
        assertFalse(trusts(list, "org.example.other", digest))
    }

    @Test
    fun `the listed package with another certificate is not trusted`() {
        assertFalse(trusts(list, "org.example.control", "cd".repeat(32)))
    }

    @Test
    fun `a malformed certificate is never trusted`() {
        assertFalse(trusts(list, "org.example.control", "abab"))
        assertFalse(trusts(list, "org.example.control", ""))
    }

    @Test
    fun `nothing is trusted when the list is empty`() {
        assertFalse(trusts(emptyList(), "org.example.control", digest))
    }
}
