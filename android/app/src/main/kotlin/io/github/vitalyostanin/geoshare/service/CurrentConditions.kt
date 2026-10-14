package io.github.vitalyostanin.geoshare.service

import android.Manifest
import android.content.Context
import io.github.vitalyostanin.geoshare.permission.backgroundLocationGranted
import io.github.vitalyostanin.geoshare.permission.granted
import io.github.vitalyostanin.geoshare.project.Connections

/**
 * The conditions as they stand on the device, ready for [refusalToStart].
 *
 * The reading is kept apart from the decision so that the rules can be checked
 * without a device to read them from.
 */
fun startConditions(context: Context, fromBackground: Boolean): StartConditions = StartConditions(
    connected = Connections.connected,
    // Signed in to any one of the groups is reason enough to start: the
    // service writes into every group that has a session, and skips the rest.
    signedIn = Connections.all.value.any { it.auth.currentUser != null },
    enabled = SharingSetting(context).enabled,
    locationGranted = granted(context, Manifest.permission.ACCESS_FINE_LOCATION),
    backgroundLocationGranted = backgroundLocationGranted(context),
    fromBackground = fromBackground,
)
