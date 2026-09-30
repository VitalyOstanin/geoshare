package io.github.vitalyostanin.geoshare.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import io.github.vitalyostanin.geoshare.project.FamilyProject

/**
 * The conditions as they stand on the device, ready for [refusalToStart].
 *
 * The reading is kept apart from the decision so that the rules can be checked
 * without a device to read them from.
 */
fun startConditions(context: Context, fromBackground: Boolean): StartConditions = StartConditions(
    connected = FamilyProject.connected,
    // Asked only once there is a project to ask it of: FirebaseAuth throws
    // rather than answering "nobody" when no default app has been started.
    signedIn = FamilyProject.connected && FirebaseAuth.getInstance().currentUser != null,
    enabled = SharingSetting(context).enabled,
    locationGranted = granted(context, Manifest.permission.ACCESS_FINE_LOCATION),
    backgroundLocationGranted = granted(
        context,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
    ),
    fromBackground = fromBackground,
)

private fun granted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
