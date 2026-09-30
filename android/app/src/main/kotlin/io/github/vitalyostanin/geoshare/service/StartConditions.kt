package io.github.vitalyostanin.geoshare.service

/** Everything the decision to run the service depends on, gathered by the caller. */
data class StartConditions(
    /** Whether this phone has a family's project to sign in and write into at all. */
    val connected: Boolean,
    val signedIn: Boolean,
    /** Whether the member asked for their position to be shared at all. */
    val enabled: Boolean,
    val locationGranted: Boolean,
    val backgroundLocationGranted: Boolean,
    /**
     * Whether the start comes from somewhere other than a screen of ours — a
     * restart after boot, say. The platform judges those starts differently.
     */
    val fromBackground: Boolean,
)

/** Why the service is not being started. */
enum class StartRefusal {
    /** No family's project has been connected yet; there is nothing to sign in to. */
    NotConnected,

    NotSignedIn,

    /** The member turned sharing off. */
    TurnedOff,

    NoLocationPermission,

    /**
     * A location service started from the background needs the background
     * permission on top of the ordinary one; without it the platform refuses
     * the start with an exception rather than a denial.
     */
    NoBackgroundLocationPermission,
}

/** The refusal that applies, or null when the service may run. */
fun refusalToStart(conditions: StartConditions): StartRefusal? = when {
    !conditions.connected -> StartRefusal.NotConnected

    !conditions.signedIn -> StartRefusal.NotSignedIn

    !conditions.enabled -> StartRefusal.TurnedOff

    !conditions.locationGranted -> StartRefusal.NoLocationPermission

    conditions.fromBackground && !conditions.backgroundLocationGranted ->
        StartRefusal.NoBackgroundLocationPermission

    else -> null
}
