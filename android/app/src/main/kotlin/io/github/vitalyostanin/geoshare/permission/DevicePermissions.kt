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
    backgroundLocationGranted = granted(
        context,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
    ),
)

private fun granted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
