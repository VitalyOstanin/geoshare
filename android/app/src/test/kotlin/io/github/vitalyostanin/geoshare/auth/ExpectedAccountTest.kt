package io.github.vitalyostanin.geoshare.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// DEV-15: following the device policy controller of a managed device (ADR-0043).
class ExpectedAccountTest {
    @Test
    fun `the same address in another case is not a difference`() {
        assertFalse(differsFromExpectedAccount("Ivan@Example.com", "ivan@example.com"))
    }

    @Test
    fun `another address is a difference`() {
        assertTrue(differsFromExpectedAccount("ivan@example.com", "anya@example.com"))
    }

    @Test
    fun `a longer address with the same start is a difference`() {
        assertTrue(differsFromExpectedAccount("ivan@example.com", "ivan@example.com.au"))
    }

    @Test
    fun `no expected account gives no verdict`() {
        assertFalse(differsFromExpectedAccount(null, "ivan@example.com"))
        assertFalse(differsFromExpectedAccount("  ", "ivan@example.com"))
    }

    @Test
    fun `no signed-in address gives no verdict`() {
        assertFalse(differsFromExpectedAccount("ivan@example.com", null))
    }
}
