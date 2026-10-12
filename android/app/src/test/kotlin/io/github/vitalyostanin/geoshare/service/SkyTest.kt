package io.github.vitalyostanin.geoshare.service

import android.location.GnssStatus
import io.github.vitalyostanin.geoshare.collection.Constellation
import io.github.vitalyostanin.geoshare.collection.Satellite
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class SkyTest {
    @Test
    fun `every satellite named is kept, the silent ones too`() {
        val status =
            mockk<GnssStatus>(relaxed = true) {
                every { satelliteCount } returns 2
                every { getConstellationType(0) } returns GnssStatus.CONSTELLATION_GALILEO
                every { getSvid(0) } returns 11
                every { getCn0DbHz(0) } returns 34.5f
                every { getElevationDegrees(0) } returns 40f
                every { getAzimuthDegrees(0) } returns 180f
                every { hasCarrierFrequencyHz(0) } returns true
                every { getCarrierFrequencyHz(0) } returns 1_500_000_000f
                every { usedInFix(0) } returns true
                every { hasEphemerisData(0) } returns true
                every { getConstellationType(1) } returns 99
                every { getSvid(1) } returns 3
            }

        assertEquals(
            listOf(
                Satellite(
                    constellation = Constellation.Galileo,
                    id = 11,
                    loudness = 34.5,
                    basebandLoudness = null,
                    elevationDegrees = 40.0,
                    azimuthDegrees = 180.0,
                    carrierHz = 1_500_000_000.0,
                    usedInFix = true,
                    hasEphemeris = true,
                    hasAlmanac = false,
                ),
                Satellite(
                    constellation = Constellation.Other,
                    id = 3,
                    loudness = 0.0,
                    basebandLoudness = null,
                    elevationDegrees = 0.0,
                    azimuthDegrees = 0.0,
                    carrierHz = null,
                    usedInFix = false,
                    hasEphemeris = false,
                    hasAlmanac = false,
                ),
            ),
            skyOf(status),
        )
    }
}
