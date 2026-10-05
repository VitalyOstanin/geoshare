package io.github.vitalyostanin.geoshare.permission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * What the application asks the phone for, held to a list written down here.
 *
 * Every permission below is used, and the manifest is read on every review to
 * check that it still is. What this test is for is the other direction: a
 * permission added later — by a library's manifest merged into ours, or by a
 * change that seemed to need it — arrives silently, and a phone that asks for
 * the microphone to share a position is the kind of thing nobody notices until
 * somebody else does. Changing the set here is a deliberate act.
 */
class ManifestTest {
    private val manifest = File(MANIFEST).readText()

    @Test
    fun `the manifest is where this test thinks it is`() {
        assertTrue("$MANIFEST is not there", File(MANIFEST).isFile)
    }

    @Test
    fun `the application asks for these permissions and no others`() {
        assertEquals(WANTED_PERMISSIONS, namesOf("uses-permission"))
    }

    @Test
    fun `the packages the application can see are the startup screens and no others`() {
        assertEquals(WANTED_QUERIES, namesOf("package"))
    }

    // Anything exported can be started by any other application on the phone.
    // The launcher screen is meant to be; the service that holds the position
    // and the receiver that starts it after a restart are not.
    @Test
    fun `only the screen that starts the application is open to other applications`() {
        val exported = EXPORTED.findAll(manifest).map { it.groupValues[1] to it.groupValues[2] }

        assertEquals(
            mapOf(
                "activity" to "true",
                "service" to "false",
                "receiver" to "false",
            ),
            exported.toMap(),
        )
    }

    @Test
    fun `the application does not let the phone keep copies of its data`() {
        assertTrue(manifest.contains("android:allowBackup=\"false\""))
    }

    private fun namesOf(element: String): Set<String> = Regex("<$element android:name=\"([^\"]+)\"")
        .findAll(manifest)
        .map { it.groupValues[1] }
        .toSet()

    private companion object {
        /** Relative to the module, which is where Gradle runs these tests from. */
        const val MANIFEST = "src/main/AndroidManifest.xml"

        val EXPORTED =
            Regex(
                "<(activity|service|receiver)\\s[^>]*?android:exported=\"(true|false)\"",
                RegexOption.DOT_MATCHES_ALL,
            )

        val WANTED_PERMISSIONS =
            setOf(
                "android.permission.INTERNET",
                "android.permission.CAMERA",
                "android.permission.VIBRATE",
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.ACCESS_BACKGROUND_LOCATION",
                "android.permission.ACTIVITY_RECOGNITION",
                "com.google.android.gms.permission.ACTIVITY_RECOGNITION",
                "android.permission.ACCESS_LOCATION_EXTRA_COMMANDS",
                "android.permission.FOREGROUND_SERVICE",
                "android.permission.FOREGROUND_SERVICE_LOCATION",
                "android.permission.POST_NOTIFICATIONS",
                // The siren sounds through "Do Not Disturb" set to total silence.
                "android.permission.ACCESS_NOTIFICATION_POLICY",
                "android.permission.RECEIVE_BOOT_COMPLETED",
                // The screen comes on while the siren sounds.
                "android.permission.WAKE_LOCK",
                "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
            )

        val WANTED_QUERIES =
            setOf(
                "com.hihonor.systemmanager",
                "com.huawei.systemmanager",
                "com.miui.securitycenter",
                "com.coloros.safecenter",
                "com.vivo.permissionmanager",
            )
    }
}
