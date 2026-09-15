package io.github.vitalyostanin.geoshare.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssMeasurementsEvent
import android.location.GnssNavigationMessage
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import io.github.vitalyostanin.geoshare.collection.Clock
import io.github.vitalyostanin.geoshare.collection.Constellation
import io.github.vitalyostanin.geoshare.collection.Counted
import io.github.vitalyostanin.geoshare.collection.Epochs
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.Gain
import io.github.vitalyostanin.geoshare.collection.Interference
import io.github.vitalyostanin.geoshare.collection.Measurement
import io.github.vitalyostanin.geoshare.collection.NavMessage
import io.github.vitalyostanin.geoshare.collection.NavSoFar
import io.github.vitalyostanin.geoshare.collection.SHARE_THE_GPS_BAND
import io.github.vitalyostanin.geoshare.collection.Satellite
import io.github.vitalyostanin.geoshare.collection.acrossTheBand
import io.github.vitalyostanin.geoshare.collection.counted
import io.github.vitalyostanin.geoshare.collection.gainPerBand
import io.github.vitalyostanin.geoshare.collection.hexOf
import io.github.vitalyostanin.geoshare.collection.inOrder
import io.github.vitalyostanin.geoshare.collection.interferenceFrom
import io.github.vitalyostanin.geoshare.collection.loudnessPerBand
import io.github.vitalyostanin.geoshare.collection.navConstellation
import io.github.vitalyostanin.geoshare.collection.reported
import io.github.vitalyostanin.geoshare.collection.whatTheBandsSay
import io.github.vitalyostanin.geoshare.collection.word
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.log.SkyLog
import java.time.Duration
import java.time.Instant
import java.util.Locale

private const val TAG = "geoshare.radio"

/**
 * How many epochs are held at once.
 *
 * Ten recent against fifty before them, which is what Google's own logger
 * compares. At one epoch a second that is a minute of history: long enough
 * that the earlier half is what the radio was like before whatever is
 * happening started, short enough that walking indoors does not look like the
 * start of an attack for the next quarter of an hour.
 */
private const val RECENT_EPOCHS = 10
private const val EARLIER_EPOCHS = 50

/** How often the watch admits to having nothing, when it has nothing. */
private val SILENCE_REPORTED_EVERY: Duration = Duration.ofMinutes(5)

/**
 * How often the log says how much the satellites have said.
 *
 * Said on its own schedule rather than beside a fix: the messages arrive
 * whether or not a position comes out of them, and on the phones this was
 * written for the fixes are the rare part.
 */
private val NAVIGATION_REPORTED_EVERY: Duration = Duration.ofMinutes(5)

/**
 * How old the last epoch may be and still be taken as being about a fix.
 *
 * Five seconds. The status arrives about once a second while the receiver is
 * running, so anything older than this is a receiver that has stopped
 * reporting rather than one reporting about this moment — and the rule read
 * off the epoch, which asks what a fix was built out of, would then be
 * answering about some other minute.
 */
private val SKY_STILL_CURRENT: Duration = Duration.ofSeconds(5)

/**
 * What the radio is being subjected to, as the receiver's own measures say.
 *
 * Subscriptions the phone already has the permission for, none of which asks
 * for a position: the satellites it can hear and how loud, the gain its
 * amplifier is running at, and the messages the satellites themselves are
 * broadcasting. The first pair is what separates a jammed receiver from a
 * spoofed one — see [interferenceFrom] for which way each moves — and neither
 * of the two alone says much. The third is not a measure at all but the
 * content of the signal: see
 * [io.github.vitalyostanin.geoshare.collection.NavMessage].
 *
 * Both are kept apart by the system the satellites belong to, which is a third
 * thing to read and the only one that names where the interference is coming
 * from: see [whatTheBandsSay]. The gain is reported per band as well as the
 * loudness, and the two are read the same way.
 *
 * The measurements arrive whether or not a position comes out of them, which
 * is the case this exists for: a phone that has gone silent is a phone with
 * nothing to write down, and this is what can still be written.
 */
class RadioWatch(private val context: Context) {
    // Kept apart by the system the satellites belong to, because the answer
    // this exists for is a disagreement between them: averaged together, a
    // band gone loud and a band holding still come out as a sky that has not
    // moved. See Constellations.kt for which bands those are.
    private val heard = mutableMapOf<Constellation, ArrayDeque<Double>>()
    private val gains = mutableMapOf<Constellation, ArrayDeque<Double>>()

