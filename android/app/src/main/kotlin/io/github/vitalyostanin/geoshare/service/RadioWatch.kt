package io.github.vitalyostanin.geoshare.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssMeasurementsEvent
import android.location.GnssNavigationMessage
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import io.github.vitalyostanin.geoshare.collection.AidingDoubt
import io.github.vitalyostanin.geoshare.collection.Clock
import io.github.vitalyostanin.geoshare.collection.Constellation
import io.github.vitalyostanin.geoshare.collection.Counted
import io.github.vitalyostanin.geoshare.collection.DRIFT_LEAST_APART_MS
import io.github.vitalyostanin.geoshare.collection.DRIFT_MOST_APART_MS
import io.github.vitalyostanin.geoshare.collection.Drift
import io.github.vitalyostanin.geoshare.collection.EPOCHS_READ_TOGETHER
import io.github.vitalyostanin.geoshare.collection.Epochs
import io.github.vitalyostanin.geoshare.collection.Fix
import io.github.vitalyostanin.geoshare.collection.Gain
import io.github.vitalyostanin.geoshare.collection.Interference
import io.github.vitalyostanin.geoshare.collection.Measurement
import io.github.vitalyostanin.geoshare.collection.NavMessage
import io.github.vitalyostanin.geoshare.collection.NavSoFar
import io.github.vitalyostanin.geoshare.collection.SHARE_THE_GPS_BAND
import io.github.vitalyostanin.geoshare.collection.SLANT_MIN_SATELLITES
import io.github.vitalyostanin.geoshare.collection.Satellite
import io.github.vitalyostanin.geoshare.collection.SatelliteClockVerdict
import io.github.vitalyostanin.geoshare.collection.SatelliteClockWatch
import io.github.vitalyostanin.geoshare.collection.SignalApart
import io.github.vitalyostanin.geoshare.collection.SkyDrift
import io.github.vitalyostanin.geoshare.collection.SkySays
import io.github.vitalyostanin.geoshare.collection.SkyShape
import io.github.vitalyostanin.geoshare.collection.Slant
import io.github.vitalyostanin.geoshare.collection.acrossTheBand
import io.github.vitalyostanin.geoshare.collection.counted
import io.github.vitalyostanin.geoshare.collection.driftOf
import io.github.vitalyostanin.geoshare.collection.driftShapeOf
import io.github.vitalyostanin.geoshare.collection.gainPerBand
import io.github.vitalyostanin.geoshare.collection.inOrder
import io.github.vitalyostanin.geoshare.collection.interferenceFrom
import io.github.vitalyostanin.geoshare.collection.leftThePhonesClock
import io.github.vitalyostanin.geoshare.collection.loudnessPerBand
import io.github.vitalyostanin.geoshare.collection.parted
import io.github.vitalyostanin.geoshare.collection.presenceOf
import io.github.vitalyostanin.geoshare.collection.reported
import io.github.vitalyostanin.geoshare.collection.satelliteTimeParted
import io.github.vitalyostanin.geoshare.collection.shapeOf
import io.github.vitalyostanin.geoshare.collection.slantOf
import io.github.vitalyostanin.geoshare.collection.theOtherBandIsSilent
import io.github.vitalyostanin.geoshare.collection.useOf
import io.github.vitalyostanin.geoshare.collection.whatTheBandsSay
import io.github.vitalyostanin.geoshare.collection.word
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.log.SkyLog
import java.time.Duration
import java.time.Instant
import java.util.Locale

private const val TAG = "geoshare.radio"

/**
 * How many epochs are held at once.
 *
 * Ten recent against fifty before them, which is what Google's own logger
 * compares. At one epoch a second that is a minute of history: long enough
 * that the earlier half is what the radio was like before whatever is
 * happening started, short enough that walking indoors does not look like the
 * start of an attack for the next quarter of an hour.
 */
private const val RECENT_EPOCHS = 10
private const val EARLIER_EPOCHS = 50

/** How often the watch admits to having nothing, when it has nothing. */
private val SILENCE_REPORTED_EVERY: Duration = Duration.ofMinutes(5)

/**
 * How often the log says how much the satellites have said.
 *
 * Said on its own schedule rather than beside a fix: the messages arrive
 * whether or not a position comes out of them, and on the phones this was
 * written for the fixes are the rare part.
 */
private val NAVIGATION_REPORTED_EVERY: Duration = Duration.ofMinutes(5)

