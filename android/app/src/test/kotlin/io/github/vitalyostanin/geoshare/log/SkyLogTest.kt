package io.github.vitalyostanin.geoshare.log

import io.github.vitalyostanin.geoshare.collection.Clock
import io.github.vitalyostanin.geoshare.collection.Constellation
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.Gain
import io.github.vitalyostanin.geoshare.collection.Measurement
import io.github.vitalyostanin.geoshare.collection.Reported
import io.github.vitalyostanin.geoshare.collection.Satellite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Duration
import java.time.Instant

class SkyLogTest {
    @get:Rule
    val folder = TemporaryFolder()

    // The file is read by a person with a spreadsheet, months after the epoch
    // it describes, and the head of the file is the only thing saying what the
    // columns are. A row with one field more than its header silently shifts
    // every column after the one that was added.
    @Test
    fun `every row carries as many fields as its header names`() {
        SkyLog.start(folder.newFolder("sky"))

        SkyLog.note(
            at = AT,
            fix = FIX,
            clock = CLOCK,
            gains = listOf(GAIN),
            sky = SKY + SATELLITE,
            measurements = listOf(MEASUREMENT),
        )

        val written = SkyLog.read()
        val headers =
            written.lineSequence()
                .filter { it.startsWith("# ") }
                .associate { line ->
                    val columns = line.removePrefix("# ").split(",")

                    columns.first() to columns.size
                }

        val rows =
            written.lineSequence()
                .filter { it.isNotBlank() && !it.startsWith("# ") }
                .toList()

        assertTrue("nothing was written", rows.isNotEmpty())

        rows.forEach { row ->
            val kind = row.substringBefore(",")
            val named = headers[kind]

            assertEquals("no header for $kind", true, named != null)
            assertEquals("the $kind row", named, row.split(",").size)
        }
    }

    @Test
    fun `the kinds of row a block is made of are all there`() {
        SkyLog.start(folder.newFolder("kinds"))

        SkyLog.note(
            at = AT,
            fix = FIX,
            clock = CLOCK,
            gains = listOf(GAIN),
            sky = SKY + SATELLITE,
            measurements = listOf(MEASUREMENT),
        )

        val kinds =
            SkyLog.read().lineSequence()
                .filter { it.isNotBlank() && !it.startsWith("# ") }
                .map { it.substringBefore(",") }
                .toSet()

        assertEquals(setOf("Fix", "Clock", "Agc", "Slant", "Sat", "Raw"), kinds)
    }

    // The one row of a block that is not a copy of what the receiver said: it
    // is worked out from the satellites below it, and written where the
    // reader of the file meets it beside the gain rather than having to group
    // the epoch first.
    @Test
    fun `a block with satellites enough carries what the sky was shaped like`() {
        SkyLog.start(folder.newFolder("slant"))

        SkyLog.note(
            at = AT,
            fix = FIX,
            clock = CLOCK,
            gains = listOf(GAIN),
            sky = SKY,
            measurements = listOf(MEASUREMENT),
        )

        val rows = SkyLog.read().lineSequence().filter { it.startsWith("Slant,") }.toList()

        assertEquals(1, rows.size)

        val fields = rows.first().split(",")

        assertEquals("GPS", fields[2])
        assertEquals("6", fields[3])
    }

    // Six of one system and two of another: the second is left out because
    // two satellites say nothing about the shape of a sky.
    @Test
    fun `a system heard a satellite or two at a time is left out of it`() {
        SkyLog.start(folder.newFolder("thin"))

        SkyLog.note(
            at = AT,
            mode = "Watching",
            silence = Duration.ofMinutes(4),
            clock = CLOCK,
            gains = listOf(GAIN),
            sky = SKY + SATELLITE,
            measurements = listOf(MEASUREMENT),
        )

        val systems =
            SkyLog.read().lineSequence()
                .filter { it.startsWith("Slant,") }
                .map { it.split(",")[2] }
                .toList()

        assertEquals(listOf("GPS"), systems)
    }

    private companion object {
        fun sat(id: Int, loudness: Double, elevationDegrees: Double) = Satellite(
            constellation = Constellation.Gps,
            id = id,
            loudness = loudness,
            basebandLoudness = null,
            elevationDegrees = elevationDegrees,
            azimuthDegrees = 120.0,
            carrierHz = 1_575_420_032.0,
            usedInFix = true,
            hasEphemeris = true,
            hasAlmanac = true,
        )

        val AT: Instant = Instant.parse("2026-09-14T19:00:00Z")

        val FIX =
            Fix(
                latitude = 61.03268,
                longitude = 4.40300,
                takenAt = AT,
                accuracyMetres = 12.5,
                from = "gps",
                reported = Reported(altitudeMetres = 30.0, alongside = "SourceType=0"),
            )

        val CLOCK =
            Clock(
                timeNanos = 498_097_714_000_000,
                leapSecond = 18,
                timeUncertaintyNanos = null,
                fullBiasNanos = -1_472_951_481_696_920_528,
                biasNanos = 0.996,
                biasUncertaintyNanos = 177.58,
                driftNanosPerSecond = -33.759,
                driftUncertaintyNanosPerSecond = 12.554,
                discontinuities = 441,
                sinceBootNanos = 615_998_576_335_667,
                sinceBootUncertaintyNanos = null,
            )

        val GAIN =
            Gain(
                constellation = Constellation.Gps,
                carrierHz = 1_575_420_000.0,
                levelDb = -62.85,
            )

        val SKY =
            listOf(
                sat(1, 33.9, 7.0),
                sat(2, 32.8, 19.0),
                sat(3, 34.3, 31.0),
                sat(4, 33.1, 48.0),
                sat(5, 33.8, 62.0),
                sat(6, 33.9, 76.0),
            )

        val SATELLITE =
            Satellite(
                constellation = Constellation.Qzss,
                id = 3,
                loudness = 26.6,
                basebandLoudness = 23.0,
                elevationDegrees = 10.5,
                azimuthDegrees = 356.1,
                carrierHz = 1_575_420_032.0,
                usedInFix = false,
                hasEphemeris = true,
                hasAlmanac = true,
            )

        val MEASUREMENT =
            Measurement(
                constellation = Constellation.Gps,
                id = 3,
                timeOffsetNanos = 0.0,
                state = 16_431,
                receivedSvTimeNanos = 498_097_714_000_000,
                receivedSvTimeUncertaintyNanos = 21,
                loudness = 26.6,
                basebandLoudness = 23.0,
                pseudorangeRateMps = 268.3,
                pseudorangeRateUncertaintyMps = 0.2,
                deltaRangeState = 0,
                deltaRangeMetres = 0.0,
                deltaRangeUncertaintyMetres = 0.0,
                carrierHz = 1_575_420_032.0,
                multipath = 0,
                gainDb = -62.85,
                snrDb = 0.0,
                codeType = "C",
                fullInterSignalBiasNanos = 0.0,
                fullInterSignalBiasUncertaintyNanos = null,
                satelliteInterSignalBiasNanos = null,
                satelliteInterSignalBiasUncertaintyNanos = null,
                fullTracking = true,
            )
    }
}
