package io.github.vitalyostanin.geoshare.log

import androidx.annotation.MainThread
import io.github.vitalyostanin.geoshare.collection.Clock
import io.github.vitalyostanin.geoshare.collection.Constellation
import io.github.vitalyostanin.geoshare.collection.DRIFT_LEAST_APART_MS
import io.github.vitalyostanin.geoshare.collection.DRIFT_MOST_APART_MS
import io.github.vitalyostanin.geoshare.collection.Drift
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.Gain
import io.github.vitalyostanin.geoshare.collection.Measurement
import io.github.vitalyostanin.geoshare.collection.NavMessage
import io.github.vitalyostanin.geoshare.collection.SLANT_MIN_SATELLITES
import io.github.vitalyostanin.geoshare.collection.Satellite
import io.github.vitalyostanin.geoshare.collection.SignalApart
import io.github.vitalyostanin.geoshare.collection.Slant
import io.github.vitalyostanin.geoshare.collection.androidType
import io.github.vitalyostanin.geoshare.collection.driftOf
import io.github.vitalyostanin.geoshare.collection.slantOf
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.Locale

/** What one sky file may reach before it rolls over; two files are kept. */
const val SKY_FILE_LIMIT_BYTES: Long = 2L * 1024 * 1024

/**
 * The sky as the receiver reported it, beside the position it worked out.
 *
 * Apart from the prose log because it is not prose: thirty rows an epoch, of
 * numbers a reader will want to sort and average rather than read. Apart also
 * because it can be thrown away — the prose log is what says what the
 * application did, and this is the evidence behind one line of it.
 *
 * A block per fix rather than per epoch. The receiver reports its satellites
 * about once a second, which is more than any question here is asked at; what
 * matters is what the sky looked like at the moment a position was worked out
 * from it, and that is what a block is.
 *
 * The rows Google's own logger writes are written as version 3.1 of it writes
 * them — the same names, the same fields in the same order, the system as a
 * number — so that the tools made for that logger read this file as it is.
 * The rows of this application's own sit beside them under names of their
 * own, which those tools pass over.
 *
 * Written from the main thread only, like [RadioWatch] that feeds it: the
 * drift row compares a block with the one kept before it, and that pair is
 * read and replaced without a lock.
 */
@MainThread
object SkyLog {
    @Volatile
    private var files: RollingFiles? = null

    // The epoch the next drift row is measured against, and when it was
    // written. Kept here because the row is about a pair of blocks rather than
    // about one: see Drift.kt.
    private var driftFrom: List<Satellite> = emptyList()
    private var driftFromAt: Long = 0L

    /**
     * Called once, from the application object.
     *
     * [device] is what the head of every file says the file came from: the
     * application's version, Android's, the device and its receiver, in the
     * words Google's logger uses for them.
     */
    fun start(directory: File, device: String = "") {
        driftFrom = emptyList()
        driftFromAt = 0L
        files =
            RollingFiles(
                directory = directory,
                currentName = "sky.csv",
                previousName = "sky-previous.csv",
                limitBytes = SKY_FILE_LIMIT_BYTES,
                threadName = "geoshare-sky",
                head = headOf(device).toByteArray(),
            )
    }

    /**
     * One block: the position, the receiver's clock, the gains it is running
     * its bands at, every satellite it can see and every signal it measured.
     *
     * The kinds of row are told apart by their first field, and every row
     * carries the same moment, so a block stays a block however the file is
     * later cut about.
     */
    fun note(
        at: Instant,
        fix: Fix,
        clock: Clock?,
        gains: List<Gain>,
        sky: List<Satellite>,
        measurements: List<Measurement>,
    ) {
        val stamp = at.toEpochMilli()

        note(
            buildString {
                fix(fix)
                epoch(stamp, clock, gains, sky, measurements)
            },
        )
    }

    /**
     * The same block, for an epoch that produced no position at all.
     *
     * The case worth recording most. A receiver that hears twenty satellites
     * and works out nothing from them is a receiver being talked over, and
     * until this existed such an epoch left no trace whatever: the block was
     * written beside a fix, so the devices with the most to say were the ones
     * that said nothing.
     *
     * The head row carries how long the silence has lasted and which mode the
     * device was collecting in, because a minute without an answer means one
     * thing at four fixes a minute and another at one an hour.
     */
    fun note(
        at: Instant,
        mode: String,
        silence: Duration,
        clock: Clock?,
        gains: List<Gain>,
        sky: List<Satellite>,
        measurements: List<Measurement>,
    ) {
        val stamp = at.toEpochMilli()

        note(
            buildString {
                unfixed(stamp, mode, silence)
                epoch(stamp, clock, gains, sky, measurements)
            },
        )
    }