/**
 * How old the last epoch may be and still be taken as being about a fix.
 *
 * Five seconds. The status arrives about once a second while the receiver is
 * running, so anything older than this is a receiver that has stopped
 * reporting rather than one reporting about this moment — and the rule read
 * off the epoch, which asks what a fix was built out of, would then be
 * answering about some other minute.
 */
private val SKY_STILL_CURRENT: Duration = Duration.ofSeconds(5)

/**
 * What the radio is being subjected to, as the receiver's own measures say.
 *
 * Subscriptions the phone already has the permission for, none of which asks
 * for a position: the satellites it can hear and how loud, the gain its
 * amplifier is running at, and the messages the satellites themselves are
 * broadcasting. The first pair is what separates a jammed receiver from a
 * spoofed one — see [interferenceFrom] for which way each moves — and neither
 * of the two alone says much. The third is not a measure at all but the
 * content of the signal: see
 * [io.github.vitalyostanin.geoshare.collection.NavMessage].
 *
 * Both are kept apart by the system the satellites belong to, which is a third
 * thing to read and the only one that names where the interference is coming
 * from: see [whatTheBandsSay]. The gain is reported per band as well as the
 * loudness, and the two are read the same way.
 *
 * The measurements arrive whether or not a position comes out of them, which
 * is the case this exists for: a phone that has gone silent is a phone with
 * nothing to write down, and this is what can still be written.
 */
class RadioWatch(private val context: Context) {
    // Kept apart by the system the satellites belong to, because the answer
    // this exists for is a disagreement between them: averaged together, a
    // band gone loud and a band holding still come out as a sky that has not
    // moved. See Constellations.kt for which bands those are.
    private val heard = mutableMapOf<Constellation, ArrayDeque<Double>>()
    private val gains = mutableMapOf<Constellation, ArrayDeque<Double>>()

    // The third window, read on its own terms rather than as a pair of
    // averages: what it carries is not a level that has to have moved but a
    // shape the sky either has or has not — see Slant.kt. Ten epochs deep,
    // because that is what a verdict is read from.
    private val shapes = mutableMapOf<Constellation, ArrayDeque<Slant>>()

    // The fourth window: how far the satellites moved apart from one another
    // between two epochs a few seconds apart — see Drift.kt. Read beside the
    // shapes rather than instead of them, because the shape says what one
    // epoch looked like and this says what a run of them did.
    private val drifts = mutableMapOf<Constellation, ArrayDeque<Drift>>()

    // The epoch the next drift is measured against, and when it arrived. Kept
    // whole because the measure is a difference: what is wanted is the same
    // satellites' loudness a few seconds earlier, not an average of it.
    private var driftFrom: List<Satellite> = emptyList()
    private var driftFromAt: Instant? = null

    // Whether the band a transmitter on the ground cannot reach was silent,
    // epoch by epoch. Kept beside the shapes because the two are read
    // together and neither is worth acting on alone — see OneTransmitter.kt.
    private val silences = ArrayDeque<Boolean>()

    // The fifth reading, and the only one that is not about a minute: which
    // constellations the receiver has been hearing for nothing since it was
    // switched on — see AidingDoubt.kt. Its answer goes to the settings
    // screen rather than into a verdict about a fix, because what it asks for
    // is a member pressing a button.
    private val aiding = AidingDoubt()

    // The satellites' own clocks, kept from one epoch to the next: what is
    // read off them is a pair of epochs rather than a single one.
    private val satelliteClock = SatelliteClockWatch()

    // Whether that reading found the satellites' time away from the phone's,
    // epoch by epoch. A window rather than the last answer because the step it
    // catches is an event and the fixes around it are the ones worth leaving
    // out — see satelliteTimeParted.
    private val partings = ArrayDeque<Boolean>()

    // The last signal whose time left the phone's clock, kept whole for the
    // line the log writes when a fix is left out: the window says that
    // something stepped, and this says what and by how much.
    @Volatile
    private var stepped: SignalApart? = null
    private val aidingDoubted = AidingDoubtSetting(context)

    @Volatile
    private var doubted: Set<Constellation> = emptySet()

    // The last epoch of each, kept whole rather than averaged: the averages
    // answer whether anything is going on, and these are what a person looks
    // at afterwards to see what it was. Written to their own file per fix.
    @Volatile
    private var sky: List<Satellite> = emptyList()

