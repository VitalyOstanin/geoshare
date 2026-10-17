package io.github.vitalyostanin.geoshare.service

import io.github.vitalyostanin.geoshare.collection.Constellation
import io.github.vitalyostanin.geoshare.collection.Counted
import io.github.vitalyostanin.geoshare.collection.DRIFT_LEAST_APART_MS
import io.github.vitalyostanin.geoshare.collection.DRIFT_MOST_APART_MS
import io.github.vitalyostanin.geoshare.collection.Drift
import io.github.vitalyostanin.geoshare.collection.EPOCHS_READ_TOGETHER
import io.github.vitalyostanin.geoshare.collection.Epochs
import io.github.vitalyostanin.geoshare.collection.Held
import io.github.vitalyostanin.geoshare.collection.OrbitEpoch
import io.github.vitalyostanin.geoshare.collection.SkyDrift
import io.github.vitalyostanin.geoshare.collection.Slant
import io.github.vitalyostanin.geoshare.collection.driftShapeOf
import io.github.vitalyostanin.geoshare.collection.shapeOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** The decisions the radio watch takes from what the receiver hands it. */
class RadioReadingTest {
    private val now = Instant.parse("2026-01-01T12:00:00Z")

    private fun slant(spread: Double = 2.0, slope: Double? = 5.0) = Slant(
        satellites = 8,
        meanDb = 35.0,
        spreadDb = spread,
        slopeDb = slope,
        highLessLowDb = slope,
    )

    private fun drift(apart: Double = 0.2) = Drift(satellites = 6, shiftDb = -1.5, apartDb = apart)

    @Test
    fun `pushing past the limit lets the oldest go`() {
        val into = ArrayDeque(listOf(1, 2, 3))

        pushCapped(into, 4, 3)

        assertEquals(listOf(2, 3, 4), into.toList())
    }

    @Test
    fun `pushing within the limit keeps everything`() {
        val into = ArrayDeque(listOf(1))

        pushCapped(into, 2, 3)

        assertEquals(listOf(1, 2), into.toList())
    }

    @Test
    fun `a window short of full has no averages`() {
        val epochs = windowOf(List(RECENT_EPOCHS + EARLIER_EPOCHS - 1) { 1.0 })

        assertNull(epochs.recent)
        assertNull(epochs.earlier)
        assertNull(epochs.moved)
    }

    @Test
    fun `a full window splits into the recent ten and the earlier fifty`() {
        val kept = List(EARLIER_EPOCHS) { 10.0 } + List(RECENT_EPOCHS) { 7.0 }

        val epochs = windowOf(kept)

        assertEquals(7.0, epochs.recent!!, 1e-9)
        assertEquals(10.0, epochs.earlier!!, 1e-9)
        assertEquals(-3.0, epochs.moved!!, 1e-9)
    }

    @Test
    fun `the other band counts as silent only when the window is full of silence`() {
        assertTrue(otherBandSilentThroughout(List(EPOCHS_READ_TOGETHER) { true }))
        assertFalse(otherBandSilentThroughout(List(EPOCHS_READ_TOGETHER - 1) { true }))
        assertFalse(
            otherBandSilentThroughout(List(EPOCHS_READ_TOGETHER - 1) { true } + false),
        )
        assertFalse(otherBandSilentThroughout(emptyList()))
    }

    @Test
    fun `a line is due once the interval has passed and not before`() {
        val every = Duration.ofMinutes(5)

        assertFalse(dueAgain(now, now.plus(Duration.ofMinutes(4)), every))
        assertTrue(dueAgain(now, now.plus(every), every))
        assertTrue(dueAgain(Instant.EPOCH, now, every))
    }

    @Test
    fun `an epoch from the last five seconds still speaks for the fix`() {
        assertTrue(skyStillCurrent(now, now))
        assertTrue(skyStillCurrent(now.minus(SKY_STILL_CURRENT), now))
    }

