package io.github.vitalyostanin.geoshare.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssMeasurementsEvent
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import io.github.vitalyostanin.geoshare.collection.Constellation
import io.github.vitalyostanin.geoshare.collection.Epochs
import io.github.vitalyostanin.geoshare.collection.Interference
import io.github.vitalyostanin.geoshare.collection.SHARE_THE_GPS_BAND
import io.github.vitalyostanin.geoshare.collection.acrossTheBand
import io.github.vitalyostanin.geoshare.collection.interferenceFrom
import io.github.vitalyostanin.geoshare.collection.whatTheBandsSay
import io.github.vitalyostanin.geoshare.log.AppLog
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
 * What the radio is being subjected to, as the receiver's own measures say.
 *
 * Two subscriptions the phone already has the permission for, and neither of
 * which asks for a position: the satellites it can hear and how loud, and the
 * gain its amplifier is running at. The pair is what separates a jammed
 * receiver from a spoofed one — see [interferenceFrom] for which way each
 * moves — and neither alone says much.
 *
 * The satellites are kept apart by the system they belong to, which is a third
 * thing to read and the only one that names where the interference is coming
 * from: see [whatTheBandsSay].
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
    private val gain = ArrayDeque<Double>()

    private var saidNothingAt: Instant = Instant.EPOCH
    private var status: GnssStatus.Callback? = null
    private var measurements: GnssMeasurementsEvent.Callback? = null

    /**
     * What the last minute of measurements adds up to.
     *
     * The bands are asked first. They answer less often — a phone hearing one
     * system has nothing to compare — but when they do answer they name the
     * band whatever is going on is going on in, and they answer without the
     * gain, which some phones never report at all.
     */
    fun verdict(): Interference {
        val bands = windows()

        return whatTheBandsSay(sharedBand(bands), bands[Constellation.Glonass] ?: NOTHING)
            ?: interferenceFrom(gain = window(gain), noise = acrossTheBand(bands.values))
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

        val bands = windows()

        return String.format(
            Locale.ROOT,
            "the radio says %s (gain %s, the GPS band %s, GLONASS %s)",
            verdict.toString().lowercase(),
            moved(window(gain)),
            moved(sharedBand(bands)),
            moved(bands[Constellation.Glonass] ?: NOTHING),
        )
    }

    /**
     * Subscribes to both, on the handler rather than on an executor.
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
                    for ((constellation, loudness) in loudnessOf(status)) {
                        note(heard.getOrPut(constellation) { ArrayDeque() }, loudness)
                    }
                }
            }.also { manager.registerGnssStatusCallback(it, handler) }

        measurements =
            object : GnssMeasurementsEvent.Callback() {
                override fun onGnssMeasurementsReceived(event: GnssMeasurementsEvent) {
                    gainOf(event)?.let { note(gain, it) }
                }
            }.also { manager.registerGnssMeasurementsCallback(it, handler) }

        AppLog.i(TAG, "watching the radio")
    }

    fun stop() {
        val manager = context.getSystemService(LocationManager::class.java) ?: return

        status?.let { manager.unregisterGnssStatusCallback(it) }
        measurements?.let { manager.unregisterGnssMeasurementsCallback(it) }

        status = null
        measurements = null
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
        val shared = SHARE_THE_GPS_BAND.maxOf { heard[it]?.size ?: 0 }
        val glonass = heard[Constellation.Glonass]?.size ?: 0

        return "the radio has nothing to go on: ${gain.size} epochs of gain, " +
            "$shared of the GPS band, $glonass of GLONASS, $needed needed"
    }

    /** Every system's history, each read as its own pair of averages. */
    private fun windows(): Map<Constellation, Epochs> = heard.mapValues { window(it.value) }

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
 * How loud the satellites are, averaged within each system separately.
 *
 * The ones with nothing on them are left out rather than counted as silence:
 * a receiver lists the satellites it expects as well as the ones it has, and
 * counting the expected ones as zero would make every sky look like jamming.
 *
 * Averaged within the system rather than over all of them at once so that a
 * system with eight satellites in view does not speak for one with three.
 */
private fun loudnessOf(status: GnssStatus): Map<Constellation, Double> =
    (0 until status.satelliteCount)
        .filter { status.getCn0DbHz(it) > 0.0f }
        .groupBy { constellationOf(status.getConstellationType(it)) }
        .mapValues { (_, satellites) ->
            satellites.map { status.getCn0DbHz(it).toDouble() }.average()
        }

/** The receiver's number for a system, in the words the rest of this uses. */
private fun constellationOf(type: Int): Constellation = when (type) {
    GnssStatus.CONSTELLATION_GPS -> Constellation.Gps
    GnssStatus.CONSTELLATION_GLONASS -> Constellation.Glonass
    GnssStatus.CONSTELLATION_GALILEO -> Constellation.Galileo
    GnssStatus.CONSTELLATION_BEIDOU -> Constellation.Beidou
    else -> Constellation.Other
}

/** A move in decibels, or a word for a band the phone has nothing on. */
private fun moved(epochs: Epochs): String =
    epochs.moved?.let { String.format(Locale.ROOT, "%+.1f dB", it) } ?: "not heard"

/**
 * The gain the receiver is running at, averaged over the bands it reports.
 *
 * Reported once per constellation and frequency, so a phone hearing several
 * gives several numbers; one of them falling while the others hold is still a
 * fall, and the average is what a single figure can carry.
 *
 * Read two ways because the way it is read changed: from Android 13 the event
 * carries the gains apart from the measurements, and before that each
 * measurement carried its own, if it carried one at all.
 */
@Suppress("DEPRECATION")
private fun gainOf(event: GnssMeasurementsEvent): Double? {
    val levels =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            event.gnssAutomaticGainControls.map { it.levelDb }
        } else {
            event.measurements
                .filter { it.hasAutomaticGainControlLevelDb() }
                .map { it.automaticGainControlLevelDb }
        }

    return levels.takeIf { it.isNotEmpty() }?.average()
}
