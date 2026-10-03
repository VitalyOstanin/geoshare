package io.github.vitalyostanin.geoshare.service

import android.location.GnssMeasurementsEvent
import android.location.GnssNavigationMessage
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Build
import androidx.annotation.RequiresApi
import io.github.vitalyostanin.geoshare.collection.Clock
import io.github.vitalyostanin.geoshare.collection.Constellation
import io.github.vitalyostanin.geoshare.collection.Epochs
import io.github.vitalyostanin.geoshare.collection.Gain
import io.github.vitalyostanin.geoshare.collection.Measurement
import io.github.vitalyostanin.geoshare.collection.NavMessage
import io.github.vitalyostanin.geoshare.collection.Satellite
import io.github.vitalyostanin.geoshare.collection.hexOf
import io.github.vitalyostanin.geoshare.collection.navConstellation
import io.github.vitalyostanin.geoshare.collection.reported
import io.github.vitalyostanin.geoshare.collection.word
import java.util.Locale

/**
 * The epoch as the receiver reported it, every satellite it named.
 *
 * The ones it cannot hear are kept rather than dropped: a receiver lists the
 * satellites it expects as well as the ones it has, and how many it expected
 * and did not find is itself worth knowing. What drops them is the arithmetic
 * downstream, which asks for loudness above zero.
 */
internal fun skyOf(status: GnssStatus): List<Satellite> =
    (0 until status.satelliteCount).map { at ->
        Satellite(
            constellation = constellationOf(status.getConstellationType(at)),
            id = status.getSvid(at),
            loudness = status.getCn0DbHz(at).toDouble(),
            basebandLoudness = basebandOf(status, at),
            elevationDegrees = status.getElevationDegrees(at).toDouble(),
            azimuthDegrees = status.getAzimuthDegrees(at).toDouble(),
            carrierHz =
            if (status.hasCarrierFrequencyHz(at)) {
                status.getCarrierFrequencyHz(at).toDouble()
            } else {
                null
            },
            usedInFix = status.usedInFix(at),
            hasEphemeris = status.hasEphemerisData(at),
            hasAlmanac = status.hasAlmanacData(at),
        )
    }

/**
 * The loudness before the receiver's own amplifier, where it is reported.
 *
 * Android 11 and later, and not on every receiver even there. It is the half
 * of the pair that says whether a signal is genuinely louder or the receiver
 * has merely turned itself up, which is the distinction the whole gain
 * business is about.
 */
private fun basebandOf(status: GnssStatus, at: Int): Double? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || !status.hasBasebandCn0DbHz(at)) {
        return null
    }

    return status.getBasebandCn0DbHz(at).toDouble()
}

/** The receiver's number for a system, in the words the rest of this uses. */
private fun constellationOf(type: Int): Constellation = when (type) {
    GnssStatus.CONSTELLATION_GPS -> Constellation.Gps
    GnssStatus.CONSTELLATION_GLONASS -> Constellation.Glonass
    GnssStatus.CONSTELLATION_GALILEO -> Constellation.Galileo
    GnssStatus.CONSTELLATION_BEIDOU -> Constellation.Beidou
    GnssStatus.CONSTELLATION_QZSS -> Constellation.Qzss
    GnssStatus.CONSTELLATION_SBAS -> Constellation.Sbas
    GnssStatus.CONSTELLATION_IRNSS -> Constellation.Irnss
    else -> Constellation.Other
}

/** A move in decibels, or a word for a band the phone has nothing on. */
internal fun moved(epochs: Epochs): String =
    epochs.moved?.let { String.format(Locale.ROOT, "%+.1f dB", it) } ?: "not heard"

/**
 * The gain the receiver is running at, one entry per band it reports.
 *
 * Reported per constellation and frequency, so a phone hearing several gives
 * several numbers — and gives one for every band it knows of rather than every
 * band it is listening to, the idle ones carrying a number no receiver could
 * measure. Averaging the lot was the defect this replaces: see [reported],
 * which is what throws the idle ones out, and which the caller applies.
 *
 * Read two ways because the way it is read changed: from Android 13 the event
 * carries the gains apart from the measurements, and before that each
 * measurement carried its own, if it carried one at all.
 */
