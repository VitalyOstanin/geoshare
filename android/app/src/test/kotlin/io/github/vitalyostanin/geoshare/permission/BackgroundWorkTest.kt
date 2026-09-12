package io.github.vitalyostanin.geoshare.permission

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundWorkTest {
    @Test
    fun `a restricted phone that shares is asked`() {
        assertTrue(
            batteryExemptionWanted(sharing = true, unrestricted = false, askedBefore = false),
        )
    }

    @Test
    fun `a phone that already lets us run is not asked`() {
        assertFalse(
            batteryExemptionWanted(sharing = true, unrestricted = true, askedBefore = false),
        )
    }

    @Test
    fun `the request is made once and not again`() {
        assertFalse(
            batteryExemptionWanted(sharing = true, unrestricted = false, askedBefore = true),
        )
    }

    @Test
    fun `a member who shares nothing is left alone`() {
        assertFalse(
            batteryExemptionWanted(sharing = false, unrestricted = false, askedBefore = false),
        )
    }
}