    // When that epoch arrived, by the phone's own clock. Kept because the
    // epoch is read as a statement about a fix, and a statement about a fix
    // has to have been made at about the same time as the fix.
    @Volatile
    private var skyAt: Instant = Instant.EPOCH

    @Volatile
    private var running: List<Gain> = emptyList()

    @Volatile
    private var measured: List<Measurement> = emptyList()

    @Volatile
    private var clock: Clock? = null

    // What the satellites have said so far, kept rather than written straight
    // out: a page repeated every thirty seconds would fill the sky file and
    // push out everything else in it. See NavSoFar for what counts as new.
    private val navigation = NavSoFar()

    private var saidNothingAt: Instant = Instant.EPOCH
    private var saidNavigationAt: Instant = Instant.EPOCH
    private var status: GnssStatus.Callback? = null
    private var measurements: GnssMeasurementsEvent.Callback? = null
    private var navMessages: GnssNavigationMessage.Callback? = null

    /**
     * What the last minute of measurements adds up to.
     *
     * The bands are asked first. They answer less often — a phone hearing one
     * system has nothing to compare — but when they do answer they name the
     * band whatever is going on is going on in, and they answer without the
     * gain, which some phones never report at all.
     */
    fun verdict(): Interference {
        val bands = windows(heard)

        return whatTheBandsSay(sharedBand(bands), bands[Constellation.Glonass] ?: NOTHING)
            ?: interferenceFrom(
                gain = acrossTheBand(windows(gains).values),
                noise = acrossTheBand(bands.values),
            )
    }

    /**
     * The same, in the words of the log, or nothing when there is nothing to
     * say: a line per fix saying "unknown" would bury the ones that matter.
     *
     * Silence is reported too, but rarely. Not every receiver reports its
     * gain at all, and a verdict that never arrives is indistinguishable from
     * a radio nobody is interfering with — which is the one thing this must
     * not be mistaken for. So once in a while it says how much it has.
     */
    fun saying(): String? {
        val verdict = verdict()

        if (verdict == Interference.Unknown) {
            return whatIsMissing()
        }

        val loudness = windows(heard)
        val gain = windows(gains)

        return String.format(
            Locale.ROOT,
            "the radio says %s (gain: the GPS band %s, GLONASS %s; " +
                "loudness: the GPS band %s, GLONASS %s)",
            verdict.toString().lowercase(),
            moved(sharedBand(gain)),
            moved(gain[Constellation.Glonass] ?: NOTHING),
            moved(sharedBand(loudness)),
            moved(loudness[Constellation.Glonass] ?: NOTHING),
        )
    }

    /**
     * The last signal whose time stepped away from the phone's clock, or null
     * where none has since the receiver started.
     *
     * Kept for the log rather than for a verdict: the verdict is [skySays],
     * which reads the window, and this is what a person reads afterwards to
     * see which signal it was.
     */
    fun clockThatStepped(): SignalApart? = stepped

    /**
     * The two measures the jump filter reads, as the last ten epochs leave
     * them.
     *
     * Handed over as one value rather than as the windows themselves: the
     * filter is a pure function over an answer and the windows belong to the
     * subscriptions. Both are silent until the window is full, which is what
     * keeps a phone that has just started collecting from calling its first
     * minute an attack.
     */
    fun skySays(): SkySays = SkySays(
        shape = shapeOf(shapes[Constellation.Gps]?.toList().orEmpty()),
        drift = driftShapeOf(drifts[Constellation.Gps]?.toList().orEmpty()),
        otherBandSilent =
        silences.size >= EPOCHS_READ_TOGETHER && silences.all { it },
        satelliteTimeApart = satelliteTimeParted(partings.toList()),
    )

    /**
     * How many satellites each system has in the sky and how many of them the
     * receiver's own position rests on.
     *
     * The second number is the one worth having. A receiver handed a false sky
     * builds its answer out of the satellites it was handed, so a system with
     * eight in view and none of them in the fix is a system that is being
     * talked over — and a fix resting on one system alone is a fix with
     * nothing to check itself against.
     */
    fun satellitesSaying(): String? {
        val counts = counted(sky)

        if (counts.isEmpty()) {
            return null
        }

        val said =
            inOrder(counts.keys).joinToString(", ") { system ->
                val count = counts[system] ?: Counted(heard = 0, usedInFix = 0)

                "${system.word} ${count.usedInFix} of ${count.heard}"
            }

        return "the fix rests on $said"
    }

