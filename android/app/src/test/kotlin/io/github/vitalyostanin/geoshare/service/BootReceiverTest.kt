package io.github.vitalyostanin.geoshare.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BootReceiverTest {
    @Test
    fun `a phone that has just restarted brings the service back`() {
        assertEquals("boot", whatStoppedIt("android.intent.action.BOOT_COMPLETED"))
    }

    // The case that cost two phones several hours of silence: installing a
    // build stops every process of the package, and nothing was listening.
    @Test
    fun `an application that has just been updated brings the service back`() {
        assertEquals("an update", whatStoppedIt("android.intent.action.MY_PACKAGE_REPLACED"))
    }

    @Test
    fun `anything else that reaches the receiver starts nothing`() {
        assertNull(whatStoppedIt("android.intent.action.PACKAGE_REPLACED"))
        assertNull(whatStoppedIt("android.intent.action.BOOT_COMPLETED "))
        assertNull(whatStoppedIt(null))
        assertNull(whatStoppedIt(""))
    }
}
