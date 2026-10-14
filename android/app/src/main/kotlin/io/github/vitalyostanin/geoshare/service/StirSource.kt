package io.github.vitalyostanin.geoshare.service

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.os.SystemClock
import io.github.vitalyostanin.geoshare.collection.Stir
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.permission.movementGranted
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.Instant

private const val TAG = "geoshare.stirs"

/**
 * How long the step counter may hold its readings before handing them over.
 *
 * A minute. The counting is done by a chip that runs whether this application
 * asks or not; what costs battery is waking the processor to hear about it, so
 * the readings are collected and delivered in one go. A minute of lateness
 * costs nothing here, because every stir carries the moment it happened and
 * undoes the anchor covering that moment however late it lands.
 */
private const val STEP_DELIVERY_MICROS = 60_000_000

/**
 * The moments the device was picked up, pocketed or walked with.
 *
 * Read from the device's own motion sensors rather than from Play Services'
 * reading of them: the activity recogniser answers a bigger question — what is
 * this person doing — and answers it in its own time, tens of seconds after
 * the fact and only when it is confident. What the rule in [io.github
 * .vitalyostanin.geoshare.collection.driftedWhilePutDown] needs is smaller and
 * harder: was this thing moved at all, ever, since it was put down.
 *
 * Two sensors, and both of them free. The significant-motion trigger is
 * hardware: it wakes the processor once, when the device starts being carried,
 * and has to be asked again after each firing. The step counter runs in the
 * same chip and is read in batches.
 *
 * A device with neither, or an application without the permission for the step
 * counter, produces an empty flow — see [sensorsNoticeMovement] for the
 * difference between "nothing has stirred" and "nothing can be asked".
 */
@SuppressLint("MissingPermission")
fun stirs(context: Context): Flow<Stir> = callbackFlow {
    val sensors = context.getSystemService(SensorManager::class.java)
    val motion = sensors?.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)
    val counter =
        if (mayCountSteps(context)) sensors?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) else null

    if (motion == null && counter == null) {
        AppLog.i(TAG, "no motion sensors to ask; the device cannot say whether it was carried")
        awaitClose {}

        return@callbackFlow
    }

    // The trigger stops itself after firing, so every firing has to ask for
    // the next one. Declared before it is filled in because it asks for itself.
    var askAgain: (() -> Unit)? = null

    val whenItMoves =
        object : TriggerEventListener() {
            override fun onTrigger(event: TriggerEvent) {
                trySend(Stir(at = happenedAt(event.timestamp)))
                askAgain?.invoke()
            }
        }

    val whenItCounts =
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(
                    Stir(at = happenedAt(event.timestamp), steps = event.values[0].toLong()),
                )
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }

    motion?.let { sensor ->
        val ask: () -> Unit = { sensors.requestTriggerSensor(whenItMoves, sensor) }
        askAgain = ask
        ask()
    }

    counter?.let { sensor ->
        sensors.registerListener(
            whenItCounts,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL,
            STEP_DELIVERY_MICROS,
        )
    }

    AppLog.i(
        TAG,
        "asking the sensors about being carried: " +
            "${if (motion != null) "significant motion" else "no motion trigger"}, " +
            if (counter != null) "the step counter" else "no step counter",
    )

    awaitClose {
        motion?.let { sensors.cancelTriggerSensor(whenItMoves, it) }
        counter?.let { sensors.unregisterListener(whenItCounts) }
    }
}

/**
 * Whether this device can be asked whether it was carried at all.
 *
 * What separates a sensor saying nothing happened from there being no sensor:
 * the first is evidence and the second is silence, and the rule that weighs
 * them must not read one as the other.
 */
fun sensorsNoticeMovement(context: Context): Boolean {
    val sensors = context.getSystemService(SensorManager::class.java) ?: return false

    if (sensors.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION) != null) {
        return true
    }

    return mayCountSteps(context) && sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
}

/**
 * The moment an event happened, on the clock the fixes are stamped with.
 *
 * Sensor events are stamped from the count since the last boot, which is the
 * clock that does not step when the network sets the time. The difference is
 * taken now and applied to now, so a batch of readings an hour old still lands
 * an hour ago.
 */
private fun happenedAt(sinceBootNanos: Long): Instant =
    Instant.now().minusNanos(SystemClock.elapsedRealtimeNanos() - sinceBootNanos)

/** The step counter is the one of the two that needs asking for. */
private fun mayCountSteps(context: Context): Boolean = movementGranted(context)
