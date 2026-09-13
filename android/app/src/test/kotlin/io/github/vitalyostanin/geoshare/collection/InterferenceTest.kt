package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class InterferenceTest {
    private fun epochs(earlier: Double, recent: Double) = Epochs(recent = recent, earlier = earlier)

    @Test
    fun `a radio nobody is interfering with says nothing`() {
        val gain = epochs(earlier = 40.0, recent = 39.5)
        val noise = epochs(earlier = 32.0, recent = 32.4)

        assertEquals(Interference.Quiet, interferenceFrom(gain, noise))
    }

    // Noise buries the satellites, and the receiver turns itself down against
    // it: both measures fall together.
    @Test
    fun `the gain and the satellites falling together is jamming`() {
        val gain = epochs(earlier = 40.0, recent = 33.0)
        val noise = epochs(earlier = 32.0, recent = 21.0)

        assertEquals(Interference.Jamming, interferenceFrom(gain, noise))
    }

    // A false signal arrives clean and loud: the receiver turns down while the
    // satellites it can hear get louder, which nothing natural does.
    @Test
    fun `the gain down and the signal up is spoofing`() {
        val gain = epochs(earlier = 40.0, recent = 34.0)
        val noise = epochs(earlier = 30.0, recent = 40.0)

        assertEquals(Interference.Spoofing, interferenceFrom(gain, noise))
    }

    // A spoofer matching the true signal's strength, and a phone put on a car
    // roof, look the same from here. Saying nothing is the honest answer.
    @Test
    fun `the gain alone is not enough to name the kind`() {
        val gain = epochs(earlier = 40.0, recent = 34.0)
        val noise = epochs(earlier = 32.0, recent = 32.5)

        assertEquals(Interference.Quiet, interferenceFrom(gain, noise))
    }

    @Test
    fun `a measure nobody has reported says nothing rather than something`() {
        val gain = Epochs(recent = 34.0, earlier = null)
        val noise = epochs(earlier = 30.0, recent = 40.0)

        assertEquals(Interference.Unknown, interferenceFrom(gain, noise))
        assertEquals(Interference.Unknown, interferenceFrom(noise, Epochs(null, null)))
    }

    @Test
    fun `a fix taken when the phone says it was is no disagreement`() {
        val now = Instant.parse("2026-09-13T19:00:00Z")

        assertFalse(clockDisagrees(now.minusSeconds(2), now, CLOCK_TOLERANCE))
        assertFalse(clockDisagrees(now.minusSeconds(29), now, CLOCK_TOLERANCE))
    }

    @Test
    fun `a clock far from the phone's own disagrees whichever way it is out`() {
        val now = Instant.parse("2026-09-13T19:00:00Z")

        assertTrue(clockDisagrees(now.minusSeconds(120), now, CLOCK_TOLERANCE))
        assertTrue(clockDisagrees(now.plusSeconds(120), now, CLOCK_TOLERANCE))
        assertFalse(clockDisagrees(now.plusSeconds(120), now, Duration.ofMinutes(5)))
    }
}
