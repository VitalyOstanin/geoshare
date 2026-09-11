package io.github.vitalyostanin.geoshare

import android.app.Application
import io.github.vitalyostanin.geoshare.log.AppLog
import java.io.File

class GeoshareApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        AppLog.start(File(filesDir, "logs"))
        AppLog.i(TAG, "application started")
    }

    private companion object {
        const val TAG = "geoshare.app"
    }
}
