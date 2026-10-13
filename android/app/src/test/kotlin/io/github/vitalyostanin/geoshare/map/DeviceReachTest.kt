package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceReachTest {
    @Test
    fun `an administrator is offered another member's device`() {
        assertTrue(offersReach("oona.d1", "oona", "me.d0", "me", runsTheGroup = true))
    }

    @Test
    fun `a member who does not run the group is not offered another member's device`() {
        assertFalse(offersReach("oona.d1", "oona", "me.d0", "me", runsTheGroup = false))
    }

    @Test
    fun `any other device of one's own is offered whether or not one runs the group`() {
        assertTrue(offersReach("me.d1", "me", "me.d0", "me", runsTheGroup = false))
    }

    @Test
    fun `the device the application is open on is never offered`() {
        assertFalse(offersReach("me.d0", "me", "me.d0", "me", runsTheGroup = true))
    }
}
