package io.github.vitalyostanin.geoshare.permission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PermissionStepTest {
    private fun state(
        locationGranted: Boolean = true,
        notificationsGranted: Boolean = true,
        backgroundLocationGranted: Boolean = true,
    ) = PermissionState(
        locationGranted = locationGranted,
        notificationsGranted = notificationsGranted,
        backgroundLocationGranted = backgroundLocationGranted,
    )

    @Test
    fun `nothing is asked when everything is granted`() {
        assertNull(nextPermissionStep(state()))
    }

    @Test
    fun `the position comes first`() {
        assertEquals(
            PermissionStep.Location,
            nextPermissionStep(
                state(
                    locationGranted = false,
                    notificationsGranted = false,
                    backgroundLocationGranted = false,
                ),
            ),
        )
    }

    @Test
    fun `the notification is asked for once the position is allowed`() {
        assertEquals(
            PermissionStep.Notifications,
            nextPermissionStep(
                state(notificationsGranted = false, backgroundLocationGranted = false),
            ),
        )
    }

    @Test
    fun `the background position is asked for last`() {
        assertEquals(
            PermissionStep.BackgroundLocation,
            nextPermissionStep(state(backgroundLocationGranted = false)),
        )
    }
}
