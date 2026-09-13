package io.github.vitalyostanin.geoshare.permission

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MovementPermissionTest {
    @Test
    fun `asked once while sharing is on`() {
        assertTrue(movementPermissionWanted(sharing = true, granted = false, askedBefore = false))
    }

    @Test
    fun `a member who is not sharing is not interrupted`() {
        assertFalse(movementPermissionWanted(sharing = false, granted = false, askedBefore = false))
    }

    @Test
    fun `nothing is asked once it is granted`() {
        assertFalse(movementPermissionWanted(sharing = true, granted = true, askedBefore = false))
    }

    @Test
    fun `a refusal is not asked about again`() {
        assertFalse(movementPermissionWanted(sharing = true, granted = false, askedBefore = true))
    }
}
