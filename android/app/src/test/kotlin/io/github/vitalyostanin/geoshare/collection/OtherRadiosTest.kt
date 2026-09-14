package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class OtherRadiosTest {
    @Test
    fun `one answer left out is ordinary`() {
        assertFalse(otherRadiosAreDue(1, null, NOW))
        assertFalse(otherRadiosAreDue(2, null, NOW))
    }

    @Test
    fun `three in a row is the satellites having stopped answering usefully`() {
        assertTrue(otherRadiosAreDue(JUMPS_BEFORE_THE_OTHER_RADIOS, null, NOW))
    }

    // The spoofing lasts hours; without a floor the phone would scan the
    // Wi-Fi around it on every fix for all of them.
    @Test
    fun `the towers are not asked again straight away`() {
        val asked = NOW.minus(Duration.ofSeconds(30))

        assertFalse(otherRadiosAreDue(9, asked, NOW))
    }

    @Test
    fun `the towers are asked again once the floor has passed`() {
        val asked = NOW.minus(OTHER_RADIOS_ASKED_EVERY)

        assertTrue(otherRadiosAreDue(9, asked, NOW))
    }

    @Test
    fun `an answer a kilometre wide stands where an exact lie does not`() {
        assertTrue(networkAnswerStands(fixAt(60.94, 4.31, width = 1_200.0), NOW))
    }

    @Test
    fun `no answer at all does not stand`() {
        assertFalse(networkAnswerStands(null, NOW))
    }

    // The platform's own store of last known places can hold what the spoofer
    // put there an hour ago, and that store is what answers when the towers
    // are slow.
    @Test
    fun `an answer from a place the group named false does not stand`() {
        val named = listOf(FalsePlace(61.42565, 5.40377, 4_000.0, "where the phones are put"))

        assertTrue(networkAnswerStands(fixAt(61.42565, 5.40377, width = 30.0), NOW))
        assertFalse(networkAnswerStands(fixAt(61.42565, 5.40377, width = 30.0), NOW, named))
    }

    @Test
    fun `an answer wider than a district places the phone nowhere`() {
        val tooWide = NETWORK_WIDTH_LIMIT_METRES + 1.0

        val atTheLimit = fixAt(60.94, 4.31, width = NETWORK_WIDTH_LIMIT_METRES)

        assertFalse(networkAnswerStands(fixAt(60.94, 4.31, width = tooWide), NOW))
        assertTrue(networkAnswerStands(atTheLimit, NOW))
    }

    @Test
    fun `an answer from an hour ago says where the phone was, not where it is`() {
        val old = fixAt(60.94, 4.31, width = 900.0, takenAt = NOW.minus(Duration.ofHours(1)))

        assertFalse(networkAnswerStands(old, NOW))
    }

    private fun fixAt(latitude: Double, longitude: Double, width: Double, takenAt: Instant = NOW) =
        Fix(latitude = latitude, longitude = longitude, takenAt = takenAt, accuracyMetres = width)

    private companion object {
        val NOW: Instant = Instant.parse("2026-09-13T19:00:00Z")
    }
}
