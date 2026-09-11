package io.github.vitalyostanin.geoshare.collection

import java.time.Duration

/**
 * How hard the phone is working to know where it is.
 *
 * The accuracy names are the project's own rather than the ones Play Services
 * uses, so that everything in this package compiles and runs on a plain JVM.
 * The service maps them onto the platform constants.
 */
enum class Accuracy {
    None,
    LowPower,
    Balanced,
    High,
}

enum class CollectionMode(
    val accuracy: Accuracy,
    val interval: Duration,
    /** Batch window: fixes pile up in the chip and arrive together. */
    val maxUpdateDelay: Duration,
) {
    /** Inside the geofence around the last position: nothing is requested. */
    Idle(Accuracy.None, Duration.ZERO, Duration.ZERO),

    Standby(Accuracy.Balanced, Duration.ofMinutes(15), Duration.ofMinutes(45)),

    Moving(Accuracy.Balanced, Duration.ofMinutes(5), Duration.ofMinutes(15)),

    /** Somebody has the map open; the only mode that turns the receiver on. */
    Viewing(Accuracy.High, Duration.ofSeconds(15), Duration.ZERO),

    Night(Accuracy.LowPower, Duration.ofMinutes(30), Duration.ofMinutes(90)),
}