    // The last epoch of each, kept whole rather than averaged: the averages
    // answer whether anything is going on, and these are what a person looks
    // at afterwards to see what it was. Written to their own file per fix.
    @Volatile
    private var sky: List<Satellite> = emptyList()

    // When that epoch arrived, by the phone's own clock. Kept because the
    // epoch is read as a statement about a fix, and a statement about a fix
    // has to have been made at about the same time as the fix.
    @Volatile
    private var skyAt: Instant = Instant.EPOCH

    @Volatile
    private var running: List<Gain> = emptyList()

    @Volatile
    private var measured: List<Measurement> = emptyList()

    @Volatile
    private var clock: Clock? = null

    // What the satellites have said so far, kept rather than written straight
    // out: a page repeated every thirty seconds would fill the sky file and
    // push out everything else in it. See NavSoFar for what counts as new.
    private val navigation = NavSoFar()

    private var saidNothingAt: Instant = Instant.EPOCH
    private var saidNavigationAt: Instant = Instant.EPOCH
    private var status: GnssStatus.Callback? = null
    private var measurements: GnssMeasurementsEvent.Callback? = null
    private var navMessages: GnssNavigationMessage.Callback? = null

    /**
     * What the last minute of measurements adds up to.
     *
     * The bands are asked first. They answer less often — a phone hearing one
     * system has nothing to compare — but when they do answer they name the
     * band whatever is going on is going on in, and they answer without the
     * gain, which some phones never report at all.
     */
    fun verdict(): Interference {
        val bands = windows(heard)

        return whatTheBandsSay(sharedBand(bands), bands[Constellation.Glonass] ?: NOTHING)
            ?: interferenceFrom(
                gain = acrossTheBand(windows(gains).values),
                noise = acrossTheBand(bands.values),
            )
    }

    /**
     * The same, in the words of the log, or nothing when there is nothing to
     * say: a line per fix saying "unknown" would bury the ones that matter.
     *
     * Silence is reported too, but rarely. Not every receiver reports its
     * gain at all, and a verdict that never arrives is indistinguishable from
     * a radio nobody is interfering with — which is the one thing this must
     * not be mistaken for. So once in a while it says how much it has.
     */
    fun saying(): String? {
        val verdict = verdict()

        if (verdict == Interference.Unknown) {
            return whatIsMissing()
        }

        val loudness = windows(heard)
        val gain = windows(gains)

        return String.format(
            Locale.ROOT,
            "the radio says %s (gain: the GPS band %s, GLONASS %s; " +
                "loudness: the GPS band %s, GLONASS %s)",
            verdict.toString().lowercase(),
            moved(sharedBand(gain)),
            moved(gain[Constellation.Glonass] ?: NOTHING),
            moved(sharedBand(loudness)),
            moved(loudness[Constellation.Glonass] ?: NOTHING),
        )
    }

    /**
     * How many satellites each system has in the sky and how many of them the
     * receiver's own position rests on.
     *
     * The second number is the one worth having. A receiver handed a false sky
     * builds its answer out of the satellites it was handed, so a system with
     * eight in view and none of them in the fix is a system that is being
     * talked over — and a fix resting on one system alone is a fix with
     * nothing to check itself against.
     */
    fun satellitesSaying(): String? {
        val counts = counted(sky)

        if (counts.isEmpty()) {
            return null
        }

        val said =
            inOrder(counts.keys).joinToString(", ") { system ->
                val count = counts[system] ?: Counted(heard = 0, usedInFix = 0)

                "${system.word} ${count.usedInFix} of ${count.heard}"
            }

        return "the fix rests on $said"
    }

    /**
     * How much the satellites have actually said, and which of them.
     *
     * Said rarely, and said even when the answer is none. Whether a chipset
     * hands these over at all is what the subscription exists to find out, and
     * a file with no such rows in it cannot tell "nothing arrived" from
     * "nobody asked": this is the line that tells them apart.
     */
    fun navigationSaying(): String? {
        val now = Instant.now()

        if (Duration.between(saidNavigationAt, now) < NAVIGATION_REPORTED_EVERY) {
            return null
        }

        saidNavigationAt = now

        return navigation.saying()
    }