    /**
     * How the loudness of the last ten epochs stands against the height of the
     * satellites it came from, system by system.
     *
     * The line every other measure here needs and none of them carry: the gain
     * and the loudness say a band has moved, and moving is what a bus going
     * past does as well. This says whether the sky the satellites describe
     * could be a sky at all — see [io.github.vitalyostanin.geoshare.collection.SkyShape].
     *
     * The numbers go out beside the verdict rather than instead of it, because
     * the limits behind the verdict were read off two phones' logs and will
     * have to be corrected against more.
     */
    fun skySaying(): String? {
        val read =
            inOrder(shapes.keys).mapNotNull { system ->
                val epochs = shapes[system]?.toList().orEmpty()

                if (epochs.size < EPOCHS_READ_TOGETHER) {
                    return@mapNotNull null
                }

                val last = epochs.last()

                String.format(
                    Locale.ROOT,
                    "%s %s (spread %.1f dB, slope %s, %d satellites)",
                    system.word,
                    shapeOf(epochs).toString().lowercase(),
                    last.spreadDb,
                    last.slopeDb?.let { String.format(Locale.ROOT, "%+.1f dB", it) }
                        ?: "not measurable",
                    last.satellites,
                )
            }

        if (read.isEmpty()) {
            return null
        }

        return "the sky reads ${read.joinToString(", ")}"
    }

    /**
     * How far the satellites of each system moved apart from one another over
     * the last ten readings.
     *
     * Beside [skySaying] rather than inside it because the two answer
     * different questions about the same band: that one is the shape of one
     * epoch, this one is what a run of them did. Both go out with their
     * numbers, because the limits behind the verdicts were read off three
     * phones' logs and will have to be corrected against more.
     */
    fun driftSaying(): String? {
        val read =
            inOrder(drifts.keys).mapNotNull { system ->
                val epochs = drifts[system]?.toList().orEmpty()

                if (epochs.size < EPOCHS_READ_TOGETHER) {
                    return@mapNotNull null
                }

                val last = epochs.last()

                String.format(
                    Locale.ROOT,
                    "%s %s (apart %.1f dB, shift %+.1f dB, %d satellites)",
                    system.word,
                    when (driftShapeOf(epochs)) {
                        SkyDrift.Together -> "as one block"
                        SkyDrift.Apart -> "each on its own"
                        SkyDrift.Unclear -> "neither way"
                    },
                    last.apartDb,
                    last.shiftDb,
                    last.satellites,
                )
            }

        if (read.isEmpty()) {
            return null
        }

        return "the satellites move ${read.joinToString(", ")}"
    }

    /**
     * How much the satellites have actually said, and which of them.
     *
     * Said rarely, and said even when the answer is none. Whether a chipset
     * hands these over at all is what the subscription exists to find out, and
     * a file with no such rows in it cannot tell "nothing arrived" from
     * "nobody asked": this is the line that tells them apart.
     */
    fun navigationSaying(): String? {
        val now = Instant.now()

        if (Duration.between(saidNavigationAt, now) < NAVIGATION_REPORTED_EVERY) {
            return null
        }

        saidNavigationAt = now

        return navigation.saying()
    }

    /**
     * What the last epoch says the fix rests on, system by system.
     *
     * Empty rather than stale: the counts are read as a statement about the
     * fix beside them — see
     * [io.github.vitalyostanin.geoshare.collection.restsOnOneBand] — and an
     * epoch from a minute ago is a statement about a minute ago. Empty as well
     * where the receiver has said nothing at all, which is the same answer for
     * the same reason.
     */
    fun restingOn(now: Instant = Instant.now()): Map<Constellation, Counted> {
        if (Duration.between(skyAt, now).abs() > SKY_STILL_CURRENT) {
            return emptyMap()
        }

        return counted(sky)
    }

    /**
     * Whether the satellites' own time still runs along with the phone's.
     *
     * Read on every epoch rather than where the sky is written down, because
     * what it reads is a pair of epochs and the sky file is written once per
     * fix. Only the signals that parted are written: the ones that agree are
     * the whole sky, every epoch, and saying so would fill the file.
     */
    private fun noteSatelliteClocks() {
        val said = clock?.let { satelliteClock.read(it, measured) }.orEmpty()
        val apart = parted(said)

        noteParting(leftThePhonesClock(said))

        said.lastOrNull { it.verdict == SatelliteClockVerdict.PhoneApart }?.let { stepped = it }

        if (apart.isEmpty()) {
            return
        }

        SkyLog.note(Instant.now(), apart)

        AppLog.w(
            TAG,
            "the time of ${apart.size} signal(s) does not run with the phone's: " +
                apart.joinToString(", ") {
                    "${it.signal.constellation.word} ${it.signal.id} ${it.verdict}"
                },
        )
    }

