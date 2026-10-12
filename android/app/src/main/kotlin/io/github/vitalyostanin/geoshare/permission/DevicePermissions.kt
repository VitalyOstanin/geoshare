package io.github.vitalyostanin.geoshare.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** The permission behind a step, as the platform names it. */
fun permissionOf(step: PermissionStep): String = when (step) {
    PermissionStep.Location -> Manifest.permission.ACCESS_FINE_LOCATION
    PermissionStep.Notifications -> Manifest.permission.POST_NOTIFICATIONS
    PermissionStep.BackgroundLocation -> Manifest.permission.ACCESS_BACKGROUND_LOCATION
}

/** What the device says about the three permissions the service needs. */
fun devicePermissions(context: Context): PermissionState = PermissionState(
    locationGranted = granted(context, Manifest.permission.ACCESS_FINE_LOCATION),
    // The permission only exists from Android 13 onwards; before that a
    // notification is shown without asking.
    notificationsGranted =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        granted(context, Manifest.permission.POST_NOTIFICATIONS),
    backgroundLocationGranted = backgroundLocationGranted(context),
)

/**
 * Whether the position may be read while the application is off the screen.
 *
 * The permission only exists from Android 10 onwards; before that the ordinary
 * location permission covers the background as well.
 */
fun backgroundLocationGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        granted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)

/**
 * Whether the device may tell us that it is moving.
 *
 * Apart from the three above because nothing stops without it: the Moving mode
 * is an improvement on the ordinary one, so a member who refused shares their
 * position exactly as before, only without the faster collecting on the way
 * somewhere.
 */
fun movementGranted(context: Context): Boolean =
    // Before Android 10 there is nothing to ask: the step sensors answer
    // anyone, and the transitions need only the permission granted on install.
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        granted(context, Manifest.permission.ACTIVITY_RECOGNITION)

private fun granted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