    /**
     * What the last epoch says the fix rests on, system by system.
     *
     * Empty rather than stale: the counts are read as a statement about the
     * fix beside them — see
     * [io.github.vitalyostanin.geoshare.collection.restsOnOneBand] — and an
     * epoch from a minute ago is a statement about a minute ago. Empty as well
     * where the receiver has said nothing at all, which is the same answer for
     * the same reason.
     */
    fun restingOn(now: Instant = Instant.now()): Map<Constellation, Counted> {
        if (Duration.between(skyAt, now).abs() > SKY_STILL_CURRENT) {
            return emptyMap()
        }

        return counted(sky)
    }

    /**
     * The epoch itself, put beside the position that came out of it.
     *
     * Called once per fix rather than once per epoch: see [SkyLog] for why.
     */
    fun noteSky(at: Instant, fix: Fix) = SkyLog.note(
        at = at,
        fix = fix,
        clock = clock,
        gains = running,
        sky = sky,
        measurements = measured,
    )

    /**
     * The same epoch, put down while no position is coming out of it.
     *
     * A receiver hearing twenty satellites and answering nothing is the case
     * the whole file exists for, and it is exactly the case that used to leave
     * no rows at all.
     */
    fun noteSkyWithoutFix(at: Instant, mode: String, silence: Duration) = SkyLog.note(
        at = at,
        mode = mode,
        silence = silence,
        clock = clock,
        gains = running,
        sky = sky,
        measurements = measured,
    )

    /**
     * Subscribes to all three, on the handler rather than on an executor.
     *
     * The executor overloads arrive with Android 11, and the phone this was
     * written for — the one that goes silent for hours at a time — runs
     * Android 10. The handler overloads are older than anything the
     * application supports, and the callbacks do nothing but average a list.
     */
    @SuppressLint("MissingPermission")
    fun start() {
        val manager = context.getSystemService(LocationManager::class.java) ?: return
        val handler = Handler(Looper.getMainLooper())

        status =
            object : GnssStatus.Callback() {
                override fun onSatelliteStatusChanged(status: GnssStatus) {
                    val epoch = skyOf(status)

                    sky = epoch
                    skyAt = Instant.now()

                    for ((constellation, loudness) in loudnessPerBand(epoch)) {
                        note(heard.getOrPut(constellation) { ArrayDeque() }, loudness)
                    }
                }
            }.also { manager.registerGnssStatusCallback(it, handler) }

        measurements =
            object : GnssMeasurementsEvent.Callback() {
                override fun onGnssMeasurementsReceived(event: GnssMeasurementsEvent) {
                    clock = clockOf(event)
                    val tracking = fullTrackingOf(event)

                    measured = event.measurements.map { measurementOf(it, tracking) }

                    val epoch = reported(gainsOf(event))

                    if (epoch.isEmpty()) {
                        return
                    }

                    running = epoch

                    for ((constellation, level) in gainPerBand(epoch)) {
                        note(gains.getOrPut(constellation) { ArrayDeque() }, level)
                    }
                }
            }.also { manager.registerGnssMeasurementsCallback(it, handler) }

        navMessages =
            object : GnssNavigationMessage.Callback() {
                override fun onGnssNavigationMessageReceived(message: GnssNavigationMessage) {
                    val said = navMessageOf(message)

                    if (navigation.worthWriting(said)) {
                        SkyLog.note(Instant.now(), said)
                    }
                }
            }.also { manager.registerGnssNavigationMessageCallback(it, handler) }

        AppLog.i(TAG, "watching the radio")
        AppLog.i(TAG, whatTheChipsetSays(manager))
    }

    fun stop() {
        val manager = context.getSystemService(LocationManager::class.java) ?: return

        status?.let { manager.unregisterGnssStatusCallback(it) }
        measurements?.let { manager.unregisterGnssMeasurementsCallback(it) }
        navMessages?.let { manager.unregisterGnssNavigationMessageCallback(it) }

        status = null
        measurements = null
        navMessages = null
    }

