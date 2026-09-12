package io.github.vitalyostanin.geoshare

import android.app.Application
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.service.StandInServices
import java.io.File

class GeoshareApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        AppLog.start(File(filesDir, "logs"))
        AppLog.i(TAG, "application started")
        // Before the first read or write: the library refuses to be redirected
        // once it has been used.
        StandInServices.apply()
    }

    private companion object {
        const val TAG = "geoshare.app"
    }
}
