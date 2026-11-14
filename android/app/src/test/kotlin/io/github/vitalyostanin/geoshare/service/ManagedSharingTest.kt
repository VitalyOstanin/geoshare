package io.github.vitalyostanin.geoshare.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// DEV-15: following the device policy controller of a managed device (ADR-0043).
class ManagedSharingTest {
    @Test
    fun `without a managed answer the member's own stands`() {
        assertTrue(sharingWanted(null, member = true))
        assertFalse(sharingWanted(null, member = false))
    }

    @Test
    fun `a managed answer outranks the member's either way`() {
        assertTrue(sharingWanted(true, member = false))
        assertFalse(sharingWanted(false, member = true))
    }

    @Test
    fun `sharing switched on by the configuration starts the service when nothing refuses`() {
        assertEquals(ManagedSharingStep.Start, managedSharingStep(true, refusal = null))
    }

    @Test
    fun `sharing switched on by the configuration waits for what refuses the start`() {
        assertEquals(
            ManagedSharingStep.Nothing,
            managedSharingStep(true, refusal = StartRefusal.NoBackgroundLocationPermission),
        )
    }

    @Test
    fun `sharing switched off by the configuration stops the service`() {
        assertEquals(ManagedSharingStep.Stop, managedSharingStep(false, refusal = null))
    }

    @Test
    fun `a configuration silent about sharing leaves the service alone`() {
        assertEquals(ManagedSharingStep.Nothing, managedSharingStep(null, refusal = null))
    }
}