    /**
     * One position on its own, with nothing else about the epoch.
     *
     * What the raw satellite provider answers, which arrives on its own terms
     * rather than beside a block: the whole point of asking it is to have the
     * satellites' own answer next to the one that was written.
     */
    fun note(at: Instant, fix: Fix) = note(buildString { fix(fix) })

    /**
     * One navigation message, written as it arrives rather than beside a fix.
     *
     * The satellites say what they say on their own schedule, a page every few
     * seconds, and a page that arrived while the receiver was working out
     * nothing is as much worth having as one that arrived beside a position.
     * What keeps the file from filling up with them is the caller, which
     * writes a page only when it differs from the last one written for that
     * satellite — see [io.github.vitalyostanin.geoshare.collection.NavSoFar].
     *
     * The row carries no moment of its own, as it does not in Google's file:
     * when it arrived is told by the blocks on either side of it.
     */
    fun note(message: NavMessage) = note(buildString { nav(message) })

    /**
     * The signals whose own time parted from one of the device's clocks.
     *
     * Only the ones that parted: writing a row per signal per epoch would say
     * the same "nothing happened" thirty times a second and push the rest of
     * the file out.
     */
    fun note(at: Instant, apart: List<SignalApart>) {
        if (apart.isEmpty()) {
            return
        }

        note(buildString { apart.forEach { apart(at.toEpochMilli(), it) } })
    }

    /** Both files, oldest row first. */
    fun read(): String = files?.read().orEmpty()

    private fun note(rows: String) {
        files?.append(rows.toByteArray())
    }

    /**
     * The epoch's own rows, the same whether a position came out of it or not.
     *
     * The clock goes into every raw row, as Google's logger puts it, so that a
     * row read on its own is enough to work out a distance from. A row of its
     * own only where there is no raw row to carry it: an epoch the receiver
     * measured nothing in still says whether its clock stepped.
     */
    private fun StringBuilder.epoch(
        stamp: Long,
        clock: Clock?,
        gains: List<Gain>,
        sky: List<Satellite>,
        measurements: List<Measurement>,
    ) {
        if (measurements.isEmpty()) {
            clock?.let { clock(stamp, it) }
        }

        gains.forEach { gain(stamp, clock, it) }
        slants(stamp, sky)
        drifts(stamp, sky)
        sky.forEachIndexed { index, satellite -> status(stamp, sky.size, index, satellite) }
        measurements.forEach { measurement(stamp, clock, it) }
    }

    private fun StringBuilder.fix(fix: Fix) {
        val said = fix.reported

        append("Fix,")
        append(providerOf(fix.from)).append(',')
        append(String.format(Locale.ROOT, "%.7f", fix.latitude)).append(',')
        append(String.format(Locale.ROOT, "%.7f", fix.longitude)).append(',')
        append(number(said?.altitudeMetres)).append(',')
        append(number(said?.speedMps)).append(',')
        append(number(fix.accuracyMetres)).append(',')
        append(number(said?.bearingDegrees)).append(',')
        append(fix.takenAt.toEpochMilli()).append(',')
        append(number(said?.speedAccuracyMps)).append(',')
        append(number(said?.bearingAccuracyDegrees)).append(',')
        append(fix.sinceBoot?.toNanos()?.toString().orEmpty()).append(',')
        append(number(said?.verticalAccuracyMetres)).append(',')
        append(if (said?.pretended == true) '1' else '0').append(',')
        // How many signals the position rests on, how sure the receiver is
        // of its vertical speed and what kind of solution it was: Google's
        // logger has them from its own receiver, the platform does not hand
        // them over.
        append(",,,")
        append(said?.alongside.orEmpty().replace(',', ' ')).append('\n')
    }

    private fun StringBuilder.unfixed(stamp: Long, mode: String, silence: Duration) {
        append("NoFix,").append(stamp).append(',')
        append(mode).append(',')
        append(silence.toMillis()).append('\n')
    }