    /**
     * The epoch itself, put beside the position that came out of it.
     *
     * Called once per fix rather than once per epoch: see [SkyLog] for why.
     */
    fun noteSky(at: Instant, fix: Fix) = SkyLog.note(
        at = at,
        fix = fix,
        clock = clock,
        gains = running,
        sky = sky,
        measurements = measured,
    )

    /**
     * The same epoch, put down while no position is coming out of it.
     *
     * A receiver hearing twenty satellites and answering nothing is the case
     * the whole file exists for, and it is exactly the case that used to leave
     * no rows at all.
     */
    fun noteSkyWithoutFix(at: Instant, mode: String, silence: Duration) = SkyLog.note(
        at = at,
        mode = mode,
        silence = silence,
        clock = clock,
        gains = running,
        sky = sky,
        measurements = measured,
    )

    /**
     * Subscribes to all three, on the handler rather than on an executor.
     *
     * The executor overloads arrive with Android 11, and the phone this was
     * written for — the one that goes silent for hours at a time — runs
     * Android 10. The handler overloads are older than anything the
     * application supports, and the callbacks do nothing but average a list.
     */
    @SuppressLint("MissingPermission")
    fun start() {
        val manager = context.getSystemService(LocationManager::class.java) ?: return
        val handler = Handler(Looper.getMainLooper())

        status =
            object : GnssStatus.Callback() {
                override fun onSatelliteStatusChanged(status: GnssStatus) {
                    val epoch = skyOf(status)

                    sky = epoch
                    skyAt = Instant.now()

                    for ((constellation, loudness) in loudnessPerBand(epoch)) {
                        note(heard.getOrPut(constellation) { ArrayDeque() }, loudness)
                    }

                    noteSilence(theOtherBandIsSilent(presenceOf(epoch)))
                    noteDrift(epoch)
                    noteAiding(epoch)

                    for ((constellation, slant) in slantOf(epoch)) {
                        if (slant.satellites < SLANT_MIN_SATELLITES) {
                            continue
                        }

                        noteShape(shapes.getOrPut(constellation) { ArrayDeque() }, slant)
                    }
                }
            }.also { manager.registerGnssStatusCallback(it, handler) }

        measurements =
            object : GnssMeasurementsEvent.Callback() {
                override fun onGnssMeasurementsReceived(event: GnssMeasurementsEvent) {
                    clock = clockOf(event)
                    val tracking = fullTrackingOf(event)

                    measured = event.measurements.map { measurementOf(it, tracking) }

                    noteSatelliteClocks()

                    val epoch = reported(gainsOf(event))

                    if (epoch.isEmpty()) {
                        return
                    }

                    running = epoch

                    for ((constellation, level) in gainPerBand(epoch)) {
                        note(gains.getOrPut(constellation) { ArrayDeque() }, level)
                    }
                }
            }.also { manager.registerGnssMeasurementsCallback(it, handler) }

        navMessages =
            object : GnssNavigationMessage.Callback() {
                override fun onGnssNavigationMessageReceived(message: GnssNavigationMessage) {
                    val said = navMessageOf(message)

                    if (navigation.worthWriting(said)) {
                        SkyLog.note(said)
                    }
                }
            }.also { manager.registerGnssNavigationMessageCallback(it, handler) }

        AppLog.i(TAG, "watching the radio")
        AppLog.i(TAG, whatTheChipsetSays(manager))
    }

    fun stop() {
        val manager = context.getSystemService(LocationManager::class.java) ?: return

        status?.let { manager.unregisterGnssStatusCallback(it) }
        measurements?.let { manager.unregisterGnssMeasurementsCallback(it) }
        navMessages?.let { manager.unregisterGnssNavigationMessageCallback(it) }

        status = null
        measurements = null
        navMessages = null
    }

