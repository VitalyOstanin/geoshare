package io.github.vitalyostanin.geoshare.service

/** What the service does when the signed-in account changes. */
enum class SessionChange {
    /** Nothing: the collection already runs for this account, or none runs and none is signed in. */
    None,

    /** An account turned up and nothing collects yet. */
    Start,

    /** Another account signed in while the collection wrote for the last one. */
    Restart,

    /** The account signed out while the collection wrote for it. */
    Stop,
}

/**
 * Compares the account the collection writes for with the one signed in now.
 *
 * Kept apart from the service so that the one decision a phone changing hands
 * turns on can be checked without a service to run it in.
 */
fun sessionChange(collectingFor: String?, signedIn: String?): SessionChange = when {
    signedIn == collectingFor -> SessionChange.None
    signedIn == null -> SessionChange.Stop
    collectingFor == null -> SessionChange.Start
    else -> SessionChange.Restart
}
