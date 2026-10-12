package io.github.vitalyostanin.geoshare.service

import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.Reported
import java.time.Duration
import java.time.Instant

/**
 * A position as the platform handed it over, with nothing left behind.
 *
 * Written out once because three places ask for a position — the fused
 * provider, the towers, and the satellites asked directly — and a field
 * carried by one of them and dropped by another is a difference that looks
 * like a finding later.
 */
fun fixOf(location: Location): Fix = Fix(
    latitude = location.latitude,
    longitude = location.longitude,
    takenAt = Instant.ofEpochMilli(location.time),
    accuracyMetres = location.accuracy.toDouble().takeIf { location.hasAccuracy() },
    from = location.provider,
    sinceBoot = Duration.ofNanos(location.elapsedRealtimeNanos),
    reported =
    Reported(
        altitudeMetres = location.altitude.takeIf { location.hasAltitude() },
        speedMps = location.speed.toDouble().takeIf { location.hasSpeed() },
        bearingDegrees = location.bearing.toDouble().takeIf { location.hasBearing() },
        speedAccuracyMps =
        location.speedAccuracyMetersPerSecond
            .toDouble()
            .takeIf { location.hasSpeedAccuracy() },
        bearingAccuracyDegrees =
        location.bearingAccuracyDegrees
            .toDouble()
            .takeIf { location.hasBearingAccuracy() },
        verticalAccuracyMetres =
        location.verticalAccuracyMeters
            .toDouble()
            .takeIf { location.hasVerticalAccuracy() },
        pretended = pretended(location),
        alongside = alongside(location),
    ),
)

/**
 * How long the device has been awake, as of now.
 *
 * The moment to hold a fix's own count since boot against. Both come off the
 * same clock, the one the platform never sets back, so the pair says how old
 * an answer is even across a correction of the wall clock — and under a false
 * signal the wall clock is itself suspect, since the time it is set from comes
 * off the satellites being faked.
 */
fun awakeFor(): Duration = Duration.ofNanos(SystemClock.elapsedRealtimeNanos())

/**
 * Whether a position was put there by another application.
 *
 * A different thing from the false signal the filters are about: this is the
 * device being told where it is by software on it, which anybody can turn on in
 * the developer settings and which no radio can do. It costs one field to
 * check and it is the only kind of pretence the platform will admit to.
 */
fun pretended(location: Location): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    location.isMock
} else {
    @Suppress("DEPRECATION")
    location.isFromMockProvider
}

/**
 * Whatever the provider attached beyond the fields, written as it stands.
 *
 * On these devices the fused provider puts the satellite count and the loudness
 * it worked from in here, which is the one place the platform admits how a
 * fused answer was arrived at — and the answer to whether a fix rests on the
 * satellites at all.
 */
private fun alongside(location: Location): String? {
    val extras = location.extras ?: return null

    return extras
        .keySet()
        .sorted()
        .joinToString("|") { key -> key + "=" + valueOf(extras, key) }
        .takeIf { it.isNotEmpty() }
}

/**
 * One value out of a bundle whose types are not known ahead of time.
 *
 * The typed readers cannot be used: what a provider puts in here is its own
 * business, and the whole point of writing it down is that nobody knew what it
 * would be.
 */
@Suppress("DEPRECATION")
private fun valueOf(extras: Bundle, key: String): String = extras.get(key).toString()
