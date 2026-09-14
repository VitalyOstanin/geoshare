package io.github.vitalyostanin.geoshare.log

import io.github.vitalyostanin.geoshare.collection.Clock
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.Gain
import io.github.vitalyostanin.geoshare.collection.Measurement
import io.github.vitalyostanin.geoshare.collection.Satellite
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

    /** Called once, from the application object. */
    fun start(directory: File) {
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
        append(clock.discontinuities).append('\n')
    }

    private fun StringBuilder.gain(stamp: Long, gain: Gain) {
        append("Agc,").append(stamp).append(',')
        append(gain.constellation.word).append(',')
        append(number(gain.carrierHz)).append(',')
        append(number(gain.levelDb)).append('\n')
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
        append(number(measured.gainDb)).append('\n')
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
        # Clock,UnixTimeMillis,TimeNanos,LeapSecond,TimeUncertaintyNanos,FullBiasNanos,BiasNanos,BiasUncertaintyNanos,DriftNanosPerSecond,DriftUncertaintyNanosPerSecond,HardwareClockDiscontinuityCount
        # Agc,UnixTimeMillis,ConstellationType,CarrierFrequencyHz,AgcDb
        # Sat,UnixTimeMillis,ConstellationType,Svid,Cn0DbHz,BasebandCn0DbHz,ElevationDegrees,AzimuthDegrees,CarrierFrequencyHz,UsedInFix,HasEphemerisData,HasAlmanacData
        # Raw,UnixTimeMillis,ConstellationType,Svid,TimeOffsetNanos,State,ReceivedSvTimeNanos,ReceivedSvTimeUncertaintyNanos,Cn0DbHz,BasebandCn0DbHz,PseudorangeRateMetersPerSecond,PseudorangeRateUncertaintyMetersPerSecond,AccumulatedDeltaRangeState,AccumulatedDeltaRangeMeters,AccumulatedDeltaRangeUncertaintyMeters,CarrierFrequencyHz,MultipathIndicator,AgcDb
        """.trimIndent() + "\n"
}
