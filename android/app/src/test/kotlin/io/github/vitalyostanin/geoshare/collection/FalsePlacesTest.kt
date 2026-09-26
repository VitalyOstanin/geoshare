package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FalsePlacesTest {
    // A group that has named two places: the one its phones are handed while
    // the transmitter runs, and a second one thirteen kilometres away that a
    // phone without a mobile network was held at. Two entries rather than one
    // wide one, because the ground between them is ground a family stands on.
    private val named =
        listOf(
            FalsePlace(61.42565, 5.40377, 4_000.0, "where the phones are put"),
            FalsePlace(61.49782, 5.60881, 5_000.0, "and the place north-east of it"),
        )

    @Test
    fun `a place the group has named is known`() {
        assertNotNull(falsePlaceFor(fixAt(61.42565, 5.40377), named))
        assertNotNull(falsePlaceFor(fixAt(61.42545, 5.44473), named))
    }

    @Test
    fun `where the family actually lives is not a false place`() {
        assertNull(falsePlaceFor(fixAt(60.9, 4.3), named))
        assertNull(falsePlaceFor(fixAt(61.03268, 4.40300), named))
    }

    // What two entries buy over one wide one: the ground between them is not
    // called false, and a phone answering from there is still placed. Six and
    // a half kilometres from each.
    @Test
    fun `the ground between two places is not false`() {
        assertNull(falsePlaceFor(fixAt(61.4629, 5.5144), named))
    }

    @Test
    fun `a place is only itself, not everything beyond it`() {
        assertNull(falsePlaceFor(fixAt(61.60, 5.75), named))
    }

    // A place is weighed against one list rather than against all of them: a
    // group that has named the first place and not the second catches the
    // first and nothing more.
    @Test
    fun `only the places the group named are asked about`() {
        assertNull(falsePlaceFor(fixAt(61.49782, 5.60881), named.take(1)))
    }

    /**
     * Nothing is known until the group says so.
     *
     * The build carries no places at all, and a phone that has not reached the
     * database yet has none either: the rule then says nothing, which is what
     * it did before any place was ever named.
     */
    @Test
    fun `a phone that knows no places accuses nothing`() {
        assertNull(falsePlaceFor(fixAt(61.42565, 5.40377)))
        assertNull(whyAJump(null, fixAt(61.42565, 5.40377)))
    }

    // The rule that needs no anchor: a phone switched on inside the spoofed
    // area has no last position to compare its first answer with, and that
    // first answer is what every fix after it is believed against.
    @Test
    fun `the first fix of a round is caught when it lands on a false place`() {
        assertEquals(
            JumpReason.FalsePlace,
            whyAJump(null, fixAt(61.42565, 5.40377), falsePlaces = named),
        )
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

    // Weighed on the way in from the database, because the rules there hold
    // the shape of a document and cannot hold what the numbers mean.
    @Test
    fun `a place has to be somewhere, with a radius that is one`() {
        assertTrue(falsePlaceStands(named.first()))
        assertFalse(falsePlaceStands(named.first().copy(radiusMetres = 0.0)))
        assertFalse(falsePlaceStands(named.first().copy(radiusMetres = -1.0)))
        assertFalse(
            falsePlaceStands(named.first().copy(radiusMetres = FALSE_PLACE_WIDEST_METRES + 1.0)),
        )
        assertFalse(falsePlaceStands(named.first().copy(latitude = 91.0)))
        assertFalse(falsePlaceStands(named.first().copy(longitude = -181.0)))
    }

    // A position written before the place was named or widened: the phone
    // answering from home a minute later is fifty kilometres off it, and that
    // speed is the lie being measured against, not the phone.
    @Test
    fun `a last position inside a false place measures nothing`() {
        val lake = fixAt(61.42565, 5.40377)
        val home = fixAt(61.03268, 4.40300, seconds = 60)

        assertNotNull(whyAJump(lake, home))
        assertNull(whyAJump(lake, home, falsePlaces = named))
    }

    // The trap the stuck marker sat in: the phone lies still, its anchor was
    // set where the lie put it, and the first true answer is a drift from it.
    @Test
    fun `an anchor inside a false place holds nothing to it`() {
        val lake = fixAt(61.42565, 5.40377)
        val home = fixAt(61.03268, 4.40300, seconds = 7_200)
        val anchor = PutDown(at = lake, since = lake.takenAt, steps = 1_000)
        val still = Carrying(stirred = false, steps = 0)

        assertNotNull(
            whyAJump(lake, home, stillSince = lake.takenAt, putDown = anchor, carrying = still),
        )
        assertNull(
            whyAJump(
                lake,
                home,
                stillSince = lake.takenAt,
                falsePlaces = named,
                putDown = anchor,
                carrying = still,
            ),
        )
    }

    private fun fixAt(latitude: Double, longitude: Double, seconds: Long = 0) =
        Fix(latitude = latitude, longitude = longitude, takenAt = Instant.ofEpochSecond(seconds))
}