    @Test
    fun `an epoch older than five seconds does not`() {
        assertFalse(skyStillCurrent(now.minusSeconds(6), now))
        assertFalse(skyStillCurrent(Instant.EPOCH, now))
    }

    @Test
    fun `an epoch from slightly in the future still speaks for the fix`() {
        assertTrue(skyStillCurrent(now.plusSeconds(2), now))
        assertFalse(skyStillCurrent(now.plusSeconds(10), now))
    }

    @Test
    fun `with nothing kept the drift starts over`() {
        assertEquals(DriftStep.Restart, driftStepFor(null, now))
    }

    @Test
    fun `a kept epoch too recent makes the drift wait`() {
        val kept = now.minusMillis(DRIFT_LEAST_APART_MS - 1)

        assertEquals(DriftStep.Wait, driftStepFor(kept, now))
    }

    @Test
    fun `a kept epoch far enough apart is read against`() {
        assertEquals(
            DriftStep.Read,
            driftStepFor(now.minusMillis(DRIFT_LEAST_APART_MS), now),
        )
        assertEquals(
            DriftStep.Read,
            driftStepFor(now.minusMillis(DRIFT_MOST_APART_MS), now),
        )
    }

    @Test
    fun `a kept epoch from before a pause makes the drift start over`() {
        assertEquals(
            DriftStep.Restart,
            driftStepFor(now.minusMillis(DRIFT_MOST_APART_MS + 1), now),
        )
    }

    @Test
    fun `the missing line counts what is held against what is needed`() {
        assertEquals(
            "the radio has nothing to go on: 3 epochs of gain, " +
                "4 of the GPS band, 5 of GLONASS, 60 needed",
            nothingToGoOn(gain = 3, shared = 4, glonass = 5),
        )
    }

    @Test
    fun `the verdict line names the move of each band`() {
        val line = verdictLine(
            "Jamming",
            gain = Epochs(recent = 20.0, earlier = 24.0) to Epochs(recent = null, earlier = 1.0),
            loudness = Epochs(recent = 30.0, earlier = 29.5) to NOTHING,
        )

        assertEquals(
            "the radio says jamming (gain: the GPS band -4.0 dB, GLONASS not heard; " +
                "loudness: the GPS band +0.5 dB, GLONASS not heard)",
            line,
        )
    }

    @Test
    fun `no orbit epoch gives no orbits line`() {
        assertNull(orbitsLine(null))
    }

    @Test
    fun `an epoch with every satellite on its orbit says none are off`() {
        val epoch = OrbitEpoch(now, listOf(Held(Constellation.Gps, 5, 45.0, 0.0)))

        assertEquals("0 of 1 satellites off their orbits", orbitsLine(epoch))
    }

    @Test
    fun `an epoch with satellites off their orbits names each one`() {
        val epoch = OrbitEpoch(
            now,
            listOf(
                Held(Constellation.Gps, 5, 45.0, 0.0),
                Held(Constellation.Glonass, 9, -5.0, 13_000.0),
            ),
        )

        assertEquals(
            "1 of 2 satellites off their orbits: GLONASS 9 at -5° and +13 km",
            orbitsLine(epoch),
        )
    }

    @Test
    fun `the log hears when the sky leaves its orbits`() {
        assertEquals(
            "the sky is off its orbits: two of nine",
            orbitsChange(wasOff = false, isOff = true) { "two of nine" },
        )
    }

    @Test
    fun `the log hears when the sky comes back on its orbits`() {
        assertEquals(
            "the sky is back on its orbits",
            orbitsChange(wasOff = true, isOff = false) { error("not needed") },
        )
    }

    @Test
    fun `an unchanged answer about the orbits is not logged`() {
        assertNull(orbitsChange(wasOff = true, isOff = true) { error("not needed") })
        assertNull(orbitsChange(wasOff = false, isOff = false) { error("not needed") })
    }

    @Test
    fun `no satellites give no line about what the fix rests on`() {
        assertNull(satellitesLine(emptyMap()))
    }

