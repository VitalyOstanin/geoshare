package io.github.vitalyostanin.geoshare.map

import io.github.vitalyostanin.geoshare.collection.FalsePlace
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class FalseMarksTest {
    private val now = Instant.parse("2026-09-27T12:00:00Z")

    // The place two devices were put at during testing, drawn six kilometres
    // wide.
    private val lake = FalsePlace(61.11758, 5.38558, 6_000.0, "lake")

    private fun at(latitude: Double, longitude: Double, minutes: Long = 1): MemberPosition {
        val moment = now.minus(Duration.ofMinutes(minutes))

        return MemberPosition(
            positionId = "three.a",
            memberId = "three",
            deviceName = null,
            latitude = latitude,
            longitude = longitude,
            takenAt = moment,
            writtenAt = moment,
            mode = null,
            batteryPercent = null,
        )
    }

    // A device under observation: written at 20:22, 3.3 km from the centre,
    // inside once the place was widened to six.
    @Test
    fun `the marker in the lake is marked once the place is wide enough`() {
        val lakeside = at(61.14561, 5.4036)

        assertTrue(withFalsePlaces(listOf(lakeside), listOf(lake)).single().inFalsePlace)
        assertFalse(
            withFalsePlaces(listOf(lakeside), listOf(lake.copy(radiusMetres = 3_000.0)))
                .single()
                .inFalsePlace,
        )
    }

    @Test
    fun `a position at home is left as it is`() {
        val home = at(61.03249, 4.4)

        assertSame(home, withFalsePlaces(listOf(home), listOf(lake)).single())
    }

    @Test
    fun `a position inside a place is grey however fresh`() {
        assertTrue(isGrey(at(61.14561, 5.4036).copy(inFalsePlace = true), now))
    }

    @Test
    fun `a fresh position outside every place is in colour, an old one grey`() {
        assertFalse(isGrey(at(61.03249, 4.4), now))
        assertTrue(isGrey(at(61.03249, 4.4, minutes = 31), now))
    }
}
