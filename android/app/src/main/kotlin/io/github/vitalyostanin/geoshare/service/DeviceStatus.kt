package io.github.vitalyostanin.geoshare.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import io.github.vitalyostanin.geoshare.collection.CollectionMode
import io.github.vitalyostanin.geoshare.collection.IDLE_REPEAT
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.logFailure
import io.github.vitalyostanin.geoshare.map.runCatchingFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration

private const val TAG = "geoshare.service"

/**
 * How often the device says how it is when nothing else has made it say so.
 *
 * On the beat a device at rest writes its position again, for the same reason:
 * under a transmitter no position is written at all, and this is the only
 * news that keeps the marker out of the grey. An hour let it go grey for half
 * of every hour, and the card then read a device that is working and being
 * lied to as a device that has gone quiet.
 */
private val STATUS_EVERY: Duration = IDLE_REPEAT

/** How long the cable has to stay in or out before it is said. */
private val POWER_SETTLES: Duration = Duration.ofSeconds(2)

/** What a device that will not say counts as, so it is not treated as flat. */
private const val FULL_BATTERY = 100

/**
 * How the device is, apart from where it is: in touch now, in this mode, this
 * charged, on a cable or off it — read from the device and said into every
 * linked group.
 */
internal class DeviceStatus(
    private val context: Context,
    private val scope: CoroutineScope,
    private val writer: EveryGroup,
    /** Whether there is any group to say it in. */
    private val linked: () -> Boolean,
    /** The mode the device collects in now. */
    private val mode: () -> CollectionMode,
) {
    /**
     * The cable going in or coming out, said at once.
     *
     * Waiting for the next fix would leave a device reading "12%" for the
     * fifteen minutes it is already filling up, which is the case the reader
     * most wants told — and under a transmitter there is no next fix at all.
     */
    private var powerSettling: Job? = null

    /**
     * Whether [power] is registered: unregistering one that is not throws, and
     * a second stop (the service winding down twice) must not.
     */
    private var hearing = false

    private val power =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                AppLog.i(TAG, "power changed: ${intent.action}")
                // A loose cable says connected and disconnected several times
                // a second, and each would be a write into every group: only
                // where it settles is news.
                powerSettling?.cancel()
                powerSettling =
                    scope.launch {
                        delay(POWER_SETTLES.toMillis())
                        tell()
                    }
            }
        }

    /**
     * Says how the device is now.
     *
     * Not through the position, and not past the filter: a device whose
     * answers are all being thrown away is still in touch and still on its
     * cable, and a reader deciding whether it has gone quiet or run flat needs
     * to hear that. A failure costs nothing but the news, which the next mode,
     * cable or [STATUS_EVERY] says again.
     */
    fun tell() {
        if (!linked()) {
            return
        }

        val mode = mode()
        val percent = batteryPercent()
        val cable = charging()

        scope.launch {
            runCatchingFailure { writer.tellStatus(mode, percent, cable) }.onFailure { error ->
                logFailure(TAG, "could not say how the device is", error)
            }
        }
    }

    /**
     * Says how the device is every [STATUS_EVERY], whatever else it is or is
     * not writing: the moment it was last in touch is what tells a working
     * device whose position is held back from one that has gone quiet.
     */
    suspend fun keepTelling() {
        while (true) {
            delay(STATUS_EVERY.toMillis())
            tell()
        }
    }

    /** Listens for the cable going in or coming out, which is said at once. */
    fun hearTheCable() {
        if (hearing) {
            return
        }

        ContextCompat.registerReceiver(
            context,
            power,
            IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            },
            // Only the system sends these, and nothing else may reach in.
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        hearing = true
    }

    /** Stops listening for the cable; nothing if it was not being listened for. */
    fun stopHearingTheCable() {
        if (!hearing) {
            return
        }

        hearing = false
        context.unregisterReceiver(power)
    }

    fun batteryPercent(): Int = context.getSystemService<BatteryManager>()
        ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        ?: FULL_BATTERY

    /**
     * Whether the device is on a charger, which is what tells a low percentage
     * that is about to grow from one that is about to run out.
     *
     * Read as "plugged into something" rather than through `isCharging`: a
     * device that has reached a hundred percent stops charging while the cable
     * is still in it, and the reader is asking about the cable.
     */
    fun charging(): Boolean {
        val battery =
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                ?: return false

        return battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
    }
}
