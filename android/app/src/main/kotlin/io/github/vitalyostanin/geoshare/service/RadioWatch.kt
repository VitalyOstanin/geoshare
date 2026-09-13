package io.github.vitalyostanin.geoshare.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssMeasurementsEvent
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import io.github.vitalyostanin.geoshare.collection.Epochs
import io.github.vitalyostanin.geoshare.collection.Interference
import io.github.vitalyostanin.geoshare.collection.interferenceFrom
import io.github.vitalyostanin.geoshare.log.AppLog
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

/**
 * What the radio is being subjected to, as the receiver's own measures say.
 *
 * Two subscriptions the phone already has the permission for, and neither of
 * which asks for a position: the satellites it can hear and how loud, and the
 * gain its amplifier is running at. The pair is what separates a jammed
 * receiver from a spoofed one — see [interferenceFrom] for which way each
 * moves — and neither alone says much.
 *
 * The measurements arrive whether or not a position comes out of them, which
 * is the case this exists for: a phone that has gone silent is a phone with
 * nothing to write down, and this is what can still be written.
 */
class RadioWatch(private val context: Context) {
    private val noise = ArrayDeque<Double>()
    private val gain = ArrayDeque<Double>()

    private var status: GnssStatus.Callback? = null
    private var measurements: GnssMeasurementsEvent.Callback? = null

    /** What the last minute of measurements adds up to. */
    fun verdict(): Interference = interferenceFrom(gain = window(gain), noise = window(noise))

    /**
     * The same, in the words of the log, or nothing when there is nothing to
     * say: a line per fix saying "unknown" would bury the ones that matter.
     */
    fun saying(): String? {
        val verdict = verdict()

        if (verdict == Interference.Unknown) {
            return null
        }

        return String.format(
            Locale.ROOT,
            "the radio says %s (gain %+.1f dB, satellites %+.1f dB)",
            verdict.toString().lowercase(),
            window(gain).moved ?: 0.0,
            window(noise).moved ?: 0.0,
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
                    heard(status)?.let { note(noise, it) }
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

    private fun note(into: ArrayDeque<Double>, value: Double) {
        into.addLast(value)

        while (into.size > RECENT_EPOCHS + EARLIER_EPOCHS) {
            into.removeFirst()
        }
    }
}

/**
 * How loud the satellites are, averaged over the ones actually heard.
 *
 * The ones with nothing on them are left out rather than counted as silence:
 * a receiver lists the satellites it expects as well as the ones it has, and
 * counting the expected ones as zero would make every sky look like jamming.
 */
private fun heard(status: GnssStatus): Double? {
    val loudness =
        (0 until status.satelliteCount)
            .map { status.getCn0DbHz(it).toDouble() }
            .filter { it > 0.0 }

    return loudness.takeIf { it.isNotEmpty() }?.average()
}

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
