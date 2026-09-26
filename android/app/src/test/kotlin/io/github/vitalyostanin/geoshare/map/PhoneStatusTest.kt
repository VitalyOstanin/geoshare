package io.github.vitalyostanin.geoshare.map

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.Instant

/**
 * What a phone says about itself apart from where it is, and how that reaches
 * the card.
 *
 * The case this exists for: a phone whose answers are all being thrown away
 * writes no position, and its charge, its cable and the moment it was last in
 * touch have to arrive anyway.
 */
class PhoneStatusTest {
    private val position =
        MemberPosition(
            positionId = "her.phone",
            memberId = "her",
            deviceName = "MAR-LX1",
            latitude = 60.9,
            longitude = 4.3,
            takenAt = Instant.ofEpochSecond(1_000),
            writtenAt = Instant.ofEpochSecond(1_000),
            mode = "Standby",
            batteryPercent = 40,
            charging = false,
        )

    private fun grantsWith(phone: PhoneStatus) = combinedGrants(
        named = mapOf("her" to MemberGrant(memberId = "her", devices = listOf("phone"))),
        phones = mapOf("her" to mapOf("phone" to phone)),
    )

    @Test
    fun `what the phone said after its position is laid over it`() {
        val later = Instant.ofEpochSecond(9_000)
        val grants =
            grantsWith(
                PhoneStatus(
                    onlineAt = later,
                    mode = "Viewing",
                    batteryPercent = 100,
                    charging = true,
                ),
            )

        val shown = withPhones(listOf(position), grants).single()

        assertEquals(later, shown.knownAt)
        assertEquals(position.positionAt, shown.positionAt)
        assertEquals(100, shown.batteryPercent)
        assertEquals(true, shown.charging)
        assertEquals("Viewing", shown.mode)
    }

    @Test
    fun `what the phone said before its position is not`() {
        val grants = grantsWith(PhoneStatus(onlineAt = Instant.ofEpochSecond(500), charging = true))

        assertSame(position, withPhones(listOf(position), grants).single())
    }

    // An administrator reads everybody's phones and nobody's grant: the phones
    // are there to show, and no position is followed.
    @Test
    fun `phones read without the grant lead to no positions`() {
        val grants = combinedGrants(emptyMap(), mapOf("her" to mapOf("phone" to PhoneStatus())))

        assertEquals(emptyList<String>(), grants.getValue("her").devices)
        assertEquals(1, grants.getValue("her").phones.size)
    }

    // A phone of an older build names itself in the grant and nowhere else.
    @Test
    fun `the phones to follow are those of the grant and of the phones together`() {
        val named = mapOf("her" to MemberGrant(memberId = "her", devices = listOf("old", "new")))
        val phones = mapOf("her" to mapOf("new" to PhoneStatus(), "newer" to PhoneStatus()))
        val grants = combinedGrants(named, phones)

        assertEquals(listOf("old", "new", "newer"), grants.getValue("her").devices)
    }

    @Test
    fun `an entry keeps what a phone writes and drops the rest`() {
        val phones =
            phonesFrom(
                mapOf(
                    "phone" to
                        mapOf(
                            "model" to "MAR-LX1",
                            "onlineAt" to Timestamp(5, 0),
                            "batteryPercent" to 250L,
                            "charging" to "yes",
                        ),
                    "broken" to "not an entry",
                ),
            )

        assertEquals(setOf("phone"), phones.keys)
        assertEquals(
            PhoneStatus(model = "MAR-LX1", onlineAt = Instant.ofEpochSecond(5)),
            phones.getValue("phone"),
        )
    }
}