@Suppress("DEPRECATION")
internal fun gainsOf(event: GnssMeasurementsEvent): List<Gain> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        event.gnssAutomaticGainControls.map {
            Gain(
                constellation = constellationOf(it.constellationType),
                carrierHz = it.carrierFrequencyHz.toDouble(),
                levelDb = it.levelDb,
            )
        }
    } else {
        event.measurements
            .filter { it.hasAutomaticGainControlLevelDb() }
            .map {
                Gain(
                    constellation = constellationOf(it.constellationType),
                    carrierHz =
                    if (it.hasCarrierFrequencyHz()) {
                        it.carrierFrequencyHz.toDouble()
                    } else {
                        null
                    },
                    levelDb = it.automaticGainControlLevelDb,
                )
            }
    }

/**
 * The receiver's own clock as it came with the epoch.
 *
 * Most of it is optional, and a receiver that reports none of it still reports
 * the count of discontinuities — which is the field worth having on its own: a
 * receiver whose time stopped running forward raises it, and a false sky
 * carrying a false time is one of the few things that can make it happen.
 */
internal fun clockOf(event: GnssMeasurementsEvent): Clock {
    val clock = event.clock

    return Clock(
        timeNanos = clock.timeNanos,
        leapSecond = clock.leapSecond.takeIf { clock.hasLeapSecond() },
        timeUncertaintyNanos = clock.timeUncertaintyNanos.takeIf {
            clock.hasTimeUncertaintyNanos()
        },
        fullBiasNanos = clock.fullBiasNanos.takeIf { clock.hasFullBiasNanos() },
        biasNanos = clock.biasNanos.takeIf { clock.hasBiasNanos() },
        biasUncertaintyNanos =
        clock.biasUncertaintyNanos.takeIf { clock.hasBiasUncertaintyNanos() },
        driftNanosPerSecond =
        clock.driftNanosPerSecond.takeIf { clock.hasDriftNanosPerSecond() },
        driftUncertaintyNanosPerSecond =
        clock.driftUncertaintyNanosPerSecond.takeIf {
            clock.hasDriftUncertaintyNanosPerSecond()
        },
        discontinuities = clock.hardwareClockDiscontinuityCount,
        sinceBootNanos =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            clock.elapsedRealtimeNanos.takeIf { clock.hasElapsedRealtimeNanos() }
        } else {
            null
        },
        sinceBootUncertaintyNanos =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            clock.elapsedRealtimeUncertaintyNanos.takeIf {
                clock.hasElapsedRealtimeUncertaintyNanos()
            }
        } else {
            null
        },
    )
}

/** One raw measurement, in the terms the rest of this uses. */
@Suppress("DEPRECATION")
internal fun measurementOf(
    measured: android.location.GnssMeasurement,
    fullTracking: Boolean?,
    delays: Delays = delaysOf(measured),
): Measurement = Measurement(
    constellation = constellationOf(measured.constellationType),
    id = measured.svid,
    timeOffsetNanos = measured.timeOffsetNanos,
    state = measured.state,
    receivedSvTimeNanos = measured.receivedSvTimeNanos,
    receivedSvTimeUncertaintyNanos = measured.receivedSvTimeUncertaintyNanos,
    loudness = measured.cn0DbHz,
    basebandLoudness =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && measured.hasBasebandCn0DbHz()) {
        measured.basebandCn0DbHz
    } else {
        null
    },
    pseudorangeRateMps = measured.pseudorangeRateMetersPerSecond,
    pseudorangeRateUncertaintyMps = measured.pseudorangeRateUncertaintyMetersPerSecond,
    deltaRangeState = measured.accumulatedDeltaRangeState,
    deltaRangeMetres = measured.accumulatedDeltaRangeMeters,
    deltaRangeUncertaintyMetres = measured.accumulatedDeltaRangeUncertaintyMeters,
    carrierHz = measured.carrierFrequencyHz.toDouble().takeIf { measured.hasCarrierFrequencyHz() },
    multipath = measured.multipathIndicator,
    gainDb =
    measured.automaticGainControlLevelDb.takeIf {
        measured.hasAutomaticGainControlLevelDb()
    },
    snrDb = measured.snrInDb.takeIf { measured.hasSnrInDb() },
    codeType =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        measured.hasCodeType()
    ) {
        measured.codeType
    } else {
        null
    },
    fullInterSignalBiasNanos = delays.full,
    fullInterSignalBiasUncertaintyNanos = delays.fullUncertainty,
    satelliteInterSignalBiasNanos = delays.satellite,
    satelliteInterSignalBiasUncertaintyNanos = delays.satelliteUncertainty,
    fullTracking = fullTracking,
)

