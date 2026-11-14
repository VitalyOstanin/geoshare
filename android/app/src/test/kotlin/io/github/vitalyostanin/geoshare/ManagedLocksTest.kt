package io.github.vitalyostanin.geoshare

import io.github.vitalyostanin.geoshare.managed.ManagedSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// DEV-15: following the device policy controller of a managed device (ADR-0043).
class ManagedLocksTest {
    @Test
    fun `without a configuration nothing is locked`() {
        assertTrue(canStopSharing(ManagedSettings.NONE))
        assertTrue(canLeaveGroup(ManagedSettings.NONE))
        assertTrue(canChangeAccount(ManagedSettings.NONE))
    }

    @Test
    fun `lock_sharing takes away only stopping sharing`() {
        val locked = ManagedSettings(lockSharing = true)

        assertFalse(canStopSharing(locked))
        assertTrue(canLeaveGroup(locked))
        assertTrue(canChangeAccount(locked))
    }

    @Test
    fun `lock_group takes away only leaving a group`() {
        val locked = ManagedSettings(lockGroup = true)

        assertTrue(canStopSharing(locked))
        assertFalse(canLeaveGroup(locked))
        assertTrue(canChangeAccount(locked))
    }

    @Test
    fun `lock_account takes away only signing out and changing the account`() {
        val locked = ManagedSettings(lockAccount = true)

        assertTrue(canStopSharing(locked))
        assertTrue(canLeaveGroup(locked))
        assertFalse(canChangeAccount(locked))
    }
}
