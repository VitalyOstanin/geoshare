package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class ViewerDistanceTest {
    private val now = Instant.parse("2026-09-27T12:00:00Z")

    private fun at(
        positionId: String,
        latitude: Double,
        longitude: Double,
        minutes: Long = 1,
        width: Double? = 20.0,
    ): MemberPosition {
        val moment = now.minus(Duration.ofMinutes(minutes))

        return MemberPosition(
            positionId = positionId,
            memberId = positionId.substringBefore('.'),
            deviceName = null,
            latitude = latitude,
            longitude = longitude,
            takenAt = moment,
            writtenAt = moment,
            mode = null,
            batteryPercent = null,
            accuracyMetres = width,
        )
    }

    @Test
    fun `a degree of latitude is a hundred and eleven kilometres`() {
        val metres = metresBetween(at("a.1", 60.0, 30.0), at("b.1", 61.0, 30.0))

        assertEquals(111_195.0, metres, 10.0)
    }

    // Along a parallel a degree narrows with the cosine: half as wide at sixty.
    @Test
    fun `a degree of longitude at sixty north is half that`() {
        val metres = metresBetween(at("a.1", 60.0, 30.0), at("b.1", 60.0, 31.0))

        assertEquals(55_597.0, metres, 50.0)
    }

    @Test
    fun `the viewer is where the freshest of their own devices is`() {
        val positions =
            listOf(
                at("me.old", 61.0, 4.0, minutes = 10),
                at("me.new", 61.1, 4.0, minutes = 2),
                at("anna.one", 61.2, 4.0),
            )

        assertEquals("me.new", ownPlace(positions, "me", now)?.positionId)
    }

    // Half an hour is long enough to have walked off, and a distance from
    // where the viewer was is read as a distance from where they are.
    @Test
    fun `a place of the viewer gone grey is not measured from`() {
        val anna = at("anna.one", 61.2, 4.0)
        val positions = listOf(at("me.one", 61.0, 4.0, minutes = 31), anna)

        assertNull(distanceFromViewer(anna, positions, "me", now))
    }

    // A device under observation: online every minute, the marker stuck in a
    // lake since a fix went wrong.
    @Test
    fun `a marker the viewer's device has left behind is not measured from`() {
        val stuck =
            at("me.one", 61.14561, 5.4036).copy(
                takenAt = now.minus(Duration.ofMinutes(90)),
                writtenAt = now.minus(Duration.ofMinutes(1)),
            )
        val anna = at("anna.one", 61.03249, 4.4)

        assertNull(distanceFromViewer(anna, listOf(stuck, anna), "me", now))
    }

    @Test
    fun `the card of the very place measured from says nothing`() {
        val own = at("me.one", 61.0, 4.0)

        assertNull(distanceFromViewer(own, listOf(own), "me", now))
    }

    @Test
    fun `another device of the viewer's own is measured like anybody else`() {
        val phone = at("me.phone", 61.0, 4.0)
        val other = at("me.other", 61.01, 4.0, minutes = 5)

        assertEquals(1_112.0, distanceFromViewer(other, listOf(phone, other), "me", now)!!, 5.0)
    }

    @Test
    fun `a viewer with no place of their own is told nothing`() {
        val anna = at("anna.one", 61.0, 4.0)

        assertNull(distanceFromViewer(anna, listOf(anna), "me", now))
    }

    // Forty metres between two answers sixty metres wide is still forty
    // metres: the owner asked for the figure rather than a word.
    @Test
    fun `two answers inside each other's width still get a figure`() {
        val own = at("me.one", 61.0, 4.0, width = 60.0)
        val anna = at("anna.one", 61.00036, 4.0, width = 15.0)

        assertEquals(40.0, distanceFromViewer(anna, listOf(own, anna), "me", now)!!, 1.0)
    }

    @Test
    fun `a distance is said in whole metres, nought included`() {
        assertEquals(PositionWidth.Metres(0), distanceUnits(0.06))
        assertEquals(PositionWidth.Metres(40), distanceUnits(39.6))
        assertEquals(PositionWidth.Metres(999), distanceUnits(999.4))
    }

    @Test
    fun `a distance past a kilometre is said in kilometres to one place`() {
        assertEquals(PositionWidth.Kilometres(1.0), distanceUnits(999.6))
        assertEquals(PositionWidth.Kilometres(2.3), distanceUnits(2_345.0))
    }

    @Test
    fun `a place of the viewer inside a false place is not measured from`() {
        val own = at("me.one", 61.14561, 5.4036).copy(inFalsePlace = true)
        val anna = at("anna.one", 61.03249, 4.4)

        assertNull(distanceFromViewer(anna, listOf(own, anna), "me", now))
    }

    @Test
    fun `a member inside a false place is not measured to`() {
        val own = at("me.one", 61.03249, 4.4)
        val anna = at("anna.one", 61.14561, 5.4036).copy(inFalsePlace = true)

        assertNull(distanceFromViewer(anna, listOf(own, anna), "me", now))
    }
}
