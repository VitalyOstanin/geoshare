package io.github.vitalyostanin.geoshare.service

import android.content.Context

/**
 * The stand-in services, as a release build knows them: not at all.
 *
 * The working version of this file is in the debug sources, where the
 * addresses of the emulators, the test accounts and the password they share
 * belong. A release carries these empty answers instead, so that the screens
 * which offer the stand-in accounts have something to ask and get nothing.
 */
object StandInServices {
    fun apply(context: Context) = Unit

    /** Never, in a build that has no stand-ins to be pointed at. */
    val inUse: Boolean = false
}

val STAND_IN_ACCOUNTS: List<String> = emptyList()

const val STAND_IN_PASSWORD = ""