    /**
     * The averages the verdict is read from.
     *
     * Nothing is reported until both halves are full: a comparison against
     * three epochs of history would call every phone leaving a building an
     * attack.
     */
    private fun window(of: ArrayDeque<Double>): Epochs {
        if (of.size < RECENT_EPOCHS + EARLIER_EPOCHS) {
            return Epochs(recent = null, earlier = null)
        }

        val kept = of.toList()

        return Epochs(
            recent = kept.takeLast(RECENT_EPOCHS).average(),
            earlier = kept.dropLast(RECENT_EPOCHS).average(),
        )
    }

    private fun whatIsMissing(): String? {
        val now = Instant.now()

        if (Duration.between(saidNothingAt, now) < SILENCE_REPORTED_EVERY) {
            return null
        }

        saidNothingAt = now

        val needed = RECENT_EPOCHS + EARLIER_EPOCHS
        val gain = SHARE_THE_GPS_BAND.maxOf { gains[it]?.size ?: 0 }
        val shared = SHARE_THE_GPS_BAND.maxOf { heard[it]?.size ?: 0 }
        val glonass = heard[Constellation.Glonass]?.size ?: 0

        return "the radio has nothing to go on: $gain epochs of gain, " +
            "$shared of the GPS band, $glonass of GLONASS, $needed needed"
    }

    /** Every system's history, each read as its own pair of averages. */
    private fun windows(of: Map<Constellation, ArrayDeque<Double>>): Map<Constellation, Epochs> =
        of.mapValues { window(it.value) }

    /** The three systems that transmit where GPS does, read as one band. */
    private fun sharedBand(bands: Map<Constellation, Epochs>): Epochs =
        acrossTheBand(bands.filterKeys { it in SHARE_THE_GPS_BAND }.values)

    private fun noteParting(value: Boolean) {
        partings.addLast(value)

        while (partings.size > EPOCHS_READ_TOGETHER) {
            partings.removeFirst()
        }
    }

    private fun noteSilence(value: Boolean) {
        silences.addLast(value)

        while (silences.size > EPOCHS_READ_TOGETHER) {
            silences.removeFirst()
        }
    }

    /**
     * What this epoch says about a constellation being heard for nothing.
     *
     * The answer is kept here and written out only when it changes: the epochs
     * arrive about once a second, and the settings file is not a place to put
     * a second's worth of anything. It is written out whichever way it
     * changed, so that a constellation that has come good clears the line on
     * the screen as well as raising it.
     */
    private fun noteAiding(epoch: List<Satellite>) {
        val said = aiding.read(useOf(epoch), awakeFor())

        if (said == doubted) {
            return
        }

        doubted = said
        aidingDoubted.doubted = said

        AppLog.i(
            TAG,
            if (said.isEmpty()) {
                "every system heard is being made use of"
            } else {
                "heard for nothing: ${inOrder(said).joinToString(", ") { it.word }}"
            },
        )
    }

    private fun <T> noteShape(into: ArrayDeque<T>, value: T) {
        into.addLast(value)

        while (into.size > EPOCHS_READ_TOGETHER) {
            into.removeFirst()
        }
    }

    /**
     * One epoch against the one kept for it, where the two stand far enough
     * apart in time to say anything.
     *
     * The kept epoch is replaced by this one whenever a reading is taken off
     * the pair, and also whenever the gap has grown past what the measure is
     * for: a phone that stopped collecting and started again has an old epoch
     * in hand, and the difference against it would be about the hour between
     * them rather than about the sky.
     */
    private fun noteDrift(epoch: List<Satellite>) {
        val at = Instant.now()
        val since = driftFromAt
        val apart = since?.let { Duration.between(it, at).toMillis() }

        if (apart == null || apart > DRIFT_MOST_APART_MS) {
            driftFrom = epoch
            driftFromAt = at

            return
        }

        if (apart < DRIFT_LEAST_APART_MS) {
            return
        }

        for ((constellation, drift) in driftOf(driftFrom, epoch)) {
            noteShape(drifts.getOrPut(constellation) { ArrayDeque() }, drift)
        }

        driftFrom = epoch
        driftFromAt = at
    }

    private fun note(into: ArrayDeque<Double>, value: Double) {
        into.addLast(value)

        while (into.size > RECENT_EPOCHS + EARLIER_EPOCHS) {
            into.removeFirst()
        }
    }
}

/** A band with no history yet, which is what having heard nothing looks like. */
private val NOTHING = Epochs(recent = null, earlier = null)
