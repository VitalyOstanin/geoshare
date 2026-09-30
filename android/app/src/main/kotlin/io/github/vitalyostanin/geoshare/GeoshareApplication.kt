package io.github.vitalyostanin.geoshare

import android.app.Application
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.log.SkyLog
import io.github.vitalyostanin.geoshare.project.FamilyProject
import io.github.vitalyostanin.geoshare.service.CurrentGroup
import io.github.vitalyostanin.geoshare.service.StandInServices
import java.io.File

class GeoshareApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        AppLog.start(File(filesDir, "logs"))
        SkyLog.start(File(filesDir, "logs"))
        AppLog.i(TAG, "application started")
        // First of all: the automatic provider that used to do this off a
        // baked-in google-services.json is removed from the manifest, and
        // nothing below may reach Firebase before a project has been chosen
        // for it to talk to.
        FamilyProject.start(this)
        // Before the first read or write: the library refuses to be redirected
        // once it has been used.
        StandInServices.apply(this)
        // Before the first path into the database is built, which the service
        // does without waiting for a screen.
        CurrentGroup.start(this)
    }

    private companion object {
        const val TAG = "geoshare.app"
    }
}
