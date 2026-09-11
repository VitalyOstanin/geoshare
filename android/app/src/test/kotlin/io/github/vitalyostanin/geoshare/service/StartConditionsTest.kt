package io.github.vitalyostanin.geoshare.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StartConditionsTest {
    private fun conditions(
        signedIn: Boolean = true,
        enabled: Boolean = true,
        locationGranted: Boolean = true,
        backgroundLocationGranted: Boolean = true,
        fromBackground: Boolean = false,
    ) = StartConditions(
        signedIn = signedIn,
        enabled = enabled,
        locationGranted = locationGranted,
        backgroundLocationGranted = backgroundLocationGranted,
        fromBackground = fromBackground,
    )

    @Test
    fun `everything in place means the service may run`() {
        assertNull(refusalToStart(conditions()))
    }

    @Test
    fun `without a session there is nobody to share as`() {
        assertEquals(StartRefusal.NotSignedIn, refusalToStart(conditions(signedIn = false)))
    }

    @Test
    fun `the switch outranks the permissions`() {
        assertEquals(
            StartRefusal.TurnedOff,
            refusalToStart(conditions(enabled = false, locationGranted = false)),
        )
    }

    @Test
    fun `without the location permission there is nothing to share`() {
        assertEquals(
            StartRefusal.NoLocationPermission,
            refusalToStart(conditions(locationGranted = false)),
        )
    }

    @Test
    fun `a start from the background needs the background permission`() {
        assertEquals(
            StartRefusal.NoBackgroundLocationPermission,
            refusalToStart(conditions(backgroundLocationGranted = false, fromBackground = true)),
        )
    }

    @Test
    fun `a start from a screen does not need the background permission`() {
        assertNull(
            refusalToStart(conditions(backgroundLocationGranted = false, fromBackground = false)),
        )
    }

    @Test
    fun `a missing session is reported before a missing permission`() {
        assertEquals(
            StartRefusal.NotSignedIn,
            refusalToStart(conditions(signedIn = false, locationGranted = false)),
        )
    }
}
