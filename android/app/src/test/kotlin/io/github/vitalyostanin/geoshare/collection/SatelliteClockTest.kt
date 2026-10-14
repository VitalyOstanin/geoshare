package io.github.vitalyostanin.geoshare.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The numbers are a device's own raw measurements, one pair of epochs ten
 * seconds apart: GPS 11 and GPS 20 as the receiver reported them, satellite
 * time, receiver clock and clock since boot alike.
 */
class SatelliteClockTest {
    private val gps11 = Signal(Constellation.Gps, id = 11, carrierHz = 1_575_420_032.0)

    private fun before(
        sent: Long = 301_874_334_099_498L,
        receiver: Long = 1_473_594_674_418_494_464L,
        boot: Long = 60_586_853_114_102L,
    ) = SatelliteTick(gps11, sentNanos = sent, receiverNanos = receiver, sinceBootNanos = boot)

    private fun after(
        sent: Long = 301_884_334_077_975L,
        receiver: Long = 1_473_594_684_418_494_976L,
        boot: Long = 60_596_857_222_640L,
    ) = SatelliteTick(gps11, sentNanos = sent, receiverNanos = receiver, sinceBootNanos = boot)

    @Test
    fun `a satellite of the real sky keeps time with both clocks`() {
        val apart = clocksApart(before(), after())

        assertEquals(SatelliteClockVerdict.Agrees, satelliteClockVerdict(apart))
    }

    // What the real pair actually came to: twenty-two microseconds against
    // the receiver's clock and four milliseconds against the device's, which
    // is the epoch's stamp rather than the radio.
    @Test
    fun `the real pair is microseconds from the receiver and milliseconds from the device`() {
        val apart = clocksApart(before(), after())!!

        assertEquals(-22_035L, apart.receiverNanos)
        assertEquals(-4_130_061L, apart.sinceBootNanos)
    }

    // A relay that holds the signal before sending it on hands over a time
    // that is late by the holding. Half a millisecond of it is 150 km of
    // light, and SPREE takes a third of that for a spoofer.
    @Test
    fun `a signal held back by a couple of milliseconds parts from the receiver's clock`() {
        val held = after(sent = 301_884_334_077_975L - 2_000_000L)

        assertEquals(
            SatelliteClockVerdict.ReceiverApart,
            satelliteClockVerdict(clocksApart(before(), held)),
        )
    }

    // The heavier case: the receiver's own scale walked with the satellite,
    // so the two of them still agree, and only the clock no signal can reach
    // says a second went missing.
    @Test
    fun `a scale walked whole is caught by the device's own clock alone`() {
        val walked =
            after(
                sent = 301_884_334_077_975L - 1_000_000_000L,
                receiver = 1_473_594_684_418_494_976L - 1_000_000_000L,
            )

        assertEquals(
            SatelliteClockVerdict.PhoneApart,
            satelliteClockVerdict(clocksApart(before(), walked)),
        )
    }

    // Everything but GLONASS counts within the week, and the count wrapping
    // is not the satellite losing six days.
    @Test
    fun `the week wrapping over is not a disagreement`() {
        val ending = before(sent = WEEK_NANOS - 5_000_000_000L)
        val starting =
            after(
                sent = 5_000_000_000L,
                receiver = ending.receiverNanos + 10_000_000_000L,
                boot = ending.sinceBootNanos + 10_000_000_000L,
            )

        assertEquals(
            SatelliteClockVerdict.Agrees,
            satelliteClockVerdict(clocksApart(ending, starting)),
        )
    }

    // GLONASS counts within the day instead, and reading its wrap as a week's
    // would put the satellite eighty-six thousand seconds out.
    @Test
    fun `GLONASS wraps over the day rather than the week`() {
        val glonass = Signal(Constellation.Glonass, id = 7, carrierHz = 1_602_000_000.0)
        val ending =
            SatelliteTick(
                glonass,
                sentNanos = DAY_NANOS - 5_000_000_000L,
                receiverNanos = 1_473_594_674_418_494_464L,
                sinceBootNanos = 60_586_853_114_102L,
            )
        val starting =
            ending.copy(
                sentNanos = 5_000_000_000L,
                receiverNanos = ending.receiverNanos + 10_000_000_000L,
                sinceBootNanos = ending.sinceBootNanos + 10_000_000_000L,
            )

        assertEquals(
            SatelliteClockVerdict.Agrees,
            satelliteClockVerdict(clocksApart(ending, starting)),
        )
    }

    // A signal lost and found again an hour later says nothing about either
    // clock: what the pair would measure is the gap.
    @Test
    fun `epochs too far apart are not compared`() {
        val late = after(boot = 60_586_853_114_102L + 10L * 60L * 1_000_000_000L)

        assertNull(clocksApart(before(), late))
        assertEquals(
            SatelliteClockVerdict.Unknown,
            satelliteClockVerdict(clocksApart(before(), late)),
        )
    }

    // Two different satellites' times have nothing to say to each other.
    @Test
    fun `ticks of different signals are not a pair`() {
        val other = after().copy(signal = gps11.copy(id = 20))

        assertNull(clocksApart(before(), other))
    }

    // The first epoch a signal is heard in has nothing to be compared against,
    // and a watch that reported it as a disagreement would fire every time a
    // satellite rose.
    @Test
    fun `a signal heard for the first time says nothing`() {
        val watch = SatelliteClockWatch()

        assertEquals(emptyList<SignalApart>(), watch.read(clock(), listOf(measurement())))
    }

