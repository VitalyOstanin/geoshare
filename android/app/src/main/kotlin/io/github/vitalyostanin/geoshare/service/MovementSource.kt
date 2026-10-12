package io.github.vitalyostanin.geoshare.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import io.github.vitalyostanin.geoshare.collection.ActivityChange
import io.github.vitalyostanin.geoshare.collection.Moving
import io.github.vitalyostanin.geoshare.collection.movingAfter
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.permission.movementGranted
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

private const val TAG = "geoshare.movement"

/** Sent to this application alone; the receiver below is the only thing listening. */
private const val TRANSITIONS = "io.github.vitalyostanin.geoshare.ACTIVITY_TRANSITION"

/**
 * The ways of moving worth telling apart from standing still.
 *
 * Walking, running, cycling and riding are all "the device is going somewhere",
 * which is the only distinction the modes make. Standing still is asked for as
 * well, because it is what ends the others.
 */
private val TRAVELLING =
    listOf(
        DetectedActivity.WALKING,
        DetectedActivity.RUNNING,
        DetectedActivity.ON_BICYCLE,
        DetectedActivity.IN_VEHICLE,
    )

/**
 * Whether the device is going somewhere, as its own sensors see it.
 *
 * Counted by the platform rather than by us: the step counter and the
 * accelerometer are read by a chip that is awake anyway, so the answer costs
 * nothing like a fix would, and it arrives within seconds of somebody standing
 * up rather than at the next round of asking — which in the slow mode is a
 * quarter of an hour away, by when the walk is over.
 *
 * Without the permission the flow says "not moving" and stops there: the mode
 * it feeds is an improvement on the ordinary one, not a condition of sharing,
 * and a member who refused is left with everything else working.
 */
@SuppressLint("MissingPermission")
fun movement(context: Context): Flow<Moving> = callbackFlow {
    if (!allowed(context)) {
        AppLog.i(TAG, "no permission to notice movement; staying with the ordinary modes")
        send(Moving(going = false, told = false))
        awaitClose {}

        return@callbackFlow
    }

    if (!playServicesPresent(context)) {
        // The recogniser lives in Play Services and nowhere else; the ordinary
        // modes carry on without it, as they do without the permission.
        AppLog.i(TAG, "no Play Services to notice movement; staying with the ordinary modes")
        send(Moving(going = false, told = false))
        awaitClose {}

        return@callbackFlow
    }

    var moving = false

    send(Moving(going = false, told = false))

    val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val result = ActivityTransitionResult.extractResult(intent) ?: return

                val changes =
                    result.transitionEvents.map { event ->
                        ActivityChange(
                            travelling = event.activityType in TRAVELLING,
                            begun =
                            event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER,
                        )
                    }

                moving = movingAfter(changes, moving)
                AppLog.i(TAG, "the phone is ${if (moving) "on the move" else "standing still"}")
                trySend(Moving(going = moving, told = true))
            }
        }

    // Only this application sends it, and nothing else may reach in.
    ContextCompat.registerReceiver(
        context,
        receiver,
        IntentFilter(TRANSITIONS),
        ContextCompat.RECEIVER_NOT_EXPORTED,
    )

    // Mutable, because the platform fills the intent with the transitions it
    // is delivering; an immutable one would arrive empty.
    val delivery =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(TRANSITIONS).setPackage(context.packageName),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    val client = ActivityRecognition.getClient(context)

    client
        .requestActivityTransitionUpdates(transitionRequest(), delivery)
        .addOnFailureListener { error ->
            AppLog.w(TAG, "could not ask to be told about movement", error)
        }

    awaitClose {
        client.removeActivityTransitionUpdates(delivery)
        context.unregisterReceiver(receiver)
        delivery.cancel()
    }
}
    // The same answer twice changes no mode, and every one of them costs a
    // cancelled subscription and a new one.
    .distinctUntilChanged()

/** Both ends of every activity asked about: what began, and what stopped. */
private fun transitionRequest(): ActivityTransitionRequest {
    val transitions =
        (TRAVELLING + DetectedActivity.STILL).flatMap { activity ->
            listOf(
                ActivityTransition.ACTIVITY_TRANSITION_ENTER,
                ActivityTransition.ACTIVITY_TRANSITION_EXIT,
            ).map { transition ->
                ActivityTransition
                    .Builder()
                    .setActivityType(activity)
                    .setActivityTransition(transition)
                    .build()
            }
        }

    return ActivityTransitionRequest(transitions)
}

private fun allowed(context: Context): Boolean = movementGranted(context)
