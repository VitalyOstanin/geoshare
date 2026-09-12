package io.github.vitalyostanin.geoshare.permission

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * The screens a phone keeps its own list of applications allowed to start on.
 *
 * Every large manufacturer has one, none of them is part of Android, and none
 * of them can be read: there is no permission that answers "am I on that list".
 * The most that can be done is to open the screen, which is why the settings
 * row beside it is a reminder rather than a state.
 *
 * Only the first of these has been seen working here, on a HONOR phone; the
 * rest are the addresses their manufacturers are known to use, and one that
 * turns out to be wrong simply resolves to nothing.
 */
private val STARTUP_SCREENS =
    listOf(
        ComponentName(
            "com.hihonor.systemmanager",
            "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
        ),
        ComponentName(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
        ),
        ComponentName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity",
        ),
        ComponentName(
            "com.coloros.safecenter",
            "com.coloros.safecenter.permission.startup.StartupAppListActivity",
        ),
        ComponentName(
            "com.vivo.permissionmanager",
            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
        ),
    )

/** Whether the phone leaves this application out of its battery saving. */
fun batteryUnrestricted(context: Context): Boolean = context
    .getSystemService(PowerManager::class.java)
    .isIgnoringBatteryOptimizations(context.packageName)

/**
 * The system's own request to stop restricting this application.
 *
 * A dialog rather than a settings screen: the member answers it where they
 * are, and the answer is read back when the screen comes up again.
 */
fun batteryExemptionRequest(context: Context): Intent = Intent(
    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
    Uri.fromParts("package", context.packageName, null),
)

/**
 * The manufacturer's startup list, if this phone has one.
 *
 * Null on a phone whose manufacturer keeps no such list, and on one whose
 * screen is at an address not known here; the settings row asks for this and
 * leaves itself out when the answer is null.
 */
fun startupScreen(context: Context): Intent? = STARTUP_SCREENS
    .map { Intent().setComponent(it) }
    .firstOrNull { it.resolveActivity(context.packageManager) != null }
