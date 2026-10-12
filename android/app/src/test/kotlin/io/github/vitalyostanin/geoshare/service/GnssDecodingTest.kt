package io.github.vitalyostanin.geoshare.service

import android.location.GnssClock
import android.location.GnssMeasurement
import android.location.GnssMeasurementsEvent
import android.location.GnssNavigationMessage
import android.location.GnssStatus
import android.location.LocationManager
import io.github.vitalyostanin.geoshare.collection.Constellation
import io.github.vitalyostanin.geoshare.collection.Gain
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// The stub platform the JVM tests run on reports Android version 0, so what is
// held here is the reading for the versions before each newer field arrived:
// the branches for the newer ones need a device or an emulator.
class GnssDecodingTest {
    private fun measurement(
        svid: Int,
        gainDb: Double? = null,
        carrierHz: Float? = null,
    ): GnssMeasurement = mockk(relaxed = true) {
        every { constellationType } returns GnssStatus.CONSTELLATION_GPS
        every { this@mockk.svid } returns svid
        every { cn0DbHz } returns 30.0
        every { hasAutomaticGainControlLevelDb() } returns (gainDb != null)
        every { automaticGainControlLevelDb } returns (gainDb ?: 0.0)
        every { hasCarrierFrequencyHz() } returns (carrierHz != null)
        every { carrierFrequencyHz } returns (carrierHz ?: 0f)
    }

    @Test
    fun `a gain is read off the measurements that carry one, before Android 13`() {
        val event =
            mockk<GnssMeasurementsEvent> {
                every { measurements } returns
                    listOf(
                        measurement(svid = 1, gainDb = -3.5, carrierHz = 1_500_000_000f),
                        measurement(svid = 2),
                        measurement(svid = 3, gainDb = 1.0),
                    )
            }

        assertEquals(
            listOf(
                Gain(Constellation.Gps, carrierHz = 1_500_000_000.0, levelDb = -3.5),
                Gain(Constellation.Gps, carrierHz = null, levelDb = 1.0),
            ),
            gainsOf(event),
        )
    }

    @Test
    fun `a clock that reports nothing optional still reports its discontinuities`() {
        val clock =
            mockk<GnssClock>(relaxed = true) {
                every { timeNanos } returns 42L
                every { hardwareClockDiscontinuityCount } returns 7
                every { hasLeapSecond() } returns true
                every { leapSecond } returns 18
            }
        val event = mockk<GnssMeasurementsEvent> { every { this@mockk.clock } returns clock }

        val read = clockOf(event)

        assertEquals(42L, read.timeNanos)
        assertEquals(7, read.discontinuities)
        assertEquals(18, read.leapSecond)
        assertNull(read.fullBiasNanos)
        assertNull(read.driftNanosPerSecond)
        assertNull(read.sinceBootNanos)
    }

    @Test
    fun `a measurement leaves out what the receiver did not report`() {
        val read = measurementOf(measurement(svid = 5), fullTracking = null)

        assertEquals(Constellation.Gps, read.constellation)
        assertEquals(5, read.id)
        assertEquals(30.0, read.loudness, 0.0)
        assertNull(read.carrierHz)
        assertNull(read.gainDb)
        assertNull(read.basebandLoudness)
        assertNull(read.codeType)
        assertNull(read.fullInterSignalBiasNanos)
    }

    @Test
    fun `full tracking has no answer before Android 14`() {
        assertNull(fullTrackingOf(mockk(relaxed = true)))
    }

    @Test
    fun `a navigation message keeps its system, its satellite and its bytes`() {
        val message =
            mockk<GnssNavigationMessage>(relaxed = true) {
                every { type } returns GnssNavigationMessage.TYPE_GAL_F
                every { svid } returns 11
                every { data } returns byteArrayOf(0x0A, 0x7F, -1)
            }

        val read = navMessageOf(message)

        assertEquals(Constellation.Galileo, read.constellation)
        assertEquals(11, read.id)
        assertEquals("0A7FFF", read.data)
    }

    @Test
    fun `the chipset is not asked before Android 12`() {
        assertEquals(
            "this version of Android does not ask the chipset what it can hand over",
            whatTheChipsetSays(mockk<LocationManager>()),
        )
    }
}
