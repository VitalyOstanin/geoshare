package io.github.vitalyostanin.geoshare.service

import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.github.vitalyostanin.geoshare.log.AppLog

private const val TAG = "geoshare.standin"

/** Where an emulator reaches the machine it runs on, and what listens there. */
private const val HOST = "10.0.2.2"
private const val FIRESTORE_PORT = 8080
private const val AUTH_PORT = 9099

/** What qemu calls the boards it emulates; no phone answers either name. */
private val EMULATED_BOARDS = setOf("goldfish", "ranchu")

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
     */
    fun apply() {
        if (Build.HARDWARE !in EMULATED_BOARDS) {
            return
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
 */
val STAND_IN_ACCOUNTS: List<String> =
    listOf("parent@geoshare.test", "spouse@geoshare.test", "child@geoshare.test")

/** The one password all of them share; nobody types it. */
const val STAND_IN_PASSWORD = "geoshare"
