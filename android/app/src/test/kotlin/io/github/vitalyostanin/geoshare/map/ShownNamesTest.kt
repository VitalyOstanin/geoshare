package io.github.vitalyostanin.geoshare.map

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ShownNamesTest {
    private fun at(positionId: String, memberId: String, device: String? = null) = MemberPosition(
        positionId = positionId,
        memberId = memberId,
        deviceName = device,
        latitude = 60.9,
        longitude = 4.3,
        takenAt = Instant.ofEpochSecond(1_000),
        writtenAt = Instant.ofEpochSecond(1_000),
        mode = null,
        batteryPercent = null,
    )

    private val profiles = mapOf("her" to MemberProfile("her", "Anna", null))

    @Test
    fun `one device is named after its person alone`() {
        val names = shownNames(listOf(at("her.a", "her", "Pixel")), profiles, "me", "You")

        assertEquals("Anna", names.markers["her.a"])
        assertEquals("Anna", names.people["her.a"])
    }

    @Test
    fun `two devices of one person carry the device name on the marker`() {
        val names =
            shownNames(
                listOf(at("her.a", "her", "Pixel"), at("her.b", "her", "Tablet")),
                profiles,
                "me",
                "You",
            )

        assertEquals("Anna · Pixel", names.markers["her.a"])
        assertEquals("Anna · Tablet", names.markers["her.b"])
        assertEquals("Anna", names.people["her.b"])
    }

    @Test
    fun `one's own marker says what the resources call it`() {
        val names = shownNames(listOf(at("me.a", "me")), profiles, "me", "You")

        assertEquals("You", names.markers["me.a"])
    }
}
