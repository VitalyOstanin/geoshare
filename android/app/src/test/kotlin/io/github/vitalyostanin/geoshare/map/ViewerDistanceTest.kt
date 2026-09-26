package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    private fun away(distance: ViewerDistance?): Double = (distance as ViewerDistance.Away).metres

    @Test
    fun `a degree of latitude is a hundred and eleven kilometres`() {
        val metres = metresBetween(at("a.1", 61.0, 4.0), at("b.1", 62.0, 4.0))

        assertEquals(111_195.0, metres, 10.0)
    }

    // Along a parallel a degree narrows with the cosine: half as wide at sixty.
    @Test
    fun `a degree of longitude at sixty north is half that`() {
        val metres = metresBetween(at("a.1", 61.0, 4.0), at("b.1", 61.0, 5.0))

        assertEquals(55_597.0, metres, 50.0)
    }

    @Test
    fun `the viewer is where the freshest of their own phones is`() {
        val positions =
            listOf(
                at("me.old", 61.0, 4.0, minutes = 10),
                at("me.new", 61.1, 4.0, minutes = 2),
                at("oona.one", 61.2, 4.0),
            )

        assertEquals("me.new", ownPlace(positions, "me", now)?.positionId)
    }

    // Half an hour is long enough to have walked off, and a distance from
    // where the viewer was is read as a distance from where they are.
    @Test
    fun `a place of the viewer gone grey is not measured from`() {
        val oona = at("oona.one", 61.2, 4.0)
        val positions = listOf(at("me.one", 61.0, 4.0, minutes = 31), oona)

        assertNull(distanceFromViewer(oona, positions, "me", now))
    }

    // Phone 3 on 26.09: online every minute, the marker in the lake since 20:22.
    @Test
    fun `a marker the viewer's phone has left behind is not measured from`() {
        val stuck =
            at("me.one", 61.14561, 5.4036).copy(
                takenAt = now.minus(Duration.ofMinutes(90)),
                writtenAt = now.minus(Duration.ofMinutes(1)),
            )
        val oona = at("oona.one", 61.03249, 4.4)

        assertNull(distanceFromViewer(oona, listOf(stuck, oona), "me", now))
    }

    @Test
    fun `the card of the very place measured from says nothing`() {
        val own = at("me.one", 61.0, 4.0)

        assertNull(distanceFromViewer(own, listOf(own), "me", now))
    }

    @Test
    fun `another phone of the viewer's own is measured like anybody else`() {
        val phone = at("me.phone", 61.0, 4.0)
        val other = at("me.other", 61.01, 4.0, minutes = 5)

        assertEquals(1_112.0, away(distanceFromViewer(other, listOf(phone, other), "me", now)), 5.0)
    }

    @Test
    fun `a viewer with no place of their own is told nothing`() {
        val oona = at("oona.one", 61.0, 4.0)

        assertNull(distanceFromViewer(oona, listOf(oona), "me", now))
    }

    // Forty metres between two answers sixty metres wide is one flat.
    @Test
    fun `two answers inside each other's width are near, not a figure`() {
        val own = at("me.one", 61.0, 4.0, width = 60.0)
        val oona = at("oona.one", 61.00036, 4.0, width = 15.0)

        assertEquals(ViewerDistance.Near, distanceFromViewer(oona, listOf(own, oona), "me", now))
    }

    @Test
    fun `a width the phone did not name counts as none`() {
        val own = at("me.one", 61.0, 4.0, width = null)
        val oona = at("oona.one", 61.00036, 4.0, width = null)

        assertTrue(distanceFromViewer(oona, listOf(own, oona), "me", now) is ViewerDistance.Away)
    }
}
