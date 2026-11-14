package io.github.vitalyostanin.geoshare

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.log.SkyLog
import io.github.vitalyostanin.geoshare.log.UncaughtExceptionLogger
import io.github.vitalyostanin.geoshare.managed.ManagedConfiguration
import io.github.vitalyostanin.geoshare.project.Connections
import io.github.vitalyostanin.geoshare.service.ManagedSharingStep
import io.github.vitalyostanin.geoshare.service.SettingsMigrations
import io.github.vitalyostanin.geoshare.service.SharingService
import io.github.vitalyostanin.geoshare.service.StandInServices
import io.github.vitalyostanin.geoshare.service.managedSharingStep
import io.github.vitalyostanin.geoshare.service.refusalToStart
import io.github.vitalyostanin.geoshare.service.startConditions
import java.io.File

class GeoshareApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        AppLog.start(File(filesDir, "logs"))
        UncaughtExceptionLogger.install()
        SkyLog.start(File(filesDir, "logs"), device())
        AppLog.i(TAG, "application started")
        // Before anything reads a setting, and so before the connections.
        SettingsMigrations.run(this)
        // Before the first setting is read as well: on a managed device the
        // device policy controller's word on sharing outranks the member's
        // (ADR-0043), and a start after a restart must already hear it.
        ManagedConfiguration.refresh(this)
        // First of all: the automatic provider that used to do this off a
        // baked-in google-services.json is removed from the manifest, and
        // nothing below may reach Firebase before a project has been chosen
        // for it to talk to. It is also before the first path into the
        // database is built, which the service does without waiting for a
        // screen.
        Connections.start(this)
        // Before the first read or write: the library refuses to be redirected
        // once it has been used.
        StandInServices.apply(this)
        followManagedConfiguration()
    }

    /**
     * Reads the managed configuration again whenever the controller changes
     * it, and starts or stops the service when what it says about sharing
     * calls for that.
     *
     * Registered here rather than in the manifest because the platform only
     * delivers this broadcast to receivers registered at run time. Not
     * exported: the sender is the system, whose broadcasts reach an
     * unexported receiver all the same, and nothing else has any business
     * telling the application its configuration moved.
     */
    private fun followManagedConfiguration() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                ManagedConfiguration.refresh(context)

                val managed = ManagedConfiguration.current.sharingEnabled
                val refusal = refusalToStart(startConditions(context, fromBackground = true))

                when (managedSharingStep(managed, refusal)) {
                    // The platform may refuse a foreground service started
                    // while the application is in the background; the start
                    // after the next restart, or the next screen, picks it up.
                    ManagedSharingStep.Start -> try {
                        SharingService.start(context)
                        AppLog.i(TAG, "the managed configuration turned sharing on")
                    } catch (refused: IllegalStateException) {
                        AppLog.w(
                            TAG,
                            "the managed configuration turned sharing on, start refused",
                            refused,
                        )
                    }

                    // Stopped directly rather than through the service's stop
                    // action, which would also write the member's own answer
                    // down as "off" and leave it so once the configuration
                    // goes silent again.
                    ManagedSharingStep.Stop -> {
                        AppLog.i(TAG, "the managed configuration turned sharing off")
                        context.stopService(Intent(context, SharingService::class.java))
                    }

                    ManagedSharingStep.Nothing -> AppLog.i(TAG, "the managed configuration changed")
                }
            }
        }

        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    /**
     * What the head of the sky file says it came from, in the words Google's
     * logger uses at the head of its own.
     */
    private fun device(): String {
        val receiver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getSystemService(LocationManager::class.java)?.gnssHardwareModelName
        } else {
            null
        }

        return "geoshare ${BuildConfig.VERSION_NAME} Platform: ${Build.VERSION.RELEASE} " +
            "Manufacturer: ${Build.MANUFACTURER} Model: ${Build.MODEL} " +
            "GNSS Hardware Model Name: ${receiver.orEmpty()}"
    }

    private companion object {
        const val TAG = "geoshare.app"
    }
}