    /**
     * The averages the verdict is read from.
     *
     * Nothing is reported until both halves are full: a comparison against
     * three epochs of history would call every phone leaving a building an
     * attack.
     */
    private fun window(of: ArrayDeque<Double>): Epochs {
        if (of.size < RECENT_EPOCHS + EARLIER_EPOCHS) {
            return Epochs(recent = null, earlier = null)
        }

        val kept = of.toList()

        return Epochs(
            recent = kept.takeLast(RECENT_EPOCHS).average(),
            earlier = kept.dropLast(RECENT_EPOCHS).average(),
        )
    }

    private fun whatIsMissing(): String? {
        val now = Instant.now()

        if (Duration.between(saidNothingAt, now) < SILENCE_REPORTED_EVERY) {
            return null
        }

        saidNothingAt = now

        val needed = RECENT_EPOCHS + EARLIER_EPOCHS
        val gain = SHARE_THE_GPS_BAND.maxOf { gains[it]?.size ?: 0 }
        val shared = SHARE_THE_GPS_BAND.maxOf { heard[it]?.size ?: 0 }
        val glonass = heard[Constellation.Glonass]?.size ?: 0

        return "the radio has nothing to go on: $gain epochs of gain, " +
            "$shared of the GPS band, $glonass of GLONASS, $needed needed"
    }

    /** Every system's history, each read as its own pair of averages. */
    private fun windows(of: Map<Constellation, ArrayDeque<Double>>): Map<Constellation, Epochs> =
        of.mapValues { window(it.value) }

    /** The three systems that transmit where GPS does, read as one band. */
    private fun sharedBand(bands: Map<Constellation, Epochs>): Epochs =
        acrossTheBand(bands.filterKeys { it in SHARE_THE_GPS_BAND }.values)

    private fun note(into: ArrayDeque<Double>, value: Double) {
        into.addLast(value)

        while (into.size > RECENT_EPOCHS + EARLIER_EPOCHS) {
            into.removeFirst()
        }
    }
}

/** A band with no history yet, which is what having heard nothing looks like. */
private val NOTHING = Epochs(recent = null, earlier = null)

/**
 * The epoch as the receiver reported it, every satellite it named.
 *
 * The ones it cannot hear are kept rather than dropped: a receiver lists the
 * satellites it expects as well as the ones it has, and how many it expected
 * and did not find is itself worth knowing. What drops them is the arithmetic
 * downstream, which asks for loudness above zero.
 */
private fun skyOf(status: GnssStatus): List<Satellite> = (0 until status.satelliteCount).map { at ->
    Satellite(
        constellation = constellationOf(status.getConstellationType(at)),
        id = status.getSvid(at),
        loudness = status.getCn0DbHz(at).toDouble(),
        basebandLoudness = basebandOf(status, at),
        elevationDegrees = status.getElevationDegrees(at).toDouble(),
        azimuthDegrees = status.getAzimuthDegrees(at).toDouble(),
        carrierHz =
        if (status.hasCarrierFrequencyHz(at)) {
            status.getCarrierFrequencyHz(at).toDouble()
        } else {
            null
        },
        usedInFix = status.usedInFix(at),
        hasEphemeris = status.hasEphemerisData(at),
        hasAlmanac = status.hasAlmanacData(at),
    )
}

/**
 * The loudness before the receiver's own amplifier, where it is reported.
 *
 * Android 11 and later, and not on every receiver even there. It is the half
 * of the pair that says whether a signal is genuinely louder or the receiver
 * has merely turned itself up, which is the distinction the whole gain
 * business is about.
 */
private fun basebandOf(status: GnssStatus, at: Int): Double? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || !status.hasBasebandCn0DbHz(at)) {
        return null
    }

    return status.getBasebandCn0DbHz(at).toDouble()
}

/** The receiver's number for a system, in the words the rest of this uses. */
private fun constellationOf(type: Int): Constellation = when (type) {
    GnssStatus.CONSTELLATION_GPS -> Constellation.Gps
    GnssStatus.CONSTELLATION_GLONASS -> Constellation.Glonass
    GnssStatus.CONSTELLATION_GALILEO -> Constellation.Galileo
    GnssStatus.CONSTELLATION_BEIDOU -> Constellation.Beidou
    GnssStatus.CONSTELLATION_QZSS -> Constellation.Qzss
    GnssStatus.CONSTELLATION_SBAS -> Constellation.Sbas
    GnssStatus.CONSTELLATION_IRNSS -> Constellation.Irnss
    else -> Constellation.Other
}

