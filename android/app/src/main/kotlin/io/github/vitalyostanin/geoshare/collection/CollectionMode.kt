package io.github.vitalyostanin.geoshare.collection

import java.time.Duration

/**
 * How hard the device is working to know where it is.
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

/** What a still device is allowed to keep quiet about, in metres and in time. */
private val ORDINARY_DISTANCE = 75.0
private val ORDINARY_SILENCE: Duration = Duration.ofMinutes(10)

/**
 * How hard the device is working, and how readily it says what it found.
 *
 * The two halves belong together. Asking the chip often and then writing
 * seldom spends the battery of the device being watched without giving the
 * watcher anything, which is what the fast mode used to do: a fix every
 * fifteen seconds, and a marker that moved once a minute at best. And asking
 * seldom with a long batch window makes a working device look like a stopped
 * one, because the position it last wrote goes past [STALE_AFTER] before the
 * next one is even collected.
 *
 * So every mode names both, and [asksAgainWithin] is what ties them to the
 * grey: a device in any mode that asks for anything has to have written again
 * before the half hour is up.
 */
enum class CollectionMode(
    val accuracy: Accuracy,
    private val baseInterval: Duration,
    /** Batch window: fixes pile up in the chip and arrive together. */
    private val baseMaxUpdateDelay: Duration,
    /** Below this a fix is jitter rather than movement, in this mode. */
    val writeDistance: Double,
    /** However still the device is, in this mode it says so this often. */
    private val baseWriteSilence: Duration,
) {
    /**
     * Inside the geofence around the last position: nothing is requested.
     *
     * Nothing is written either, which is why this mode is the one that cannot
     * be held for longer than the grey allows; the geofence is not in place
     * yet, and when it is, leaving this mode on a timer before the half hour
     * is up is part of it.
     */
    Idle(Accuracy.None, Duration.ZERO, Duration.ZERO, ORDINARY_DISTANCE, ORDINARY_SILENCE),

    Standby(
        Accuracy.Balanced,
        Duration.ofMinutes(15),
        Duration.ofMinutes(10),
        ORDINARY_DISTANCE,
        ORDINARY_SILENCE,
    ),

    Moving(
        Accuracy.Balanced,
        Duration.ofMinutes(5),
        Duration.ofMinutes(5),
        ORDINARY_DISTANCE,
        ORDINARY_SILENCE,
    ),

    /**
     * Somebody has the map open; the only mode that turns the receiver on.
     *
     * Movement is written on the same terms it is collected on: a watcher
     * following somebody on foot should see the marker move, and fifty metres
     * is about half a minute of walking. The step is no finer because every
     * position written under watch is delivered to each watcher and costs the
     * group several reads, so a shorter step buys reads rather than anything
     * the watcher needs. Stillness is not written on those terms: one open map
     * puts every device of the group into this mode, and a group of twenty
     * devices on tables saying the same place twice a minute spends the free
     * quota of writes in a working day. Three minutes still says it within
     * one claim of [WATCH_AHEAD], and the card's "in touch" comes from the
     * device's summary, which is written the moment this mode begins.
     */
    Viewing(Accuracy.High, Duration.ofSeconds(30), Duration.ZERO, 50.0, Duration.ofMinutes(3)),

    /**
     * The small hours, when a device on a bedside table is not news.
     *
     * Twenty minutes rather than half an hour: the position has to be written
     * again before the half hour of [STALE_AFTER] runs out, or a sleeping
     * household would wake up grey on each other's maps.
     */
    Night(
        Accuracy.LowPower,
        Duration.ofMinutes(20),
        Duration.ofMinutes(5),
        ORDINARY_DISTANCE,
        ORDINARY_SILENCE,
    ),
    ;

    /**
     * How often the chip is asked, under the ceiling [PaceCeiling] holds.
     *
     * The three values a ceiling touches are computed from the mode's own
     * (`base*`) values on every read, rather than the modes being copied for a
     * ceiling: the policy has a dozen readers (the request, the write filter,
     * the second opinion, the rescue deadline), and each of them asks the mode
     * what it wants. Keeping the names and moving the ceiling behind them
     * leaves every one of them as it was, and with no ceiling the values are
     * the base ones untouched.
     */
    val interval: Duration
        get() = PaceCeiling.current.let { ceiling ->
            if (ceiling == null) baseInterval else minOf(baseInterval, ceiling)
        }

    /**
     * The batch window, shortened so that a round of asking plus the window
     * still fits in the ceiling: a position that arrives late is as late as one
     * that was asked for late.
     */
    val maxUpdateDelay: Duration
        get() = PaceCeiling.current.let { ceiling ->
            if (ceiling == null) {
                baseMaxUpdateDelay
            } else {
                minOf(baseMaxUpdateDelay, maxOf(Duration.ZERO, ceiling - interval))
            }
        }

    /** How long a still device keeps quiet, never longer than the ceiling. */
    val writeSilence: Duration
        get() = PaceCeiling.current.let { ceiling ->
            if (ceiling == null) baseWriteSilence else minOf(baseWriteSilence, ceiling)
        }

    /**
     * The longest a device in this mode can go between two positions.
     *
     * A round of asking plus the window the answers wait in. A mode that asks
     * for nothing has no such bound, and answers with nothing.
     */
    fun asksAgainWithin(): Duration? =
        if (accuracy == Accuracy.None) null else interval + maxUpdateDelay
}

/**
 * The longest a device may go between two positions while nobody watches, when
 * something outside the application has set one; null when nothing has.
 *
 * It is a ceiling laid over the policy, never a replacement of it: it only
 * ever makes a mode faster, so Viewing, which is faster than any ceiling that
 * can be set, keeps its thirty seconds, and a mode that asks for nothing is
 * asked about by [chooseMode] instead. Held in one place so that the readers of
 * [CollectionMode] need not be told about it one by one. The service sets it
 * from the managed configuration and nothing else writes it.
 */
object PaceCeiling {
    @Volatile
    var current: Duration? = null
}