    private fun StringBuilder.clock(stamp: Long, clock: Clock) {
        append("Clock,").append(stamp).append(',')
        clockFields(clock)
        append(clock.sinceBootNanos?.toString().orEmpty()).append(',')
        append(number(clock.sinceBootUncertaintyNanos)).append('\n')
    }

    /**
     * The receiver's clock as Google's logger puts it at the head of a raw row
     * and of a gain row, nine fields with a comma after each; nine empty ones
     * where the epoch came without a clock.
     */
    private fun StringBuilder.clockFields(clock: Clock?) {
        if (clock == null) {
            append(",".repeat(CLOCK_FIELDS))

            return
        }

        append(clock.timeNanos).append(',')
        append(clock.leapSecond?.toString().orEmpty()).append(',')
        append(number(clock.timeUncertaintyNanos)).append(',')
        append(clock.fullBiasNanos?.toString().orEmpty()).append(',')
        append(number(clock.biasNanos)).append(',')
        append(number(clock.biasUncertaintyNanos)).append(',')
        append(number(clock.driftNanosPerSecond)).append(',')
        append(number(clock.driftUncertaintyNanosPerSecond)).append(',')
        append(clock.discontinuities).append(',')
    }

    private fun StringBuilder.gain(stamp: Long, clock: Clock?, gain: Gain) {
        append("Agc,").append(stamp).append(',')
        clockFields(clock)
        append(number(gain.levelDb)).append(',')
        append(number(gain.carrierHz)).append(',')
        append(gain.constellation.androidType).append('\n')
    }

    /**
     * How the epoch's loudness stands against the height of the satellites,
     * one row per system.
     *
     * Worked out here rather than left to whoever reads the file, although
     * every number behind it is in the rows below: what it is for is to be
     * read beside the gain and the loudness without the reader having to
     * group the epoch first, and to be the same arithmetic the device itself
     * acted on. Only the systems with satellites enough to mean anything —
     * see [SLANT_MIN_SATELLITES].
     */
    private fun StringBuilder.slants(stamp: Long, sky: List<Satellite>) {
        for ((constellation, slant) in slantOf(sky)) {
            if (slant.satellites < SLANT_MIN_SATELLITES) {
                continue
            }

            slant(stamp, constellation, slant)
        }
    }

    /**
     * How far this epoch's satellites moved apart from the ones of a block a
     * few seconds back, system by system.
     *
     * Nothing where the two blocks stand too close together in time to have
     * moved or too far apart to be about one sky; the kept block is replaced
     * either way, so that a file picked up after an hour of silence measures
     * against the epoch beside it rather than against the hour.
     */
    private fun StringBuilder.drifts(stamp: Long, sky: List<Satellite>) {
        val apart = stamp - driftFromAt

        if (driftFromAt == 0L || apart > DRIFT_MOST_APART_MS) {
            driftFrom = sky
            driftFromAt = stamp

            return
        }

        if (apart < DRIFT_LEAST_APART_MS) {
            return
        }

        for ((constellation, drift) in driftOf(driftFrom, sky)) {
            drift(stamp, constellation, drift)
        }

        driftFrom = sky
        driftFromAt = stamp
    }

    private fun StringBuilder.drift(stamp: Long, constellation: Constellation, drift: Drift) {
        append("Drift,").append(stamp).append(',')
        append(constellation.androidType).append(',')
        append(drift.satellites).append(',')
        append(number(drift.shiftDb)).append(',')
        append(number(drift.apartDb)).append('\n')
    }

    private fun StringBuilder.slant(stamp: Long, constellation: Constellation, slant: Slant) {
        append("Slant,").append(stamp).append(',')
        append(constellation.androidType).append(',')
        append(slant.satellites).append(',')
        append(number(slant.meanDb)).append(',')
        append(number(slant.spreadDb)).append(',')
        append(number(slant.slopeDb)).append(',')
        append(number(slant.highLessLowDb)).append('\n')
    }

