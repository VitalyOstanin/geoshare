package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class FalsePlacesTest {
    // The coordinates the phones of this family were handed while the spoofer
    // was running: a place a false signal uses, seventy kilometres from where any
    // of them were standing.
    @Test
    fun `the place the spoofer hands out is known`() {
        assertNotNull(falsePlaceFor(fixAt(61.42565, 5.40377)))
        assertNotNull(falsePlaceFor(fixAt(61.42545, 5.44473)))
    }

    @Test
    fun `the city the family lives in is not a false place`() {
        assertNull(falsePlaceFor(fixAt(60.9, 4.3)))
        assertNull(falsePlaceFor(fixAt(61.03268, 4.40300)))
    }

    // The same spoofer used the airport until the end of April, and it is
    // deliberately off the list: flights land there and people work there, so
    // calling it false would throw away positions that are true.
    @Test
    fun `the airport is left off the list`() {
        assertNull(falsePlaceFor(fixAt(60.8003, 4.2625)))
    }

    @Test
    fun `the lake beyond the drawn road is not a false place`() {
        assertNull(falsePlaceFor(fixAt(61.55, 5.60)))
    }

    // The rule that needs no anchor: a phone switched on inside the spoofed
    // area has no last position to compare its first answer with, and that
    // first answer is what every fix after it is believed against.
    @Test
    fun `the first fix of a round is caught when it lands on a false place`() {
        assertEquals(JumpReason.FalsePlace, whyAJump(null, fixAt(61.42565, 5.40377)))
    }

    // The road the receiver draws away from the point agrees with itself
    // perfectly, so agreement must not be allowed to buy it back.
    @Test
    fun `a false place is not something a second fix can confirm`() {
        assertFalse(mayBeConfirmed(JumpReason.FalsePlace))
    }

    @Test
    fun `the radius is where the place stops`() {
        val places = listOf(FalsePlace(61.0, 4.0, 1_000.0, "somewhere"))

        // Nine hundred metres of latitude, and then eleven hundred.
        assertNotNull(falsePlaceFor(fixAt(61.0081, 4.0), places))
        assertNull(falsePlaceFor(fixAt(61.0099, 4.0), places))
    }

    @Test
    fun `every place on the list carries a radius and a name`() {
        KNOWN_FALSE_PLACES.forEach {
            assertEquals(true, it.radiusMetres > 0.0)
            assertEquals(true, it.named.isNotEmpty())
        }
    }

    private fun fixAt(latitude: Double, longitude: Double) =
        Fix(latitude = latitude, longitude = longitude, takenAt = Instant.ofEpochSecond(0))
}