/** A move in decibels, or a word for a band the phone has nothing on. */
private fun moved(epochs: Epochs): String =
    epochs.moved?.let { String.format(Locale.ROOT, "%+.1f dB", it) } ?: "not heard"

/**
 * The gain the receiver is running at, one entry per band it reports.
 *
 * Reported per constellation and frequency, so a phone hearing several gives
 * several numbers — and gives one for every band it knows of rather than every
 * band it is listening to, the idle ones carrying a number no receiver could
 * measure. Averaging the lot was the defect this replaces: see [reported],
 * which is what throws the idle ones out, and which the caller applies.
 *
 * Read two ways because the way it is read changed: from Android 13 the event
 * carries the gains apart from the measurements, and before that each
 * measurement carried its own, if it carried one at all.
 */
@Suppress("DEPRECATION")
private fun gainsOf(event: GnssMeasurementsEvent): List<Gain> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        event.gnssAutomaticGainControls.map {
            Gain(
                constellation = constellationOf(it.constellationType),
                carrierHz = it.carrierFrequencyHz.toDouble(),
                levelDb = it.levelDb,
            )
        }
    } else {
        event.measurements
            .filter { it.hasAutomaticGainControlLevelDb() }
            .map {
                Gain(
                    constellation = constellationOf(it.constellationType),
                    carrierHz =
                    if (it.hasCarrierFrequencyHz()) {
                        it.carrierFrequencyHz.toDouble()
                    } else {
                        null
                    },
                    levelDb = it.automaticGainControlLevelDb,
                )
            }
    }

/**
 * The receiver's own clock as it came with the epoch.
 *
 * Most of it is optional, and a receiver that reports none of it still reports
 * the count of discontinuities — which is the field worth having on its own: a
 * receiver whose time stopped running forward raises it, and a false sky
 * carrying a false time is one of the few things that can make it happen.
 */
private fun clockOf(event: GnssMeasurementsEvent): Clock {
    val clock = event.clock

    return Clock(
        timeNanos = clock.timeNanos,
        leapSecond = clock.leapSecond.takeIf { clock.hasLeapSecond() },
        timeUncertaintyNanos = clock.timeUncertaintyNanos.takeIf {
            clock.hasTimeUncertaintyNanos()
        },
        fullBiasNanos = clock.fullBiasNanos.takeIf { clock.hasFullBiasNanos() },
        biasNanos = clock.biasNanos.takeIf { clock.hasBiasNanos() },
        biasUncertaintyNanos =
        clock.biasUncertaintyNanos.takeIf { clock.hasBiasUncertaintyNanos() },
        driftNanosPerSecond =
        clock.driftNanosPerSecond.takeIf { clock.hasDriftNanosPerSecond() },
        driftUncertaintyNanosPerSecond =
        clock.driftUncertaintyNanosPerSecond.takeIf {
            clock.hasDriftUncertaintyNanosPerSecond()
        },
        discontinuities = clock.hardwareClockDiscontinuityCount,
        sinceBootNanos =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            clock.elapsedRealtimeNanos.takeIf { clock.hasElapsedRealtimeNanos() }
        } else {
            null
        },
        sinceBootUncertaintyNanos =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            clock.elapsedRealtimeUncertaintyNanos.takeIf {
                clock.hasElapsedRealtimeUncertaintyNanos()
            }
        } else {
            null
        },
    )
}