    private fun StringBuilder.status(stamp: Long, count: Int, index: Int, satellite: Satellite) {
        append("Status,").append(stamp).append(',')
        append(count).append(',')
        append(index).append(',')
        append(satellite.constellation.androidType).append(',')
        append(satellite.id).append(',')
        append(number(satellite.carrierHz)).append(',')
        append(number(satellite.loudness)).append(',')
        append(number(satellite.azimuthDegrees)).append(',')
        append(number(satellite.elevationDegrees)).append(',')
        append(if (satellite.usedInFix) '1' else '0').append(',')
        append(if (satellite.hasAlmanac) '1' else '0').append(',')
        append(if (satellite.hasEphemeris) '1' else '0').append(',')
        append(number(satellite.basebandLoudness)).append('\n')
    }

    private fun StringBuilder.apart(stamp: Long, said: SignalApart) {
        append("Apart,").append(stamp).append(',')
        append(said.signal.constellation.androidType).append(',')
        append(said.signal.id).append(',')
        append(number(said.signal.carrierHz)).append(',')
        append(said.apart.receiverNanos).append(',')
        append(said.apart.sinceBootNanos).append(',')
        append(said.verdict.name).append('\n')
    }

    private fun StringBuilder.measurement(stamp: Long, clock: Clock?, measured: Measurement) {
        append("Raw,").append(stamp).append(',')
        clockFields(clock)
        append(measured.id).append(',')
        append(number(measured.timeOffsetNanos)).append(',')
        append(measured.state).append(',')
        append(measured.receivedSvTimeNanos).append(',')
        append(measured.receivedSvTimeUncertaintyNanos).append(',')
        append(number(measured.loudness)).append(',')
        append(number(measured.pseudorangeRateMps)).append(',')
        append(number(measured.pseudorangeRateUncertaintyMps)).append(',')
        append(measured.deltaRangeState).append(',')
        append(number(measured.deltaRangeMetres)).append(',')
        append(number(measured.deltaRangeUncertaintyMetres)).append(',')
        append(number(measured.carrierHz)).append(',')
        // The carrier cycles, the phase and how sure of it the receiver is:
        // withdrawn from the platform long ago, kept empty for the columns.
        append(",,,")
        append(measured.multipath).append(',')
        append(number(measured.snrDb)).append(',')
        append(measured.constellation.androidType).append(',')
        append(number(measured.gainDb)).append(',')
        append(number(measured.basebandLoudness)).append(',')
        append(number(measured.fullInterSignalBiasNanos)).append(',')
        append(number(measured.fullInterSignalBiasUncertaintyNanos)).append(',')
        append(number(measured.satelliteInterSignalBiasNanos)).append(',')
        append(number(measured.satelliteInterSignalBiasUncertaintyNanos)).append(',')
        append(measured.codeType.orEmpty()).append(',')
        append(clock?.sinceBootNanos?.toString().orEmpty()).append(',')
        append(measured.fullTracking?.let { if (it) "1" else "0" }.orEmpty())
        // Where the satellite is, how fast it moves, its clock and the
        // ionosphere's coefficients: what the receiver worked out for itself,
        // which the platform hands over on few devices and is not asked for
        // here.
        append(",".repeat(SATELLITE_FIELDS)).append('\n')
    }

    /**
     * The words as Google's logger writes them: a byte to a field, as a
     * signed decimal number, so the row is as long as the message is.
     */
    private fun StringBuilder.nav(message: NavMessage) {
        append("Nav,")
        append(message.id).append(',')
        append(message.type).append(',')
        append(message.status).append(',')
        append(message.messageId).append(',')
        append(message.submessageId)

        message.data.chunked(2).forEach { byte -> append(',').append(byte.toInt(HEX).toByte()) }

        append('\n')
    }

    /**
     * The provider in the word Google's logger uses for it, which is the word
     * the tools made for that logger look up; any other kept as Android
     * names it.
     */
    private fun providerOf(from: String?): String = when (from) {
        "gps" -> "GPS"
        "fused" -> "FLP"
        "network" -> "NLP"
        else -> from.orEmpty()
    }

    /** A number the way a spreadsheet wants it, or nothing where there is none. */
    private fun number(value: Double?): String =
        value?.let { String.format(Locale.ROOT, "%.3f", it) }.orEmpty()

