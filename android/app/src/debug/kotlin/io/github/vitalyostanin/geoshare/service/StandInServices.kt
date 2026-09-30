package io.github.vitalyostanin.geoshare.service

import android.content.Context
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.project.FamilyProject
import io.github.vitalyostanin.geoshare.project.ProjectSettings

private const val TAG = "geoshare.standin"

/** Where an emulator reaches the machine it runs on, and what listens there. */
private const val HOST = "10.0.2.2"
private const val FIRESTORE_PORT = 8080
private const val AUTH_PORT = 9099

/** What qemu calls the boards it emulates; no phone answers either name. */
private val EMULATED_BOARDS = setOf("goldfish", "ranchu")

/**
 * What this build points a fresh emulator at, since there is no site to
 * download settings from here.
 *
 * A project id beginning with "demo-" is what `tools/run-services.sh` starts
 * the emulators under, and the Local Emulator Suite asks nothing real of a
 * project named that way — which is what lets the rest of these stand in for
 * credentials without being any.
 */
private val DEMO_SETTINGS = ProjectSettings(
    version = 1,
    projectId = "demo-geoshare",
    applicationId = "1:0:android:0",
    apiKey = "demo-geoshare",
    gcmSenderId = "0",
    site = "demo-geoshare",
)

/**
 * The stand-in database and sign-in an emulator talks to instead of the real
 * project.
 *
 * For working on the exchange without phones: the group, the accounts and the
 * positions all live in Firebase emulators on the machine running the Android
 * emulator, so both sides of a change can be watched at once and nothing
 * reaches what the household actually uses.
 *
 * Decided by the device rather than by a setting: an emulator has no Google
 * account to sign in with and no household to belong to, so there is nothing
 * it could usefully do against the real project. A debug build on a phone is
 * untouched.
 *
 * This file belongs to the debug build alone; the release build has a version
 * of the same names that does nothing and carries nothing. Kept in the main
 * sources, the addresses of the test accounts and the password they share went
 * into every release, where they were unreachable — which is a thing to be
 * shown rather than argued about.
 */
object StandInServices {
    /**
     * Points Firebase at the emulators when this is one. Called before
     * anything touches the database: the library refuses to be redirected
     * once it has been used.
     *
     * An emulator that has never been connected to a family's project has
     * nothing [FamilyProject] could have started Firebase with by this point,
     * so it is given the demo settings first — otherwise there would be no
     * default app for [FirebaseFirestore.getInstance] below to find.
     */
    fun apply(context: Context) {
        if (Build.HARDWARE !in EMULATED_BOARDS) {
            return
        }

        if (!FamilyProject.connected) {
            FamilyProject.save(context, DEMO_SETTINGS)
        }

        AppLog.i(TAG, "talking to the stand-in services at $HOST")
        FirebaseFirestore.getInstance().useEmulator(HOST, FIRESTORE_PORT)
        FirebaseAuth.getInstance().useEmulator(HOST, AUTH_PORT)
        inUse = true
    }

    /** True once this build has been pointed at the stand-ins. */
    var inUse: Boolean = false
        private set
}

/**
 * The accounts the stand-in sign-in is filled with, as the seed writes them.
 *
 * Offered as buttons on the sign-in screen: there is no Google account behind
 * an emulator, and typing an address on a phone keyboard to try a change is
 * time spent on nothing.
 *
 * The last two are on no roster: one founds a group, the other joins it on an
 * invitation, and the way into a group is watched from both ends on the one
 * emulator by signing out and back in as the other.
 */
val STAND_IN_ACCOUNTS: List<String> =
    listOf(
        "parent@geoshare.test",
        "spouse@geoshare.test",
        "child@geoshare.test",
        "stranger@geoshare.test",
        "newcomer@geoshare.test",
    )

/** The one password all of them share; nobody types it. */
const val STAND_IN_PASSWORD = "geoshare"
