package io.github.vitalyostanin.geoshare

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.permission.PermissionStep
import io.github.vitalyostanin.geoshare.permission.batteryExemptionRequest
import io.github.vitalyostanin.geoshare.permission.batteryExemptionWanted
import io.github.vitalyostanin.geoshare.permission.batteryUnrestricted
import io.github.vitalyostanin.geoshare.permission.devicePermissions
import io.github.vitalyostanin.geoshare.permission.movementGranted
import io.github.vitalyostanin.geoshare.permission.movementPermissionWanted
import io.github.vitalyostanin.geoshare.permission.nextPermissionStep
import io.github.vitalyostanin.geoshare.permission.permissionOf
import io.github.vitalyostanin.geoshare.permission.startupScreen
import io.github.vitalyostanin.geoshare.service.SharingService
import io.github.vitalyostanin.geoshare.service.refusalToStart
import io.github.vitalyostanin.geoshare.service.startConditions

/**
 * What sharing waits for before the service starts — the permissions, asked
 * one after another — and the screens of the system the settings rows open.
 *
 * Made together with [activity] rather than when sharing is switched on: the
 * requests are registered while the activity is being built, before it is
 * started, which is the only time the platform takes them.
 */
internal class SharingSetup(private val activity: MainActivity) {
    /**
     * The steps the system has already been asked about. A second request for
     * the same one is answered without showing anything, so asking twice would
     * look to the member like a button that does nothing.
     */
    private val asked = mutableSetOf<PermissionStep>()

    private val askPermission =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            continueSetup()
        }

    /**
     * The one request the service can do without.
     *
     * Its own launcher rather than the one above, because the answer leads
     * somewhere else: the three the service needs are asked again until they
     * are granted, while this one is asked once and then left alone.
     */
    private val askMovement =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            askBatteryExemptionOnce()
        }

    // The answer arrives as a closed dialog rather than as a result: the
    // system tells nobody what was pressed, and the state is read back.
    private val askBattery =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            activity.batteryFree = batteryUnrestricted(activity)
        }

    /**
     * Asks for the next permission, or starts the service when none is left.
     *
     * Called again after every answer, so the three requests arrive one after
     * another rather than all at once.
     */
    fun continueSetup() {
        if (!activity.sharing.enabled) {
            return
        }

        val step = nextPermissionStep(devicePermissions(activity))

        if (step == null) {
            activity.permissionNotice = null
            startSharingService()

            // Movement first and the battery after it, one dialog at a time:
            // the second is launched from the answer to the first.
            askMovementOnce()

            return
        }

        if (step in asked) {
            AppLog.i(MainActivity.TAG, "waiting for $step to be granted in the settings")
            activity.permissionNotice = step

            return
        }

        asked += step
        askPermission.launch(arrayOf(permissionOf(step)))
    }

    private fun startSharingService() {
        val refusal = refusalToStart(startConditions(activity, fromBackground = false))

        if (refusal == null) {
            SharingService.start(activity)
        } else {
            AppLog.i(MainActivity.TAG, "not starting the service: $refusal")
        }
    }

    /**
     * Asks once for the device to say when its owner is on the move.
     *
     * Refusing costs the Moving mode and nothing else, so a member who says no
     * is not asked again and is not told anything is wrong. When there is
     * nothing to ask, the battery request follows straight away — otherwise it
     * follows the answer.
     */
    private fun askMovementOnce() {
        val wanted =
            movementPermissionWanted(
                sharing = activity.sharing.enabled,
                granted = movementGranted(activity),
                askedBefore = activity.backgroundWork.movementAsked,
            )

        if (!wanted) {
            askBatteryExemptionOnce()

            return
        }

        activity.backgroundWork.movementAsked = true
        askMovement.launch(Manifest.permission.ACTIVITY_RECOGNITION)
    }

    /**
     * Asks once, and only once, for the device to stop restricting us.
     *
     * Made where the permissions end rather than at startup: a member who has
     * just allowed the position is being asked about the same thing, and a
     * member who shares nothing has nothing to be interrupted.
     */
    private fun askBatteryExemptionOnce() {
        val wanted =
            batteryExemptionWanted(
                sharing = activity.sharing.enabled,
                unrestricted = activity.batteryFree,
                askedBefore = activity.backgroundWork.batteryAsked,
            )

        if (!wanted) {
            return
        }

        activity.backgroundWork.batteryAsked = true
        askBatteryExemption()
    }

    fun askSirenThroughSilence() {
        activity.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    fun askBatteryExemption() {
        askBattery.launch(batteryExemptionRequest(activity))
    }

    /**
     * Opens the manufacturer's own startup list.
     *
     * The row that calls this is shown only when such a screen was found, so
     * the absent case is a device whose screen went away between the two, and
     * the application settings are the nearest thing left.
     */
    fun openStartupScreen() {
        val screen = startupScreen(activity)

        if (screen == null) {
            openApplicationSettings()

            return
        }

        activity.startActivity(screen)
    }

    fun openApplicationSettings() {
        activity.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", activity.packageName, null),
            ),
        )
    }
}