    @Test
    fun `the epoch after it is read against the one before`() {
        val watch = SatelliteClockWatch()

        watch.read(clock(), listOf(measurement()))

        val said =
            watch.read(
                clock(timeNanos = 60_270_030_000_000L, boot = 60_596_857_222_640L),
                listOf(measurement(sent = 301_884_334_077_975L)),
            )

        assertEquals(1, said.size)
        assertEquals(SatelliteClockVerdict.Agrees, said.single().verdict)
    }

    // A state without the decoded time carries a time within the millisecond
    // under the same name, so two of them subtract to nothing meaningful.
    @Test
    fun `a measurement whose satellite time is not decoded is left alone`() {
        val watch = SatelliteClockWatch()
        val ambiguous = measurement(state = 17)

        watch.read(clock(), listOf(ambiguous))

        assertEquals(
            emptyList<SignalApart>(),
            watch.read(
                clock(timeNanos = 60_270_030_000_000L, boot = 60_596_857_222_640L),
                listOf(ambiguous),
            ),
        )
    }

    // A receiver that reports no bias at all cannot place its own clock on the
    // satellites' scale, and the pair has nothing to stand on.
    @Test
    fun `an epoch without the full bias is not read`() {
        val watch = SatelliteClockWatch()
        val blind = clock().copy(fullBiasNanos = null)

        watch.read(blind, listOf(measurement()))

        assertEquals(emptyList<SignalApart>(), watch.read(blind, listOf(measurement())))
    }

    @Test
    fun `only the signals that parted are carried out of a reading`() {
        val watch = SatelliteClockWatch()

        watch.read(clock(), listOf(measurement(), measurement(id = 20)))

        val said =
            watch.read(
                clock(timeNanos = 60_270_030_000_000L, boot = 60_596_857_222_640L),
                listOf(
                    measurement(sent = 301_884_334_077_975L),
                    measurement(id = 20, sent = 301_884_334_077_975L - 2_000_000L),
                ),
            )

        assertEquals(2, said.size)
        assertEquals(listOf(20), parted(said).map { it.signal.id })

        // Two milliseconds is the receiver's clock, not the device's: only a
        // signal past a tenth of a second is held against the count since the
        // boot, and only that one leaves a fix out.
        assertFalse(leftThePhonesClock(said))
    }

    @Test
    fun `a reading where every signal agrees has not left the device's clock`() {
        val watch = SatelliteClockWatch()

        watch.read(clock(), listOf(measurement()))

        val said =
            watch.read(
                clock(timeNanos = 60_270_030_000_000L, boot = 60_596_857_222_640L),
                listOf(measurement(sent = 301_884_334_077_975L)),
            )

        assertEquals(1, said.size)
        assertFalse(leftThePhonesClock(said))
    }

    @Test
    fun `a signal whose time stepped a whole second has left the device's clock`() {
        val watch = SatelliteClockWatch()

        watch.read(clock(), listOf(measurement()))

        val said =
            watch.read(
                clock(timeNanos = 60_270_030_000_000L, boot = 60_596_857_222_640L),
                listOf(measurement(sent = 301_884_334_077_975L - 1_000_000_000L)),
            )

        assertEquals(
            listOf(SatelliteClockVerdict.PhoneApart),
            said.map { it.verdict },
        )
        assertTrue(leftThePhonesClock(said))
    }

    @Test
    fun `a window that is not full yet says nothing`() {
        assertFalse(satelliteTimeParted(List(EPOCHS_READ_TOGETHER - 1) { true }))
    }

    @Test
    fun `one epoch of a full window is enough to raise the answer`() {
        val read = MutableList(EPOCHS_READ_TOGETHER) { false }

        assertFalse(satelliteTimeParted(read))

        read[EPOCHS_READ_TOGETHER / 2] = true

        assertTrue(satelliteTimeParted(read))
    }

    @Test
    fun `a window that has run past the step says nothing again`() {
        assertFalse(satelliteTimeParted(List(EPOCHS_READ_TOGETHER) { false }))
    }

    private fun clock(timeNanos: Long = 60_260_030_000_000L, boot: Long = 60_586_853_114_102L) =
        Clock(
            timeNanos = timeNanos,
            leapSecond = 18,
            timeUncertaintyNanos = null,
            fullBiasNanos = -1_473_534_414_388_494_680L,
            biasNanos = 0.0,
            biasUncertaintyNanos = null,
            driftNanosPerSecond = null,
            driftUncertaintyNanosPerSecond = null,
            discontinuities = 16,
            sinceBootNanos = boot,
            sinceBootUncertaintyNanos = null,
        )

    private fun measurement(id: Int = 11, sent: Long = 301_874_334_099_498L, state: Int = 16_399) =
        Measurement(
            constellation = Constellation.Gps,
            id = id,
            timeOffsetNanos = 0.0,
            state = state,
            receivedSvTimeNanos = sent,
            receivedSvTimeUncertaintyNanos = 40,
            loudness = 25.8,
            basebandLoudness = null,
            pseudorangeRateMps = 268.3,
            pseudorangeRateUncertaintyMps = 0.2,
            deltaRangeState = 0,
            deltaRangeMetres = 0.0,
            deltaRangeUncertaintyMetres = 0.0,
            carrierHz = 1_575_420_032.0,
            multipath = 0,
            gainDb = null,
            snrDb = null,
            codeType = "C",
            fullInterSignalBiasNanos = null,
            fullInterSignalBiasUncertaintyNanos = null,
            satelliteInterSignalBiasNanos = null,
            satelliteInterSignalBiasUncertaintyNanos = null,
            fullTracking = true,
        )
}