/** One raw measurement, in the terms the rest of this uses. */
@Suppress("DEPRECATION")
private fun measurementOf(
    measured: android.location.GnssMeasurement,
    fullTracking: Boolean?,
    delays: Delays = delaysOf(measured),
): Measurement = Measurement(
    constellation = constellationOf(measured.constellationType),
    id = measured.svid,
    timeOffsetNanos = measured.timeOffsetNanos,
    state = measured.state,
    receivedSvTimeNanos = measured.receivedSvTimeNanos,
    receivedSvTimeUncertaintyNanos = measured.receivedSvTimeUncertaintyNanos,
    loudness = measured.cn0DbHz,
    basebandLoudness =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && measured.hasBasebandCn0DbHz()) {
        measured.basebandCn0DbHz
    } else {
        null
    },
    pseudorangeRateMps = measured.pseudorangeRateMetersPerSecond,
    pseudorangeRateUncertaintyMps = measured.pseudorangeRateUncertaintyMetersPerSecond,
    deltaRangeState = measured.accumulatedDeltaRangeState,
    deltaRangeMetres = measured.accumulatedDeltaRangeMeters,
    deltaRangeUncertaintyMetres = measured.accumulatedDeltaRangeUncertaintyMeters,
    carrierHz = measured.carrierFrequencyHz.toDouble().takeIf { measured.hasCarrierFrequencyHz() },
    multipath = measured.multipathIndicator,
    gainDb =
    measured.automaticGainControlLevelDb.takeIf {
        measured.hasAutomaticGainControlLevelDb()
    },
    snrDb = measured.snrInDb.takeIf { measured.hasSnrInDb() },
    codeType = measured.codeType.takeIf { measured.hasCodeType() },
    fullInterSignalBiasNanos = delays.full,
    fullInterSignalBiasUncertaintyNanos = delays.fullUncertainty,
    satelliteInterSignalBiasNanos = delays.satellite,
    satelliteInterSignalBiasUncertaintyNanos = delays.satelliteUncertainty,
    fullTracking = fullTracking,
)

/**
 * Whether the receiver was asked for everything it can hear, where the
 * platform says.
 *
 * Android 14 and later. Below it the question has no answer at all rather than
 * a negative one: a receiver may well have been tracking everything, and the
 * epoch simply does not carry the fact.
 */
private fun fullTrackingOf(event: GnssMeasurementsEvent): Boolean? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        event.isFullTracking
    } else {
        null
    }

/**
 * The delays the receiver applies between one signal and another.
 *
 * Four numbers that arrive together or not at all: Android 11 brought the lot,
 * and below it the epoch does not carry them. Kept as one value so that the
 * version is asked about once rather than four times.
 */
private data class Delays(
    val full: Double? = null,
    val fullUncertainty: Double? = null,
    val satellite: Double? = null,
    val satelliteUncertainty: Double? = null,
)

private fun delaysOf(measured: android.location.GnssMeasurement): Delays =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        reportedDelays(measured)
    } else {
        Delays()
    }

/** The same, on a version that has them, each guarded by its own `has`. */
@RequiresApi(Build.VERSION_CODES.R)
private fun reportedDelays(measured: android.location.GnssMeasurement): Delays = Delays(
    full = measured.fullInterSignalBiasNanos.takeIf {
        measured.hasFullInterSignalBiasNanos()
    },
    fullUncertainty = measured.fullInterSignalBiasUncertaintyNanos.takeIf {
        measured.hasFullInterSignalBiasUncertaintyNanos()
    },
    satellite = measured.satelliteInterSignalBiasNanos.takeIf {
        measured.hasSatelliteInterSignalBiasNanos()
    },
    satelliteUncertainty = measured.satelliteInterSignalBiasUncertaintyNanos.takeIf {
        measured.hasSatelliteInterSignalBiasUncertaintyNanos()
    },
)

/** One navigation message, in the terms the rest of this uses. */
private fun navMessageOf(message: GnssNavigationMessage): NavMessage = NavMessage(
    constellation = navConstellation(message.type),
    id = message.svid,
    type = message.type,
    status = message.status,
    messageId = message.messageId,
    submessageId = message.submessageId,
    data = hexOf(message.data),
)

/**
 * What the chipset admits it can hand over, where the platform asks it.
 *
 * Android 12 and later. Below that the question has no answer at all, which is
 * not the same as a negative one, and the subscriptions are made either way:
 * what arrives is the answer that counts. Worth writing at every start because
 * it is the one line that says whether a phone with nothing in its navigation
 * rows was ever going to have anything there.
 */
private fun whatTheChipsetSays(manager: LocationManager): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return "this version of Android does not ask the chipset what it can hand over"
    }

    val can = manager.gnssCapabilities

    return "the chipset ${saysItCan(can.hasMeasurements())} hand over raw measurements, and " +
        "${saysItCan(can.hasNavigationMessages())} hand over navigation messages"
}

private fun saysItCan(can: Boolean): String = if (can) "says it can" else "says it cannot"