/**
 * Whether the receiver was asked for everything it can hear, where the
 * platform says.
 *
 * Android 14 and later. Below it the question has no answer at all rather than
 * a negative one: a receiver may well have been tracking everything, and the
 * epoch simply does not carry the fact.
 */
internal fun fullTrackingOf(event: GnssMeasurementsEvent): Boolean? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        event.isFullTracking
    } else {
        null
    }

/**
 * The delays the receiver applies between one signal and another.
 *
 * Four numbers that arrive together or not at all: Android 11 brought the lot,
 * and below it the epoch does not carry them. Kept as one value so that the
 * version is asked about once rather than four times.
 */
internal data class Delays(
    val full: Double? = null,
    val fullUncertainty: Double? = null,
    val satellite: Double? = null,
    val satelliteUncertainty: Double? = null,
)

private fun delaysOf(measured: android.location.GnssMeasurement): Delays =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        reportedDelays(measured)
    } else {
        Delays()
    }

/** The same, on a version that has them, each guarded by its own `has`. */
@RequiresApi(Build.VERSION_CODES.R)
private fun reportedDelays(measured: android.location.GnssMeasurement): Delays = Delays(
    full = measured.fullInterSignalBiasNanos.takeIf {
        measured.hasFullInterSignalBiasNanos()
    },
    fullUncertainty = measured.fullInterSignalBiasUncertaintyNanos.takeIf {
        measured.hasFullInterSignalBiasUncertaintyNanos()
    },
    satellite = measured.satelliteInterSignalBiasNanos.takeIf {
        measured.hasSatelliteInterSignalBiasNanos()
    },
    satelliteUncertainty = measured.satelliteInterSignalBiasUncertaintyNanos.takeIf {
        measured.hasSatelliteInterSignalBiasUncertaintyNanos()
    },
)

/** One navigation message, in the terms the rest of this uses. */
internal fun navMessageOf(message: GnssNavigationMessage): NavMessage = NavMessage(
    constellation = navConstellation(message.type),
    id = message.svid,
    type = message.type,
    status = message.status,
    messageId = message.messageId,
    submessageId = message.submessageId,
    data = hexOf(message.data),
)

/**
 * What the chipset admits it can hand over, where the platform asks it.
 *
 * Android 12 and later. Below that the question has no answer at all, which is
 * not the same as a negative one, and the subscriptions are made either way:
 * what arrives is the answer that counts. Worth writing at every start because
 * it is the one line that says whether a phone with nothing in its navigation
 * rows was ever going to have anything there.
 */
internal fun whatTheChipsetSays(manager: LocationManager): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return "this version of Android does not ask the chipset what it can hand over"
    }

    val can = manager.gnssCapabilities

    return "the chipset ${saysItCan(can.hasMeasurements())} hand over raw measurements, and " +
        "${saysItCan(can.hasNavigationMessages())} hand over navigation messages"
}

private fun saysItCan(can: Boolean): String = if (can) "says it can" else "says it cannot"