    /**
     * What the columns are, at the head of every file.
     *
     * Laid out the way Google's logger lays out its own, raw row first: the
     * tools made for that logger take the column names from the first head
     * row naming "Raw" and read no further down the head than that.
     */
    private fun headOf(device: String): String =
        """
        #
        # Header Description:
        #
        # Version: $device
        #
        # Raw,utcTimeMillis,TimeNanos,LeapSecond,TimeUncertaintyNanos,FullBiasNanos,BiasNanos,BiasUncertaintyNanos,DriftNanosPerSecond,DriftUncertaintyNanosPerSecond,HardwareClockDiscontinuityCount,Svid,TimeOffsetNanos,State,ReceivedSvTimeNanos,ReceivedSvTimeUncertaintyNanos,Cn0DbHz,PseudorangeRateMetersPerSecond,PseudorangeRateUncertaintyMetersPerSecond,AccumulatedDeltaRangeState,AccumulatedDeltaRangeMeters,AccumulatedDeltaRangeUncertaintyMeters,CarrierFrequencyHz,CarrierCycles,CarrierPhase,CarrierPhaseUncertainty,MultipathIndicator,SnrInDb,ConstellationType,AgcDb,BasebandCn0DbHz,FullInterSignalBiasNanos,FullInterSignalBiasUncertaintyNanos,SatelliteInterSignalBiasNanos,SatelliteInterSignalBiasUncertaintyNanos,CodeType,ChipsetElapsedRealtimeNanos,IsFullTracking,SvPositionEcefXMeters,SvPositionEcefYMeters,SvPositionEcefZMeters,SvVelocityEcefXMetersPerSecond,SvVelocityEcefYMetersPerSecond,SvVelocityEcefZMetersPerSecond,SvClockBiasMeters,SvClockDriftMetersPerSecond,KlobucharAlpha0,KlobucharAlpha1,KlobucharAlpha2,KlobucharAlpha3,KlobucharBeta0,KlobucharBeta1,KlobucharBeta2,KlobucharBeta3
        #
        # Fix,Provider,LatitudeDegrees,LongitudeDegrees,AltitudeMeters,SpeedMps,AccuracyMeters,BearingDegrees,UnixTimeMillis,SpeedAccuracyMps,BearingAccuracyDegrees,elapsedRealtimeNanos,VerticalAccuracyMeters,MockLocation,NumberOfUsedSignals,VerticalSpeedAccuracyMps,SolutionType,ExtraKeyValuePairs
        #
        # Nav,Svid,Type,Status,MessageId,Sub-messageId,Data(Bytes)
        #
        # Status,UnixTimeMillis,SignalCount,SignalIndex,ConstellationType,Svid,CarrierFrequencyHz,Cn0DbHz,AzimuthDegrees,ElevationDegrees,UsedInFix,HasAlmanacData,HasEphemerisData,BasebandCn0DbHz
        #
        # Agc,utcTimeMillis,TimeNanos,LeapSecond,TimeUncertaintyNanos,FullBiasNanos,BiasNanos,BiasUncertaintyNanos,DriftNanosPerSecond,DriftUncertaintyNanosPerSecond,HardwareClockDiscontinuityCount,AgcDb,CarrierFrequencyHz,ConstellationType
        #
        # NoFix,UnixTimeMillis,Mode,SilenceMillis
        # Clock,UnixTimeMillis,TimeNanos,LeapSecond,TimeUncertaintyNanos,FullBiasNanos,BiasNanos,BiasUncertaintyNanos,DriftNanosPerSecond,DriftUncertaintyNanosPerSecond,HardwareClockDiscontinuityCount,ElapsedRealtimeNanos,ElapsedRealtimeUncertaintyNanos
        # Slant,UnixTimeMillis,ConstellationType,Satellites,MeanCn0DbHz,SpreadDbHz,SlopeDbPerSineOfElevation,HighLessLowDbHz
        # Drift,UnixTimeMillis,ConstellationType,Satellites,ShiftDbHz,ApartDbHz
        # Apart,UnixTimeMillis,ConstellationType,Svid,CarrierFrequencyHz,AgainstReceiverNanos,AgainstPhoneNanos,Verdict
        #
        """.trimIndent() + "\n"

    /** The fields of the receiver's clock a raw row and a gain row carry. */
    private const val CLOCK_FIELDS = 9

    /** The satellite's own position, speed, clock and ionosphere, left empty. */
    private const val SATELLITE_FIELDS = 16

    private const val HEX = 16
}
