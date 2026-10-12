package io.github.vitalyostanin.geoshare.service

import android.location.Location
import android.os.Bundle
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class FixesTest {
    private val takenAt = Instant.parse("2026-10-12T19:30:00Z")

    /** A location that says nothing beyond its coordinates unless told to. */
    private fun location(extras: Bundle? = null): Location = mockk(relaxed = true) {
        every { latitude } returns 59.93
        every { longitude } returns 30.31
        every { time } returns takenAt.toEpochMilli()
        every { provider } returns "fused"
        every { elapsedRealtimeNanos } returns 5_000_000_000
        every { this@mockk.extras } returns extras
    }

    @Test
    fun `the coordinates, the moment and the clock since boot come through`() {
        val fix = fixOf(location())

        assertEquals(59.93, fix.latitude, 0.0)
        assertEquals(30.31, fix.longitude, 0.0)
        assertEquals(takenAt, fix.takenAt)
        assertEquals("fused", fix.from)
        assertEquals(Duration.ofSeconds(5), fix.sinceBoot)
    }

    @Test
    fun `a field the provider did not give is absent rather than zero`() {
        val fix = fixOf(location())
        val reported = fix.reported!!

        assertNull(fix.accuracyMetres)
        assertNull(reported.altitudeMetres)
        assertNull(reported.speedMps)
        assertNull(reported.bearingDegrees)
        assertNull(reported.verticalAccuracyMetres)
        assertNull(reported.alongside)
    }

    @Test
    fun `a field the provider gave is carried`() {
        val given = location()

        every { given.hasAccuracy() } returns true
        every { given.accuracy } returns 12f
        every { given.hasSpeed() } returns true
        every { given.speed } returns 1.5f

        val fix = fixOf(given)

        assertEquals(12.0, fix.accuracyMetres!!, 0.0)
        assertEquals(1.5, fix.reported!!.speedMps!!, 0.0)
    }

    @Test
    fun `a position from a mock provider is said to be pretended`() {
        val given = location()

        @Suppress("DEPRECATION")
        every { given.isFromMockProvider } returns true
        every { given.isMock } returns true

        assertTrue(fixOf(given).reported!!.pretended)
    }

    @Test
    fun `what the provider attached is written out in the order of its keys`() {
        val extras = mockk<Bundle>()

        every { extras.keySet() } returns setOf("satellites", "meanCn0")
        @Suppress("DEPRECATION")
        every { extras.get("satellites") } returns 9
        @Suppress("DEPRECATION")
        every { extras.get("meanCn0") } returns 31.5

        assertEquals("meanCn0=31.5|satellites=9", fixOf(location(extras)).reported!!.alongside)
    }
}
