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

    // The spoofing lasts hours; without a floor the device would scan the
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

    // What was watched happening on a device standing in its own flat: an
    // answer from the Wi-Fi sixty metres wide, then a tower answering about
    // the same district one thousand six hundred and sixty metres wide, and
    // the marker walking four hundred metres to meet it.
    @Test
    fun `a tower answering about the district does not replace the flat`() {
        val fromTheWiFi = fixAt(61.03268, 4.40304, width = 62.5, takenAt = NOW.minusSeconds(120))
        val fromATower = fixAt(61.03253, 4.39526, width = 1_660.0)

        assertFalse(saysMoreThan(fromATower, fromTheWiFi, NOW))
    }

    @Test
    fun `a narrower answer is written whatever it says`() {
        val fromATower = fixAt(61.03253, 4.39526, width = 1_660.0, takenAt = NOW.minusSeconds(60))
        val fromTheWiFi = fixAt(61.03268, 4.40304, width = 62.5)

        assertTrue(saysMoreThan(fromTheWiFi, fromATower, NOW))
    }

    // A device really carried across the city: the answer is wider than the one
    // written and names a place the written one does not reach.
    @Test
    fun `a wider answer about another place is worth writing`() {
        val atHome = fixAt(61.03268, 4.40304, width = 62.5, takenAt = NOW.minusSeconds(120))
        val acrossTheCity = fixAt(61.11817, 4.38151, width = 900.0)

        assertTrue(saysMoreThan(acrossTheCity, atHome, NOW))
    }

    @Test
    fun `anything replaces a position nobody has refreshed`() {
        val stale = fixAt(61.03268, 4.40304, width = 20.0, takenAt = NOW.minus(Duration.ofHours(1)))
        val fromATower = fixAt(61.03253, 4.39526, width = 1_660.0)

        assertTrue(saysMoreThan(fromATower, stale, NOW))
        assertTrue("nothing written yet", saysMoreThan(fromATower, null, NOW))
    }

    // Six minutes is long enough for an answer from the towers to be called
    // old, and nothing like long enough for a device standing in its own flat
    // to have gone anywhere. The first version of this rule counted the two
    // the same way, and the marker walked four hundred and thirty metres.
    @Test
    fun `a position still worth showing is not overwritten by a vaguer one`() {
        val minutesOld = NOW.minus(Duration.ofMinutes(6))
        val fromTheWiFi = fixAt(61.03268, 4.40304, width = 42.5, takenAt = minutesOld)
        val fromATower = fixAt(61.03253, 4.39526, width = 1_677.0)

        assertFalse(saysMoreThan(fromATower, fromTheWiFi, NOW))
    }

    // The flat again, and the answer that moved the device out of it: a tower
    // one thousand seven hundred metres wide whose circle fell one hundred and
    // fifty-six metres short of reaching the position already written. Outside
    // the doubt by arithmetic, and inside it by any honest reading of what a
    // network width means.
    @Test
    fun `a tower missing the flat by a hundred metres has not found the phone elsewhere`() {
        val minutesOld = NOW.minus(Duration.ofMinutes(2))
        val inTheFlat = fixAt(61.03268, 4.40300, width = 28.1, takenAt = minutesOld)
        val fromATower = fixAt(61.03545, 4.36954, width = 1_700.0)

        assertFalse(saysMoreThan(fromATower, inTheFlat, NOW))
    }

    private fun fixAt(latitude: Double, longitude: Double, width: Double, takenAt: Instant = NOW) =
        Fix(latitude = latitude, longitude = longitude, takenAt = takenAt, accuracyMetres = width)

    private companion object {
        val NOW: Instant = Instant.parse("2026-09-13T19:00:00Z")
    }
}