    @Test
    fun `the fix line lists systems in the log order`() {
        val line = satellitesLine(
            mapOf(
                Constellation.Glonass to Counted(heard = 6, usedInFix = 0),
                Constellation.Gps to Counted(heard = 9, usedInFix = 7),
            ),
        )

        assertEquals("the fix rests on GPS 7 of 9, GLONASS 0 of 6", line)
    }

    @Test
    fun `a sky window short of ten epochs gives no sky line`() {
        val shapes = mapOf(
            Constellation.Gps to List(EPOCHS_READ_TOGETHER - 1) { slant() },
        )

        assertNull(skyLine(shapes))
        assertNull(skyLine(emptyMap()))
    }

    @Test
    fun `a full sky window reads the shape and the last epoch`() {
        val epochs = List(EPOCHS_READ_TOGETHER) { slant(spread = 2.5, slope = 4.0) }

        val line = skyLine(mapOf(Constellation.Gps to epochs))

        assertEquals(
            "the sky reads GPS ${shapeOf(epochs).toString().lowercase()} " +
                "(spread 2.5 dB, slope +4.0 dB, 8 satellites)",
            line,
        )
    }

    @Test
    fun `a sky epoch without a slope says it is not measurable`() {
        val epochs = List(EPOCHS_READ_TOGETHER) { slant(slope = null) }

        assertTrue(skyLine(mapOf(Constellation.Gps to epochs))!!.contains("slope not measurable"))
    }

    @Test
    fun `the sky line skips systems with short windows and keeps the rest`() {
        val full = List(EPOCHS_READ_TOGETHER) { slant() }
        val line = skyLine(
            mapOf(
                Constellation.Galileo to List(2) { slant() },
                Constellation.Glonass to full,
            ),
        )!!

        assertTrue(line.contains("GLONASS"))
        assertFalse(line.contains("Galileo"))
    }

    @Test
    fun `a drift window short of ten epochs gives no drift line`() {
        val drifts = mapOf(Constellation.Gps to List(EPOCHS_READ_TOGETHER - 1) { drift() })

        assertNull(driftLine(drifts))
        assertNull(driftLine(emptyMap()))
    }

    @Test
    fun `a full drift window says how the satellites moved`() {
        val epochs = List(EPOCHS_READ_TOGETHER) { drift(apart = 0.2) }
        val word = when (driftShapeOf(epochs)) {
            SkyDrift.Together -> "as one block"
            SkyDrift.Apart -> "each on its own"
            SkyDrift.Unclear -> "neither way"
        }

        assertEquals(
            "the satellites move GPS $word (apart 0.2 dB, shift -1.5 dB, 6 satellites)",
            driftLine(mapOf(Constellation.Gps to epochs)),
        )
    }

    @Test
    fun `drift windows that move together and apart are worded differently`() {
        val together = List(EPOCHS_READ_TOGETHER) { drift(apart = 0.1) }
        val apart = List(EPOCHS_READ_TOGETHER) { drift(apart = 9.0) }

        val a = driftLine(mapOf(Constellation.Gps to together))
        val b = driftLine(mapOf(Constellation.Gps to apart))

        assertNotNull(a)
        assertNotNull(b)
        assertTrue(a!!.contains("as one block") != b!!.contains("as one block"))
    }

    @Test
    fun `systems heard for nothing are logged when they first appear`() {
        assertEquals(
            "heard for nothing: GPS, Galileo",
            aidingChange(
                said = setOf(Constellation.Galileo, Constellation.Gps),
                doubted = emptySet(),
            ),
        )
    }

    @Test
    fun `a system that came good is logged as everything being made use of`() {
        assertEquals(
            "every system heard is being made use of",
            aidingChange(said = emptySet(), doubted = setOf(Constellation.Gps)),
        )
    }

    @Test
    fun `an unchanged set of systems heard for nothing is not logged`() {
        assertNull(aidingChange(setOf(Constellation.Gps), setOf(Constellation.Gps)))
        assertNull(aidingChange(emptySet(), emptySet()))
    }
}
