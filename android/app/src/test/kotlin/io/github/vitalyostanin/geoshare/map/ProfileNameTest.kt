package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileNameTest {
    @Test
    fun `a member goes by the name of their account`() {
        assertEquals("Anna Petrova", profileName("  Anna Petrova ", "anna.petrova@example.com"))
    }

    // A fresh account can come without a name, and the group would otherwise
    // see the start of an id where an administrator invited an address.
    @Test
    fun `an account without a name goes by its shortened address`() {
        assertEquals("test…@example.com", profileName(null, "tester@example.com"))
        assertEquals("test…@example.com", profileName("   ", "tester@example.com"))
    }

    @Test
    fun `an account with neither has nothing to say`() {
        assertNull(profileName(null, null))
        assertNull(profileName("", " "))
    }
}
