package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * That two epochs are read as satellites moving together or apart, and that
 * the two skies the logs hold are told apart by it.
 *
 * The numbers are not invented. The block that moves together is the GPS band
 * of the device under a false signal in the night, twenty-one seconds apart:
 * nine satellites, none of them moving more than three tenths of a decibel
 * and all of them the same way. The one that moves apart is the device in
 * another part of the city, the same twenty-one seconds: eleven satellites,
 * one of them gaining seven decibels while another loses two.
 */
class DriftTest {
    private fun satellite(
        id: Int,
        loudness: Double,
        constellation: Constellation = Constellation.Gps,
    ) = Satellite(
        constellation = constellation,
        id = id,
        loudness = loudness,
        basebandLoudness = null,
        elevationDegrees = 40.0,
        azimuthDegrees = 120.0,
        carrierHz = null,
        usedInFix = true,
        hasEphemeris = true,
        hasAlmanac = true,
    )

    private fun sky(vararg loudness: Pair<Int, Double>) =
        loudness.map { (id, db) -> satellite(id, db) }

    private val blockBefore =
        sky(
            4 to 36.5, 9 to 36.0, 16 to 36.0, 18 to 36.2, 25 to 36.0, 26 to 36.1, 28 to 36.0,
            29 to 36.3, 31 to 36.2,
        )

    private val blockAfter =
        sky(
            4 to 36.3, 9 to 36.2, 16 to 36.3, 18 to 36.2, 25 to 36.2, 26 to 36.4, 28 to 36.4,
            29 to 36.2, 31 to 36.4,
        )

    private val honestBefore =
        sky(
            1 to 38.0, 2 to 37.0, 10 to 29.0, 13 to 14.0, 14 to 31.0, 15 to 37.0, 17 to 38.0,
            19 to 38.0, 20 to 38.0, 22 to 39.0, 24 to 38.0,
        )

    private val honestAfter =
        sky(
            1 to 36.0, 2 to 37.0, 10 to 36.0, 13 to 16.0, 14 to 38.0, 15 to 38.0, 17 to 37.0,
            19 to 37.0, 20 to 38.0, 22 to 38.0, 24 to 37.0,
        )

    @Test
    fun `satellites off one transmitter stay together`() {
        val drift = driftOf(blockBefore, blockAfter)[Constellation.Gps]

        requireNotNull(drift)
        assertEquals(9, drift.satellites)
        assertTrue("apart ${drift.apartDb}", drift.apartDb <= TOGETHER_DRIFT_DB)
    }

    @Test
    fun `satellites of a real sky move apart from one another`() {
        val drift = driftOf(honestBefore, honestAfter)[Constellation.Gps]

        requireNotNull(drift)
        assertEquals(11, drift.satellites)
        assertTrue("apart ${drift.apartDb}", drift.apartDb >= APART_DRIFT_DB)
    }

    @Test
    fun `the block's own shift is told from the moving apart`() {
        val louder = blockAfter.map { it.copy(loudness = it.loudness + 4.0) }
        val drift = driftOf(blockBefore, louder)[Constellation.Gps]

        requireNotNull(drift)
        assertEquals(4.0, drift.shiftDb, 0.3)
        assertTrue("apart ${drift.apartDb}", drift.apartDb <= TOGETHER_DRIFT_DB)
    }

    @Test
    fun `satellites heard in one epoch only are left out`() {
        val drift = driftOf(blockBefore, blockAfter + satellite(7, 20.0))[Constellation.Gps]

        requireNotNull(drift)
        assertEquals(9, drift.satellites)
    }

    @Test
    fun `satellites listed without being heard are left out`() {
        val silent = blockAfter.map { it.copy(loudness = 0.0) }

        assertTrue(driftOf(blockBefore, silent).isEmpty())
    }

    @Test
    fun `too few satellites say nothing at all`() {
        val few = 7

        assertTrue(driftOf(blockBefore.take(few), blockAfter.take(few)).isEmpty())
    }

    @Test
    fun `systems are read apart from one another`() {
        val mixed = blockBefore + blockBefore.map { it.copy(constellation = Constellation.Glonass) }
        val read = driftOf(mixed, mixed)

        assertEquals(setOf(Constellation.Gps, Constellation.Glonass), read.keys)
    }

    @Test
    fun `a run of epochs moving together reads as one block`() {
        val together =
            List(EPOCHS_READ_TOGETHER) { Drift(satellites = 9, shiftDb = 0.2, apartDb = 0.3) }

        assertEquals(SkyDrift.Together, driftShapeOf(together))
    }

    @Test
    fun `a run of epochs moving apart reads as a real sky`() {
        val apart =
            List(EPOCHS_READ_TOGETHER) { Drift(satellites = 11, shiftDb = 0.5, apartDb = 1.2) }

        assertEquals(SkyDrift.Apart, driftShapeOf(apart))
    }

    @Test
    fun `one wild pair does not decide the run`() {
        val epochs =
            List(EPOCHS_READ_TOGETHER - 1) { Drift(satellites = 9, shiftDb = 0.1, apartDb = 0.2) } +
                Drift(satellites = 9, shiftDb = 6.0, apartDb = 9.0)

        assertEquals(SkyDrift.Together, driftShapeOf(epochs))
    }

    @Test
    fun `a run too short to read says nothing`() {
        val epochs =
            List(EPOCHS_READ_TOGETHER - 1) { Drift(satellites = 9, shiftDb = 0.1, apartDb = 0.2) }

        assertEquals(SkyDrift.Unclear, driftShapeOf(epochs))
    }

    @Test
    fun `a run between the limits says nothing`() {
        val between =
            List(EPOCHS_READ_TOGETHER) { Drift(satellites = 9, shiftDb = 0.3, apartDb = 0.75) }

        assertTrue(abs(0.75 - TOGETHER_DRIFT_DB) > 0.0)
        assertEquals(SkyDrift.Unclear, driftShapeOf(between))
    }
}
