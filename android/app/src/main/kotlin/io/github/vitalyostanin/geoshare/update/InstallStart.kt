package io.github.vitalyostanin.geoshare.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

/** What handing a downloaded APK to the system's installer came to. */
enum class InstallStart {
    /** The installer's own screen is open; the rest is the system's. */
    Started,

    /**
     * The member has not allowed this application to install packages. The
     * system's page for that was opened instead; once they come back, call
     * [startInstall] again.
     */
    NeedsPermission,

    /** Nothing on the device handles an install, or the settings page is missing. */
    Unavailable,
}

/**
 * Hands [apk] (a file [downloadApk] made, in [updateDirectory]) to the
 * system's package installer. An ACTION_VIEW on a FileProvider URI rather
 * than a PackageInstaller session: minSdk is 26, where the "install unknown
 * apps" permission is per application and the installer's confirmation
 * screen is the same either way, so a session would add a service, a
 * receiver and a status callback for nothing the member can see.
 */
fun startInstall(context: Context, apk: File): InstallStart {
    if (!context.packageManager.canRequestPackageInstalls()) {
        return launch(context, permissionPage(context))
    }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.reports", apk)
    val install = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, APK_MIME_TYPE)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    return if (launch(context, install) == InstallStart.Started) {
        InstallStart.Started
    } else {
        InstallStart.Unavailable
    }
}

private fun permissionPage(context: Context): Intent =
    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

/** Opens [intent], from a non-activity [context] too; a missing handler is a result. */
private fun launch(context: Context, intent: Intent): InstallStart = try {
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    if (intent.action == Intent.ACTION_VIEW) InstallStart.Started else InstallStart.NeedsPermission
} catch (missing: ActivityNotFoundException) {
    InstallStart.Unavailable
}
