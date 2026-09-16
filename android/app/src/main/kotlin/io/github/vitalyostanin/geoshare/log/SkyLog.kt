package io.github.vitalyostanin.geoshare.log

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
import io.github.vitalyostanin.geoshare.collection.driftOf
import io.github.vitalyostanin.geoshare.collection.slantOf
import io.github.vitalyostanin.geoshare.collection.word
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
 * The rows are the ones Google's own logger writes, under the same names and
 * carrying the same fields, so that a question answered from one file can be
 * asked of the other.
 */
object SkyLog {
    @Volatile
    private var files: RollingFiles? = null

    // The epoch the next drift row is measured against, and when it was
    // written. Kept here because the row is about a pair of blocks rather than
    // about one: see Drift.kt.
    private var driftFrom: List<Satellite> = emptyList()
    private var driftFromAt: Long = 0L

    /** Called once, from the application object. */
    fun start(directory: File) {
        driftFrom = emptyList()
        driftFromAt = 0L
        files =
            RollingFiles(
                directory = directory,
                currentName = "sky.csv",
                previousName = "sky-previous.csv",
                limitBytes = SKY_FILE_LIMIT_BYTES,
                threadName = "geoshare-sky",
            )

        note(HEADER)
    }

    /**
     * One block: the position, the receiver's clock, the gains it is running
     * its bands at, every satellite it can see and every signal it measured.
     *
     * The kinds of row are told apart by their first field, and every row
     * carries the same moment, so a block stays a block however the file is
     * later cut about.
     */
    @Suppress("LongParameterList")
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
                fix(stamp, fix)
                clock?.let { clock(stamp, it) }
                gains.forEach { gain(stamp, it) }
                slants(stamp, sky)
                drifts(stamp, sky)
                sky.forEach { satellite(stamp, it) }
                measurements.forEach { measurement(stamp, it) }
            },
        )
    }

    /**
     * The same block, for an epoch that produced no position at all.
     *
     * The case worth recording most. A receiver that hears twenty satellites
     * and works out nothing from them is a receiver being talked over, and
     * until this existed such an epoch left no trace whatever: the block was
     * written beside a fix, so the phones with the most to say were the ones
     * that said nothing.
     *
     * The head row carries how long the silence has lasted and which mode the
     * phone was collecting in, because a minute without an answer means one
     * thing at four fixes a minute and another at one an hour.
     */
    @Suppress("LongParameterList")
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
                clock?.let { clock(stamp, it) }
                gains.forEach { gain(stamp, it) }
                slants(stamp, sky)
                drifts(stamp, sky)
                sky.forEach { satellite(stamp, it) }
                measurements.forEach { measurement(stamp, it) }
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
    fun note(at: Instant, fix: Fix) = note(buildString { fix(at.toEpochMilli(), fix) })

    /**
     * One navigation message, written as it arrives rather than beside a fix.
     *
     * The satellites say what they say on their own schedule, a page every few
     * seconds, and a page that arrived while the receiver was working out
     * nothing is as much worth having as one that arrived beside a position.
     * What keeps the file from filling up with them is the caller, which
     * writes a page only when it differs from the last one written for that
     * satellite — see [io.github.vitalyostanin.geoshare.collection.NavSoFar].
     */
    fun note(at: Instant, message: NavMessage) =
        note(buildString { nav(at.toEpochMilli(), message) })

    /**
     * The signals whose own time parted from one of the phone's clocks.
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

    private fun StringBuilder.fix(stamp: Long, fix: Fix) {
        val said = fix.reported

        append("Fix,").append(stamp).append(',')
        append(fix.from.orEmpty()).append(',')
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
        append(said?.alongside.orEmpty().replace(',', ' ')).append('\n')
    }

    private fun StringBuilder.unfixed(stamp: Long, mode: String, silence: Duration) {
        append("NoFix,").append(stamp).append(',')
        append(mode).append(',')
        append(silence.toMillis()).append('\n')
    }

    private fun StringBuilder.clock(stamp: Long, clock: Clock) {
        append("Clock,").append(stamp).append(',')
        append(clock.timeNanos).append(',')
        append(clock.leapSecond?.toString().orEmpty()).append(',')
        append(number(clock.timeUncertaintyNanos)).append(',')
        append(clock.fullBiasNanos?.toString().orEmpty()).append(',')
        append(number(clock.biasNanos)).append(',')
        append(number(clock.biasUncertaintyNanos)).append(',')
        append(number(clock.driftNanosPerSecond)).append(',')
        append(number(clock.driftUncertaintyNanosPerSecond)).append(',')
        append(clock.discontinuities).append(',')
        append(clock.sinceBootNanos?.toString().orEmpty()).append(',')
        append(number(clock.sinceBootUncertaintyNanos)).append('\n')
    }

    private fun StringBuilder.gain(stamp: Long, gain: Gain) {
        append("Agc,").append(stamp).append(',')
        append(gain.constellation.word).append(',')
        append(number(gain.carrierHz)).append(',')
        append(number(gain.levelDb)).append('\n')
    }

    /**
     * How the epoch's loudness stands against the height of the satellites,
     * one row per system.
     *
     * Worked out here rather than left to whoever reads the file, although
     * every number behind it is in the rows below: what it is for is to be
     * read beside the gain and the loudness without the reader having to
     * group the epoch first, and to be the same arithmetic the phone itself
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
        append(constellation.word).append(',')
        append(drift.satellites).append(',')
        append(number(drift.shiftDb)).append(',')
        append(number(drift.apartDb)).append('\n')
    }

    private fun StringBuilder.slant(stamp: Long, constellation: Constellation, slant: Slant) {
        append("Slant,").append(stamp).append(',')
        append(constellation.word).append(',')
        append(slant.satellites).append(',')
        append(number(slant.meanDb)).append(',')
        append(number(slant.spreadDb)).append(',')
        append(number(slant.slopeDb)).append(',')
        append(number(slant.highLessLowDb)).append('\n')
    }

    private fun StringBuilder.satellite(stamp: Long, satellite: Satellite) {
        append("Sat,").append(stamp).append(',')
        append(satellite.constellation.word).append(',')
        append(satellite.id).append(',')
        append(number(satellite.loudness)).append(',')
        append(number(satellite.basebandLoudness)).append(',')
        append(number(satellite.elevationDegrees)).append(',')
        append(number(satellite.azimuthDegrees)).append(',')
        append(number(satellite.carrierHz)).append(',')
        append(if (satellite.usedInFix) '1' else '0').append(',')
        append(if (satellite.hasEphemeris) '1' else '0').append(',')
        append(if (satellite.hasAlmanac) '1' else '0').append('\n')
    }

    private fun StringBuilder.apart(stamp: Long, said: SignalApart) {
        append("Apart,").append(stamp).append(',')
        append(said.signal.constellation.word).append(',')
        append(said.signal.id).append(',')
        append(number(said.signal.carrierHz)).append(',')
        append(said.apart.receiverNanos).append(',')
        append(said.apart.sinceBootNanos).append(',')
        append(said.verdict.name).append('\n')
    }

    private fun StringBuilder.measurement(stamp: Long, measured: Measurement) {
        append("Raw,").append(stamp).append(',')
        append(measured.constellation.word).append(',')
        append(measured.id).append(',')
        append(number(measured.timeOffsetNanos)).append(',')
        append(measured.state).append(',')
        append(measured.receivedSvTimeNanos).append(',')
        append(measured.receivedSvTimeUncertaintyNanos).append(',')
        append(number(measured.loudness)).append(',')
        append(number(measured.basebandLoudness)).append(',')
        append(number(measured.pseudorangeRateMps)).append(',')
        append(number(measured.pseudorangeRateUncertaintyMps)).append(',')
        append(measured.deltaRangeState).append(',')
        append(number(measured.deltaRangeMetres)).append(',')
        append(number(measured.deltaRangeUncertaintyMetres)).append(',')
        append(number(measured.carrierHz)).append(',')
        append(measured.multipath).append(',')
        append(number(measured.gainDb)).append(',')
        append(number(measured.snrDb)).append(',')
        append(measured.codeType.orEmpty()).append(',')
        append(number(measured.fullInterSignalBiasNanos)).append(',')
        append(number(measured.fullInterSignalBiasUncertaintyNanos)).append(',')
        append(number(measured.satelliteInterSignalBiasNanos)).append(',')
        append(number(measured.satelliteInterSignalBiasUncertaintyNanos)).append(',')
        append(measured.fullTracking?.let { if (it) "1" else "0" }.orEmpty()).append('\n')
    }

    private fun StringBuilder.nav(stamp: Long, message: NavMessage) {
        append("Nav,").append(stamp).append(',')
        append(message.constellation.word).append(',')
        append(message.id).append(',')
        append(message.type).append(',')
        append(message.status).append(',')
        append(message.messageId).append(',')
        append(message.submessageId).append(',')
        append(message.data).append('\n')
    }

    /** A number the way a spreadsheet wants it, or nothing where there is none. */
    private fun number(value: Double?): String =
        value?.let { String.format(Locale.ROOT, "%.3f", it) }.orEmpty()

    /**
     * What the columns are, written at the head of every file.
     *
     * Written on every start rather than on a fresh file: a rollover cuts the
     * head off, and a reader who gets the second file alone would otherwise
     * have to know the columns by heart.
     */
    private val HEADER =
        """
        # Fix,UnixTimeMillis,Provider,LatitudeDegrees,LongitudeDegrees,AltitudeMeters,SpeedMps,AccuracyMeters,BearingDegrees,TakenAtMillis,SpeedAccuracyMps,BearingAccuracyDegrees,ElapsedRealtimeNanos,VerticalAccuracyMeters,MockLocation,ExtraKeyValuePairs
        # NoFix,UnixTimeMillis,Mode,SilenceMillis
        # Clock,UnixTimeMillis,TimeNanos,LeapSecond,TimeUncertaintyNanos,FullBiasNanos,BiasNanos,BiasUncertaintyNanos,DriftNanosPerSecond,DriftUncertaintyNanosPerSecond,HardwareClockDiscontinuityCount,ElapsedRealtimeNanos,ElapsedRealtimeUncertaintyNanos
        # Agc,UnixTimeMillis,ConstellationType,CarrierFrequencyHz,AgcDb
        # Slant,UnixTimeMillis,ConstellationType,Satellites,MeanCn0DbHz,SpreadDbHz,SlopeDbPerSineOfElevation,HighLessLowDbHz
        # Drift,UnixTimeMillis,ConstellationType,Satellites,ShiftDbHz,ApartDbHz
        # Sat,UnixTimeMillis,ConstellationType,Svid,Cn0DbHz,BasebandCn0DbHz,ElevationDegrees,AzimuthDegrees,CarrierFrequencyHz,UsedInFix,HasEphemerisData,HasAlmanacData
        # Raw,UnixTimeMillis,ConstellationType,Svid,TimeOffsetNanos,State,ReceivedSvTimeNanos,ReceivedSvTimeUncertaintyNanos,Cn0DbHz,BasebandCn0DbHz,PseudorangeRateMetersPerSecond,PseudorangeRateUncertaintyMetersPerSecond,AccumulatedDeltaRangeState,AccumulatedDeltaRangeMeters,AccumulatedDeltaRangeUncertaintyMeters,CarrierFrequencyHz,MultipathIndicator,AgcDb,SnrInDb,CodeType,FullInterSignalBiasNanos,FullInterSignalBiasUncertaintyNanos,SatelliteInterSignalBiasNanos,SatelliteInterSignalBiasUncertaintyNanos,IsFullTracking
        # Nav,UnixTimeMillis,ConstellationType,Svid,Type,Status,MessageId,SubmessageId,DataHex
        # Apart,UnixTimeMillis,ConstellationType,Svid,CarrierFrequencyHz,AgainstReceiverNanos,AgainstPhoneNanos,Verdict
        """.trimIndent() + "\n"
}
